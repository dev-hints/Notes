package com.strangeinfinity.notes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.strangeinfinity.notes.NotesApplication
import com.strangeinfinity.notes.feature.backup.BackupScreen
import com.strangeinfinity.notes.feature.backup.BackupViewModel
import com.strangeinfinity.notes.feature.drawing.DrawingScreen
import com.strangeinfinity.notes.feature.drawing.DrawingViewModel
import com.strangeinfinity.notes.feature.editor.EditorScreen
import com.strangeinfinity.notes.feature.editor.EditorViewModel
import com.strangeinfinity.notes.feature.folders.FoldersScreen
import com.strangeinfinity.notes.feature.folders.FoldersViewModel
import com.strangeinfinity.notes.feature.home.HomeScreen
import com.strangeinfinity.notes.feature.home.HomeViewModel
import com.strangeinfinity.notes.feature.settings.PrivacyScreen
import com.strangeinfinity.notes.feature.settings.SettingsScreen
import com.strangeinfinity.notes.feature.settings.SettingsViewModel
import com.strangeinfinity.notes.feature.trash.TrashScreen
import com.strangeinfinity.notes.feature.trash.TrashViewModel

@Composable
fun NotesNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current.applicationContext as NotesApplication
    val notesRepo = context.notesRepository
    val prefsRepo = context.preferencesRepository

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // HOME
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.provideFactory(notesRepo, prefsRepo)
            )
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToEditor = { noteId, folderId, type ->
                    navController.navigate(Screen.Editor.createRoute(noteId, folderId, type))
                },
                onNavigateToDrawing = { noteId ->
                    navController.navigate(Screen.Drawing.createRoute(noteId))
                },
                onNavigateToFolders = {
                    navController.navigate(Screen.Folders.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        // EDITOR
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType; defaultValue = "" },
                navArgument("folderId") { type = NavType.StringType; defaultValue = "" },
                navArgument("type") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val noteIdArg = backStackEntry.arguments?.getString("noteId")?.ifEmpty { null }
            val folderIdArg = backStackEntry.arguments?.getString("folderId")?.ifEmpty { null }
            val typeArg = backStackEntry.arguments?.getString("type")?.ifEmpty { null }

            val editorViewModel: EditorViewModel = viewModel(
                key = noteIdArg ?: "new_note_${folderIdArg}_$typeArg",
                factory = EditorViewModel.provideFactory(notesRepo, noteIdArg, folderIdArg, typeArg)
            )
            EditorScreen(
                viewModel = editorViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDrawing = { nid ->
                    navController.navigate(Screen.Drawing.createRoute(nid))
                }
            )
        }

        // DRAWING
        composable(
            route = Screen.Drawing.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val noteIdArg = backStackEntry.arguments?.getString("noteId")?.ifEmpty { null }
            val drawingViewModel: DrawingViewModel = viewModel(
                key = noteIdArg ?: "new_drawing",
                factory = DrawingViewModel.provideFactory(notesRepo, noteIdArg)
            )
            DrawingScreen(
                viewModel = drawingViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // FOLDERS
        composable(Screen.Folders.route) {
            val foldersViewModel: FoldersViewModel = viewModel(
                factory = FoldersViewModel.provideFactory(notesRepo)
            )
            FoldersScreen(
                viewModel = foldersViewModel,
                onNavigateBack = { navController.popBackStack() },
                onSelectFolder = { folderId ->
                    navController.navigate(Screen.Editor.createRoute(folderId = folderId))
                }
            )
        }

        // TRASH
        composable(Screen.Trash.route) {
            val trashViewModel: TrashViewModel = viewModel(
                factory = TrashViewModel.provideFactory(notesRepo)
            )
            TrashScreen(
                viewModel = trashViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // BACKUP & RESTORE
        composable(Screen.Backup.route) {
            val backupViewModel: BackupViewModel = viewModel(
                factory = BackupViewModel.provideFactory(context.backupManager)
            )
            BackupScreen(
                viewModel = backupViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // SETTINGS
        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.provideFactory(prefsRepo, notesRepo)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTrash = { navController.navigate(Screen.Trash.route) },
                onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                onNavigateToPrivacy = { navController.navigate(Screen.Privacy.route) }
            )
        }

        // PRIVACY
        composable(Screen.Privacy.route) {
            PrivacyScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
