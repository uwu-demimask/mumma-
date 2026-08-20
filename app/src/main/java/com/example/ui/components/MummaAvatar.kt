package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ElegantDeepViolet
import com.example.ui.theme.ElegantEmerald
import com.example.ui.theme.ElegantLavenderPrimary
import com.example.ui.theme.ElegantVioletContainer
import com.example.ui.theme.MummaEmerald
import com.example.ui.theme.MummaPrimaryAmber
import com.example.ui.theme.MummaSecondaryCyan
import com.example.ui.theme.MummaTertiaryViolet
import kotlin.math.cos
import kotlin.math.sin

enum class AvatarState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    TEACHING
}

@Composable
fun MummaAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    state: AvatarState = AvatarState.IDLE,
    audioLevel: Float = 0f
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    val (coreColor, outerColor, secondaryColor) = when (state) {
        AvatarState.IDLE -> Triple(ElegantLavenderPrimary, ElegantDeepViolet, Color(0xFF1D192B))
        AvatarState.LISTENING -> Triple(Color(0xFFE8DEF8), ElegantLavenderPrimary, Color(0xFF2B2930))
        AvatarState.THINKING -> Triple(ElegantLavenderPrimary, ElegantVioletContainer, Color(0xFF381E72))
        AvatarState.SPEAKING -> Triple(Color(0xFFF2E7FE), ElegantLavenderPrimary, Color(0xFF381E72))
        AvatarState.TEACHING -> Triple(ElegantEmerald, ElegantLavenderPrimary, Color(0xFF1C1B1F))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val centerOffset = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.72f

            // Dynamic scale factoring audio level if listening or speaking
            val dynamicScale = when (state) {
                AvatarState.LISTENING -> 1f + (audioLevel * 0.35f)
                AvatarState.SPEAKING -> pulseScale * 1.08f
                AvatarState.THINKING -> pulseScale * 0.98f
                else -> pulseScale
            }

            val currentRadius = baseRadius * dynamicScale

            // Outer Atmospheric Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        outerColor.copy(alpha = auraAlpha * 0.4f),
                        outerColor.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = currentRadius * 1.55f
                ),
                radius = currentRadius * 1.55f,
                center = centerOffset
            )

            // Dynamic Orbiting Energy Rings
            val numRings = if (state == AvatarState.THINKING || state == AvatarState.TEACHING) 3 else 2
            for (i in 0 until numRings) {
                val ringAngle = Math.toRadians((rotationAngle * (if (i % 2 == 0) 1 else -1) + (i * 60)).toDouble())
                val orbitRadius = currentRadius * (1.1f + i * 0.15f)
                val nodeX = centerOffset.x + (cos(ringAngle) * orbitRadius).toFloat()
                val nodeY = centerOffset.y + (sin(ringAngle) * orbitRadius).toFloat()

                drawCircle(
                    color = coreColor.copy(alpha = 0.22f),
                    radius = orbitRadius,
                    center = centerOffset,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                drawCircle(
                    color = coreColor.copy(alpha = 0.9f),
                    radius = 3.dp.toPx(),
                    center = Offset(nodeX, nodeY)
                )
            }

            // Core Companion Sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        coreColor,
                        outerColor,
                        secondaryColor
                    ),
                    center = Offset(centerOffset.x - currentRadius * 0.25f, centerOffset.y - currentRadius * 0.25f),
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = centerOffset
            )

            // Inner Core Highlight Specular
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(centerOffset.x - currentRadius * 0.35f, centerOffset.y - currentRadius * 0.35f),
                    radius = currentRadius * 0.4f
                ),
                radius = currentRadius * 0.4f,
                center = Offset(centerOffset.x - currentRadius * 0.35f, centerOffset.y - currentRadius * 0.35f)
            )
        }
    }
}
