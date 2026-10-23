package com.strangeinfinity.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.strangeinfinity.notes.core.designsystem.StrangeInfinityTheme
import com.strangeinfinity.notes.ui.navigation.NotesNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NotesApplication
        val preferencesRepository = app.preferencesRepository

        setContent {
            val themeConfig by preferencesRepository.themeConfig.collectAsState()

            StrangeInfinityTheme(themeConfig = themeConfig) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NotesNavHost(navController = navController)
                }
            }
        }
    }
}
