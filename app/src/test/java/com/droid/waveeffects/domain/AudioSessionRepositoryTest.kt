package com.droid.waveeffects.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AudioSessionRepositoryTest {

    private lateinit var repository: AudioSessionRepository

    @Before
    fun setUp() {
        repository = AudioSessionRepository()
    }

    @Test
    fun defaultSession_isGlobalSessionZero() {
        val selected = repository.selectedSession.value
        assertNotNull(selected)
        assertEquals(0, selected?.sessionId)
        assertEquals("Global Output", selected?.appName)
    }

    @Test
    fun addSession_addsToActiveSessionsList() {
        val session1 = AudioSessionInfo(
            sessionId = 42,
            packageName = "com.spotify.music",
            appName = "Spotify"
        )
        repository.addSession(session1)

        val sessions = repository.activeSessions.value
        assertEquals(2, sessions.size) // Global session 0 + Spotify
        assertTrue(sessions.any { it.sessionId == 42 && it.appName == "Spotify" })
    }

    @Test
    fun selectSession_updatesCurrentSession() {
        val session1 = AudioSessionInfo(
            sessionId = 42,
            packageName = "com.spotify.music",
            appName = "Spotify"
        )
        repository.addSession(session1)
        repository.selectSession(42)

        assertEquals(42, repository.selectedSession.value?.sessionId)
    }

    @Test
    fun removeSession_resetsToGlobalIfActiveRemoved() {
        val session1 = AudioSessionInfo(
            sessionId = 42,
            packageName = "com.spotify.music",
            appName = "Spotify"
        )
        repository.addSession(session1)
        repository.selectSession(42)
        assertEquals(42, repository.selectedSession.value?.sessionId)

        repository.removeSession(42)
        assertEquals(1, repository.activeSessions.value.size)
        assertEquals(0, repository.selectedSession.value?.sessionId)
    }
}
