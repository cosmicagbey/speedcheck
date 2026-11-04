package com.speedcheck.android.ui.dashboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedcheck.android.model.SpeedAlertStatus
import com.speedcheck.android.theme.GhanaRed
import com.speedcheck.android.theme.SpeedApproachingAmber
import com.speedcheck.android.theme.SpeedExceededRed
import com.speedcheck.android.theme.SpeedSafeGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Modern electric-cockpit digital speedometer gauge.
 * Features dynamic multi-color gradient sweeps, radial ambient glow,
 * clean aerodynamic typography, and an authentic circular Ghana speed limit road sign.
 */
@Composable
fun SpeedometerGauge(
    currentSpeedKmh: Float,
    speedLimitKmh: Int,
    alertStatus: SpeedAlertStatus,
    maxGaugeSpeed: Float = 160f,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateFloatAsState(
        targetValue = currentSpeedKmh.coerceIn(0f, maxGaugeSpeed),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "speedAnimation"
    )

    val targetColor = when (alertStatus) {
        SpeedAlertStatus.NORMAL -> SpeedSafeGreen
        SpeedAlertStatus.APPROACHING -> SpeedApproachingAmber
        SpeedAlertStatus.EXCEEDED -> SpeedExceededRed
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 300),
        label = "colorAnimation"
    )

    // Pulse animation when speeding over limit
    val infiniteTransition = rememberInfiniteTransition(label = "warningPulse")
    val pulseAlpha by if (alertStatus == SpeedAlertStatus.EXCEEDED) {
        infiniteTransition.animateFloat(
            initialValue = 0.65f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
    } else {
        animateFloatAsState(targetValue = 1.0f, label = "idleAlpha")
    }

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        // 0. Soft Ambient Cockpit Radial Glow
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            animatedColor.copy(alpha = if (alertStatus == SpeedAlertStatus.EXCEEDED) 0.20f else 0.10f),
                            animatedColor.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )
        )

        // 1. Canvas Dial Arc with Tick Marks
        Canvas(modifier = Modifier.size(260.dp)) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = size.width - strokeWidth
            val arcOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

            val startAngle = 140f
            val sweepAngle = 260f

            // A. Dark Outer Track Ring
            drawArc(
                color = Color(0xFF1E293B).copy(alpha = 0.8f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcOffset,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // B. Active Speed Gradient Sweep Arc
            val progressFraction = (animatedSpeed / maxGaugeSpeed).coerceIn(0f, 1f)
            val activeSweep = sweepAngle * progressFraction

            if (activeSweep > 0f) {
                val gradientBrush = when (alertStatus) {
                    SpeedAlertStatus.NORMAL -> Brush.sweepGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF10B981)),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                    SpeedAlertStatus.APPROACHING -> Brush.sweepGradient(
                        colors = listOf(Color(0xFFF59E0B), Color(0xFFFB923C), Color(0xFFF59E0B)),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                    SpeedAlertStatus.EXCEEDED -> Brush.sweepGradient(
                        colors = listOf(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFFEF4444)),
                        center = Offset(size.width / 2f, size.height / 2f)
                    )
                }

                drawArc(
                    brush = gradientBrush,
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = Size(arcSize, arcSize),
                    alpha = pulseAlpha,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // C. Speed Limit Marker on the Dial
            val limitFraction = (speedLimitKmh / maxGaugeSpeed).coerceIn(0f, 1f)
            val limitAngle = startAngle + (sweepAngle * limitFraction)
            val limitAngleRad = (limitAngle * PI / 180f).toFloat()

            val center = Offset(size.width / 2f, size.height / 2f)
            val innerRadius = (arcSize / 2f) - (strokeWidth / 2f) - 6.dp.toPx()
            val outerRadius = (arcSize / 2f) + (strokeWidth / 2f) + 6.dp.toPx()

            val tickStart = Offset(
                center.x + innerRadius * cos(limitAngleRad),
                center.y + innerRadius * sin(limitAngleRad)
            )
            val tickEnd = Offset(
                center.x + outerRadius * cos(limitAngleRad),
                center.y + outerRadius * sin(limitAngleRad)
            )

            // Red/Gold limit marker needle
            drawLine(
                color = Color(0xFFEF3340),
                start = tickStart,
                end = tickEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 2. Center HUD Speed Display & Authentic Ghana Speed Sign
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${currentSpeedKmh.toInt()}",
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = animatedColor.copy(alpha = pulseAlpha),
                letterSpacing = (-2).sp,
                lineHeight = 80.sp
            )
            Text(
                text = "KM / H",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Authentic Ghana Road Speed Sign Badge (White circular disk with red ring)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                    .padding(start = 3.dp, end = 10.dp, top = 3.dp, bottom = 3.dp)
            ) {
                // Circular White Sign with Ghana Red Border
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Color(0xFFEF3340), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$speedLimitKmh",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "LIMIT $speedLimitKmh",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (alertStatus) {
                        SpeedAlertStatus.EXCEEDED -> SpeedExceededRed
                        SpeedAlertStatus.APPROACHING -> SpeedApproachingAmber
                        SpeedAlertStatus.NORMAL -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    },
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
