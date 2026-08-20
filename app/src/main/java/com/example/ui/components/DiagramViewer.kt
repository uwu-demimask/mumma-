package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.DiagramLabel
import com.example.core.ai.VisualDiagramData
import com.example.ui.theme.MummaEmerald
import com.example.ui.theme.MummaPrimaryAmber
import com.example.ui.theme.MummaRose
import com.example.ui.theme.MummaSecondaryCyan
import com.example.ui.theme.MummaTertiaryViolet

@Composable
fun DiagramViewer(
    diagram: VisualDiagramData,
    modifier: Modifier = Modifier
) {
    var selectedLabel by remember { mutableStateOf<DiagramLabel?>(diagram.labels.firstOrNull()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MummaEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hub,
                        contentDescription = null,
                        tint = MummaEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = diagram.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Schematic Structural Blueprint",
                        style = MaterialTheme.typography.labelSmall,
                        color = MummaEmerald
                    )
                }
            }

            if (diagram.description.isNotBlank()) {
                Text(
                    text = diagram.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Schematic Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f

                    // Draw technical structural geometry
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                        radius = size.height * 0.38f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.1f),
                        radius = size.height * 0.22f,
                        center = Offset(cx, cy)
                    )

                    // Draw connections between labels
                    val points = diagram.labels.map {
                        Offset(size.width * it.xPercent, size.height * it.yPercent)
                    }

                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = Color(0xFF64748B).copy(alpha = 0.5f),
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 1.5f
                        )
                    }

                    // Draw hotspot pins
                    diagram.labels.forEach { label ->
                        val pos = Offset(size.width * label.xPercent, size.height * label.yPercent)
                        val isSelected = selectedLabel?.label == label.label

                        drawCircle(
                            color = if (isSelected) Color(0xFFF43F5E) else Color(0xFF38BDF8),
                            radius = if (isSelected) 10f else 7f,
                            center = pos
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = pos
                        )
                    }
                }
            }

            // Interactive Pin Selection List
            Text(
                text = "Interactive Anatomy / Components",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                diagram.labels.forEach { label ->
                    val isSelected = selectedLabel?.label == label.label
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) MummaSecondaryCyan.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                1.dp,
                                if (isSelected) MummaSecondaryCyan else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedLabel = label }
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MummaRose else MummaSecondaryCyan)
                            )
                            Column {
                                Text(
                                    text = label.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (label.description.isNotBlank()) {
                                    Text(
                                        text = label.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
