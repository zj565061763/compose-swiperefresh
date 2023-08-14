package com.sd.lib.compose.swiperefresh

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.sd.lib.compose.swiperefresh.indicator.DefaultSwipeRefreshIndicator

/**
 * Indicator container for the start direction.
 */
@SuppressLint("ModifierParameter")
@Composable
fun StartIndicatorContainer(
    state: IndicatorContainerState = rememberStartIndicatorContainerState(),
    modifier: Modifier = Modifier,
    indicator: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
) {
    require(state is ContainerApiForIndicator) { "state should be instance of ContainerApiForIndicator." }
    check(state.direction == RefreshDirection.Start) { "state.direction != RefreshDirection.Start" }

    DefaultIndicatorContainer(
        state = state,
        modifier = modifier,
        indicator = indicator,
    )
}

/**
 * Indicator container for the end direction.
 */
@SuppressLint("ModifierParameter")
@Composable
fun EndIndicatorContainer(
    state: IndicatorContainerState = rememberEndIndicatorContainerState(),
    modifier: Modifier = Modifier,
    indicator: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
) {
    require(state is ContainerApiForIndicator) { "state should be instance of ContainerApiForIndicator." }
    check(state.direction == RefreshDirection.End) { "state.direction != RefreshDirection.End" }

    DefaultIndicatorContainer(
        state = state,
        modifier = modifier,
        indicator = indicator,
    )
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
    indicator: @Composable () -> Unit,
) {
    require(state is ContainerApiForIndicator) { "state should be instance of ContainerApiForIndicator." }
    require(state is ContainerApiForSwipeRefresh) { "state should be instance of ContainerApiForSwipeRefresh." }

    val swipeRefreshState = checkNotNull(LocalFSwipeRefreshState.current)

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    if (state is BaseIndicatorContainerState) {
        check(state.swipeRefreshState === swipeRefreshState) { "state.swipeRefreshState != LocalFSwipeRefreshState.current" }
        state.containerSizeState = when (swipeRefreshState.orientationMode) {
            OrientationMode.Vertical -> containerSize.height
            OrientationMode.Horizontal -> containerSize.width
        }
    }

    DisposableEffect(state) {
        when (state.direction) {
            RefreshDirection.Start -> {
                if (swipeRefreshState.startContainerApi == null) {
                    swipeRefreshState.startContainerApi = state
                } else error("State's start container api has been specified.")
            }

            RefreshDirection.End -> {
                if (swipeRefreshState.endContainerApi == null) {
                    swipeRefreshState.endContainerApi = state
                } else error("State's end container api has been specified.")
            }
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

    val contentScope: @Composable BoxScope.() -> Unit = {
        CompositionLocalProvider(
            LocalIndicatorContainerState provides state,
            LocalContainerApiForIndicator provides state,
        ) {
            Box(
                modifier = Modifier.graphicsLayer {
                    alpha = if (swipeRefreshState.currentDirection == state.direction) 1f else 0f
                }
            ) {
                indicator()
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
            modifier = modifier.fillMaxWidth(),
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
            modifier = modifier.fillMaxHeight(),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
