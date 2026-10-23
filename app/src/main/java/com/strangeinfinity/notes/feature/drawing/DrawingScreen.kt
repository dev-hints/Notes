package com.strangeinfinity.notes.feature.drawing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.strangeinfinity.notes.core.designsystem.ConfirmDialog
import com.strangeinfinity.notes.core.designsystem.LocalIsDarkTheme
import com.strangeinfinity.notes.feature.drawing.components.DrawingCanvas
import com.strangeinfinity.notes.feature.drawing.components.DrawingToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawingScreen(
    viewModel: DrawingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val darkTheme = LocalIsDarkTheme.current
    val canvasBg = if (darkTheme) Color(0xFF111827) else Color(0xFFFFFFFF)

    var showClearDialog by remember { mutableStateOf(false) }

    val handleBack: () -> Unit = {
        viewModel.saveDrawing {
            onNavigateBack()
        }
    }

    BackHandler {
        handleBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Handwriting & Sketch") },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.saveDrawing {
                                onNavigateBack()
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save Drawing",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = canvasBg
                )
            )
        },
        bottomBar = {
            DrawingToolbar(
                selectedColor = uiState.selectedColor,
                onColorSelected = viewModel::onColorSelected,
                selectedWidth = uiState.selectedWidth,
                onWidthSelected = viewModel::onWidthSelected,
                isEraser = uiState.isEraser,
                onToggleEraser = viewModel::onToggleEraser,
                canUndo = uiState.canUndo,
                onUndo = viewModel::undo,
                canRedo = uiState.canRedo,
                onRedo = viewModel::redo,
                onClear = { showClearDialog = true },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DrawingCanvas(
                strokes = uiState.strokes,
                currentStrokeColor = uiState.selectedColor,
                currentStrokeWidth = uiState.selectedWidth,
                isEraser = uiState.isEraser,
                canvasBackgroundColor = canvasBg,
                onStrokeFinished = viewModel::onAddStroke
            )
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "Clear Canvas?",
            message = "This will erase all strokes from this drawing. You can undo this action.",
            confirmText = "Clear",
            isDestructive = true,
            onConfirm = {
                showClearDialog = false
                viewModel.clearCanvas()
            },
            onDismiss = { showClearDialog = false }
        )
    }
}
