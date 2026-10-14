package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.JarvisAlert
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisElectricBlue
import com.example.ui.theme.JarvisNeonGreen

enum class JarvisHudState(val label: String, val color: Color) {
    STANDBY("SYSTEM ONLINE", JarvisCyan),
    THINKING("PROCESSING QUERY", JarvisElectricBlue),
    LISTENING("VOICE RECOGNITION", JarvisNeonGreen),
    SPEAKING("VOCAL SYNTHESIS", JarvisCyan),
    ERROR("DIAGNOSTIC ALERT", JarvisAlert)
}

@Composable
fun JarvisCoreHud(
    state: JarvisHudState,
    modifier: Modifier = Modifier,
    volumeLevel: Float = 0f,
    sizeDp: Int = 140
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_hud")

    val rotationClockwise by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_rot_cw"
    )

    val rotationCounter by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hud_rot_ccw"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hud_pulse"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(sizeDp.dp),
            contentAlignment = Alignment.Center
        ) {
            // Outer dynamic animated tech rings
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 2.dp.toPx()
                val radius = (size.minDimension / 2) - 4.dp.toPx()

                // Outer dashed ring
                drawCircle(
                    color = state.color.copy(alpha = 0.35f),
                    radius = radius,
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), rotationClockwise * 2f)
                    )
                )

                // Middle segment ring
                drawCircle(
                    color = state.color.copy(alpha = 0.5f),
                    radius = radius - 10.dp.toPx(),
                    style = Stroke(
                        width = strokeWidth * 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 20f, 10f, 20f), rotationCounter * 1.5f)
                    )
                )
            }

            // Central pulsing core reactor
            Box(
                modifier = Modifier
                    .size((sizeDp * 0.72).dp)
                    .scale(if (state == JarvisHudState.THINKING || state == JarvisHudState.LISTENING) pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                state.color.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.5.dp, state.color.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.miya_ai_core),
                    contentDescription = "Miya AI Core",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status pill
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(state.color.copy(alpha = 0.12f))
                .border(1.dp, state.color.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(state.color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = state.label,
                color = state.color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        // Audio visualizer bars when listening or speaking
        if (state == JarvisHudState.LISTENING || state == JarvisHudState.SPEAKING) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(18.dp)
            ) {
                val heights = listOf(0.4f, 0.7f, 1.0f, 0.6f, 0.9f, 0.5f, 0.8f)
                heights.forEachIndexed { index, mult ->
                    val factor = if (volumeLevel > 0.05f) volumeLevel * mult else 0.2f * mult
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height((18 * factor.coerceIn(0.15f, 1f)).dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(state.color)
                    )
                }
            }
        }
    }
}
