/*
 * SPDX-FileCopyrightText: The Resurrected Remix Project
 * SPDX-License-Identifier: Apache-2.0
 */

package io.chaldeaprjkt.gamespace.ui.theme

import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.chaldeaprjkt.gamespace.R
import kotlin.math.max

private const val RR_MARK_W = 716f
private const val RR_MARK =
    "M430.4 0L703.1 0A12.2 12.2 0 0 1 714.6 16.2C697.7 65.5 668.3 118 632.3 118L437.3 118" +
        "C372 118 321.8 173.6 321.5 174L137.5 398C132.6 404 84.6 442 3.7 390.8A8 8 0 0 1 1.8 379" +
        "L217 117C276 45.2 356.2 0 430.4 0Z" +
        "M483.5 187L697.6 187A11.1 11.1 0 0 1 708 201.8C681.8 274.3 654.4 283 641.2 283L482.9 283" +
        "C454.1 283 414.3 301.8 387.7 334.2L245.5 507.3C245.2 507.7 208.2 545 139.2 502" +
        "A8.2 8.2 0 0 1 137.2 489.8L307 283.1C361.8 216.4 426.6 187 483.5 187Z"

private val rrMark: Path by lazy { PathParser().parsePathString(RR_MARK).toPath() }

private val NeonBase = Color(0xFF0A0A0C)

internal val NeonLabel = Color(0xFFA9AEB5)
internal val NeonText = Color(0xFFE6E1E5)
internal val NeonTrack = Color.White.copy(alpha = 0.09f)

internal val Poppins = FontFamily(Font(R.font.poppins_semibold, FontWeight.SemiBold))

internal val Tight = TextStyle(
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
)

@Immutable
internal class NeonColors(
    val accent: Color,
    val bright: Color,
    val text: Color,
    val deep: Color,
)

@Composable
internal fun neonColors() = NeonColors(
    accent = colorResource(android.R.color.system_accent1_400),
    bright = colorResource(android.R.color.system_accent1_300),
    text = colorResource(android.R.color.system_accent1_200),
    deep = colorResource(android.R.color.system_accent1_900),
)

@Composable
internal fun neonSwitchColors(): SwitchColors {
    val accent = neonColors().accent
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = accent,
        checkedBorderColor = Color.Transparent,
        checkedIconColor = accent,
    )
}

@Composable
internal fun NeonCaps(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 9.sp,
    letterSpacing: TextUnit = 0.14.em,
    lineHeight: TextUnit = fontSize,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontFamily = Poppins,
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        overflow = TextOverflow.Ellipsis,
        maxLines = maxLines,
        style = Tight,
    )
}

internal fun Modifier.neonCard(
    colors: NeonColors,
    glowAtStart: Boolean = false,
    alpha: Float = 1f,
): Modifier = drawWithCache {
    val glowLeft = glowAtStart != (layoutDirection == LayoutDirection.Rtl)
    val base = Brush.linearGradient(
        0f to colors.deep.copy(alpha = alpha),
        0.72f to NeonBase.copy(alpha = alpha),
        start = Offset(size.width * 0.3f, 0f),
        end = Offset(size.width * 0.7f, size.height),
    )
    val glow = Brush.radialGradient(
        0f to colors.accent.copy(alpha = 0.42f),
        0.42f to colors.accent.copy(alpha = 0.1f),
        0.64f to Color.Transparent,
        center = Offset(if (glowLeft) 0f else size.width, 0f),
        radius = max(size.width * 0.85f, 1f),
    )
    val corner = Brush.radialGradient(
        0f to colors.accent.copy(alpha = 0.14f),
        0.62f to Color.Transparent,
        center = Offset(if (glowLeft) size.width else 0f, size.height),
        radius = max(size.width * 0.6f, 1f),
    )
    onDrawBehind {
        drawRect(base)
        drawRect(glow)
        drawRect(corner)
    }
}

internal fun Modifier.neonFill(
    accent: Color,
    from: Float,
    to: Float,
    centerY: Float = 0f,
    reach: Float = 0.9f,
): Modifier = drawWithCache {
    val brush = Brush.radialGradient(
        0f to accent.copy(alpha = from),
        1f to accent.copy(alpha = to),
        center = Offset(size.width / 2f, size.height * centerY),
        radius = max(size.maxDimension * reach, 1f),
    )
    onDrawBehind { drawRect(brush) }
}

internal fun Modifier.neonGlow(shape: Shape, color: Color, spread: Dp = 5.dp): Modifier =
    drawWithCache {
        val path = Path().apply {
            addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache))
        }
        val wide = Stroke(spread.toPx() * 2f)
        val near = Stroke(spread.toPx())
        onDrawBehind {
            drawPath(path, color.copy(alpha = 0.1f), style = wide)
            drawPath(path, color.copy(alpha = 0.2f), style = near)
        }
    }

internal fun Modifier.neonHalo(color: Color): Modifier = drawWithCache {
    val radius = max(size.minDimension, 1f)
    val brush = Brush.radialGradient(
        0f to color.copy(alpha = 0.45f),
        1f to Color.Transparent,
        center = size.center,
        radius = radius,
    )
    onDrawBehind { drawCircle(brush, radius = radius, center = size.center) }
}

internal fun DrawScope.drawMark(color: Color) {
    val k = size.width / RR_MARK_W
    withTransform({ scale(k, k, pivot = Offset.Zero) }) {
        drawPath(
            rrMark,
            color.copy(alpha = 0.3f),
            style = Stroke(width = 2.4.dp.toPx() / k, join = StrokeJoin.Round),
        )
        drawPath(rrMark, color)
    }
}

internal fun DrawScope.drawMarkOutline(
    left: Float,
    top: Float,
    width: Float,
    stroke: Float,
    color: Color,
    alpha: Float,
) {
    val k = width / RR_MARK_W
    val x = if (layoutDirection == LayoutDirection.Rtl) size.width - left - width else left
    withTransform({
        translate(x, top)
        scale(k, k, pivot = Offset.Zero)
    }) {
        drawPath(rrMark, color.copy(alpha = alpha * 0.25f), style = Stroke(width = stroke * 3.5f / k))
        drawPath(rrMark, color.copy(alpha = alpha), style = Stroke(width = stroke / k))
    }
}

internal fun DrawScope.drawGauge(
    progress: Float,
    radius: Float,
    stroke: Float,
    color: Color,
    track: Color,
) {
    val topLeft = Offset(center.x - radius, center.y - radius)
    val arc = Size(radius * 2, radius * 2)
    drawCircle(track, radius, style = Stroke(stroke))
    if (!(progress > 0f)) return
    val sweep = 360f * progress.coerceAtMost(1f)
    drawArc(
        color.copy(alpha = 0.25f), -90f, sweep, false, topLeft, arc,
        style = Stroke(stroke * 2.6f, cap = StrokeCap.Round),
    )
    drawArc(
        color, -90f, sweep, false, topLeft, arc,
        style = Stroke(stroke, cap = StrokeCap.Round),
    )
}
