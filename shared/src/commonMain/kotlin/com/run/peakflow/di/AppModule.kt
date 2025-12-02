package com.run.peakflow.di

import com.run.peakflow.data.network.ApiService
import com.run.peakflow.data.network.MockApiService
import com.run.peakflow.data.repository.CommunityRepository
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.UserRepository
import com.run.peakflow.domain.usecases.CheckInToEvent
import com.run.peakflow.domain.usecases.CreateUserUseCase
import com.run.peakflow.domain.usecases.GetCommunityById
import com.run.peakflow.domain.usecases.GetCommunityEvents
import com.run.peakflow.domain.usecases.GetCurrentUserUseCase
import com.run.peakflow.domain.usecases.GetEventById
import com.run.peakflow.domain.usecases.GetEventCheckInStatus
import com.run.peakflow.domain.usecases.GetEventRsvpStatus
import com.run.peakflow.domain.usecases.GetNearbyCommunities
import com.run.peakflow.domain.usecases.IsUserLoggedInUseCase
import com.run.peakflow.domain.usecases.RsvpToEvent
import org.koin.dsl.module

val appModule = module {

    // Network
    single<ApiService> { MockApiService() }

    // Repositories
    single { UserRepository(get()) }
    single { CommunityRepository(get()) }
    single { EventRepository(get()) }

    // Use Cases - User
    factory { CreateUserUseCase(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { IsUserLoggedInUseCase(get()) }

    // Use Cases - Community
    factory { GetNearbyCommunities(get()) }
    factory { GetCommunityById(get()) }
    factory { GetCommunityEvents(get()) }

    // Use Cases - Event
    factory { GetEventById(get()) }
    factory { GetEventRsvpStatus(get()) }
    factory { GetEventCheckInStatus(get()) }
    factory { RsvpToEvent(get()) }
    factory { CheckInToEvent(get()) }
}