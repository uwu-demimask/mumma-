package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Mumma")
    object Conversation : Screen("conversation", "Talk to Mumma")
    object StudyMode : Screen("study_mode", "Study Mode")
    object TeachMumma : Screen("teach_mumma", "Teach Mumma")
    object MemoryManager : Screen("memory_manager", "Memory System")
    object Settings : Screen("settings", "Settings")
}
