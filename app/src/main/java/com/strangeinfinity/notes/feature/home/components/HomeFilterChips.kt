package com.strangeinfinity.notes.feature.home.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.strangeinfinity.notes.core.designsystem.LocalIsDarkTheme
import com.strangeinfinity.notes.core.designsystem.PinGoldDark
import com.strangeinfinity.notes.core.designsystem.PinGoldLight
import com.strangeinfinity.notes.core.model.Folder
import com.strangeinfinity.notes.core.model.Tag

sealed class HomeFilter {
    object All : HomeFilter()
    object Pinned : HomeFilter()
    data class ByFolder(val folder: Folder) : HomeFilter()
    data class ByTag(val tag: Tag) : HomeFilter()
}

@Composable
fun HomeFilterChips(
    selectedFilter: HomeFilter,
    onSelectFilter: (HomeFilter) -> Unit,
    folders: List<Folder>,
    tags: List<Tag>,
    modifier: Modifier = Modifier
) {
    val darkTheme = LocalIsDarkTheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        val isAllSelected = selectedFilter is HomeFilter.All
        FilterChip(
            selected = isAllSelected,
            onClick = { onSelectFilter(HomeFilter.All) },
            label = {
                Text(
                    "All",
                    fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                selectedBorderColor = MaterialTheme.colorScheme.primary,
                enabled = true,
                selected = isAllSelected
            )
        )

        // "Pinned" chip
        val isPinnedSelected = selectedFilter is HomeFilter.Pinned
        FilterChip(
            selected = isPinnedSelected,
            onClick = { onSelectFilter(HomeFilter.Pinned) },
            label = {
                Text(
                    "Pinned",
                    fontWeight = if (isPinnedSelected) FontWeight.Bold else FontWeight.Medium
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.PushPin,
                    contentDescription = null,
                    tint = if (isPinnedSelected) {
                        if (darkTheme) Color(0xFFFBBF24) else Color(0xFFFFFFFF)
                    } else {
                        if (darkTheme) PinGoldDark else PinGoldLight
                    },
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = if (darkTheme) Color(0xFF452B09) else Color(0xFFFEF3C7),
                selectedLabelColor = if (darkTheme) Color(0xFFFDE68A) else Color(0xFF92400E)
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                selectedBorderColor = if (darkTheme) PinGoldDark else PinGoldLight,
                enabled = true,
                selected = isPinnedSelected
            )
        )

        // Folders
        folders.forEach { folder ->
            val isSelected = selectedFilter is HomeFilter.ByFolder && selectedFilter.folder.id == folder.id
            FilterChip(
                selected = isSelected,
                onClick = { onSelectFilter(HomeFilter.ByFolder(folder)) },
                label = {
                    Text(
                        folder.name,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = Color(folder.color),
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = Color(folder.color).copy(alpha = if (darkTheme) 0.35f else 0.18f),
                    selectedLabelColor = if (darkTheme) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = Color(folder.color),
                    enabled = true,
                    selected = isSelected
                )
            )
        }

        // Tags
        tags.forEach { tag ->
            val isSelected = selectedFilter is HomeFilter.ByTag && selectedFilter.tag.id == tag.id
            FilterChip(
                selected = isSelected,
                onClick = { onSelectFilter(HomeFilter.ByTag(tag)) },
                label = {
                    Text(
                        "#${tag.name}",
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = MaterialTheme.colorScheme.secondary,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}
