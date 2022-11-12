package com.sd.lib.compose.swiperefresh.indicator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.sd.lib.compose.swiperefresh.*

/**
 * Indicator container for the start direction.
 */
@Composable
fun StartIndicatorContainer(
    containerState: IndicatorContainerState? = null,
    indicator: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
) {
    val state = checkNotNull(LocalFSwipeRefreshState.current)

    val containerState = containerState ?: when (state.startIndicatorMode) {
        IndicatorMode.Above -> rememberIndicatorContainerStateAbove(state, RefreshDirection.Start)
        IndicatorMode.Drag -> rememberIndicatorContainerStateDrag(state, RefreshDirection.Start)
        IndicatorMode.Below -> rememberIndicatorContainerStateBelow(state, RefreshDirection.Start)
        IndicatorMode.Invisible -> rememberIndicatorContainerStateInvisible(state, RefreshDirection.Start)
    }

    FSwipeRefreshIndicatorContainer(containerState) {
        indicator()
    }
}


/**
 * Indicator container for the end direction.
 */
@Composable
fun EndIndicatorContainer(
    containerState: IndicatorContainerState? = null,
    indicator: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
) {
    val state = checkNotNull(LocalFSwipeRefreshState.current)

    val containerState = containerState ?: when (state.endIndicatorMode) {
        IndicatorMode.Above -> rememberIndicatorContainerStateAbove(state, RefreshDirection.End)
        IndicatorMode.Drag -> rememberIndicatorContainerStateDrag(state, RefreshDirection.End)
        IndicatorMode.Below -> rememberIndicatorContainerStateBelow(state, RefreshDirection.End)
        IndicatorMode.Invisible -> rememberIndicatorContainerStateInvisible(state, RefreshDirection.End)
    }

    FSwipeRefreshIndicatorContainer(containerState) {
        indicator()
    }
}


/**
 * [IndicatorContainerState] for [IndicatorMode.Above].
 */
@Composable
fun rememberIndicatorContainerStateAbove(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
): IndicatorContainerState {
    return remember(swipeRefreshState, direction) {
        IndicatorContainerStateAbove(swipeRefreshState, direction)
    }
}

/**
 * [IndicatorContainerState] for [IndicatorMode.Drag].
 */
@Composable
fun rememberIndicatorContainerStateDrag(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
): IndicatorContainerState {
    return remember(swipeRefreshState, direction) {
        IndicatorContainerStateDrag(swipeRefreshState, direction)
    }
}

/**
 * [IndicatorContainerState] for [IndicatorMode.Below].
 */
@Composable
fun rememberIndicatorContainerStateBelow(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
): IndicatorContainerState {
    return remember(swipeRefreshState, direction) {
        IndicatorContainerStateBelow(swipeRefreshState, direction)
    }
}

/**
 * [IndicatorContainerState] for [IndicatorMode.Invisible].
 */
@Composable
fun rememberIndicatorContainerStateInvisible(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
): IndicatorContainerState {
    return remember(swipeRefreshState, direction) {
        IndicatorContainerStateInvisible(swipeRefreshState, direction)
    }
}

