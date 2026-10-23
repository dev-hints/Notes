package com.strangeinfinity.notes.feature.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.strangeinfinity.notes.core.designsystem.LocalIsDarkTheme
import com.strangeinfinity.notes.core.designsystem.NoteColorPresets
import com.strangeinfinity.notes.core.designsystem.PinGoldDark
import com.strangeinfinity.notes.core.designsystem.PinGoldLight
import com.strangeinfinity.notes.core.designsystem.formatRelativeTimestamp
import com.strangeinfinity.notes.core.model.ChecklistItem
import com.strangeinfinity.notes.core.model.DrawingData
import com.strangeinfinity.notes.core.model.NoteWithDetails
import kotlinx.serialization.json.Json

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteCard(
    noteWithDetails: NoteWithDetails,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val note = noteWithDetails.note
    val darkTheme = LocalIsDarkTheme.current
    val preset = NoteColorPresets.getOrElse(note.colorIndex) { NoteColorPresets[0] }
    val containerColor = if (darkTheme) preset.darkContainer else preset.lightContainer
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else if (darkTheme) {
        preset.darkBorder
    } else {
        preset.lightBorder
    }

    // Parse checklist items if any
    val checklistItems = remember(note.checklistJson) {
        if (!note.checklistJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<List<ChecklistItem>>(note.checklistJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else emptyList()
    }

    // Parse drawing data if any
    val drawingData = remember(note.drawingJson) {
        if (!note.drawingJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<DrawingData>(note.drawingJson)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (darkTheme) 0.dp else 1.5.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Selection / Pin / Folder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Select Note",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (noteWithDetails.folder != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(noteWithDetails.folder.color).copy(alpha = if (darkTheme) 0.28f else 0.14f),
                        border = BorderStroke(1.dp, Color(noteWithDetails.folder.color).copy(alpha = if (darkTheme) 0.5f else 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(noteWithDetails.folder.color),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = noteWithDetails.folder.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (darkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (note.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned Note",
                        tint = if (darkTheme) PinGoldDark else PinGoldLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Note Title (High Contrast)
            if (note.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (darkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Content Snippet (High Contrast, Crisp & Premium)
            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (darkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
                    fontWeight = FontWeight.Normal,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            // Checklist preview
            if (checklistItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val completedCount = checklistItems.count { it.isCompleted }
                val totalCount = checklistItems.size
                val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (darkTheme) Color(0xFF818CF8) else MaterialTheme.colorScheme.primary,
                    trackColor = if (darkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$completedCount/$totalCount items completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (darkTheme) Color(0xFF94A3B8) else Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold
                )

                // Preview up to 2 items
                checklistItems.take(2).forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isCompleted) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = if (item.isCompleted) {
                                if (darkTheme) Color(0xFF34D399) else Color(0xFF059669) // Emerald
                            } else {
                                if (darkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            },
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodySmall ?: MaterialTheme.typography.bodyMedium,
                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (item.isCompleted) {
                                if (darkTheme) Color(0xFF64748B) else Color(0xFF94A3B8)
                            } else {
                                if (darkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            },
                            fontWeight = if (item.isCompleted) FontWeight.Normal else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Drawing Thumbnail Preview
            if (drawingData != null && drawingData.strokes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (darkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                ) {
                    Canvas(modifier = Modifier.matchParentSize().padding(4.dp)) {
                        drawingData.strokes.forEach { stroke ->
                            if (!stroke.isEraser && stroke.points.size > 1) {
                                val path = Path()
                                path.moveTo(stroke.points[0].x * 0.25f, stroke.points[0].y * 0.25f)
                                for (i in 1 until stroke.points.size) {
                                    path.lineTo(stroke.points[i].x * 0.25f, stroke.points[i].y * 0.25f)
                                }
                                drawPath(
                                    path = path,
                                    color = Color(stroke.color),
                                    style = Stroke(
                                        width = (stroke.strokeWidth * 0.4f).coerceAtLeast(1.5f),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Tags
            if (noteWithDetails.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    noteWithDetails.tags.take(3).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "#${tag.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Footer: Timestamp
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatRelativeTimestamp(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = if (darkTheme) Color(0xFF94A3B8) else Color(0xFF475569),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
