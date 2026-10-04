package com.luauai.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.luauai.ui.screens.*

sealed class Screen(val route: String) {
    object Home       : Screen("home")
    object Chat       : Screen("chat/{projectId}") {
        fun withProject(id: Long) = "chat/$id"
    }
    object Projects   : Screen("projects")
    object Editor     : Screen("editor/{projectId}/{fileId}") {
        fun open(projectId: Long, fileId: Long) = "editor/$projectId/$fileId"
    }
    object Rules      : Screen("rules/{projectId}") {
        fun forProject(id: Long) = "rules/$id"
    }
    object Settings   : Screen("settings")
    object ModelSetup : Screen("model_setup")
}

@Composable
fun LuauAINavHost() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onOpenChat     = { projectId -> nav.navigate(Screen.Chat.withProject(projectId)) },
                onOpenProjects = { nav.navigate(Screen.Projects.route) },
                onOpenSettings = { nav.navigate(Screen.Settings.route) },
                onModelSetup   = { nav.navigate(Screen.ModelSetup.route) }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { back ->
            val projectId = back.arguments?.getLong("projectId") ?: 0L
            ChatScreen(
                projectId   = projectId,
                onOpenEditor = { fId -> nav.navigate(Screen.Editor.open(projectId, fId)) },
                onOpenRules  = { nav.navigate(Screen.Rules.forProject(projectId)) },
                onBack       = { nav.popBackStack() }
            )
        }

        composable(Screen.Projects.route) {
            ProjectsScreen(
                onOpenProject = { id -> nav.navigate(Screen.Chat.withProject(id)) },
                onBack        = { nav.popBackStack() }
            )
        }

        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.LongType },
                navArgument("fileId")    { type = NavType.LongType }
            )
        ) { back ->
            val projectId = back.arguments?.getLong("projectId") ?: 0L
            val fileId    = back.arguments?.getLong("fileId")    ?: 0L
            EditorScreen(
                projectId = projectId,
                fileId    = fileId,
                onBack    = { nav.popBackStack() }
            )
        }

        composable(
            route = Screen.Rules.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { back ->
            val projectId = back.arguments?.getLong("projectId") ?: 0L
            RulesScreen(
                projectId = projectId,
                onBack    = { nav.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onModelSetup = { nav.navigate(Screen.ModelSetup.route) },
                onBack       = { nav.popBackStack() }
            )
        }

        composable(Screen.ModelSetup.route) {
            ModelSetupScreen(
                onContinue = {
                    nav.navigate(Screen.Home.route) {
                        popUpTo(Screen.ModelSetup.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}
