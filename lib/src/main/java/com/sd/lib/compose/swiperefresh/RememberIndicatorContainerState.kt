package com.sd.lib.compose.swiperefresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberStartIndicatorContainerState(): IndicatorContainerState {
    val swipeState = checkNotNull(LocalFSwipeRefreshState.current)
    return when (swipeState.startIndicatorMode) {
        IndicatorMode.Above -> rememberIndicatorContainerStateAbove(swipeState, RefreshDirection.Start)
        IndicatorMode.Drag -> rememberIndicatorContainerStateDrag(swipeState, RefreshDirection.Start)
        IndicatorMode.Below -> rememberIndicatorContainerStateBelow(swipeState, RefreshDirection.Start)
        IndicatorMode.Boundary -> rememberIndicatorContainerStateBoundary(swipeState, RefreshDirection.Start)
    }
}

@Composable
internal fun rememberEndIndicatorContainerState(): IndicatorContainerState {
    val swipeState = checkNotNull(LocalFSwipeRefreshState.current)
    return when (swipeState.endIndicatorMode) {
        IndicatorMode.Above -> rememberIndicatorContainerStateAbove(swipeState, RefreshDirection.End)
        IndicatorMode.Drag -> rememberIndicatorContainerStateDrag(swipeState, RefreshDirection.End)
        IndicatorMode.Below -> rememberIndicatorContainerStateBelow(swipeState, RefreshDirection.End)
        IndicatorMode.Boundary -> rememberIndicatorContainerStateBoundary(swipeState, RefreshDirection.End)
    }
}

/**
 * [IndicatorContainerState] for [IndicatorMode.Above].
 */
@Composable
private fun rememberIndicatorContainerStateAbove(
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
private fun rememberIndicatorContainerStateDrag(
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
private fun rememberIndicatorContainerStateBelow(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
): IndicatorContainerState {
    return remember(swipeRefreshState, direction) {
        IndicatorContainerStateBelow(swipeRefreshState, direction)
    }
}

/**
 * [IndicatorContainerState] for [IndicatorMode.Boundary].
 */
@Composable
private fun rememberIndicatorContainerStateBoundary(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
): IndicatorContainerState {
    return remember(swipeRefreshState, direction) {
        IndicatorContainerStateBoundary(swipeRefreshState, direction)
    }
}