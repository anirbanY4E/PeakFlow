package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
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
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RootComponentFactory : KoinComponent {

    private val createUserUseCase: CreateUserUseCase by inject()
    private val getCurrentUserUseCase: GetCurrentUserUseCase by inject()
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase by inject()
    private val getNearbyCommunities: GetNearbyCommunities by inject()
    private val getCommunityById: GetCommunityById by inject()
    private val getCommunityEvents: GetCommunityEvents by inject()
    private val getEventById: GetEventById by inject()
    private val getEventRsvpStatus: GetEventRsvpStatus by inject()
    private val getEventCheckInStatus: GetEventCheckInStatus by inject()
    private val rsvpToEvent: RsvpToEvent by inject()
    private val checkInToEvent: CheckInToEvent by inject()

    fun create(componentContext: ComponentContext): RootComponent {
        return RootComponent(
            componentContext = componentContext,
            createUserUseCase = createUserUseCase,
            getCurrentUserUseCase = getCurrentUserUseCase,
            isUserLoggedInUseCase = isUserLoggedInUseCase,
            getNearbyCommunities = getNearbyCommunities,
            getCommunityById = getCommunityById,
            getCommunityEvents = getCommunityEvents,
            getEventById = getEventById,
            getEventRsvpStatus = getEventRsvpStatus,
            getEventCheckInStatus = getEventCheckInStatus,
            rsvpToEvent = rsvpToEvent,
            checkInToEvent = checkInToEvent
        )
    }
}