package com.fpf.smartscan.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.fpf.smartscan.R

@Composable
fun OverflowMenu(
    onRefreshImageIndex: () -> Unit,
    onRefreshVideoIndex: () -> Unit
){
    var expanded by remember { mutableStateOf(false) }
    Box() {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "menu"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = (-40).dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.setting_refresh_image_index)) },
                leadingIcon = {
                    Box {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Image"
                        )
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .offset(x= 6.dp, y = 6.dp)
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onRefreshImageIndex()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.setting_refresh_video_index)) },
                leadingIcon = {
                    Box {
                        Icon(
                            imageVector = Icons.Filled.VideoLibrary,
                            contentDescription = "Video"
                        )
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .offset(x= 6.dp, y = 6.dp)
                        )
                    }
                },
                onClick = {
                    expanded = false
                    onRefreshVideoIndex()
                }
            )
        }
    }
}