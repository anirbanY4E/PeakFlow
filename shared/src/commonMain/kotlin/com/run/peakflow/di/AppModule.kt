package com.run.peakflow.di

import com.run.peakflow.data.auth.GoogleAuthProvider
import com.run.peakflow.data.network.ApiService
import com.run.peakflow.data.network.MockApiService
import com.run.peakflow.data.repository.AuthRepository
import com.run.peakflow.data.repository.CommunityRepository
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.InviteRepository
import com.run.peakflow.data.repository.MembershipRepository
import com.run.peakflow.data.repository.PostRepository
import com.run.peakflow.data.repository.UserRepository
import com.run.peakflow.domain.usecases.AddCommentUseCase
import com.run.peakflow.domain.usecases.ApproveJoinRequestUseCase
import com.run.peakflow.domain.usecases.CancelRsvpUseCase
import com.run.peakflow.domain.usecases.CheckInToEvent
import com.run.peakflow.domain.usecases.CompleteProfileUseCase
import com.run.peakflow.domain.usecases.CreateEventUseCase
import com.run.peakflow.domain.usecases.CreatePostUseCase
import com.run.peakflow.domain.usecases.GenerateInviteCodeUseCase
import com.run.peakflow.domain.usecases.GetAllUserEventsUseCase
import com.run.peakflow.domain.usecases.GetCommunityById
import com.run.peakflow.domain.usecases.GetCommunityEvents
import com.run.peakflow.domain.usecases.GetCommunityMembersUseCase
import com.run.peakflow.domain.usecases.GetCommunityPostsUseCase
import com.run.peakflow.domain.usecases.GetCurrentUserUseCase
import com.run.peakflow.domain.usecases.GetDiscoverCommunitiesUseCase
import com.run.peakflow.domain.usecases.GetEventById
import com.run.peakflow.domain.usecases.GetEventCheckInStatus
import com.run.peakflow.domain.usecases.GetEventRsvpStatus
import com.run.peakflow.domain.usecases.GetFeedPostsUseCase
import com.run.peakflow.domain.usecases.GetNearbyEventsUseCase
import com.run.peakflow.domain.usecases.GetNearbyCommunities
import com.run.peakflow.domain.usecases.GetPendingJoinRequestsUseCase
import com.run.peakflow.domain.usecases.GetPostByIdUseCase
import com.run.peakflow.domain.usecases.GetPostCommentsUseCase
import com.run.peakflow.domain.usecases.GetUserAttendanceHistoryUseCase
import com.run.peakflow.domain.usecases.GetUserByIdUseCase
import com.run.peakflow.domain.usecases.GetUserCommunitiesUseCase
import com.run.peakflow.domain.usecases.GetUserInviteCodesUseCase
import com.run.peakflow.domain.usecases.GetUserMembershipsUseCase
import com.run.peakflow.domain.usecases.GetUserRoleInCommunityUseCase
import com.run.peakflow.domain.usecases.GetUserRsvpsUseCase
import com.run.peakflow.domain.usecases.HasUserLikedPostUseCase
import com.run.peakflow.domain.usecases.HasUserRequestedToJoinUseCase
import com.run.peakflow.domain.usecases.IsUserLoggedInUseCase
import com.run.peakflow.domain.usecases.JoinCommunityViaInviteUseCase
import com.run.peakflow.domain.usecases.LikePostUseCase
import com.run.peakflow.domain.usecases.LogoutUseCase
import com.run.peakflow.domain.usecases.RejectJoinRequestUseCase
import com.run.peakflow.domain.usecases.RequestToJoinCommunityUseCase
import com.run.peakflow.domain.usecases.ResendOtpUseCase
import com.run.peakflow.domain.usecases.RsvpToEvent
import com.run.peakflow.domain.usecases.SignInUseCase
import com.run.peakflow.domain.usecases.SignInWithGoogleUseCase
import com.run.peakflow.domain.usecases.SignUpUseCase
import com.run.peakflow.domain.usecases.ValidateInviteCodeUseCase
import com.run.peakflow.domain.usecases.VerifyOtpUseCase
import com.run.peakflow.presentation.components.RootComponentFactory
import org.koin.dsl.module

