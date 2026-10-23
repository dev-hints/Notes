package com.strangeinfinity.notes.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Editor : Screen("editor?noteId={noteId}&folderId={folderId}&type={type}") {
        fun createRoute(noteId: String? = null, folderId: String? = null, type: String? = null): String {
            val n = noteId ?: ""
            val f = folderId ?: ""
            val t = type ?: ""
            return "editor?noteId=$n&folderId=$f&type=$t"
        }
    }
    object Drawing : Screen("drawing?noteId={noteId}") {
        fun createRoute(noteId: String? = null): String {
            val n = noteId ?: ""
            return "drawing?noteId=$n"
        }
    }
    object Folders : Screen("folders")
    object Trash : Screen("trash")
    object Backup : Screen("backup")
    object Settings : Screen("settings")
    object Privacy : Screen("privacy")
}
