package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OutfitFontFamily

/**
 * Stylish custom "S&S" monogram badge used in Splash and About screens.
 */
@Composable
fun SSLogoBadge(
    size: Dp = 88.dp,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            Color(0xFF6366F1),
            MaterialTheme.colorScheme.secondary
        )
    )
    Box(
        modifier = modifier
            .size(size)
            .shadow(14.dp, CircleShape, ambientColor = MaterialTheme.colorScheme.primary)
            .clip(CircleShape)
            .background(gradient)
            .border(
                width = 2.5.dp,
                brush = Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.8f), Color(0xFFFBBF24))
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "S&S",
            fontFamily = OutfitFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.34f).sp,
            color = Color.White,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Expressive custom Compose Canvas empty-state illustration for Home screen.
 */
@Composable
fun EmptyPlansState(
    title: String,
    subtitle: String,
    actionLabel: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val surface = MaterialTheme.colorScheme.surface

    val transition = rememberInfiniteTransition(label = "empty_float")
    val floatOffset by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_y"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Canvas(modifier = Modifier.size(180.dp)) {
            val w = size.width
            val h = size.height

            // Soft aura circle
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.48f
                )
            )

            // Back floating card
            drawRoundRect(
                color = surfaceVariant,
                topLeft = Offset(w * 0.18f, h * 0.24f + floatOffset * 0.5f),
                size = Size(w * 0.64f, h * 0.52f),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // Front main planner card
            drawRoundRect(
                color = surface,
                topLeft = Offset(w * 0.23f, h * 0.30f + floatOffset),
                size = Size(w * 0.58f, h * 0.46f),
                cornerRadius = CornerRadius(22f, 22f)
            )
            drawRoundRect(
                color = primary.copy(alpha = 0.22f),
                topLeft = Offset(w * 0.23f, h * 0.30f + floatOffset),
                size = Size(w * 0.58f, h * 0.46f),
                cornerRadius = CornerRadius(22f, 22f),
                style = Stroke(width = 3f)
            )

            // Checklist lines inside card
            val baseLineY = h * 0.42f + floatOffset
            listOf(0f, 0.11f, 0.22f).forEachIndexed { idx, step ->
                val y = baseLineY + h * step
                val dotColor = when (idx) {
                    0 -> primary
                    1 -> secondary
                    else -> tertiary
                }
                drawCircle(
                    color = dotColor,
                    radius = 7f,
                    center = Offset(w * 0.33f, y)
                )
                drawLine(
                    color = dotColor.copy(alpha = 0.45f),
                    start = Offset(w * 0.40f, y),
                    end = Offset(w * (0.70f - idx * 0.06f), y),
                    strokeWidth = 7f,
                    cap = StrokeCap.Round
                )
            }

            // Floating glowing alarm badge top-right
            val bellCenter = Offset(w * 0.74f, h * 0.26f - floatOffset)
            drawCircle(color = tertiary, radius = 22f, center = bellCenter)
            drawCircle(
                color = Color.White,
                radius = 9f,
                center = bellCenter,
                style = Stroke(width = 3.5f)
            )

            // Decorative star accents
            drawCircle(color = secondary, radius = 5f, center = Offset(w * 0.16f, h * 0.36f - floatOffset))
            drawCircle(color = primary.copy(alpha = 0.6f), radius = 4f, center = Offset(w * 0.84f, h * 0.65f + floatOffset))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onActionClick,
            modifier = Modifier.testTag("empty_create_plan_button")
        ) {
            Text(text = actionLabel)
        }
    }
}

/**
 * Custom Canvas illustration for an empty day in the Calendar Schedule screen.
 */
@Composable
fun EmptyCalendarState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(150.dp)) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = w * 0.46f
                )
            )

            // Calendar body
            drawRoundRect(
                color = surface,
                topLeft = Offset(w * 0.2f, h * 0.22f),
                size = Size(w * 0.6f, h * 0.56f),
                cornerRadius = CornerRadius(20f, 20f)
            )
            drawRoundRect(
                color = primary.copy(alpha = 0.25f),
                topLeft = Offset(w * 0.2f, h * 0.22f),
                size = Size(w * 0.6f, h * 0.56f),
                cornerRadius = CornerRadius(20f, 20f),
                style = Stroke(3f)
            )
            // Calendar header bar
            drawRoundRect(
                color = primary,
                topLeft = Offset(w * 0.2f, h * 0.22f),
                size = Size(w * 0.6f, h * 0.14f),
                cornerRadius = CornerRadius(20f, 20f)
            )
            // Rings
            drawLine(
                color = tertiary,
                start = Offset(w * 0.35f, h * 0.16f),
                end = Offset(w * 0.35f, h * 0.27f),
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = tertiary,
                start = Offset(w * 0.65f, h * 0.16f),
                end = Offset(w * 0.65f, h * 0.27f),
                strokeWidth = 7f,
                cap = StrokeCap.Round
            )

            // Peace checkmark in center
            val checkPath = Path().apply {
                moveTo(w * 0.40f, h * 0.55f)
                lineTo(w * 0.48f, h * 0.63f)
                lineTo(w * 0.63f, h * 0.46f)
            }
            drawPath(
                path = checkPath,
                color = secondary,
                style = Stroke(width = 7f, cap = StrokeCap.Round)
            )
            drawCircle(
                color = surfaceVariant,
                radius = 5f,
                center = Offset(w * 0.15f, h * 0.72f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
