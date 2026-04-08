package com.run.peakflow.data.network

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.models.CommunityGroup
import com.run.peakflow.data.models.CommunityMembership
import com.run.peakflow.data.models.Event
import com.run.peakflow.data.models.EventCategory
import com.run.peakflow.data.models.InviteCode
import com.run.peakflow.data.models.JoinRequest
import com.run.peakflow.data.models.MembershipRole
import com.run.peakflow.data.models.Post
import com.run.peakflow.data.models.PostComment
import com.run.peakflow.data.models.RequestStatus
import com.run.peakflow.data.models.Rsvp
import com.run.peakflow.data.models.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.PostgresChangeFilter
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.storage.storage
import io.ktor.client.call.body
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Real Supabase implementation of [ApiService].
 * Replaces MockApiService for production use.
 */
@OptIn(kotlin.time.ExperimentalTime::class)
class SupabaseApiService(
    private val client: SupabaseClient
) : ApiService {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Mutex to prevent concurrent token refresh races (which cause token revocation). */
    private val authMutex = Mutex()

    /**
     * Ensures the Supabase session is valid before executing [block].
     *
     * 1. Waits for session initialization from storage.
     * 2. Proactively refreshes the token if it's expired or expiring within 30s.
     * 3. On auth errors (PostgrestRestException with "JWT expired" OR
     *    UnauthorizedRestException from Edge Functions), does one Mutex-guarded
     *    refresh and retries the block.
     */
    private suspend fun <T> ensureAuthenticated(block: suspend () -> T): T {
        // 1. Wait for initialization if necessary with a strict timeout
        if (client.auth.sessionStatus.value is SessionStatus.Initializing) {
            println("SupabaseApiService: Waiting for session initialization...")
            withTimeoutOrNull(5000) {
                client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
            }
            // After initialization, if we are NotAuthenticated, give it one more short wait
            // to handle the "flicker" where it briefly reports NotAuthenticated before restoration.
            if (client.auth.sessionStatus.value is SessionStatus.NotAuthenticated) {
                println("SupabaseApiService: Post-init flicker check (waiting 1s for potential session restoration)...")
                delay(1000) 
            }
            println("SupabaseApiService: Session initialization finished or timed out (final status: ${client.auth.sessionStatus.value})")
        }

        // 2. Check if we have a session
        if (client.auth.currentSessionOrNull() == null) {
            // Final attempt: wait for session loaded if we just finished initializing
            val settledUserId = waitForSessionLoaded()
            if (settledUserId == null) {
                throw AuthenticationException("No active session")
            }
        }

        // 3. Proactively refresh if token is expired or expiring within 30 seconds
        try {
            val session = client.auth.currentSessionOrNull()
            if (session != null) {
                val now = kotlin.time.Clock.System.now()
                if (session.expiresAt < now + 30.seconds) {
                    authMutex.withLock {
                        // Re-check after acquiring lock — another coroutine may have refreshed
                        val freshSession = client.auth.currentSessionOrNull()
                        if (freshSession == null || freshSession.expiresAt < now + 30.seconds) {
                            client.auth.refreshCurrentSession()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Proactive token refresh failed: ${e.message}")
            // Continue — the block() call may still succeed with a marginally valid token,
            // and the reactive catch below will handle a definitive failure.
        }

        return try {
            block()
        } catch (e: Exception) {
            // 4. Reactively catch auth failures from BOTH Postgrest and Edge Functions
            val errorMessage = e.message?.lowercase() ?: ""
            val isJwtError = errorMessage.contains("jwt expired") || 
                            errorMessage.contains("invalid jwt") || 
                            errorMessage.contains("token expired") ||
                            errorMessage.contains("unauthorized")
            
            val isUnauthorized = e is io.github.jan.supabase.exceptions.UnauthorizedRestException ||
                                e is io.github.jan.supabase.postgrest.exception.PostgrestRestException && e.statusCode == 401

            if (isJwtError || isUnauthorized) {
                println("SupabaseApiService: Auth error detected ($e). Attempting token refresh...")
                try {
                    authMutex.withLock {
                        val beforeRefresh = client.auth.currentSessionOrNull()?.expiresAt
                        client.auth.refreshCurrentSession()
                        val afterRefresh = client.auth.currentSessionOrNull()?.expiresAt
                        println("SupabaseApiService: Token refresh complete. Before: $beforeRefresh, After: $afterRefresh")
                    }
                    block() // Retry with refreshed token
                } catch (refreshError: Exception) {
                    println("SupabaseApiService: Token refresh failed: ${refreshError.message}")
                    throw AuthenticationException("Session expired and refresh failed: ${refreshError.message}")
                }
            } else {
                throw e
            }
        }
    }

    private suspend fun invalidateCache(
        action: String,
        id: String? = null,
        communityId: String? = null,
        userId: String? = null
    ) {
        try {
            val body = buildJsonObject {
                put("action", action)
                if (id != null) put("id", id)
                if (communityId != null) put("communityId", communityId)
                if (userId != null) put("userId", userId)
            }
            client.functions.invoke("invalidate-cache", body = body)
        } catch (e: Exception) {
            println("Failed to invalidate cache: ${e.message}")
        }
    }

    init {
        try {
            com.mmk.kmpnotifier.notification.NotifierManager.addListener(object : com.mmk.kmpnotifier.notification.NotifierManager.Listener {
                override fun onNewToken(token: String) {
                    println("FCM Token updated: $token")
                    scope.launch {
                        syncFcmToken(token)
                    }
                }

                override fun onPushNotification(title: String?, body: String?) {
                    println("Push notification received: title=$title, body=$body")
                }

                override fun onNotificationClicked(data: com.mmk.kmpnotifier.notification.PayloadData) {
                    super.onNotificationClicked(data)
                    println("Notification clicked, payload: $data")
                    val communityId = data["community_id"] as? String
                    if (communityId != null) {
                        com.run.peakflow.presentation.navigation.DeepLinkNavigator.navigate(
                            com.run.peakflow.presentation.navigation.DeepLinkNavigator.DeepLink.CommunityPost(
                                communityId = communityId,
                                postId = data["post_id"] as? String
                            )
                        )
                    }
                }
            })

            // Sync token on authentication
            scope.launch {
                client.auth.sessionStatus.collect { status ->
                    if (status is SessionStatus.Authenticated) {
                        try {
                            val token = com.mmk.kmpnotifier.notification.NotifierManager.getPushNotifier().getToken()
                            if (token != null) syncFcmToken(token)
                        } catch (e: Exception) {
                            println("Failed to fetch current FCM token: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Failed to initialize push notifications: ${e.message}")
        }
    }

    // ==================== Internal DTOs for Supabase row mapping ====================

    @Serializable
    private data class ProfileRow(
        val id: String,
        val name: String,
        val email: String? = null,
        val phone: String? = null,
        val city: String = "",
        val avatar_url: String? = null,
        val interests: List<String> = emptyList(),
        val is_verified: Boolean = false,
        val created_at: String? = null
    )

    @Serializable
    private data class PostAuthorProfile(
        val name: String = "",
        val avatar_url: String? = null
    )

    @Serializable
    private data class CommunityRow(
        val id: String,
        val title: String,
        val description: String,
        val category: String,
        val city: String,
        val member_count: Int = 0,
        val created_by: String = "",
        val image_url: String? = null,
        val cover_url: String? = null,
        val rules: List<String> = emptyList(),
        val created_at: String? = null
    )

    @Serializable
    private data class MembershipRow(
        val id: String,
        val user_id: String,
        val community_id: String,
        val role: String,
        val joined_at: String? = null,
        val invited_by: String? = null
    )

    @Serializable
    private data class InviteCodeRow(
        val id: String,
        val code: String,
        val community_id: String,
        val created_by: String,
        val max_uses: Int? = null,
        val current_uses: Int = 0,
        val expires_at: String? = null,
        val is_active: Boolean = true,
        val created_at: String? = null
    )

    @Serializable
    private data class JoinRequestRow(
        val id: String,
        val user_id: String,
        val community_id: String,
        val status: String,
        val requested_at: String? = null,
        val reviewed_at: String? = null,
        val reviewed_by: String? = null
    )

    @Serializable
    private data class PostRow(
        val id: String,
        val community_id: String,
        val author_id: String,
        val content: String,
        val image_url: String? = null,
        val likes_count: Int = 0,
        val comments_count: Int = 0,
        val created_at: String? = null
    )

    @Serializable
    private data class CommentRow(
        val id: String,
        val post_id: String,
        val user_id: String,
        val content: String,
        val likes_count: Int = 0,
        val created_at: String? = null
    )

    @Serializable
    private data class EventRow(
        val id: String,
        val community_id: String,
        val title: String,
        val description: String,
        val category: String,
        val date: String,
        val time: String,
        val end_time: String? = null,
        val location: String,
        val latitude: Double? = null,
        val longitude: Double? = null,
        val image_url: String? = null,
        val max_participants: Int,
        val current_participants: Int = 0,
        val is_free: Boolean = true,
        val price: Double? = null,
        val created_at: String? = null
    )

    @Serializable
    private data class RsvpRow(
        val id: String,
        val user_id: String,
        val event_id: String,
        val created_at: String? = null
    )

    @Serializable
    private data class AttendanceRow(
        val id: String,
        val user_id: String,
        val event_id: String,
        val checked_in_at: String? = null
    )

    @Serializable
    private data class PostLikeRow(
        val id: String,
        val post_id: String,
        val user_id: String,
        val created_at: String? = null
    )

    @Serializable
    private data class IdOnlyRow(val id: String)

    // ==================== Mappers ====================

    private fun parseTimestamp(ts: String?): Long {
        if (ts == null) return 0L
        return try {
            kotlin.time.Instant.parse(ts).toEpochMilliseconds()
        } catch (_: Exception) {
            0L
        }
    }

    companion object {
        fun parseTimestampStatic(ts: String?): Long {
            if (ts == null) return 0L
            return try {
                kotlin.time.Instant.parse(ts).toEpochMilliseconds()
            } catch (_: Exception) {
                0L
            }
        }
    }

    private fun parseCategory(cat: String): EventCategory {
        return try { EventCategory.valueOf(cat) } catch (_: Exception) { EventCategory.OTHER }
    }

    private fun ProfileRow.toUser(): User = User(
        id = id,
        name = name,
        email = email,
        phone = phone,
        city = city,
        avatarUrl = avatar_url,
        interests = interests.map { parseCategory(it) },
        createdAt = parseTimestamp(created_at),
        isVerified = is_verified
    )

    private fun CommunityRow.toCommunityGroup(): CommunityGroup = CommunityGroup(
        id = id,
        title = title,
        description = description,
        category = parseCategory(category),
        city = city,
        memberCount = member_count,
        createdBy = created_by,
        imageUrl = image_url,
        coverUrl = cover_url,
        rules = rules,
        createdAt = parseTimestamp(created_at)
    )

    private fun MembershipRow.toCommunityMembership(): CommunityMembership = CommunityMembership(
        id = id,
        userId = user_id,
        communityId = community_id,
        role = try { MembershipRole.valueOf(role) } catch (_: Exception) { MembershipRole.MEMBER },
        joinedAt = parseTimestamp(joined_at),
        invitedBy = invited_by
    )

    private fun InviteCodeRow.toInviteCode(): InviteCode = InviteCode(
        id = id,
        code = code,
        communityId = community_id,
        createdBy = created_by,
        maxUses = max_uses,
        currentUses = current_uses,
        expiresAt = if (expires_at != null) parseTimestamp(expires_at) else null,
        isActive = is_active,
        createdAt = parseTimestamp(created_at)
    )

    private fun JoinRequestRow.toJoinRequest(): JoinRequest = JoinRequest(
        id = id,
        userId = user_id,
        communityId = community_id,
        status = try { RequestStatus.valueOf(status) } catch (_: Exception) { RequestStatus.PENDING },
        requestedAt = parseTimestamp(requested_at),
        reviewedAt = if (reviewed_at != null) parseTimestamp(reviewed_at) else null,
        reviewedBy = reviewed_by
    )

    private fun EventRow.toEvent(): Event = Event(
        id = id,
        groupId = community_id,
        title = title,
        description = description,
        category = parseCategory(category),
        date = date,
        time = time,
        endTime = end_time,
        location = location,
        latitude = latitude,
        longitude = longitude,
        imageUrl = image_url,
        maxParticipants = max_participants,
        currentParticipants = current_participants,
        isFree = is_free,
        price = price,
        createdAt = parseTimestamp(created_at)
    )

    private fun RsvpRow.toRsvp(): Rsvp = Rsvp(
        id = id,
        userId = user_id,
        eventId = event_id,
        timestamp = parseTimestamp(created_at)
    )

    private fun AttendanceRow.toAttendance(): Attendance = Attendance(
        id = id,
        eventId = event_id,
        userId = user_id,
        checkInTimestamp = parseTimestamp(checked_in_at)
    )

    // ==================== AUTH ====================

    override suspend fun signUp(email: String?, phone: String?, password: String): User {
        if (!email.isNullOrBlank()) {
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
        } else if (!phone.isNullOrBlank()) {
            client.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Phone) {
                this.phone = phone
                this.password = password
            }
        } else {
            throw Exception("Email or phone is required")
        }
        val session = client.auth.currentSessionOrNull()
        val userId = session?.user?.id ?: throw Exception("Sign up failed: no session")
        // Profile is auto-created by database trigger
        return getUser(userId) ?: throw Exception("Profile not created")
    }

    override suspend fun signIn(emailOrPhone: String, password: String): User? {
        client.auth.signInWith(Email) {
            this.email = emailOrPhone
            this.password = password
        }
        val session = client.auth.currentSessionOrNull()
        val userId = session?.user?.id ?: return null
        return getUser(userId)
    }

    override suspend fun signInWithGoogle(
        idToken: String,
        email: String,
        displayName: String?,
        photoUrl: String?
    ): User {
        // Sign in using the native ID Token obtained from Google CredentialManager API
        client.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.IDToken) {
            this.idToken = idToken
            this.provider = io.github.jan.supabase.auth.providers.Google
        }
        
        val session = client.auth.currentSessionOrNull()
        val userId = session?.user?.id ?: throw Exception("Google sign-in failed: no session")
        return getUser(userId) ?: throw Exception("Profile not found after Google sign-in")
    }

    override suspend fun startGoogleBrowserOAuth() {
        client.auth.signInWith(io.github.jan.supabase.auth.providers.Google)
    }

    override suspend fun verifyOtp(userId: String, otp: String): Boolean {
        return try {
            val user = getUser(userId) ?: return false
            val email = user.email ?: return false
            client.auth.verifyEmailOtp(
                type = io.github.jan.supabase.auth.OtpType.Email.SIGNUP,
                email = email,
                token = otp
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun resendOtp(userId: String): Boolean {
        return try {
            val user = getUser(userId) ?: return false
            val email = user.email ?: return false
            client.auth.resendEmail(
                type = io.github.jan.supabase.auth.OtpType.Email.SIGNUP,
                email = email
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getSessionUserId(): String? {
        // Wait for session to finish loading from storage (not Initializing)
        // because currentSessionOrNull() returns null during initialization.
        if (client.auth.sessionStatus.value is SessionStatus.Initializing) {
            withTimeoutOrNull(5000) {
                client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
            }
        }
        return client.auth.currentSessionOrNull()?.user?.id
    }

    override fun observeSessionStatus(): Flow<AuthSessionStatus> {
        return client.auth.sessionStatus.map { status ->
            val mapped = when (status) {
                is SessionStatus.Authenticated -> AuthSessionStatus.Authenticated(status.session.user?.id ?: "")
                is SessionStatus.NotAuthenticated -> AuthSessionStatus.NotAuthenticated
                is SessionStatus.Initializing -> AuthSessionStatus.Loading
                // Use string comparison or check if it's a known RefreshFailure if the type is not directly accessible
                else -> {
                    val statusName = status::class.simpleName
                    if (statusName == "NetworkError") {
                        AuthSessionStatus.NetworkError
                    } else {
                        AuthSessionStatus.NotAuthenticated
                    }
                }
            }
            println("SupabaseApiService: Mapping SessionStatus $status to $mapped")
            mapped
        }
    }

    override suspend fun waitForSessionLoaded(): String? {
        println("SupabaseApiService: waitForSessionLoaded start (current status: ${client.auth.sessionStatus.value})")
        // Wait for session to finish loading from storage (not Initializing)
        return withTimeoutOrNull(5000) {
            // Wait for the session status to move OUT of Initializing.
            // If it starts in NotAuthenticated, it might be because the SDK hasn't
            // triggered the 'Initializing' state yet. We wait for it to either
            // start initializing or become authenticated.
            var currentStatus = client.auth.sessionStatus.value
            
            // If we are at NotAuthenticated at the very start of the process, 
            // wait longer for the 'Initializing' transition (e.g. storage reader warm-up)
            if (currentStatus is SessionStatus.NotAuthenticated) {
                println("SupabaseApiService: status is NotAuthenticated, waiting for Initializing/Authenticated...")
                // Wait up to 2.5s for it to enter Initializing
                withTimeoutOrNull(2500) {
                    client.auth.sessionStatus.first { it is SessionStatus.Initializing || it is SessionStatus.Authenticated }
                }
                currentStatus = client.auth.sessionStatus.value
                println("SupabaseApiService: status after first wait: $currentStatus")
            }

            if (currentStatus is SessionStatus.Initializing) {
                println("SupabaseApiService: Entering Initializing state, waiting for completion...")
                client.auth.sessionStatus.first { it !is SessionStatus.Initializing }
                println("SupabaseApiService: Leaving Initializing state (new status: ${client.auth.sessionStatus.value})")
            }

            // Then return the user ID if authenticated
            when (val status = client.auth.sessionStatus.value) {
                is SessionStatus.Authenticated -> {
                    println("SupabaseApiService: waitForSessionLoaded returning Authenticated (${status.session.user?.id})")
                    status.session.user?.id
                }
                else -> {
                    println("SupabaseApiService: waitForSessionLoaded returning null (final status: $status)")
                    null
                }
            }
        }
    }

    override suspend fun logout() {
        try {
            client.auth.signOut()
        } catch (_: Exception) {
            // Ignore sign out errors
        }
    }

    // ==================== STORAGE ====================

    override suspend fun uploadImage(bucket: String, fileName: String, imageData: ByteArray): String {
        return ensureAuthenticated {
            val mimeType = when {
                fileName.endsWith(".png", ignoreCase = true) -> io.ktor.http.ContentType.Image.PNG
                fileName.endsWith(".webp", ignoreCase = true) -> io.ktor.http.ContentType("image", "webp")
                fileName.endsWith(".gif", ignoreCase = true) -> io.ktor.http.ContentType.Image.GIF
                else -> io.ktor.http.ContentType.Image.JPEG
            }
            val storageBucket = client.storage.from(bucket)
            storageBucket.upload(fileName, imageData) {
                upsert = true
                contentType = mimeType
            }
            storageBucket.publicUrl(fileName)
        }
    }

    // ==================== USER ====================

    override suspend fun getUser(userId: String): User? {
        return try {
            ensureAuthenticated {
                val row = client.postgrest.from("profiles")
                    .select { filter { eq("id", userId) } }
                    .decodeSingle<ProfileRow>()
                row.toUser()
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun updateUser(user: User): User {
        return ensureAuthenticated {
            client.postgrest.from("profiles")
                .update({
                    set("name", user.name)
                    set("city", user.city)
                    set("avatar_url", user.avatarUrl)
                    set("interests", user.interests.map { it.name })
                }) {
                    filter { eq("id", user.id) }
                }
            invalidateCache("update_profile", userId = user.id)
            getUser(user.id) ?: user
        }
    }

    override suspend fun completeProfile(
        userId: String,
        name: String,
        city: String,
        interests: List<EventCategory>,
        avatarUrl: String?
    ): User {
        return ensureAuthenticated {
            client.postgrest.from("profiles")
                .update({
                    set("name", name)
                    set("city", city)
                    set("interests", interests.map { it.name })
                    set("avatar_url", avatarUrl)
                }) {
                    filter { eq("id", userId) }
                }
            invalidateCache("update_profile", userId = userId)
            getUser(userId) ?: throw Exception("User not found")
        }
    }

    override suspend fun syncFcmToken(token: String) {
        val userId = getSessionUserId() ?: return
        try {
            ensureAuthenticated {
                client.postgrest.from("profiles").update(
                    {
                        set("fcm_token", token)
                    }
                ) {
                    filter { eq("id", userId) }
                }
            }
        } catch (e: Exception) {
            println("Failed to sync FCM token: ${e.message}")
        }
    }

    // ==================== INVITE CODES ====================

    override suspend fun validateInviteCode(code: String): InviteCode? {
        return try {
            ensureAuthenticated {
                val body = buildJsonObject { put("code", code) }
                val response = client.functions.invoke("validate-invite-code", body = body)
                val json = Json.decodeFromString<JsonObject>(response.body<String>())
                val ic = json["inviteCode"]?.jsonObject ?: return@ensureAuthenticated null
                InviteCode(
                    id = ic["id"]?.jsonPrimitive?.content ?: "",
                    code = ic["code"]?.jsonPrimitive?.content ?: "",
                    communityId = ic["communityId"]?.jsonPrimitive?.content ?: "",
                    createdBy = "",
                    maxUses = ic["maxUses"]?.jsonPrimitive?.int,
                    currentUses = ic["currentUses"]?.jsonPrimitive?.int ?: 0,
                    expiresAt = null,
                    isActive = true,
                    createdAt = 0L
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun useInviteCode(code: String, userId: String): CommunityMembership {
        return ensureAuthenticated {
            val body = buildJsonObject { put("code", code) }
            val response = client.functions.invoke("use-invite-code", body = body)
            val json = Json.decodeFromString<JsonObject>(response.body<String>())
            val m = json["membership"]?.jsonObject ?: throw Exception("Failed to use invite code")
            CommunityMembership(
                id = m["id"]?.jsonPrimitive?.content ?: "",
                userId = m["userId"]?.jsonPrimitive?.content ?: "",
                communityId = m["communityId"]?.jsonPrimitive?.content ?: "",
                role = try { MembershipRole.valueOf(m["role"]?.jsonPrimitive?.content ?: "MEMBER") } catch (_: Exception) { MembershipRole.MEMBER },
                joinedAt = 0L,
                invitedBy = m["invitedBy"]?.jsonPrimitive?.content
            )
        }
    }

    override suspend fun generateInviteCode(
        communityId: String,
        createdBy: String,
        maxUses: Int?,
        expiresInDays: Int?
    ): InviteCode {
        return ensureAuthenticated {
            val body = buildJsonObject {
                put("communityId", communityId)
                if (maxUses != null) put("maxUses", maxUses)
                if (expiresInDays != null) put("expiresInDays", expiresInDays)
            }
            val response = client.functions.invoke("generate-invite-code", body = body)
            val json = Json.decodeFromString<JsonObject>(response.body<String>())
            val ic = json["inviteCode"]?.jsonObject ?: throw Exception("Failed to generate invite code")
            InviteCode(
            id = ic["id"]?.jsonPrimitive?.content ?: "",
            code = ic["code"]?.jsonPrimitive?.content ?: "",
            communityId = ic["communityId"]?.jsonPrimitive?.content ?: "",
            createdBy = ic["createdBy"]?.jsonPrimitive?.content ?: "",
            maxUses = ic["maxUses"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content?.toIntOrNull(),
            currentUses = ic["currentUses"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
            expiresAt = ic["expiresAt"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content?.let { parseTimestampStatic(it) },
            isActive = ic["isActive"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true,
            createdAt = ic["createdAt"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonPrimitive?.content?.let { parseTimestampStatic(it) } ?: 0L
            )
        }
    }

    override suspend fun getUserInviteCodes(userId: String, communityId: String): List<InviteCode> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("invite_codes")
                .select {
                    filter {
                        eq("created_by", userId)
                        eq("community_id", communityId)
                        eq("is_active", true)
                    }
                }
                .decodeList<InviteCodeRow>()
            rows.map { it.toInviteCode() }
        }
    }

    // ==================== COMMUNITIES ====================

    override suspend fun getCommunities(): List<CommunityGroup> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("communities")
                .select()
                .decodeList<CommunityRow>()
            rows.map { it.toCommunityGroup() }
        }
    }

    override suspend fun getUserCommunities(userId: String): List<Pair<CommunityGroup, MembershipRole>> {
        return ensureAuthenticated {
            @Serializable
            data class UserCommunityRow(
                val id: String,
                val title: String,
                val description: String,
                val category: String,
                val city: String,
                val member_count: Int,
                val image_url: String?,
                val cover_url: String?,
                val created_at: String?,
                val role: String,
                val joined_at: String?
            )

            val params = buildJsonObject { put("p_user_id", userId) }
            val body = buildJsonObject {
                put("rpc", "get_user_communities")
                put("params", params)
            }
            
            val rows = try {
                val response = client.functions.invoke("get-cached-data", body = body)
                Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<List<UserCommunityRow>>(response.body<String>())
            } catch (e: Exception) {
                println("getUserCommunities: Fallback to direct RPC due to ${e.message}")
                client.postgrest.rpc("get_user_communities", parameters = params)
                    .decodeList<UserCommunityRow>()
            }

            rows.map { row ->
                CommunityGroup(
                    id = row.id,
                    title = row.title,
                    description = row.description,
                    category = parseCategory(row.category),
                    city = row.city,
                    memberCount = row.member_count,
                    createdBy = "", // Not returned by this RPC
                    imageUrl = row.image_url,
                    coverUrl = row.cover_url,
                    rules = emptyList(), // Not returned by this RPC
                    createdAt = parseTimestamp(row.created_at)
                ) to try { MembershipRole.valueOf(row.role) } catch (_: Exception) { MembershipRole.MEMBER }
            }
        }
    }

    override suspend fun getCommunitiesByCity(city: String): List<CommunityGroup> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("communities")
                .select {
                    filter { ilike("city", city) }
                }
                .decodeList<CommunityRow>()
            rows.map { it.toCommunityGroup() }
        }
    }

    override suspend fun getCommunityById(communityId: String): CommunityGroup? {
        return try {
            ensureAuthenticated {
                val row = client.postgrest.from("communities")
                    .select { filter { eq("id", communityId) } }
                    .decodeSingle<CommunityRow>()
                row.toCommunityGroup()
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getDiscoverCommunitiesOptimized(
        userId: String,
        city: String
    ): List<CommunityGroup> {
        return ensureAuthenticated {
            val params = buildJsonObject {
                put("p_user_id", userId)
                put("p_city", city)
                put("p_limit", 50)
            }
            val body = buildJsonObject {
                put("rpc", "get_discover_communities")
                put("params", params)
            }
            val rows = try {
                val response = client.functions.invoke("get-cached-data", body = body)
                Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<List<CommunityRow>>(response.body<String>())
            } catch (e: Exception) {
                println("getDiscoverCommunitiesOptimized: Fallback to direct RPC due to ${e.message}")
                client.postgrest.rpc("get_discover_communities", parameters = params)
                    .decodeList<CommunityRow>()
            }
            rows.map { it.toCommunityGroup() }
        }
    }

    override suspend fun searchCommunities(
        query: String,
        category: EventCategory?,
        city: String
    ): List<CommunityGroup> {
        return ensureAuthenticated {
            val params = buildJsonObject {
                put("p_query", query)
                if (category != null) put("p_category", category.name)
                put("p_city", city)
            }
            val body = buildJsonObject {
                put("rpc", "search_communities")
                put("params", params)
            }
            val rows = try {
                val response = client.functions.invoke("get-cached-data", body = body)
                Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<List<CommunityRow>>(response.body<String>())
            } catch (e: Exception) {
                println("searchCommunities: Fallback to direct RPC due to ${e.message}")
                client.postgrest.rpc("search_communities", parameters = params)
                    .decodeList<CommunityRow>()
            }
            rows.map { it.toCommunityGroup() }
        }
    }

    override suspend fun createCommunity(
        title: String,
        description: String,
        category: EventCategory,
        city: String,
        rules: List<String>,
        createdBy: String,
        imageUrl: String?,
        coverUrl: String?
    ): CommunityGroup {
        return ensureAuthenticated {
            val body = buildJsonObject {
                put("title", title)
                put("description", description)
                put("category", category.name)
                put("city", city)
                put("rules", rules.joinToString("\n"))
                put("created_by", createdBy)
                if (imageUrl != null) put("image_url", imageUrl)
                if (coverUrl != null) put("cover_url", coverUrl)
            }
            val response = client.functions.invoke("create-community", body = body)
            val jsonStr = response.body<String>()
            val json = Json.decodeFromString<JsonObject>(jsonStr)
            val c = json["community"]?.jsonObject ?: throw Exception("Failed to create community")
            
            // Use the actual memberCount from the response if available, otherwise 1
            val memberCount = c["memberCount"]?.jsonPrimitive?.int ?: 1
            
            CommunityGroup(
                id = c["id"]?.jsonPrimitive?.content ?: "",
                title = c["title"]?.jsonPrimitive?.content ?: title,
                description = c["description"]?.jsonPrimitive?.content ?: description,
                category = category,
                city = c["city"]?.jsonPrimitive?.content ?: city,
                memberCount = memberCount,
                createdBy = createdBy,
                imageUrl = imageUrl,
                coverUrl = coverUrl,
                rules = rules,
                createdAt = parseTimestamp(c["createdAt"]?.jsonPrimitive?.content)
            )
        }
    }

    // ==================== MEMBERSHIPS ====================

    override suspend fun getUserMemberships(userId: String): List<CommunityMembership> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("memberships")
                .select {
                    filter {
                        eq("user_id", userId)
                        neq("role", "PENDING")
                    }
                }
                .decodeList<MembershipRow>()
            rows.map { it.toCommunityMembership() }
        }
    }

    override suspend fun getCommunityMemberships(communityId: String): List<CommunityMembership> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("memberships")
                .select {
                    filter {
                        eq("community_id", communityId)
                        neq("role", "PENDING")
                    }
                }
                .decodeList<MembershipRow>()
            rows.map { it.toCommunityMembership() }
        }
    }

    override suspend fun getCommunityMembersWithProfiles(communityId: String): List<CommunityMemberWithProfile> {
        return ensureAuthenticated {
            @Serializable
            data class MemberWithProfileRow(
                val id: String,
                val user_id: String,
                val community_id: String,
                val role: String,
                val joined_at: String? = null,
                val invited_by: String? = null,
                val user_name: String? = null,
                val user_email: String? = null,
                val user_avatar_url: String? = null
            )

            val params = buildJsonObject { put("p_community_id", communityId) }
            val body = buildJsonObject {
                put("rpc", "get_community_members_with_profiles")
                put("params", params)
            }
            val rows = try {
                val response = client.functions.invoke("get-cached-data", body = body)
                Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<List<MemberWithProfileRow>>(response.body<String>())
            } catch (e: Exception) {
                println("getCommunityMembersWithProfiles: Fallback to direct RPC due to ${e.message}")
                client.postgrest.rpc("get_community_members_with_profiles", parameters = params)
                    .decodeList<MemberWithProfileRow>()
            }

            rows.map { row ->
                CommunityMemberWithProfile(
                    membership = CommunityMembership(
                        id = row.id,
                        userId = row.user_id,
                        communityId = row.community_id,
                        role = try { MembershipRole.valueOf(row.role) } catch (_: Exception) { MembershipRole.MEMBER },
                        joinedAt = parseTimestamp(row.joined_at),
                        invitedBy = row.invited_by
                    ),
                    userName = row.user_name ?: "Member",
                    userEmail = row.user_email,
                    userAvatarUrl = row.user_avatar_url
                )
            }
        }
    }

    override suspend fun getMembershipRole(userId: String, communityId: String): CommunityMembership? {
        return try {
            ensureAuthenticated {
                val row = client.postgrest.from("memberships")
                    .select {
                        filter {
                            eq("user_id", userId)
                            eq("community_id", communityId)
                        }
                    }
                    .decodeSingle<MembershipRow>()
                row.toCommunityMembership()
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun isUserMemberOf(userId: String, communityId: String): Boolean {
        return ensureAuthenticated {
            val rows = client.postgrest.from("memberships")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                        eq("community_id", communityId)
                        neq("role", "PENDING")
                    }
                }
                .decodeList<IdOnlyRow>()
            rows.isNotEmpty()
        }
    }

    // ==================== JOIN REQUESTS ====================

    override suspend fun requestToJoin(userId: String, communityId: String): JoinRequest {
        return ensureAuthenticated {
            val row = client.postgrest.from("join_requests")
                .insert(buildJsonObject {
                    put("user_id", userId)
                    put("community_id", communityId)
                    put("status", "PENDING")
                }) { select() }
                .decodeSingle<JoinRequestRow>()
            row.toJoinRequest()
        }
    }

    override suspend fun getPendingJoinRequests(communityId: String): List<JoinRequest> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("join_requests")
                .select {
                    filter {
                        eq("community_id", communityId)
                        eq("status", "PENDING")
                    }
                }
                .decodeList<JoinRequestRow>()
            rows.map { it.toJoinRequest() }
        }
    }

    override suspend fun getUserJoinRequests(userId: String): List<JoinRequest> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("join_requests")
                .select {
                    filter { eq("user_id", userId) }
                }
                .decodeList<JoinRequestRow>()
            rows.map { it.toJoinRequest() }
        }
    }

    override suspend fun approveJoinRequest(
        requestId: String,
        reviewedBy: String
    ): CommunityMembership {
        return ensureAuthenticated {
            val body = buildJsonObject { put("requestId", requestId) }
            val response = client.functions.invoke("approve-join-request", body = body)
            val json = Json.decodeFromString<JsonObject>(response.body<String>())
            val m = json["membership"]?.jsonObject ?: throw Exception("Failed to approve join request")
            CommunityMembership(
                id = m["id"]?.jsonPrimitive?.content ?: "",
                userId = m["userId"]?.jsonPrimitive?.content ?: "",
                communityId = m["communityId"]?.jsonPrimitive?.content ?: "",
                role = MembershipRole.MEMBER,
                joinedAt = 0L,
                invitedBy = m["invitedBy"]?.jsonPrimitive?.content
            )
        }
    }

    override suspend fun rejectJoinRequest(
        requestId: String,
        reviewedBy: String
    ): JoinRequest {
        return ensureAuthenticated {
            val body = buildJsonObject { put("requestId", requestId) }
            val response = client.functions.invoke("reject-join-request", body = body)
            val json = Json.decodeFromString<JsonObject>(response.body<String>())
            val jr = json["joinRequest"]?.jsonObject ?: throw Exception("Failed to reject join request")
            JoinRequest(
                id = jr["id"]?.jsonPrimitive?.content ?: "",
                userId = jr["userId"]?.jsonPrimitive?.content ?: "",
                communityId = jr["communityId"]?.jsonPrimitive?.content ?: "",
                status = RequestStatus.REJECTED,
                requestedAt = 0L,
                reviewedAt = null,
                reviewedBy = jr["reviewedBy"]?.jsonPrimitive?.content
            )
        }
    }

    override suspend fun hasUserRequestedToJoin(userId: String, communityId: String): Boolean {
        return ensureAuthenticated {
            val rows = client.postgrest.from("join_requests")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                        eq("community_id", communityId)
                        eq("status", "PENDING")
                    }
                }
                .decodeList<IdOnlyRow>()
            rows.isNotEmpty()
        }
    }

    override suspend fun getPendingJoinRequestCommunityIds(userId: String): Set<String> {
        return ensureAuthenticated {
            @Serializable
            data class CommunityIdRow(val community_id: String)

            val rows = client.postgrest.rpc(
                "get_pending_join_request_community_ids",
                buildJsonObject { put("p_user_id", userId) }
            ).decodeList<CommunityIdRow>()

            return@ensureAuthenticated rows.map { it.community_id }.toSet()
        }
    }

    // ==================== POSTS ====================

    override suspend fun getCommunityPosts(communityId: String, limit: Int, offset: Int): List<Post> {
        return ensureAuthenticated {
            @Serializable
            data class PostWithAuthorRow(
                val id: String,
                val author_id: String,
                val author_name: String,
                val author_avatar_url: String?,
                val content: String,
                val image_url: String?,
                val likes_count: Int,
                val comments_count: Int,
                val created_at: String?,
                val is_liked: Boolean = false
            )

            val params = buildJsonObject {
                put("p_community_id", communityId)
                put("p_limit", limit)
                put("p_offset", offset)
            }
            val body = buildJsonObject {
                put("rpc", "get_community_posts")
                put("params", params)
            }
            val rows = try {
                val response = client.functions.invoke("get-cached-data", body = body)
                Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<List<PostWithAuthorRow>>(response.body<String>())
            } catch (e: Exception) {
                println("getCommunityPosts: Fallback to direct RPC due to ${e.message}")
                client.postgrest.rpc("get_community_posts", parameters = params)
                    .decodeList<PostWithAuthorRow>()
            }

            rows.map { row ->
                Post(
                    id = row.id,
                    communityId = communityId,
                    authorId = row.author_id,
                    authorName = row.author_name,
                    authorAvatarUrl = row.author_avatar_url,
                    content = row.content,
                    imageUrl = row.image_url,
                    likesCount = row.likes_count,
                    commentsCount = row.comments_count,
                    createdAt = parseTimestamp(row.created_at),
                    isLiked = row.is_liked
                )
            }
        }
    }

    override suspend fun getFeedPosts(communityIds: List<String>, limit: Int, offset: Int): List<Post> {
        return ensureAuthenticated {
            val userId = getSessionUserId() ?: throw AuthenticationException()
            
            @Serializable
            data class FeedPostRow(
                val id: String,
                val community_id: String,
                val community_name: String,
                val community_image: String?,
                val author_id: String,
                val author_name: String,
                val author_avatar_url: String?,
                val content: String,
                val image_url: String?,
                val likes_count: Int,
                val comments_count: Int,
                val created_at: String?,
                val is_liked: Boolean = false
            )

            val params = buildJsonObject {
                put("p_user_id", userId)
                put("p_limit", limit)
                put("p_offset", offset)
            }
            val body = buildJsonObject {
                put("rpc", "get_user_feed")
                put("params", params)
            }
            val rows = try {
                val response = client.functions.invoke("get-cached-data", body = body)
                Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<List<FeedPostRow>>(response.body<String>())
            } catch (e: Exception) {
                println("getFeedPosts: Fallback to direct RPC due to ${e.message}")
                client.postgrest.rpc("get_user_feed", parameters = params)
                    .decodeList<FeedPostRow>()
            }

            rows.map { row ->
                Post(
                    id = row.id,
                    communityId = row.community_id,
                    communityName = row.community_name,
                    authorId = row.author_id,
                    authorName = row.author_name,
                    authorAvatarUrl = row.author_avatar_url,
                    content = row.content,
                    imageUrl = row.image_url,
                    likesCount = row.likes_count,
                    commentsCount = row.comments_count,
                    createdAt = parseTimestamp(row.created_at),
                    isLiked = row.is_liked
                )
            }
        }
    }

    override suspend fun getPostById(postId: String): Post? {
        return try {
            ensureAuthenticated {
                // Fallback to basic join if no RPC for single post
                @Serializable
                data class PostAuthorProfile(
                    val name: String = "",
                    val avatar_url: String? = null
                )

                @Serializable
                data class PostWithAuthorRow(
                    val id: String,
                    val community_id: String,
                    val author_id: String,
                    val content: String,
                    val image_url: String? = null,
                    val likes_count: Int = 0,
                    val comments_count: Int = 0,
                    val created_at: String? = null,
                    val profiles: PostAuthorProfile? = null
                )

                val row = client.postgrest.from("posts")
                    .select(columns = Columns.raw("*, profiles!posts_author_id_fkey(name, avatar_url)")) {
                        filter { eq("id", postId) }
                    }
                    .decodeSingle<PostWithAuthorRow>()

                Post(
                    id = row.id,
                    communityId = row.community_id,
                    authorId = row.author_id,
                    authorName = row.profiles?.name ?: "Unknown",
                    authorAvatarUrl = row.profiles?.avatar_url,
                    content = row.content,
                    imageUrl = row.image_url,
                    likesCount = row.likes_count,
                    commentsCount = row.comments_count,
                    createdAt = parseTimestamp(row.created_at)
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun createPost(
        communityId: String,
        authorId: String,
        content: String,
        imageUrl: String?
    ): Post {
        return ensureAuthenticated {
            val row = client.postgrest.from("posts")
                .insert(buildJsonObject {
                    put("community_id", communityId)
                    put("author_id", authorId)
                    put("content", content)
                    if (imageUrl != null) put("image_url", imageUrl)
                }) { select() }
                .decodeSingle<PostRow>()

            // Fetch author info
            val profile = getUser(authorId)
            invalidateCache("new_post", communityId = communityId)
            Post(
                id = row.id,
                communityId = row.community_id,
                authorId = row.author_id,
                authorName = profile?.name ?: "Unknown",
                authorAvatarUrl = profile?.avatarUrl,
                content = row.content,
                imageUrl = row.image_url,
                likesCount = 0,
                commentsCount = 0,
                createdAt = parseTimestamp(row.created_at)
            )
        }
    }

    override suspend fun deletePost(postId: String): Boolean {
        return try {
            ensureAuthenticated {
                val post = getPostById(postId)
                client.postgrest.from("posts")
                    .delete { filter { eq("id", postId) } }
                post?.communityId?.let { invalidateCache("delete_post", communityId = it) }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun likePost(postId: String, userId: String): Boolean {
        return try {
            ensureAuthenticated {
                client.postgrest.from("post_likes")
                    .insert(buildJsonObject {
                        put("post_id", postId)
                        put("user_id", userId)
                    })
                val post = getPostById(postId)
                post?.communityId?.let { invalidateCache("like_post", communityId = it) }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun unlikePost(postId: String, userId: String): Boolean {
        return try {
            ensureAuthenticated {
                client.postgrest.from("post_likes")
                    .delete {
                        filter {
                            eq("post_id", postId)
                            eq("user_id", userId)
                        }
                    }
                val post = getPostById(postId)
                post?.communityId?.let { invalidateCache("unlike_post", communityId = it) }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun hasUserLikedPost(postId: String, userId: String): Boolean {
        return ensureAuthenticated {
            val rows = client.postgrest.from("post_likes")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<IdOnlyRow>()
            rows.isNotEmpty()
        }
    }

    // ==================== COMMENTS ====================

    override suspend fun getPostComments(postId: String): List<PostComment> {
        return ensureAuthenticated {
            @Serializable
            data class CommentWithAuthor(
                val id: String,
                val post_id: String,
                val user_id: String,
                val content: String,
                val likes_count: Int = 0,
                val created_at: String? = null,
                val profiles: PostAuthorProfile? = null
            )

            val rows = client.postgrest.from("comments")
                .select(columns = Columns.raw("*, profiles!comments_user_id_fkey(name, avatar_url)")) {
                    filter { eq("post_id", postId) }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<CommentWithAuthor>()

            rows.map {
                PostComment(
                    id = it.id,
                    postId = it.post_id,
                    userId = it.user_id,
                    userName = it.profiles?.name ?: "Unknown",
                    userAvatarUrl = it.profiles?.avatar_url,
                    content = it.content,
                    likesCount = it.likes_count,
                    createdAt = parseTimestamp(it.created_at)
                )
            }
        }
    }

    override suspend fun addComment(
        postId: String,
        userId: String,
        content: String
    ): PostComment {
        return ensureAuthenticated {
            val row = client.postgrest.from("comments")
                .insert(buildJsonObject {
                    put("post_id", postId)
                    put("user_id", userId)
                    put("content", content)
                }) { select() }
                .decodeSingle<CommentRow>()

            val profile = getUser(userId)
            val post = getPostById(postId)
            post?.communityId?.let { invalidateCache("new_post", communityId = it) }
            PostComment(
                id = row.id,
                postId = row.post_id,
                userId = row.user_id,
                userName = profile?.name ?: "Unknown",
                userAvatarUrl = profile?.avatarUrl,
                content = row.content,
                likesCount = 0,
                createdAt = parseTimestamp(row.created_at)
            )
        }
    }

    override suspend fun deleteComment(commentId: String): Boolean {
        return try {
            ensureAuthenticated {
                val comment = client.postgrest.from("comments")
                    .select { filter { eq("id", commentId) } }
                    .decodeSingle<CommentRow>()
                val post = getPostById(comment.post_id)
                client.postgrest.from("comments")
                    .delete { filter { eq("id", commentId) } }
                post?.communityId?.let { invalidateCache("delete_post", communityId = it) }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    // ==================== EVENTS ====================

    override suspend fun createEvent(
        communityId: String,
        title: String,
        description: String,
        category: EventCategory,
        date: String,
        time: String,
        location: String,
        maxParticipants: Int,
        isFree: Boolean,
        price: Double?,
        imageUrl: String?
    ): Event {
        return ensureAuthenticated {
            val row = client.postgrest.from("events")
                .insert(buildJsonObject {
                    put("community_id", communityId)
                    put("title", title)
                    put("description", description)
                    put("category", category.name)
                    put("date", date)
                    put("time", time)
                    put("location", location)
                    put("max_participants", maxParticipants)
                    put("is_free", isFree)
                    if (price != null) put("price", price)
                    if (imageUrl != null) put("image_url", imageUrl)
                }) { select() }
                .decodeSingle<EventRow>()
            row.toEvent()
        }
    }

    override suspend fun getEventsByGroupId(groupId: String): List<Event> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("events")
                .select {
                    filter { eq("community_id", groupId) }
                    order("date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<EventRow>()
            rows.map { it.toEvent() }
        }
    }

    override suspend fun getCommunityEventsWithRsvp(communityId: String): List<Pair<Event, Boolean>> {
        return ensureAuthenticated {
            @Serializable
            data class EventWithRsvpRow(
                val id: String,
                val community_id: String,
                val title: String,
                val description: String,
                val category: String,
                val event_date: String,
                val event_time: String,
                val end_time: String? = null,
                val location: String,
                val latitude: Double? = null,
                val longitude: Double? = null,
                val image_url: String? = null,
                val max_participants: Int,
                val current_participants: Int = 0,
                val is_free: Boolean = true,
                val price: Double? = null,
                val created_at: String? = null,
                val is_rsvped: Boolean = false
            )

            val params = buildJsonObject { put("p_community_id", communityId) }
            val body = buildJsonObject {
                put("rpc", "get_community_events_with_rsvp")
                put("params", params)
            }
            val rows = try {
                val response = client.functions.invoke("get-cached-data", body = body)
                Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<List<EventWithRsvpRow>>(response.body<String>())
            } catch (e: Exception) {
                println("getCommunityEventsWithRsvp: Fallback to direct RPC due to ${e.message}")
                client.postgrest.rpc("get_community_events_with_rsvp", parameters = params)
                    .decodeList<EventWithRsvpRow>()
            }

            rows.map { row ->
                Event(
                    id = row.id,
                    groupId = row.community_id,
                    title = row.title,
                    description = row.description,
                    category = parseCategory(row.category),
                    date = row.event_date,
                    time = row.event_time,
                    endTime = row.end_time,
                    location = row.location,
                    latitude = row.latitude,
                    longitude = row.longitude,
                    imageUrl = row.image_url,
                    maxParticipants = row.max_participants,
                    currentParticipants = row.current_participants,
                    isFree = row.is_free,
                    price = row.price,
                    createdAt = parseTimestamp(row.created_at)
                ) to row.is_rsvped
            }
        }
    }

    override suspend fun getEventById(eventId: String): Event? {
        return try {
            ensureAuthenticated {
                val row = client.postgrest.from("events")
                    .select { filter { eq("id", eventId) } }
                    .decodeSingle<EventRow>()
                row.toEvent()
            }
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun getNearbyEvents(
        city: String,
        category: EventCategory?,
        communityIds: List<String>
    ): List<Event> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("events")
                .select {
                    filter {
                        if (communityIds.isNotEmpty()) {
                            isIn("community_id", communityIds)
                        }
                        if (category != null) {
                            eq("category", category.name)
                        }
                    }
                    order("date", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<EventRow>()
            rows.map { it.toEvent() }
        }
    }

    override suspend fun getAllAccessibleEvents(communityIds: List<String>): List<Event> {
        return ensureAuthenticated {
            val userId = getSessionUserId() ?: throw AuthenticationException()

            @Serializable
            data class UpcomingEventRow(
                val id: String,
                val community_id: String,
                val community_name: String,
                val title: String,
                val description: String,
                val category: String,
                val date: String,
                val time: String,
                val end_time: String?,
                val location: String,
                val latitude: Double?,
                val longitude: Double?,
                val image_url: String?,
                val max_participants: Int,
                val current_participants: Int,
                val is_free: Boolean,
                val price: Double?,
                val has_rsvp: Boolean
            )

            val params = buildJsonObject { put("p_user_id", userId) }
            val body = buildJsonObject {
                put("rpc", "get_user_upcoming_events")
                put("params", params)
            }
            val rows = try {
                val response = client.functions.invoke("get-cached-data", body = body)
                Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<List<UpcomingEventRow>>(response.body<String>())
            } catch (e: Exception) {
                println("getAllAccessibleEvents: Fallback to direct RPC due to ${e.message}")
                client.postgrest.rpc("get_user_upcoming_events", parameters = params)
                    .decodeList<UpcomingEventRow>()
            }

            rows.map { row ->
                Event(
                    id = row.id,
                    groupId = row.community_id,
                    title = row.title,
                    description = row.description,
                    category = parseCategory(row.category),
                    date = row.date,
                    time = row.time,
                    endTime = row.end_time,
                    location = row.location,
                    latitude = row.latitude,
                    longitude = row.longitude,
                    imageUrl = row.image_url,
                    maxParticipants = row.max_participants,
                    currentParticipants = row.current_participants,
                    isFree = row.is_free,
                    price = row.price,
                    createdAt = 0L // Not returned by this RPC
                )
            }
        }
    }

    override suspend fun getUserEventsWithRsvp(category: EventCategory?): List<Pair<Event, Boolean>> {
        return ensureAuthenticated {
            val userId = getSessionUserId() ?: throw AuthenticationException()

            @Serializable
            data class EventWithRsvpRow(
                val id: String,
                val community_id: String,
                val community_name: String,
                val title: String,
                val description: String,
                val category: String,
                val event_date: String,
                val event_time: String,
                val end_time: String? = null,
                val location: String,
                val latitude: Double? = null,
                val longitude: Double? = null,
                val image_url: String? = null,
                val max_participants: Int,
                val current_participants: Int = 0,
                val is_free: Boolean = true,
                val price: Double? = null,
                val created_at: String? = null,
                val is_rsvped: Boolean = false
            )

            val params = buildJsonObject {
                put("p_user_id", userId)
                if (category != null) put("p_category", category.name)
            }
            val body = buildJsonObject {
                put("rpc", "get_user_events_with_rsvp")
                put("params", params)
            }
            val rows = try {
                val response = client.functions.invoke("get-cached-data", body = body)
                Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString<List<EventWithRsvpRow>>(response.body<String>())
            } catch (e: Exception) {
                println("getUserEventsWithRsvp: Fallback to direct RPC due to ${e.message}")
                client.postgrest.rpc("get_user_events_with_rsvp", parameters = params)
                    .decodeList<EventWithRsvpRow>()
            }

            rows.map { row ->
                Event(
                    id = row.id,
                    groupId = row.community_id,
                    title = row.title,
                    description = row.description,
                    category = parseCategory(row.category),
                    date = row.event_date,
                    time = row.event_time,
                    endTime = row.end_time,
                    location = row.location,
                    latitude = row.latitude,
                    longitude = row.longitude,
                    imageUrl = row.image_url,
                    maxParticipants = row.max_participants,
                    currentParticipants = row.current_participants,
                    isFree = row.is_free,
                    price = row.price,
                    createdAt = parseTimestamp(row.created_at)
                ) to row.is_rsvped
            }
        }
    }

    override suspend fun getEventDetail(eventId: String): EventDetailResult? {
        return try {
            ensureAuthenticated {
                val userId = getSessionUserId() ?: throw AuthenticationException()

                @Serializable
                data class EventDetailRow(
                    val id: String,
                    val community_id: String,
                    val community_name: String,
                    val community_image: String? = null,
                    val title: String,
                    val description: String,
                    val category: String,
                    val event_date: String,
                    val event_time: String,
                    val end_time: String? = null,
                    val location: String,
                    val latitude: Double? = null,
                    val longitude: Double? = null,
                    val image_url: String? = null,
                    val max_participants: Int,
                    val current_participants: Int = 0,
                    val is_free: Boolean = true,
                    val price: Double? = null,
                    val created_at: String? = null,
                    val is_rsvped: Boolean = false,
                    val is_checked_in: Boolean = false
                )

                val params = buildJsonObject {
                    put("p_event_id", eventId)
                    put("p_user_id", userId)
                }
                val body = buildJsonObject {
                    put("rpc", "get_event_detail")
                    put("params", params)
                }
                val rows = try {
                    val response = client.functions.invoke("get-cached-data", body = body)
                    Json {
                        ignoreUnknownKeys = true; coerceInputValues = true
                    }.decodeFromString<List<EventDetailRow>>(response.body<String>())
                } catch (e: Exception) {
                    println("getEventDetail: Fallback to direct RPC due to ${e.message}")
                    client.postgrest.rpc("get_event_detail", parameters = params)
                        .decodeList<EventDetailRow>()
                }

                val row = rows.firstOrNull() ?: return@ensureAuthenticated null

                EventDetailResult(
                    event = Event(
                        id = row.id,
                        groupId = row.community_id,
                        title = row.title,
                        description = row.description,
                        category = parseCategory(row.category),
                        date = row.event_date,
                        time = row.event_time,
                        endTime = row.end_time,
                        location = row.location,
                        latitude = row.latitude,
                        longitude = row.longitude,
                        imageUrl = row.image_url,
                        maxParticipants = row.max_participants,
                        currentParticipants = row.current_participants,
                        isFree = row.is_free,
                        price = row.price,
                        createdAt = parseTimestamp(row.created_at)
                    ),
                    communityName = row.community_name,
                    communityImage = row.community_image,
                    hasRsvped = row.is_rsvped,
                    hasCheckedIn = row.is_checked_in
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    // ==================== RSVP ====================

    override suspend fun rsvpToEvent(userId: String, eventId: String): Rsvp {
        return ensureAuthenticated {
            val row = client.postgrest.from("rsvps")
                .insert(buildJsonObject {
                    put("user_id", userId)
                    put("event_id", eventId)
                }) { select() }
                .decodeSingle<RsvpRow>()
            val event = getEventById(eventId)
            event?.groupId?.let {
                invalidateCache(
                    "rsvp_event",
                    id = eventId,
                    communityId = it,
                    userId = userId
                )
            }
            row.toRsvp()
        }
    }

    override suspend fun cancelRsvp(userId: String, eventId: String): Boolean {
        return try {
            ensureAuthenticated {
                val event = getEventById(eventId)
                client.postgrest.from("rsvps")
                    .delete {
                        filter {
                            eq("user_id", userId)
                            eq("event_id", eventId)
                        }
                    }
                event?.groupId?.let {
                    invalidateCache(
                        "unrsvp_event",
                        id = eventId,
                        communityId = it,
                        userId = userId
                    )
                }
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    override suspend fun getUserRsvps(userId: String): List<Rsvp> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("rsvps")
                .select { filter { eq("user_id", userId) } }
                .decodeList<RsvpRow>()
            rows.map { it.toRsvp() }
        }
    }

    override suspend fun getEventRsvps(eventId: String): List<Rsvp> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("rsvps")
                .select { filter { eq("event_id", eventId) } }
                .decodeList<RsvpRow>()
            rows.map { it.toRsvp() }
        }
    }

    override suspend fun hasUserRsvped(userId: String, eventId: String): Boolean {
        return ensureAuthenticated {
            val rows = client.postgrest.from("rsvps")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                        eq("event_id", eventId)
                    }
                }
                .decodeList<IdOnlyRow>()
            rows.isNotEmpty()
        }
    }

    // ==================== ATTENDANCE ====================

    override suspend fun checkInToEvent(userId: String, eventId: String): Attendance {
        return ensureAuthenticated {
            val row = client.postgrest.from("attendances")
                .insert(buildJsonObject {
                    put("user_id", userId)
                    put("event_id", eventId)
                }) { select() }
                .decodeSingle<AttendanceRow>()
            row.toAttendance()
        }
    }

    override suspend fun hasUserCheckedIn(userId: String, eventId: String): Boolean {
        return ensureAuthenticated {
            val rows = client.postgrest.from("attendances")
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("user_id", userId)
                        eq("event_id", eventId)
                    }
                }
                .decodeList<IdOnlyRow>()
            rows.isNotEmpty()
        }
    }

    override suspend fun getEventAttendance(eventId: String): List<Attendance> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("attendances")
                .select { filter { eq("event_id", eventId) } }
                .decodeList<AttendanceRow>()
            rows.map { it.toAttendance() }
        }
    }

    override suspend fun getUserAttendanceHistory(userId: String): List<Attendance> {
        return ensureAuthenticated {
            val rows = client.postgrest.from("attendances")
                .select { filter { eq("user_id", userId) } }
                .decodeList<AttendanceRow>()
            rows.map { it.toAttendance() }
        }
    }

    // ==================== REALTIME ====================

    override fun observePosts(communityId: String): Flow<Post> {
        return callbackFlow {
            val uniqueSuffix = kotlin.random.Random.nextLong(0, Long.MAX_VALUE).toString(16)
            val uniqueName = "posts_${communityId}_${uniqueSuffix}"
            val channel = client.channel(uniqueName)

            // IMPORTANT: Configure postgresChangeFlow BEFORE subscribing
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "posts"
            }

            // Subscribe first, then collect
            channel.subscribe()

            val job = launch {
                changeFlow.collect { action ->
                    val row = action.decodeRecord<PostRow>()
                    if (row.community_id != communityId) return@collect
                    val profile = com.run.peakflow.data.cache.ProfileCache.get(row.author_id)
                        ?: try { getUser(row.author_id)?.also { com.run.peakflow.data.cache.ProfileCache.put(row.author_id, it) } } catch (_: Exception) { null }
                    trySend(
                        Post(
                            id = row.id,
                            communityId = row.community_id,
                            authorId = row.author_id,
                            authorName = profile?.name ?: "Someone",
                            authorAvatarUrl = profile?.avatarUrl,
                            content = row.content,
                            imageUrl = row.image_url,
                            likesCount = 0,
                            commentsCount = 0,
                            createdAt = parseTimestamp(row.created_at)
                        )
                    )
                }
            }

            awaitClose {
                job.cancel()
                // Use an independent scope for cleanup — the ProducerScope is being
                // cancelled so launch{} on it would never execute removeChannel.
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        client.realtime.removeChannel(channel)
                    } catch (_: Exception) {
                        // Channel may already be removed
                    }
                }
            }
        }
    }

    override fun observeComments(postId: String): Flow<PostComment> {
        return callbackFlow {
            val uniqueSuffix = kotlin.random.Random.nextLong(0, Long.MAX_VALUE).toString(16)
            val uniqueName = "comments_${postId}_${uniqueSuffix}"
            val channel = client.channel(uniqueName)

            // Configure BEFORE subscribing
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "comments"
            }

            channel.subscribe()

            val job = launch {
                changeFlow.collect { action ->
                    val row = action.decodeRecord<CommentRow>()
                    if (row.post_id != postId) return@collect
                    val profile = com.run.peakflow.data.cache.ProfileCache.get(row.user_id)
                        ?: try { getUser(row.user_id)?.also { com.run.peakflow.data.cache.ProfileCache.put(row.user_id, it) } } catch (_: Exception) { null }
                    trySend(
                        PostComment(
                            id = row.id,
                            postId = row.post_id,
                            userId = row.user_id,
                            userName = profile?.name ?: "Someone",
                            userAvatarUrl = profile?.avatarUrl,
                            content = row.content,
                            likesCount = 0,
                            createdAt = parseTimestamp(row.created_at)
                        )
                    )
                }
            }

            awaitClose {
                job.cancel()
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        client.realtime.removeChannel(channel)
                    } catch (_: Exception) { }
                }
            }
        }
    }

    override fun observeJoinRequests(communityId: String): Flow<JoinRequest> {
        return callbackFlow {
            val uniqueSuffix = kotlin.random.Random.nextLong(0, Long.MAX_VALUE).toString(16)
            val uniqueName = "join_requests_${communityId}_${uniqueSuffix}"
            val channel = client.channel(uniqueName)

            // Configure BEFORE subscribing
            val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "join_requests"
            }

            channel.subscribe()

            val job = launch {
                changeFlow.collect { action ->
                    val row = action.decodeRecord<JoinRequestRow>()
                    if (row.community_id != communityId) return@collect
                    trySend(row.toJoinRequest())
                }
            }

            awaitClose {
                job.cancel()
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        client.realtime.removeChannel(channel)
                    } catch (_: Exception) { }
                }
            }
        }
    }
}
