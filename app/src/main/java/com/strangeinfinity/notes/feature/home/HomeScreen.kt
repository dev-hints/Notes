package com.strangeinfinity.notes.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeinfinity.notes.core.designsystem.StrangeInfinityBrandLogo
import com.strangeinfinity.notes.core.model.LayoutMode
import com.strangeinfinity.notes.feature.home.components.HomeFilterChips
import com.strangeinfinity.notes.feature.home.components.HomeTopBar
import com.strangeinfinity.notes.feature.home.components.NoteCard

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToEditor: (String?, String?, String?) -> Unit, // noteId, folderId, initialType
    onNavigateToDrawing: (String?) -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                isSearchActive = uiState.isSearchActive,
                onToggleSearch = viewModel::onToggleSearch,
                layoutMode = uiState.layoutMode,
                onToggleLayoutMode = viewModel::onToggleLayoutMode,
                sortOrder = uiState.sortOrder,
                onSelectSortOrder = viewModel::onSelectSortOrder,
                onNavigateFolders = onNavigateToFolders,
                onNavigateSettings = onNavigateToSettings,
                isSelectionMode = uiState.isSelectionMode,
                selectedCount = uiState.selectedNoteIds.size,
                onSelectAll = viewModel::onSelectAll,
                onClearSelection = viewModel::onClearSelection,
                onBatchPin = viewModel::onBatchPin,
                onBatchMoveFolder = { viewModel.showMoveFolderDialog(true) },
                onBatchDelete = viewModel::onBatchDelete
            )
        },
        floatingActionButton = {
            if (!uiState.isSelectionMode) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedVisibility(
                        visible = isFabMenuExpanded,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Drawing Note Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        "Drawing Note",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                SmallFloatingActionButton(
                                    onClick = {
                                        isFabMenuExpanded = false
                                        onNavigateToDrawing(null)
                                    },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Icon(Icons.Default.Draw, contentDescription = "New Drawing")
                                }
                            }

                            // Checklist Note Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        "Checklist Note",
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                SmallFloatingActionButton(
                                    onClick = {
                                        isFabMenuExpanded = false
                                        onNavigateToEditor(null, null, "checklist")
                                    },
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Icon(Icons.Default.Checklist, contentDescription = "New Checklist")
                                }
                            }
                        }
                    }

                    // Main FAB
                    FloatingActionButton(
                        onClick = {
                            if (isFabMenuExpanded) {
                                isFabMenuExpanded = false
                            } else {
                                onNavigateToEditor(null, null, "text")
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isFabMenuExpanded) Icons.Default.Add else Icons.Default.EditNote,
                            contentDescription = "New Note",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter Chips Row
            HomeFilterChips(
                selectedFilter = uiState.selectedFilter,
                onSelectFilter = viewModel::onSelectFilter,
                folders = uiState.folders,
                tags = uiState.tags
            )

            // Content Area
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize()
                )
            } else if (uiState.notes.isEmpty()) {
                // Empty States
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.searchQuery.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No notes found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No notes match \"${uiState.searchQuery}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            StrangeInfinityBrandLogo(size = 64)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "Capture what matters",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create rich notes, interactive checklists, or hand-drawn sketches. Fully local and private.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { onNavigateToEditor(null, null, "text") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create First Note")
                            }
                        }
                    }
                }
            } else {
                // Responsive Grid or Centered List View for phones & tablets
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (uiState.layoutMode == LayoutMode.GRID) {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Adaptive(minSize = 175.dp),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalItemSpacing = 12.dp,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.notes, key = { it.note.id }) { item ->
                                NoteCard(
                                    noteWithDetails = item,
                                    isSelected = uiState.selectedNoteIds.contains(item.note.id),
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = {
                                        viewModel.onNoteClick(item.note.id) { noteId ->
                                            onNavigateToEditor(noteId, null, null)
                                        }
                                    },
                                    onLongClick = { viewModel.onNoteLongClick(item.note.id) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = if (androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600) 64.dp else 0.dp)
                        ) {
                            items(uiState.notes, key = { it.note.id }) { item ->
                                NoteCard(
                                    noteWithDetails = item,
                                    isSelected = uiState.selectedNoteIds.contains(item.note.id),
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = {
                                        viewModel.onNoteClick(item.note.id) { noteId ->
                                            onNavigateToEditor(noteId, null, null)
                                        }
                                    },
                                    onLongClick = { viewModel.onNoteLongClick(item.note.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Move to folder dialog
    if (uiState.isMoveFolderDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.showMoveFolderDialog(false) },
            title = { Text("Move to Folder") },
            text = {
                Column {
                    // Option: Unfiled (No folder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.onBatchMoveToFolder(null) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("No Folder (Unfiled)")
                    }

                    uiState.folders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.onBatchMoveToFolder(folder.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Color(folder.color))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(folder.name, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.showMoveFolderDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }
}
