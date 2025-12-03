package com.run.peakflow.presentation.state

data class NotificationsState(
    val notifications: List<NotificationItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val actionData: String? = null
)

enum class NotificationType {
    NEW_POST,
    EVENT_REMINDER,
    JOIN_REQUEST_APPROVED,
    JOIN_REQUEST_REJECTED,
    NEW_JOIN_REQUEST,
    LIKE,
    COMMENT,
    POINTS_EARNED
}