expect fun platformGoogleAuthProvider(): GoogleAuthProvider

val appModule = module {

    // ==================== NETWORK ====================

    single<ApiService> { MockApiService() }

    // ==================== AUTH PROVIDERS ====================

    single<GoogleAuthProvider> { platformGoogleAuthProvider() }

    // ==================== REPOSITORIES ====================

    single { UserRepository(get()) }
    single { AuthRepository(get(), get()) }
    single { InviteRepository(get()) }
    single { MembershipRepository(get()) }
    single { PostRepository(get()) }
    single { CommunityRepository(get()) }
    single { EventRepository(get()) }

    // ==================== USE CASES: AUTH ====================

    factory { SignUpUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignInWithGoogleUseCase(get(), get()) }
    factory { VerifyOtpUseCase(get()) }
    factory { ResendOtpUseCase(get()) }
    factory { LogoutUseCase(get()) }

    // ==================== USE CASES: USER ====================

    factory { GetCurrentUserUseCase(get()) }
    factory { IsUserLoggedInUseCase(get()) }
    factory { CompleteProfileUseCase(get(), get()) }
    factory { GetUserByIdUseCase(get()) }

    // ==================== USE CASES: INVITE ====================

    factory { ValidateInviteCodeUseCase(get()) }
    factory { JoinCommunityViaInviteUseCase(get(), get()) }
    factory { GenerateInviteCodeUseCase(get(), get()) }
    factory { GetUserInviteCodesUseCase(get(), get()) }

    // ==================== USE CASES: MEMBERSHIP ====================

    factory { GetUserMembershipsUseCase(get(), get()) }
    factory { GetCommunityMembersUseCase(get()) }
    factory { GetUserRoleInCommunityUseCase(get(), get()) }
    factory { RequestToJoinCommunityUseCase(get(), get()) }
    factory { GetPendingJoinRequestsUseCase(get()) }
    factory { ApproveJoinRequestUseCase(get(), get()) }
    factory { RejectJoinRequestUseCase(get(), get()) }
    factory { HasUserRequestedToJoinUseCase(get(), get()) }

    // ==================== USE CASES: COMMUNITY ====================

    factory { GetNearbyCommunities(get()) }
    factory { GetUserCommunitiesUseCase(get(), get(), get()) }
    factory { GetDiscoverCommunitiesUseCase(get(), get(), get()) }
    factory { GetCommunityById(get()) }

    // ==================== USE CASES: POST ====================

    factory { GetFeedPostsUseCase(get(), get(), get()) }
    factory { GetCommunityPostsUseCase(get()) }
    factory { GetPostByIdUseCase(get()) }
    factory { CreatePostUseCase(get(), get(), get()) }
    factory { LikePostUseCase(get(), get()) }
    factory { HasUserLikedPostUseCase(get(), get()) }

    // ==================== USE CASES: COMMENT ====================

    factory { GetPostCommentsUseCase(get()) }
    factory { AddCommentUseCase(get(), get()) }

    // ==================== USE CASES: EVENT ====================

    factory { CreateEventUseCase(get(), get(), get()) }
    factory { GetCommunityEvents(get()) }
    factory { GetEventById(get()) }
    factory { GetNearbyEventsUseCase(get(), get(), get()) }
    factory { GetAllUserEventsUseCase(get(), get(), get()) }
    factory { RsvpToEvent(get(), get()) }
    factory { CancelRsvpUseCase(get(), get()) }
    factory { GetEventRsvpStatus(get(), get()) }
    factory { GetUserRsvpsUseCase(get(), get()) }
    factory { CheckInToEvent(get(), get()) }
    factory { GetEventCheckInStatus(get(), get()) }
    factory { GetUserAttendanceHistoryUseCase(get(), get()) }

    // ==================== COMPONENT FACTORIES ====================

    factory { RootComponentFactory() }
}