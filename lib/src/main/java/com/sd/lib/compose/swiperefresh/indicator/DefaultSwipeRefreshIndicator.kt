package com.sd.lib.compose.swiperefresh.indicator

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
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
    padding: PaddingValues = PaddingValues(5.dp)
) {
    val swipeRefreshState = checkNotNull(LocalFSwipeRefreshState.current)
    val containerApi = checkNotNull(LocalContainerApiForIndicator.current)

    // Configure the refresh trigger distance.
    val refreshingDistance = containerApi.refreshingDistance
    DisposableEffect(refreshingDistance) {
        val triggerDistance = if (refreshingDistance > 0) {
            (refreshingDistance * RefreshingDistanceMultiplier).roundToInt()
        } else {
            null
        }
        containerApi.setRefreshTriggerDistance(triggerDistance)
        onDispose {
            containerApi.setRefreshTriggerDistance(null)
        }
    }


    // Configure the ignored progress distance.
    val containerSize = containerApi.containerSize
    DisposableEffect(containerSize) {
        val ignoredDistance = containerSize / 2f
        containerApi.setIgnoredProgressDistance(ignoredDistance.roundToInt())
        onDispose {
            containerApi.setIgnoredProgressDistance(null)
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
    LaunchedEffect(swipeRefreshState) {
        snapshotFlow { swipeRefreshState.refreshState }
            .collect {
                if (it == RefreshState.None) {
                    animScale.snapTo(1f)
                }
            }
    }


    val rotation = when (swipeRefreshState.orientationMode) {
        OrientationMode.Vertical -> {
            when (swipeRefreshState.currentDirection) {
                RefreshDirection.End -> 180f
                else -> 0f
            }
        }
        OrientationMode.Horizontal -> {
            when (swipeRefreshState.currentDirection) {
                RefreshDirection.End -> 90f
                RefreshDirection.Start -> 270f
                else -> 0f
            }
        }
    }

    GoogleSwipeRefreshIndicator(
        isRefreshing = swipeRefreshState.isRefreshing,
        progress = containerApi.progress,
        modifier = modifier.graphicsLayer {
            scaleX = animScale.value
            scaleY = animScale.value
            rotationZ = rotation
        },
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        strokeWidth = strokeWidth,
        size = size,
        spinnerSize = spinnerSize,
        padding = padding,
    )
}
