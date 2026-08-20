package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ai.Visual3DModel
import com.example.ui.theme.MummaEmerald
import com.example.ui.theme.MummaPrimaryAmber
import com.example.ui.theme.MummaRose
import com.example.ui.theme.MummaSecondaryCyan
import com.example.ui.theme.MummaTertiaryViolet
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Visual3DCanvas(
    model: Visual3DModel,
    modifier: Modifier = Modifier
) {
    var userRotationX by remember { mutableFloatStateOf(25f) }
    var userRotationY by remember { mutableFloatStateOf(35f) }
    var speedMultiplier by remember { mutableFloatStateOf(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "3d_rotation")
    val baseAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbital_angle"
    )

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MummaTertiaryViolet.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewInAr,
                            contentDescription = null,
                            tint = MummaTertiaryViolet,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = model.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Interactive 3D Kinetic Simulation",
                            style = MaterialTheme.typography.labelSmall,
                            color = MummaSecondaryCyan
                        )
                    }
                }

                IconButton(
                    onClick = {
                        userRotationX = 25f
                        userRotationY = 35f
                        speedMultiplier = 1f
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = "Reset Angle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (model.description.isNotBlank()) {
                Text(
                    text = model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 3D Canvas Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F0E17))
                    .border(1.dp, Color(0xFF2E2B44), RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            userRotationY += dragAmount.x * 0.4f
                            userRotationX = (userRotationX - dragAmount.y * 0.4f).coerceIn(-75f, 75f)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    // Draw grid/background glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0x336750A4), Color.Transparent),
                            center = Offset(centerX, centerY),
                            radius = size.width * 0.4f
                        ),
                        radius = size.width * 0.4f,
                        center = Offset(centerX, centerY)
                    )

                    val rotXRad = Math.toRadians(userRotationX.toDouble())
                    val rotYRad = Math.toRadians(userRotationY.toDouble())

                    when (model.type) {
                        "DNA_HELIX" -> {
                            val rungs = 16
                            for (i in 0 until rungs) {
                                val t = i.toFloat() / rungs
                                val y = centerY - 90f + (t * 180f)
                                val angle = Math.toRadians((baseAngle * speedMultiplier + i * 25.0) % 360.0)
                                val xOffset = (cos(angle) * 60f * cos(rotYRad)).toFloat()
                                val zOffset = sin(angle) * 60f

                                val leftPoint = Offset(centerX - xOffset, y)
                                val rightPoint = Offset(centerX + xOffset, y)

                                val alpha = (((zOffset + 60.0) / 120.0).coerceIn(0.2, 1.0)).toFloat()

                                drawLine(
                                    color = Color(0xFF00E5FF).copy(alpha = alpha * 0.7f),
                                    start = leftPoint,
                                    end = rightPoint,
                                    strokeWidth = 3f
                                )
                                drawCircle(
                                    color = Color(0xFFD0BCFF).copy(alpha = alpha),
                                    radius = 7f,
                                    center = leftPoint
                                )
                                drawCircle(
                                    color = Color(0xFF76FF03).copy(alpha = (1f - alpha).coerceIn(0.3f, 1f)),
                                    radius = 7f,
                                    center = rightPoint
                                )
                            }
                        }
                        else -> {
                            // Render Orbital Simulation (Atom / Planetary / Cell Organelles)
                            // 1. Center Core (Nucleus)
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFFFF5252), Color(0xFFB71C1C)),
                                    center = Offset(centerX, centerY),
                                    radius = 24f
                                ),
                                radius = 24f,
                                center = Offset(centerX, centerY)
                            )
                            drawCircle(
                                color = Color(0xFFFF8A80).copy(alpha = 0.6f),
                                radius = 28f,
                                center = Offset(centerX, centerY),
                                style = Stroke(width = 2f)
                            )

                            // 2. Orbits and Electrons
                            val elements = if (model.elements.isNotEmpty()) model.elements else listOf(
                                com.example.core.ai.Visual3DElement("e- K-Shell", "Orbital", 12f, 50f, 2.0f, "#00E5FF", -25f),
                                com.example.core.ai.Visual3DElement("e- L-Shell", "Orbital", 14f, 85f, 1.3f, "#76FF03", 35f),
                                com.example.core.ai.Visual3DElement("e- M-Shell", "Orbital", 14f, 115f, 0.8f, "#FFD700", -50f)
                            )

                            elements.forEachIndexed { idx, el ->
                                val orbitR = if (el.orbitRadius > 0) el.orbitRadius * 1.1f else (50f + idx * 32f)
                                val tiltRad = Math.toRadians((el.zOffset + userRotationX).toDouble())
                                val elementColor = try {
                                    Color(android.graphics.Color.parseColor(el.colorHex))
                                } catch (e: Exception) {
                                    if (idx % 2 == 0) MummaSecondaryCyan else MummaEmerald
                                }

                                // Draw Elliptical Track
                                val trackSteps = 60
                                for (step in 0 until trackSteps) {
                                    val a1 = Math.toRadians((step * 360.0 / trackSteps))
                                    val a2 = Math.toRadians(((step + 1) * 360.0 / trackSteps))

                                    val x1 = centerX + (cos(a1) * orbitR * cos(rotYRad) - sin(a1) * orbitR * sin(tiltRad) * sin(rotYRad)).toFloat()
                                    val y1 = centerY + (sin(a1) * orbitR * cos(tiltRad)).toFloat()
                                    val x2 = centerX + (cos(a2) * orbitR * cos(rotYRad) - sin(a2) * orbitR * sin(tiltRad) * sin(rotYRad)).toFloat()
                                    val y2 = centerY + (sin(a2) * orbitR * cos(tiltRad)).toFloat()

                                    drawLine(
                                        color = elementColor.copy(alpha = 0.25f),
                                        start = Offset(x1, y1),
                                        end = Offset(x2, y2),
                                        strokeWidth = 1.8f
                                    )
                                }

                                // Draw Orbiting Particle
                                val particleAngle = Math.toRadians(((baseAngle * el.orbitSpeed * speedMultiplier + idx * 120.0) % 360.0))
                                val px = centerX + (cos(particleAngle) * orbitR * cos(rotYRad) - sin(particleAngle) * orbitR * sin(tiltRad) * sin(rotYRad)).toFloat()
                                val py = centerY + (sin(particleAngle) * orbitR * cos(tiltRad)).toFloat()

                                // Particle glow
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(elementColor, elementColor.copy(alpha = 0f)),
                                        center = Offset(px, py),
                                        radius = 16f
                                    ),
                                    radius = 16f,
                                    center = Offset(px, py)
                                )
                                drawCircle(
                                    color = elementColor,
                                    radius = 6.5f,
                                    center = Offset(px, py)
                                )
                            }
                        }
                    }
                }

                // Interactive hint overlay
                Text(
                    text = "Drag to rotate in 3D space",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                )
            }

            // Speed Control Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Kinetic Simulation Speed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${String.format("%.1f", speedMultiplier)}x",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MummaSecondaryCyan
                )
            }

            Slider(
                value = speedMultiplier,
                onValueChange = { speedMultiplier = it },
                valueRange = 0.2f..3.0f,
                colors = SliderDefaults.colors(
                    thumbColor = MummaSecondaryCyan,
                    activeTrackColor = MummaSecondaryCyan
                )
            )

            // Legend / Elements Chips
            if (model.elements.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    model.elements.take(3).forEach { el ->
                        val elColor = try {
                            Color(android.graphics.Color.parseColor(el.colorHex))
                        } catch (e: Exception) {
                            MummaSecondaryCyan
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(elColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(elColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${el.name} (${el.role})",
                                    fontSize = 11.sp,
                                    color = elColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
