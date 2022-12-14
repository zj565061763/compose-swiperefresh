package com.sd.lib.compose.swiperefresh

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sd.lib.compose.swiperefresh.indicator.DefaultSwipeRefreshIndicator

/**
 * Indicator container for the start direction.
 */
@Composable
fun StartIndicatorContainer(
    state: IndicatorContainerState = rememberStartIndicatorContainerState(),
    modifier: Modifier = Modifier,
    indicator: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
) {
    DefaultIndicatorContainer(
        state = state,
        modifier = modifier,
    ) {
        indicator()
    }
}

/**
 * Indicator container for the end direction.
 */
@Composable
fun EndIndicatorContainer(
    state: IndicatorContainerState = rememberEndIndicatorContainerState(),
    modifier: Modifier = Modifier,
    indicator: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
) {
    DefaultIndicatorContainer(
        state = state,
        modifier = modifier,
    ) {
        indicator()
    }
}

val LocalIndicatorContainerState = staticCompositionLocalOf<IndicatorContainerState?> { null }
val LocalContainerApiForIndicator = staticCompositionLocalOf<ContainerApiForIndicator?> { null }

/**
 * Default indicator container.
 */
@Composable
private fun DefaultIndicatorContainer(
    state: IndicatorContainerState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    require(state is ContainerApiForIndicator) { "state should be instance of ContainerApiForIndicator." }
    require(state is ContainerApiForSwipeRefresh) { "state should be instance of ContainerApiForSwipeRefresh." }

    val swipeRefreshState = checkNotNull(LocalFSwipeRefreshState.current)

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    if (state is BaseIndicatorContainerState) {
        state.setContainerSize(
            when (swipeRefreshState.orientationMode) {
                OrientationMode.Vertical -> containerSize.height
                OrientationMode.Horizontal -> containerSize.width
            }
        )
    }

    DisposableEffect(state) {
        when (state.direction) {
            RefreshDirection.Start -> swipeRefreshState.startContainerApi = state
            RefreshDirection.End -> swipeRefreshState.endContainerApi = state
        }
        onDispose {
            when (state.direction) {
                RefreshDirection.Start -> {
                    if (swipeRefreshState.startContainerApi == state) {
                        swipeRefreshState.startContainerApi = null
                    }
                }
                RefreshDirection.End -> {
                    if (swipeRefreshState.endContainerApi == state) {
                        swipeRefreshState.endContainerApi = null
                    }
                }
            }
        }
    }

    val isActive = swipeRefreshState.currentDirection == state.direction

    val contentScope: @Composable BoxScope.() -> Unit = {
        CompositionLocalProvider(
            LocalIndicatorContainerState provides state,
            LocalContainerApiForIndicator provides state,
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = if (isActive) 1f else 0f
                },
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }

    when (swipeRefreshState.orientationMode) {
        OrientationMode.Vertical -> {
            VerticalBox(
                offset = state.offset,
                modifier = modifier.onSizeChanged { containerSize = it },
                content = contentScope,
            )
        }
        OrientationMode.Horizontal -> {
            HorizontalBox(
                offset = state.offset,
                modifier = modifier.onSizeChanged { containerSize = it },
                content = contentScope,
            )
        }
    }
}

@Composable
private fun VerticalBox(
    modifier: Modifier = Modifier,
    offset: Int,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.offset { IntOffset(0, offset) }
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 50.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
private fun HorizontalBox(
    modifier: Modifier = Modifier,
    offset: Int,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier.offset { IntOffset(offset, 0) }
    ) {
        Box(
            modifier = modifier
                .fillMaxHeight()
                .defaultMinSize(minWidth = 50.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
