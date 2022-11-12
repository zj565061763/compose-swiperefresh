package com.sd.lib.compose.swiperefresh.indicator

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.swiperefresh.*
import kotlin.math.roundToInt

private const val RefreshingDistanceMultiplier = 1.3f

/**
 * Default indicator style.
 */
@Composable
fun DefaultSwipeRefreshIndicator(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp,
    size: Dp = 40.dp,
    spinnerSize: Dp = size.times(0.5f),
    padding: PaddingValues = PaddingValues(5.dp),
    configRefreshTriggerDistance: Boolean = true,
    configIgnoredProgressDistance: Boolean = true,
) {
    val state = checkNotNull(LocalFSwipeRefreshState.current)
    val containerApi = checkNotNull(LocalContainerApiForIndicator.current)

    var indicatorSize by remember { mutableStateOf(IntSize.Zero) }


    if (configRefreshTriggerDistance) {
        val refreshingDistance = containerApi.refreshingDistance
        if (refreshingDistance > 0) {
            DisposableEffect(refreshingDistance) {
                val triggerDistance = refreshingDistance * RefreshingDistanceMultiplier
                containerApi.setRefreshTriggerDistance(triggerDistance.roundToInt())
                onDispose {
                    containerApi.setRefreshTriggerDistance(null)
                }
            }
        }
    }


    if (configIgnoredProgressDistance) {
        val orientationSize = when (state.orientationMode) {
            OrientationMode.Vertical -> indicatorSize.height
            OrientationMode.Horizontal -> indicatorSize.width
        }
        if (orientationSize > 0) {
            DisposableEffect(orientationSize) {
                val ignoredDistance = orientationSize / 2f
                containerApi.setIgnoredProgressDistance(ignoredDistance.roundToInt())
                onDispose {
                    containerApi.setIgnoredProgressDistance(null)
                }
            }
        }
    }


    val animScale = remember { Animatable(1f) }
    DisposableEffect(containerApi) {
        val callback: suspend () -> Unit = {
            animScale.animateTo(0f)
        }
        containerApi.registerHideRefreshing(callback)
        onDispose {
            containerApi.unregisterHideRefreshing(callback)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.refreshState }
            .collect {
                if (it == RefreshState.None) {
                    animScale.snapTo(1f)
                }
            }
    }


    val rotation = when (state.orientationMode) {
        OrientationMode.Vertical -> {
            when (state.currentDirection) {
                RefreshDirection.End -> 180f
                else -> 0f
            }
        }
        OrientationMode.Horizontal -> {
            when (state.currentDirection) {
                RefreshDirection.End -> 90f
                RefreshDirection.Start -> 270f
                else -> 0f
            }
        }
    }


    GoogleSwipeRefreshIndicator(
        isRefreshing = state.isRefreshing,
        progress = containerApi.progress,
        modifier = modifier
            .graphicsLayer {
                scaleX = animScale.value
                scaleY = animScale.value
                rotationZ = rotation
            }
            .onSizeChanged {
                indicatorSize = it
            },
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        strokeWidth = strokeWidth,
        size = size,
        spinnerSize = spinnerSize,
        padding = padding,
    )
}
