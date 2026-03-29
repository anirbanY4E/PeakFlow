package com.run.peakflow.presentation.state

import com.run.peakflow.data.models.EventCategory

data class EditProfileState(
    val name: String = "",
    val city: String = "",
    val interests: List<EventCategory> = emptyList(),
    val avatarBytes: ByteArray? = null,
    val currentAvatarUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EditProfileState) return false
        return name == other.name &&
            city == other.city &&
            interests == other.interests &&
            (avatarBytes contentEquals other.avatarBytes) &&
            currentAvatarUrl == other.currentAvatarUrl &&
            isLoading == other.isLoading &&
            error == other.error &&
            isSuccess == other.isSuccess
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + city.hashCode()
        result = 31 * result + interests.hashCode()
        result = 31 * result + (avatarBytes?.contentHashCode() ?: 0)
        result = 31 * result + (currentAvatarUrl?.hashCode() ?: 0)
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (error?.hashCode() ?: 0)
        result = 31 * result + isSuccess.hashCode()
        return result
    }
}