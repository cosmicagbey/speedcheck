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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speedcheck.android.model.SpeedAlertStatus
import com.speedcheck.android.theme.SpeedApproachingAmber
import com.speedcheck.android.theme.SpeedExceededRed
import com.speedcheck.android.theme.SpeedSafeGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-contrast speedometer arc gauge designed for driving visibility.
 * Renders an arc dial with limit markers and animated alert indicators.
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
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
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
            initialValue = 0.6f,
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
        Canvas(modifier = Modifier.size(260.dp)) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = size.width - strokeWidth
            val arcOffset = Offset(strokeWidth / 2f, strokeWidth / 2f)

            val startAngle = 140f
            val sweepAngle = 260f

            // 1. Background Track Arc
            drawArc(
                color = Color.DarkGray.copy(alpha = 0.35f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = arcOffset,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 2. Active Speed Sweep Arc
            val progressFraction = (animatedSpeed / maxGaugeSpeed).coerceIn(0f, 1f)
            val activeSweep = sweepAngle * progressFraction

            if (activeSweep > 0f) {
                drawArc(
                    color = animatedColor.copy(alpha = pulseAlpha),
                    startAngle = startAngle,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = arcOffset,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // 3. Speed Limit Tick Mark on the Arc
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

            // Red marker for the set limit
            drawLine(
                color = Color.Red,
                start = tickStart,
                end = tickEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Digital HUD speed readout in center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${currentSpeedKmh.toInt()}",
                fontSize = 82.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = animatedColor.copy(alpha = pulseAlpha),
                letterSpacing = (-2).sp
            )
            Text(
                text = "km/h",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "LIMIT: $speedLimitKmh",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (alertStatus) {
                    SpeedAlertStatus.EXCEEDED -> SpeedExceededRed
                    SpeedAlertStatus.APPROACHING -> SpeedApproachingAmber
                    SpeedAlertStatus.NORMAL -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                }
            )
        }
    }
}
