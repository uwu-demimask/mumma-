package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.navigation.Screen
import com.example.ui.screens.ConversationScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudyModeScreen
import com.example.ui.screens.TeachMummaScreen
import com.example.ui.theme.MummaTheme
import com.example.ui.viewmodel.MummaViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MummaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MummaTheme {
                MummaApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MummaApp(viewModel: MummaViewModel) {
    val navController = rememberNavController()

    // Request Audio Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            // Permission denied note
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val context = LocalContext.current

    // Collect Toast notifications
    LaunchedEffect(viewModel) {
        viewModel.toastEvent.collectLatest { message ->
            if (message.isNotBlank()) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToConversation = { navController.navigate(Screen.Conversation.route) },
                onNavigateToStudy = { navController.navigate(Screen.StudyMode.route) },
                onNavigateToMemories = { navController.navigate(Screen.MemoryManager.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Conversation.route) {
            ConversationScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToMemories = { navController.navigate(Screen.MemoryManager.route) }
            )
        }

        composable(Screen.StudyMode.route) {
            StudyModeScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToTeachMumma = { topic ->
                    viewModel.setSelectedStudyTopic(topic)
                    navController.navigate("${Screen.TeachMumma.route}/$topic")
                }
            )
        }

        composable(
            route = "${Screen.TeachMumma.route}/{topic}",
            arguments = listOf(navArgument("topic") { type = NavType.StringType })
        ) { backStackEntry ->
            val topic = backStackEntry.arguments?.getString("topic") ?: "Photosynthesis"
            TeachMummaScreen(
                topic = topic,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToChatWithQuestion = { question ->
                    viewModel.sendMessage(question, isStudyMode = true, studyTopic = topic)
                    navController.navigate(Screen.Conversation.route)
                }
            )
        }

        composable(Screen.MemoryManager.route) {
            MemoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
