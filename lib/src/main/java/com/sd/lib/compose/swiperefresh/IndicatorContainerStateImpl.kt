package com.sd.lib.compose.swiperefresh

import androidx.annotation.CallSuper
import androidx.compose.runtime.*
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

abstract class BaseIndicatorContainerState(
    val swipeRefreshState: FSwipeRefreshState,
    val direction: RefreshDirection,
) : IndicatorContainerState {

    internal var containerSizeState by mutableIntStateOf(0)

    private val _defaultOffsetState by derivedStateOf {
        when (direction) {
            RefreshDirection.Start -> -containerSizeState
            RefreshDirection.End -> {
                when (swipeRefreshState.orientationMode) {
                    OrientationMode.Vertical -> swipeRefreshState.layoutSize.height
                    OrientationMode.Horizontal -> swipeRefreshState.layoutSize.width
                }
            }
        }
    }

    override val offset: Int
        get() = _defaultOffsetState
}

abstract class ExpandedIndicatorContainerState(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
) : BaseIndicatorContainerState(swipeRefreshState, direction),
    ContainerApiForIndicator,
    ContainerApiForSwipeRefresh {

    //-------------------- ContainerApiForIndicator --------------------

    private var _refreshingDistanceState by mutableStateOf<Int?>(null)
    private var _refreshTriggerDistanceState by mutableStateOf<Int?>(null)
    private var _ignoredProgressDistanceState by mutableStateOf<Int?>(null)

    private val _hideRefreshingCallbacks: MutableMap<suspend () -> Unit, String> = hashMapOf()

    private val _progressState by derivedStateOf {
        val distance = refreshTriggerDistance
        if (distance <= 0) {
            0f
        } else {
            if (swipeRefreshState.isRefreshing) 1f
            else calculateProgress(
                ignored = (_ignoredProgressDistanceState ?: 0).toFloat(),
                distance = swipeRefreshState.sharedOffset.absoluteValue,
                total = distance.toFloat()
            )
        }.absoluteValue.coerceIn(0f, 1f)
    }

    override val containerSize: Int
        get() = containerSizeState

    override val refreshingDistance: Int by derivedStateOf {
        _refreshingDistanceState ?: containerSize
    }

    override val refreshTriggerDistance: Int by derivedStateOf {
        _refreshTriggerDistanceState ?: ((containerSize * 1.3f).toInt())
    }

    override val maxDragDistance: Int by derivedStateOf {
        maxOf(refreshingDistance, refreshTriggerDistance) * 3
    }

    override val reachRefreshDistance: Boolean by derivedStateOf {
        val triggerDistance = refreshTriggerDistance
        triggerDistance > 0 && swipeRefreshState.sharedOffset.absoluteValue >= triggerDistance
    }

    override val progress: Float
        get() = _progressState

    override fun setRefreshingDistance(distance: Int?) {
        if (distance != null) require(distance >= 0)
        _refreshingDistanceState = distance
    }

    override fun setRefreshTriggerDistance(distance: Int?) {
        if (distance != null) require(distance >= 0)
        _refreshTriggerDistanceState = distance
    }

    override fun setIgnoredProgressDistance(distance: Int?) {
        if (distance != null) require(distance >= 0)
        _ignoredProgressDistanceState = distance
    }

    override fun registerHideRefreshing(callback: suspend () -> Unit) {
        _hideRefreshingCallbacks[callback] = ""
    }

    final override fun unregisterHideRefreshing(callback: suspend () -> Unit) {
        _hideRefreshingCallbacks.remove(callback)
    }

    //-------------------- ContainerApiForSwipeRefresh --------------------

    private var _swipeRefreshApi: SwipeRefreshApiForContainer? = null
    private var _refreshingDistanceJob: Job? = null

    val swipeRefreshApi: SwipeRefreshApiForContainer?
        get() = _swipeRefreshApi

    override val zIndex: Float
        get() = 1f

    @CallSuper
    override fun onAttach(api: SwipeRefreshApiForContainer) {
        _swipeRefreshApi = api

        _refreshingDistanceJob?.cancel()
        _refreshingDistanceJob = swipeRefreshState.coroutineScope.launch {
            snapshotFlow { refreshingDistance }
                .filter { it > 0 }
                .distinctUntilChanged()
                .collect {
                    if (swipeRefreshState.currentDirection == direction) {
                        swipeRefreshState.syncRefreshingState()
                    }
                }
        }
    }

    override fun onPreScroll(available: Float, source: NestedScrollSource): Float {
        return 0f
    }

    override fun onPostScroll(available: Float, source: NestedScrollSource): Float {
        return 0f
    }

    override fun onPreFling(available: Float): Float {
        return 0f
    }

    override fun onOffsetChanged() {
    }

    override suspend fun showRefreshing(): Boolean {
        return false
    }

    @CallSuper
    override suspend fun hideRefreshing(anim: Boolean): Boolean {
        if (swipeRefreshState.isRefreshing) {
            val holder = _hideRefreshingCallbacks.keys.toTypedArray()
            holder.forEach {
                it.invoke()
            }
        }
        return false
    }

    @CallSuper
    override fun onDetach() {
        _refreshingDistanceJob?.cancel()
        _swipeRefreshApi = null
    }
}

