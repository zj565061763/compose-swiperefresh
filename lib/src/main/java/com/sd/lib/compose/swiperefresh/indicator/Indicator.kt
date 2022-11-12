package com.sd.lib.compose.swiperefresh.indicator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.sd.lib.compose.swiperefresh.*

/**
 * Indicator container for the start direction.
 */
@Composable
fun StartIndicatorContainer(
    state: IndicatorContainerState? = null,
    modifier: Modifier = Modifier,
    indicator: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
) {
    val swipeState = checkNotNull(LocalFSwipeRefreshState.current)

    val containerState = state ?: when (swipeState.startIndicatorMode) {
        IndicatorMode.Above -> rememberIndicatorContainerStateAbove(swipeState, RefreshDirection.Start)
        IndicatorMode.Drag -> rememberIndicatorContainerStateDrag(swipeState, RefreshDirection.Start)
        IndicatorMode.Below -> rememberIndicatorContainerStateBelow(swipeState, RefreshDirection.Start)
        IndicatorMode.Invisible -> rememberIndicatorContainerStateInvisible(swipeState, RefreshDirection.Start)
    }

    FSwipeRefreshIndicatorContainer(
        state = containerState,
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
    state: IndicatorContainerState? = null,
    modifier: Modifier = Modifier,
    indicator: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
) {
    val swipeState = checkNotNull(LocalFSwipeRefreshState.current)

    val containerState = state ?: when (swipeState.endIndicatorMode) {
        IndicatorMode.Above -> rememberIndicatorContainerStateAbove(swipeState, RefreshDirection.End)
        IndicatorMode.Drag -> rememberIndicatorContainerStateDrag(swipeState, RefreshDirection.End)
        IndicatorMode.Below -> rememberIndicatorContainerStateBelow(swipeState, RefreshDirection.End)
        IndicatorMode.Invisible -> rememberIndicatorContainerStateInvisible(swipeState, RefreshDirection.End)
    }

    FSwipeRefreshIndicatorContainer(
        state = containerState,
        modifier = modifier,
    ) {
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

