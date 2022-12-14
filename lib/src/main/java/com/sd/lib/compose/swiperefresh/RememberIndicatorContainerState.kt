package com.sd.lib.compose.swiperefresh

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberStartIndicatorContainerState(): IndicatorContainerState {
    val state = checkNotNull(LocalFSwipeRefreshState.current)
    val mode = state.startIndicatorMode
    return remember(state, mode) {
        when (mode) {
            IndicatorMode.Above -> IndicatorContainerStateAbove(state, RefreshDirection.Start)
            IndicatorMode.Drag -> IndicatorContainerStateDrag(state, RefreshDirection.Start)
            IndicatorMode.Below -> IndicatorContainerStateBelow(state, RefreshDirection.Start)
            IndicatorMode.Boundary -> IndicatorContainerStateBoundary(state, RefreshDirection.Start)
        }
    }
}

@Composable
internal fun rememberEndIndicatorContainerState(): IndicatorContainerState {
    val state = checkNotNull(LocalFSwipeRefreshState.current)
    val mode = state.endIndicatorMode
    return remember(state, mode) {
        when (mode) {
            IndicatorMode.Above -> IndicatorContainerStateAbove(state, RefreshDirection.End)
            IndicatorMode.Drag -> IndicatorContainerStateDrag(state, RefreshDirection.End)
            IndicatorMode.Below -> IndicatorContainerStateBelow(state, RefreshDirection.End)
            IndicatorMode.Boundary -> IndicatorContainerStateBoundary(state, RefreshDirection.End)
        }
    }
}