abstract class DraggableIndicatorContainerState(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
) : ExpandedIndicatorContainerState(swipeRefreshState, direction) {

    override fun onPreScroll(available: Float, source: NestedScrollSource): Float {
        if (source != NestedScrollSource.Drag) return 0f

        if (available == 0f) return 0f
        val swipeRefreshApi = swipeRefreshApi ?: return 0f

        val maxDragDistance = maxDragDistance
        if (maxDragDistance <= 0) return 0f

        if (swipeRefreshState.refreshState == RefreshState.Drag) {
            val offset = transformOffset(available, maxDragDistance)
            swipeRefreshApi.appendOffset(offset)
            return available
        }
        return 0f
    }

    override fun onPostScroll(available: Float, source: NestedScrollSource): Float {
        if (source != NestedScrollSource.Drag) return 0f

        if (available == 0f) return 0f
        val swipeRefreshApi = swipeRefreshApi ?: return 0f

        val maxDragDistance = maxDragDistance
        if (maxDragDistance <= 0) return 0f

        if (swipeRefreshState.refreshState == RefreshState.None) {
            val offsetDirection = if (available > 0) RefreshDirection.Start else RefreshDirection.End
            if (offsetDirection == direction) {
                val offset = transformOffset(available, maxDragDistance)
                swipeRefreshApi.appendOffset(offset)
                return available
            }
        }
        return 0f
    }

    override fun onPreFling(available: Float): Float {
        if (swipeRefreshState.refreshState != RefreshState.Drag) return 0f
        val swipeRefreshApi = swipeRefreshApi ?: return 0f

        val offset = swipeRefreshState.sharedOffset
        if (offset == 0f) return 0f

        val notifyRefresh = reachRefreshDistance

        swipeRefreshApi.launch {
            if (notifyRefresh) {
                if (showRefreshing()) {
                    swipeRefreshApi.notifyCallback()
                    delay(100)
                }
            }
            hideRefreshing(true)
        }

        return if (notifyRefresh) available else 0f
    }

    override suspend fun showRefreshing(): Boolean {
        val swipeRefreshApi = swipeRefreshApi ?: return false
        val distance = refreshingDistance
        if (distance < 0) return false

        val targetOffset = if (direction == RefreshDirection.Start) distance else -distance
        swipeRefreshApi.animateToOffset(targetOffset.toFloat(), RefreshState.Refreshing)
        return true
    }

    override suspend fun hideRefreshing(anim: Boolean): Boolean {
        super.hideRefreshing(anim)
        val swipeRefreshApi = swipeRefreshApi ?: return false
        swipeRefreshApi.resetOffset(anim)
        return true
    }

    protected open fun transformOffset(available: Float, maxDragDistance: Int): Float {
        require(maxDragDistance > 0)
        val progress = (swipeRefreshState.sharedOffset / maxDragDistance).absoluteValue.coerceIn(0f, 1f)
        val multiplier = (1f - progress).coerceIn(0f, 0.5f)
        return available * multiplier
    }
}

//-------------------- IndicatorContainerState impl --------------------

/**
 * [IndicatorMode.Above]
 */
open class IndicatorContainerStateAbove(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
) : DraggableIndicatorContainerState(swipeRefreshState, direction) {

    override val offset: Int
        get() = super.offset + swipeRefreshState.sharedOffset.roundToInt()
}

/**
 * [IndicatorMode.Drag]
 */
open class IndicatorContainerStateDrag(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
) : DraggableIndicatorContainerState(swipeRefreshState, direction) {

    override val offset: Int
        get() = super.offset + swipeRefreshState.sharedOffset.roundToInt()

    override fun onOffsetChanged() {
        swipeRefreshApi?.setContentOffset(swipeRefreshState.sharedOffset)
    }
}

/**
 * [IndicatorMode.Below]
 */
open class IndicatorContainerStateBelow(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
) : DraggableIndicatorContainerState(swipeRefreshState, direction) {

    override val zIndex: Float
        get() = -1f

    override val offset: Int
        get() {
            val defaultOffset = super.offset
            val append = if (direction == RefreshDirection.Start) containerSize else -containerSize
            return defaultOffset + append
        }

    override fun onOffsetChanged() {
        swipeRefreshApi?.setContentOffset(swipeRefreshState.sharedOffset)
    }
}

/**
 * [IndicatorMode.Boundary]
 */
open class IndicatorContainerStateBoundary(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
) : ExpandedIndicatorContainerState(swipeRefreshState, direction) {

    override val offset: Int
        get() = super.offset + swipeRefreshState.sharedOffset.roundToInt()

    override val progress: Float
        get() = 0f

    override fun onPostScroll(available: Float, source: NestedScrollSource): Float {
        if (available == 0f) return 0f
        val swipeRefreshApi = swipeRefreshApi ?: return 0f

        if (swipeRefreshState.refreshState == RefreshState.None) {
            val offsetDirection = if (available > 0) RefreshDirection.Start else RefreshDirection.End
            if (offsetDirection == direction) {
                swipeRefreshApi.notifyCallback()
            }
        }
        return 0f
    }

    override suspend fun showRefreshing(): Boolean {
        val swipeRefreshApi = swipeRefreshApi ?: return false
        val distance = refreshingDistance
        if (distance < 0) return false

        val targetOffset = if (direction == RefreshDirection.Start) distance else -distance
        val delta = targetOffset - swipeRefreshState.sharedOffset
        swipeRefreshApi.appendOffset(delta)
        return true
    }
}

private fun calculateProgress(
    ignored: Float,
    distance: Float,
    total: Float,
): Float {
    require(ignored >= 0f)
    require(distance >= 0f)
    require(total >= 0f)

    if (total == 0f) return 0f
    if (ignored == 0f) return distance / total

    val newDistance = distance - ignored
    if (newDistance <= 0f) return 0f

    val newTotal = total - ignored
    if (newTotal <= 0f) return 0f

    return newDistance / newTotal
}