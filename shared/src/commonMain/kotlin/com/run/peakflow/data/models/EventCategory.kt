package com.run.peakflow.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class EventCategory(val displayName: String, val emoji: String) {
    RUNNING("Running", "🏃"),
    CALISTHENICS("Calisthenics", "💪"),
    TREKKING("Trekking", "🥾"),
    CYCLING("Cycling", "🚴"),
    KAYAKING("Kayaking", "🛶"),
    ROCK_CLIMBING("Rock Climbing", "🧗"),
    YOGA("Yoga", "🧘"),
    CROSSFIT("CrossFit", "🏋️"),
    SWIMMING("Swimming", "🏊"),
    ADVENTURE_SPORTS("Adventure Sports", "🪂"),
    OTHER("Other", "⭐")
}