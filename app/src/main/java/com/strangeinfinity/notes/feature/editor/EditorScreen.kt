package com.strangeinfinity.notes.feature.editor

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeinfinity.notes.core.designsystem.ConfirmDialog
import com.strangeinfinity.notes.core.designsystem.LocalIsDarkTheme
import com.strangeinfinity.notes.core.designsystem.NoteColorPicker
import com.strangeinfinity.notes.core.designsystem.NoteColorPresets
import com.strangeinfinity.notes.core.designsystem.PinGoldDark
import com.strangeinfinity.notes.core.designsystem.PinGoldLight
import com.strangeinfinity.notes.core.designsystem.formatRelativeTimestamp
import com.strangeinfinity.notes.core.model.DrawingData
import com.strangeinfinity.notes.feature.editor.components.AttachmentStrip
import com.strangeinfinity.notes.feature.editor.components.ChecklistEditor
import com.strangeinfinity.notes.feature.editor.components.FormattingBar
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDrawing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val darkTheme = LocalIsDarkTheme.current

    // Autosave on disposal (leaving screen or back)
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                viewModel.saveImmediately()
            }
        }
    }

    val handleBack: () -> Unit = {
        scope.launch {
            viewModel.saveImmediately()
            onNavigateBack()
        }
    }

    BackHandler {
        handleBack()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onAddAttachmentUri(uri)
        }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    val preset = NoteColorPresets.getOrElse(uiState.colorIndex) { NoteColorPresets[0] }
    val editorBackground = if (darkTheme) preset.darkContainer else preset.lightContainer

    // Drawing data
    val drawingData = remember(uiState.drawingJson) {
        if (!uiState.drawingJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<DrawingData>(uiState.drawingJson!!)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        containerColor = editorBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = editorBackground,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    val statusText = when (uiState.saveStatus) {
                        SaveStatus.SAVED -> "Saved"
                        SaveStatus.SAVING -> "Saving..."
                        SaveStatus.ERROR -> "Save error"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                navigationIcon = {
                    IconButton(onClick = handleBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::undo,
                        enabled = uiState.canUndo
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (uiState.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(
                        onClick = viewModel::redo,
                        enabled = uiState.canRedo
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (uiState.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(onClick = viewModel::onTogglePin) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (uiState.isPinned) {
                                if (darkTheme) PinGoldDark else PinGoldLight
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    IconButton(onClick = { viewModel.toggleColorPicker(!uiState.isColorPickerOpen) }) {
                        Icon(
                            Icons.Default.Palette,
                            contentDescription = "Note Color",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Overflow Menu
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Move to Folder") },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showFolderDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Tag") },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.toggleAddTagDialog(true)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate Note") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                viewModel.duplicateNote { handleBack() }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Note Info") },
                            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showStatsDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                // Color Picker Drawer
                AnimatedVisibility(
                    visible = uiState.isColorPickerOpen,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        NoteColorPicker(
                            selectedColorIndex = uiState.colorIndex,
                            onColorSelected = viewModel::onSelectColorIndex,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // Formatting Bar
                FormattingBar(
                    onBold = viewModel::applyBold,
                    onItalic = viewModel::applyItalic,
                    onUnderline = viewModel::applyUnderline,
                    onStrikethrough = viewModel::applyStrikethrough,
                    onHeading = viewModel::applyHeading,
                    onBulletList = viewModel::applyBulletList,
                    onNumberedList = viewModel::applyNumberedList,
                    onToggleChecklist = viewModel::toggleChecklistMode,
                    onAddImage = { imagePickerLauncher.launch("image/*") },
                    onOpenDrawing = { onNavigateToDrawing(uiState.noteId) },
                    onToggleColorPicker = { viewModel.toggleColorPicker(!uiState.isColorPickerOpen) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
            // Folder and Tag Metadata Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Folder Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (uiState.folder != null) {
                        Color(uiState.folder!!.color).copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.clickable { showFolderDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = if (uiState.folder != null) Color(uiState.folder!!.color) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.folder?.name ?: "No Folder",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Tag Chips
                uiState.tags.forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${tag.name}",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove tag",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.onRemoveTag(tag.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Note Title
            TextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Title",
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                    )
                },
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (darkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Attachments Strip
            AttachmentStrip(
                attachments = uiState.attachments,
                onRemoveAttachment = viewModel::onRemoveAttachment
            )

            // Drawing Thumbnail
            if (drawingData != null && drawingData.strokes.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigateToDrawing(uiState.noteId) },
                    color = if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Draw, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Handwritten Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("Edit Drawing", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                drawingData.strokes.forEach { stroke ->
                                    if (!stroke.isEraser && stroke.points.size > 1) {
                                        val path = Path()
                                        path.moveTo(stroke.points[0].x * 0.4f, stroke.points[0].y * 0.4f)
                                        for (i in 1 until stroke.points.size) {
                                            path.lineTo(stroke.points[i].x * 0.4f, stroke.points[i].y * 0.4f)
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color(stroke.color),
                                            style = Stroke(
                                                width = stroke.strokeWidth * 0.5f,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Body content or Checklist
            if (uiState.isChecklistMode) {
                ChecklistEditor(
                    items = uiState.checklistItems,
                    onToggleItem = viewModel::onToggleChecklistItem,
                    onUpdateItemText = viewModel::onUpdateChecklistItemText,
                    onDeleteItem = viewModel::onDeleteChecklistItem,
                    onAddItem = viewModel::onAddChecklistItem
                )
            } else {
                TextField(
                    value = uiState.content,
                    onValueChange = viewModel::onContentChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    placeholder = {
                        Text(
                            "Type your note here. Format with bold, lists, and headings using the toolbar below.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = if (darkTheme) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                        fontWeight = FontWeight.Normal,
                        lineHeight = 24.sp
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

    // Dialog: Folder Chooser
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Assign to Folder") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.onSelectFolder(null)
                                showFolderDialog = false
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("No Folder (Unfiled)")
                    }

                    uiState.availableFolders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.onSelectFolder(folder.id)
                                    showFolderDialog = false
                                }
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
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Dialog: Add Tag
    if (uiState.isAddTagDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.toggleAddTagDialog(false) },
            title = { Text("Add Tag") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("Tag name (e.g. work, ideas)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAddTag(newTagInput)
                        newTagInput = ""
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleAddTagDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Stats
    if (showStatsDialog) {
        val dateFormat = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { showStatsDialog = false },
            title = { Text("Note Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Words: ${uiState.wordCount}", fontWeight = FontWeight.Medium)
                    Text("Characters: ${uiState.charCount}", fontWeight = FontWeight.Medium)
                    Text("Created: ${dateFormat.format(Date(uiState.createdAt))}", style = MaterialTheme.typography.bodySmall)
                    Text("Modified: ${dateFormat.format(Date(uiState.updatedAt))}", style = MaterialTheme.typography.bodySmall)
                    Text("ID: ${uiState.noteId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatsDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog: Delete Confirmation
    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete Note?",
            message = "This note will be moved to the Trash. You can restore it anytime.",
            confirmText = "Delete",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteNote { onNavigateBack() }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
