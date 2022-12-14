package com.sd.lib.compose.swiperefresh

import androidx.annotation.CallSuper
import androidx.compose.runtime.*
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

abstract class BaseIndicatorContainerState(
    val swipeRefreshState: FSwipeRefreshState,
    val direction: RefreshDirection,
) : IndicatorContainerState {

    private val _containerSizeState = mutableStateOf(0)

    private val _defaultOffsetState = derivedStateOf {
        when (direction) {
            RefreshDirection.Start -> -_containerSizeState.value
            RefreshDirection.End -> {
                when (swipeRefreshState.orientationMode) {
                    OrientationMode.Vertical -> swipeRefreshState.layoutSize.height
                    OrientationMode.Horizontal -> swipeRefreshState.layoutSize.width
                }
            }
        }
    }

    val containerSize: Int
        get() = _containerSizeState.value

    override val offset: Int
        get() = _defaultOffsetState.value

    fun setContainerSize(size: Int) {
        if (size < 0) return
        _containerSizeState.value = size
    }
}

abstract class ExpandedIndicatorContainerState(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
) : BaseIndicatorContainerState(swipeRefreshState, direction),
    ContainerApiForIndicator,
    ContainerApiForSwipeRefresh {

    //-------------------- Api for Indicator --------------------

    private var _refreshingDistance by mutableStateOf<Int?>(null)
    private var _refreshTriggerDistance by mutableStateOf<Int?>(null)
    private var _ignoredProgressDistance by mutableStateOf<Int?>(null)

    private val _callbackHideRefreshing: MutableMap<suspend () -> Unit, String> = ConcurrentHashMap()

    private val _stateProgress = derivedStateOf {
        val distance = refreshTriggerDistance
        if (distance <= 0) {
            0f
        } else {
            when {
                swipeRefreshState.isRefreshing -> 1f
                else -> {
                    calculateProgress(
                        ignored = (_ignoredProgressDistance ?: 0).toFloat(),
                        distance = swipeRefreshState.sharedOffset.absoluteValue,
                        total = distance.toFloat()
                    )
                }
            }
        }.absoluteValue.coerceIn(0f, 1f)
    }

    override val refreshingDistance: Int by derivedStateOf {
        _refreshingDistance ?: containerSize
    }

    override val refreshTriggerDistance: Int by derivedStateOf {
        _refreshTriggerDistance ?: containerSize
    }

    override val maxDragDistance: Int by derivedStateOf {
        maxOf(refreshingDistance, refreshTriggerDistance) * 3
    }

    override val reachRefreshDistance: Boolean by derivedStateOf {
        val triggerDistance = refreshTriggerDistance
        triggerDistance > 0 && swipeRefreshState.sharedOffset.absoluteValue >= triggerDistance
    }

    override val progress: Float
        get() = _stateProgress.value

    override fun setRefreshingDistance(distance: Int?) {
        if (distance != null) require(distance >= 0)
        _refreshingDistance = distance
    }

    override fun setRefreshTriggerDistance(distance: Int?) {
        if (distance != null) require(distance >= 0)
        _refreshTriggerDistance = distance
    }

    override fun setIgnoredProgressDistance(distance: Int?) {
        if (distance != null) require(distance >= 0)
        _ignoredProgressDistance = distance
    }

    override fun registerHideRefreshing(callback: suspend () -> Unit) {
        _callbackHideRefreshing[callback] = ""
    }

    final override fun unregisterHideRefreshing(callback: suspend () -> Unit) {
        _callbackHideRefreshing.remove(callback)
    }

    //-------------------- Api for SwipeRefresh --------------------

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
        _callbackHideRefreshing.forEach {
            it.key.invoke()
        }
        return false
    }

    @CallSuper
    override fun onDetach() {
        _refreshingDistanceJob?.cancel()
        _swipeRefreshApi = null
    }

    companion object {
        private fun calculateProgress(ignored: Float, distance: Float, total: Float): Float {
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
            if (notifyRefresh && swipeRefreshApi.hasCallback()) {
                if (showRefreshing()) {
                    swipeRefreshApi.notifyCallback()
                    delay(50)
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
        val swipeRefreshApi = swipeRefreshApi ?: return false

        if (swipeRefreshState.isRefreshing) {
            super.hideRefreshing(anim)
        }

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