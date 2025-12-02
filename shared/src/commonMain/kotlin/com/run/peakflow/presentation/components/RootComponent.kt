package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
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
import kotlinx.serialization.Serializable

class RootComponent(
    componentContext: ComponentContext,
    private val createUserUseCase: CreateUserUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val isUserLoggedInUseCase: IsUserLoggedInUseCase,
    private val getNearbyCommunities: GetNearbyCommunities,
    private val getCommunityById: GetCommunityById,
    private val getCommunityEvents: GetCommunityEvents,
    private val getEventById: GetEventById,
    private val getEventRsvpStatus: GetEventRsvpStatus,
    private val getEventCheckInStatus: GetEventCheckInStatus,
    private val rsvpToEvent: RsvpToEvent,
    private val checkInToEvent: CheckInToEvent
) : ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val childStack: Value<ChildStack<*, Child>> = childStack(
        source = navigation,
        serializer = Config.serializer(),
        initialConfiguration = if (isUserLoggedInUseCase()) Config.Home else Config.Onboarding,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(config: Config, componentContext: ComponentContext): Child {
        return when (config) {
            is Config.Onboarding -> Child.Onboarding(
                OnboardingComponent(
                    componentContext = componentContext,
                    createUserUseCase = createUserUseCase,
                    onOnboardingComplete = { navigation.replaceAll(Config.Home) }
                )
            )
            is Config.Home -> Child.Home(
                HomeComponent(
                    componentContext = componentContext,
                    getNearbyCommunities = getNearbyCommunities,
                    onCommunitySelected = { community ->
                        navigation.push(Config.CommunityDetail(community.id))
                    }
                )
            )
            is Config.CommunityDetail -> Child.CommunityDetail(
                CommunityDetailComponent(
                    componentContext = componentContext,
                    communityId = config.communityId,
                    getCommunityById = getCommunityById,
                    getCommunityEvents = getCommunityEvents,
                    onEventSelected = { event ->
                        navigation.push(Config.EventDetail(event.id))
                    },
                    onBack = { navigation.pop() }
                )
            )
            is Config.EventDetail -> Child.EventDetail(
                EventDetailComponent(
                    componentContext = componentContext,
                    eventId = config.eventId,
                    getEventById = getEventById,
                    getCurrentUser = getCurrentUserUseCase,
                    getEventRsvpStatus = getEventRsvpStatus,
                    getEventCheckInStatus = getEventCheckInStatus,
                    rsvpToEvent = rsvpToEvent,
                    checkInToEvent = checkInToEvent,
                    onBack = { navigation.pop() }
                )
            )
        }
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object Onboarding : Config()

        @Serializable
        data object Home : Config()

        @Serializable
        data class CommunityDetail(val communityId: String) : Config()

        @Serializable
        data class EventDetail(val eventId: String) : Config()
    }

    sealed class Child {
        data class Onboarding(val component: OnboardingComponent) : Child()
        data class Home(val component: HomeComponent) : Child()
        data class CommunityDetail(val component: CommunityDetailComponent) : Child()
        data class EventDetail(val component: EventDetailComponent) : Child()
    }
}