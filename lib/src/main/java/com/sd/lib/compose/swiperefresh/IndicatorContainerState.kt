package com.sd.lib.compose.swiperefresh

import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource


interface IndicatorContainerState {
    /**
     * Direction of the container.
     */
    val direction: RefreshDirection

    /**
     * The current offset for the container.
     */
    val offset: Int
}


interface ContainerApiForIndicator {
    /**
     * The width or height of the container, which is determined by the [FSwipeRefreshState.orientationMode].
     */
    val containerSize: Int
    /**
     * The distance between the container idle state and the refreshing state.
     */
    val refreshingDistance: Int

    /**
     * The distance at which a refresh can be triggered when the drag is released.
     */
    val refreshTriggerDistance: Int

    /**
     * Max drag distance.
     */
    val maxDragDistance: Int

    /**
     * Whether the drag distance is greater than the [refreshTriggerDistance].
     */
    val reachRefreshDistance: Boolean

    /**
     * A suggested progress value bounds in [0-1].
     */
    val progress: Float

    /**
     * [refreshingDistance]
     */
    fun setRefreshingDistance(distance: Int?)

    /**
     * [refreshTriggerDistance]
     */
    fun setRefreshTriggerDistance(distance: Int?)

    /**
     * By default, the [progress] value is calculated from [FSwipeRefreshState.sharedOffset] and [refreshTriggerDistance].
     *
     * For example, if you want to calculate progress value start from the drag distance is greater than [containerSize],
     * pass the [containerSize] value to the [distance] parameter.
     */
    fun setIgnoredProgressDistance(distance: Int?)

    /**
     * Register a [callback], it will be called when hide refreshing.
     */
    fun registerHideRefreshing(callback: suspend () -> Unit)

    /**
     * Unregister the [callback].
     */
    fun unregisterHideRefreshing(callback: suspend () -> Unit)
}


interface ContainerApiForSwipeRefresh {
    /**
     * Called when this api is attached.
     * See [FSwipeRefreshState.startContainerApi] [FSwipeRefreshState.endContainerApi]
     */
    fun onAttach(api: SwipeRefreshApiForContainer)

    /**
     * [NestedScrollConnection.onPreScroll]
     */
    fun onPreScroll(available: Float, source: NestedScrollSource): Float

    /**
     * [NestedScrollConnection.onPostScroll]
     */
    fun onPostScroll(available: Float, source: NestedScrollSource): Float

    /**
     * Called when the drag is released.
     */
    fun onRelease(): Boolean

    /**
     * Called when [FSwipeRefreshState.sharedOffset] is changed.
     */
    fun onOffsetChanged()

    /**
     * Show refreshing.
     */
    suspend fun showRefreshing(): Boolean

    /**
     * Hide refreshing.
     */
    suspend fun hideRefreshing(anim: Boolean): Boolean

    /**
     * Called when this api is detached.
     * See [FSwipeRefreshState.startContainerApi] [FSwipeRefreshState.endContainerApi]
     */
    fun onDetach()
}