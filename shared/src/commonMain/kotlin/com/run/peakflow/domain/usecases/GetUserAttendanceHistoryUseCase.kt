package com.run.peakflow.domain.usecases

import com.run.peakflow.data.models.Attendance
import com.run.peakflow.data.repository.EventRepository
import com.run.peakflow.data.repository.UserRepository

class GetUserAttendanceHistoryUseCase(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): List<Attendance> {
        val userId = userRepository.getCurrentUserId() ?: return emptyList()
        return eventRepository.getUserAttendanceHistory(userId)
    }
}