package com.strangeinfinity.notes.feature.editor.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FormattingBar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrikethrough: () -> Unit,
    onHeading: () -> Unit,
    onBulletList: () -> Unit,
    onNumberedList: () -> Unit,
    onToggleChecklist: () -> Unit,
    onAddImage: () -> Unit,
    onOpenDrawing: () -> Unit,
    onToggleColorPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        val iconTint = MaterialTheme.colorScheme.onSurface

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = onBold, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onItalic, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onUnderline, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline", tint = iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onStrikethrough, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.FormatStrikethrough, contentDescription = "Strikethrough", tint = iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onHeading, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.FormatSize, contentDescription = "Heading", tint = iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onBulletList, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List", tint = iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onNumberedList, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List", tint = iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onToggleChecklist, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Checklist, contentDescription = "Checklist Mode", tint = iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onOpenDrawing, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Brush, contentDescription = "Handwriting / Drawing", tint = iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onAddImage, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Attach Image", tint = iconTint, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onToggleColorPicker, modifier = Modifier.size(38.dp)) {
                Icon(Icons.Default.Palette, contentDescription = "Note Color", tint = iconTint, modifier = Modifier.size(20.dp))
            }
        }
    }
}
