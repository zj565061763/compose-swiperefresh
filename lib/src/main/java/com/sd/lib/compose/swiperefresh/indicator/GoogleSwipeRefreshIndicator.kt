/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sd.lib.compose.swiperefresh.indicator

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow

@Composable
internal fun GoogleSwipeRefreshIndicator(
    isRefreshing: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
    strokeWidth: Dp,
    size: Dp,
    spinnerSize: Dp,
    padding: PaddingValues,
    shadow: Boolean,
    rotationZ: Float = 0f,
) {
    PaddingSizedBox(
        size = size,
        padding = padding,
        modifier = modifier,
    ) {
        CircularBox(
            backgroundColor = backgroundColor,
            shadow = shadow,
        ) {
            Crossfade(
                targetState = isRefreshing,
                animationSpec = tween(CrossfadeDurationMs),
            ) { refreshing ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            this.rotationZ = rotationZ
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (refreshing) {
                        CircularProgressIndicator(
                            color = contentColor,
                            strokeWidth = strokeWidth,
                            modifier = Modifier.size(spinnerSize),
                        )
                    } else {
                        CircularArrowIndicator(
                            progress = progress,
                            color = contentColor,
                            strokeWidth = strokeWidth,
                            arcRadius = spinnerSize.div(2) - strokeWidth,
                            modifier = Modifier.size(spinnerSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaddingSizedBox(
    size: Dp,
    padding: PaddingValues = PaddingValues(5.dp),
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.padding(padding),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun CircularBox(
    backgroundColor: Color,
    shadow: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shadowColor = contentColorFor(backgroundColor)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor, CircleShape)
            .let {
                if (shadow) {
                    it.drawBehind {
                        drawIntoCanvas { canvas ->
                            val paint = Paint()
                            with(paint.asFrameworkPaint()) {
                                this.color = backgroundColor.toArgb()
                                this.setShadowLayer(
                                    5.dp.toPx(),
                                    0f,
                                    0f,
                                    shadowColor
                                        .copy(0.2f)
                                        .toArgb(),
                                )
                            }

                            val outline = CircleShape.createOutline(size, layoutDirection, this)
                            canvas.drawOutline(outline, paint)
                        }
                    }
                } else {
                    it
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Modifier.size MUST be specified.
 */
@Composable
private fun CircularArrowIndicator(
    progress: Float,
    color: Color,
    strokeWidth: Dp,
    arcRadius: Dp,
    modifier: Modifier,
) {
    val path = remember { Path().apply { fillType = PathFillType.EvenOdd } }

    Canvas(modifier.semantics { contentDescription = "Refreshing" }) {
        val values = ArrowValues(progress)

        rotate(degrees = values.rotation) {
            val arcRadiusPx = arcRadius.toPx() + strokeWidth.toPx() / 2f
            val arcBounds = Rect(
                size.center.x - arcRadiusPx,
                size.center.y - arcRadiusPx,
                size.center.x + arcRadiusPx,
                size.center.y + arcRadiusPx
            )
            drawArc(
                color = color,
                alpha = values.alpha,
                startAngle = values.startAngle,
                sweepAngle = values.endAngle - values.startAngle,
                useCenter = false,
                topLeft = arcBounds.topLeft,
                size = arcBounds.size,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Square
                )
            )
            drawArrow(
                path = path,
                bounds = arcBounds,
                color = color,
                strokeWidth = strokeWidth,
                values = values,
            )
        }
    }
}

@Immutable
private class ArrowValues(
    val alpha: Float,
    val rotation: Float,
    val startAngle: Float,
    val endAngle: Float,
    val scale: Float,
)

private fun ArrowValues(progress: Float): ArrowValues {
    // Discard first 40% of progress. Scale remaining progress to full range between 0 and 100%.
//    val adjustedPercent = max(min(1f, progress) - 0.4f, 0f) * 5 / 3
    val adjustedPercent = progress
    // How far beyond the threshold pull has gone, as a percentage of the threshold.
    val overshootPercent = abs(progress) - 1.0f
    // Limit the overshoot to 200%. Linear between 0 and 200.
    val linearTension = overshootPercent.coerceIn(0f, 2f)
    // Non-linear tension. Increases with linearTension, but at a decreasing rate.
    val tensionPercent = linearTension - linearTension.pow(2) / 4

    // Calculations based on SwipeRefreshLayout specification.
    val alpha = progress.coerceIn(0f, 1f)
    val endTrim = adjustedPercent * MaxProgressArc
    val rotation = (-0.25f + 0.4f * adjustedPercent + tensionPercent) * 0.5f
    val startAngle = rotation * 360
    val endAngle = (rotation + endTrim) * 360
    val scale = min(1f, adjustedPercent)

    return ArrowValues(alpha, rotation, startAngle, endAngle, scale)
}

private fun DrawScope.drawArrow(
    path: Path,
    bounds: Rect,
    color: Color,
    strokeWidth: Dp,
    values: ArrowValues,
) {
    path.reset()
    path.moveTo(0f, 0f) // Move to left corner
    path.lineTo(x = ArrowWidth.toPx() * values.scale, y = 0f) // Line to right corner

    // Line to tip of arrow
    path.lineTo(
        x = ArrowWidth.toPx() * values.scale / 2,
        y = ArrowHeight.toPx() * values.scale
    )

    val radius = min(bounds.width, bounds.height) / 2f
    val inset = ArrowWidth.toPx() * values.scale / 2f
    path.translate(
        Offset(
            x = radius + bounds.center.x - inset,
            y = bounds.center.y + strokeWidth.toPx() / 2f
        )
    )
    path.close()
    rotate(degrees = values.endAngle) {
        drawPath(path = path, color = color, alpha = values.alpha)
    }
}

private const val CrossfadeDurationMs = 100
private const val MaxProgressArc = 0.8f

private val ArrowWidth = 10.dp
private val ArrowHeight = 5.dp
