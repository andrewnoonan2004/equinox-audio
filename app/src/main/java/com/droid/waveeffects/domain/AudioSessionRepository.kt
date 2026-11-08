package com.droid.waveeffects.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioSessionRepository {

    private val globalSession = AudioSessionInfo(
        sessionId = 0,
        packageName = "android.media",
        appName = "Global Output"
    )

    private val _activeSessions = MutableStateFlow<List<AudioSessionInfo>>(listOf(globalSession))
    val activeSessions: StateFlow<List<AudioSessionInfo>> = _activeSessions.asStateFlow()

    private val _selectedSession = MutableStateFlow<AudioSessionInfo?>(globalSession)
    val selectedSession: StateFlow<AudioSessionInfo?> = _selectedSession.asStateFlow()

    fun addSession(session: AudioSessionInfo) {
        val current = _activeSessions.value.toMutableList()
        val index = current.indexOfFirst { it.sessionId == session.sessionId }
        if (index >= 0) {
            current[index] = session
        } else {
            current.add(session)
        }
        _activeSessions.value = current
    }

    fun removeSession(sessionId: Int) {
        if (sessionId == 0) return // Never remove global session
        val current = _activeSessions.value.toMutableList()
        current.removeAll { it.sessionId == sessionId }
        _activeSessions.value = current

        if (_selectedSession.value?.sessionId == sessionId) {
            _selectedSession.value = globalSession
        }
    }

    fun selectSession(sessionId: Int) {
        val target = _activeSessions.value.find { it.sessionId == sessionId } ?: globalSession
        _selectedSession.value = target
    }
}
