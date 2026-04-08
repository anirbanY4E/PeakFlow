package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.run.peakflow.data.network.AuthenticationException
import com.run.peakflow.data.repository.AuthRepository
import com.run.peakflow.domain.usecases.GetCurrentUserUseCase
import com.run.peakflow.domain.usecases.GetUserAttendanceHistoryUseCase
import com.run.peakflow.domain.usecases.GetUserMembershipsUseCase
import com.run.peakflow.presentation.state.ProfileState
import com.run.peakflow.presentation.state.ProfileStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import com.arkivanov.essenty.lifecycle.doOnResume
import org.koin.core.component.inject

class ProfileComponent(
    componentContext: ComponentContext,
    private val onNavigateToSettings: () -> Unit,
    private val onNavigateToEditProfile: () -> Unit,
    private val onLogout: () -> Unit
) : ComponentContext by componentContext, KoinComponent {

    private val getCurrentUser: GetCurrentUserUseCase by inject()
    private val getUserMemberships: GetUserMembershipsUseCase by inject()
    private val getUserAttendanceHistory: GetUserAttendanceHistoryUseCase by inject()
    private val authRepository: AuthRepository by inject()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    // Guard: prevent double-load when init triggers both loadProfile() and doOnResume
    private var hasLoadedOnce = false

    init {
        lifecycle.doOnDestroy { scope.cancel() }
        // Load immediately on creation (needed for lazy-created components
        // where the lifecycle is already RESUMED)
        loadProfile()
        // Also reload on subsequent resumes (e.g. coming back from EditProfile)
        lifecycle.doOnResume {
            if (hasLoadedOnce) {
                loadProfile()
            }
        }
    }

    fun loadProfile() {
        scope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                // Parallelize all 3 independent calls
                val userDeferred = async { getCurrentUser() }
                val membershipsDeferred = async { getUserMemberships() }
                val attendanceDeferred = async { getUserAttendanceHistory() }

                val user = userDeferred.await()
                val memberships = membershipsDeferred.await()
                val attendance = attendanceDeferred.await()

                val stats = ProfileStats(
                    communitiesCount = memberships.size,
                    eventsAttended = attendance.size,
                    points = attendance.size * 10 // 10 points per event attended
                )

                _state.update {
                    it.copy(
                        isLoading = false,
                        user = user,
                        stats = stats,
                        interests = user?.interests ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                if (e is AuthenticationException) {
                    authRepository.handleAuthenticationError()
                }
                _state.update { it.copy(isLoading = false, error = e.message) }
            }

            hasLoadedOnce = true
        }
    }

    fun onSettingsClick() {
        onNavigateToSettings()
    }

    fun onEditProfileClick() {
        onNavigateToEditProfile()
    }

    fun onLogoutClick() {
        onLogout()
    }
}