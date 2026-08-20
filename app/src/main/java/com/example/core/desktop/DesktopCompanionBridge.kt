package com.example.core.desktop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionType {
    LOCAL_NETWORK,
    BLUETOOTH,
    USB,
    DISCONNECTED
}

data class DesktopCommand(
    val action: String, // "OPEN_APP", "START_FOCUS_MODE", "CREATE_WORKSPACE", "OPEN_FILE", "EXECUTE_SCRIPT"
    val target: String, // "VS Code", "Chrome", "biology_notes.md", etc.
    val parameters: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

data class DesktopCompanionStatus(
    val isConnected: Boolean = false,
    val connectionType: ConnectionType = ConnectionType.DISCONNECTED,
    val deviceName: String? = null,
    val activeWorkspace: String? = null,
    val latencyMs: Long = 0
)

interface DesktopCompanionBridge {
    val status: StateFlow<DesktopCompanionStatus>
    suspend fun connect(type: ConnectionType, address: String): Boolean
    suspend fun disconnect()
    suspend fun sendCommand(command: DesktopCommand): Result<String>
    fun parsePotentialDesktopIntent(userSpeech: String): DesktopCommand?
}

class DesktopCompanionBridgeImpl : DesktopCompanionBridge {
    private val _status = MutableStateFlow(
        DesktopCompanionStatus(
            isConnected = false,
            connectionType = ConnectionType.DISCONNECTED,
            deviceName = "Windows Desktop (Ready for Pairing)"
        )
    )
    override val status: StateFlow<DesktopCompanionStatus> = _status.asStateFlow()

    override suspend fun connect(type: ConnectionType, address: String): Boolean {
        _status.value = DesktopCompanionStatus(
            isConnected = true,
            connectionType = type,
            deviceName = "Host Desktop ($address)",
            activeWorkspace = "Default Study Workspace",
            latencyMs = 12
        )
        return true
    }

    override suspend fun disconnect() {
        _status.value = DesktopCompanionStatus(
            isConnected = false,
            connectionType = ConnectionType.DISCONNECTED
        )
    }

    override suspend fun sendCommand(command: DesktopCommand): Result<String> {
        return if (_status.value.isConnected) {
            Result.success("Dispatched '${command.action}' for '${command.target}' to Desktop")
        } else {
            Result.failure(IllegalStateException("Desktop Companion is not currently connected"))
        }
    }

    override fun parsePotentialDesktopIntent(userSpeech: String): DesktopCommand? {
        val lower = userSpeech.lowercase()
        return when {
            lower.startsWith("open ") -> {
                val app = userSpeech.substring(5).trim()
                DesktopCommand("OPEN_APP", app)
            }
            lower.contains("focus mode") -> {
                DesktopCommand("START_FOCUS_MODE", "Pomodoro Workspace")
            }
            lower.contains("study workspace") || lower.contains("workspace for") -> {
                DesktopCommand("CREATE_WORKSPACE", "Study Suite")
            }
            lower.contains("open my ") && (lower.contains("notes") || lower.contains("pdf") || lower.contains("file")) -> {
                val file = userSpeech.substringAfter("open my ").trim()
                DesktopCommand("OPEN_FILE", file)
            }
            else -> null
        }
    }
}
