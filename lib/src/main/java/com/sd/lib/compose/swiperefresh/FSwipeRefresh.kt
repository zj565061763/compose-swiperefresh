package com.sd.lib.compose.swiperefresh

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import com.sd.lib.compose.swiperefresh.indicator.DefaultSwipeRefreshIndicator
import com.sd.lib.compose.swiperefresh.indicator.FSwipeRefreshIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

val LocalFSwipeRefreshState = staticCompositionLocalOf<FSwipeRefreshState?> { null }

@Composable
fun rememberFSwipeRefreshState(): FSwipeRefreshState {
    val coroutineScope = rememberCoroutineScope()
    return remember { FSwipeRefreshState(coroutineScope) }
}

@Composable
fun FSwipeRefresh(
    state: FSwipeRefreshState = rememberFSwipeRefreshState(),
    onRefreshStart: (() -> Unit)? = null,
    onRefreshEnd: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    orientationMode: OrientationMode = OrientationMode.Vertical,
    indicatorStart: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
    indicatorEnd: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
    indicator: @Composable () -> Unit = {
        FSwipeRefreshIndicator(
            start = indicatorStart,
            end = indicatorEnd
        )
    },
    content: @Composable () -> Unit,
) {
    SwipeRefresh(
        state = state,
        modifier = modifier,
        orientationMode = orientationMode,
        onRefreshStart = onRefreshStart,
        onRefreshEnd = onRefreshEnd,
        indicator = indicator,
        content = content,
    )
}


@Composable
private fun SwipeRefresh(
    state: FSwipeRefreshState,
    modifier: Modifier = Modifier,
    orientationMode: OrientationMode,
    onRefreshStart: (() -> Unit)? = null,
    onRefreshEnd: (() -> Unit)? = null,
    indicator: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }

    state._orientationMode = orientationMode
    state._onRefreshStart = onRefreshStart
    state._onRefreshEnd = onRefreshEnd
    state.layoutSize = layoutSize

    val nestedScrollConnection = remember(state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return state.handlePreScroll(available, source)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                return state.handlePostScroll(available, source)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (state.handleRelease()) return available
                return super.onPreFling(available)
            }
        }
    }

    Box(
        modifier = modifier
            .zIndex(0f)
            .nestedScroll(nestedScrollConnection)
            .onSizeChanged { layoutSize = it }
            .clipToBounds()
    ) {
        Box(
            modifier = Modifier.offset {
                when (state.orientationMode) {
                    OrientationMode.Vertical -> IntOffset(0, state.contentOffset.roundToInt())
                    OrientationMode.Horizontal -> IntOffset(state.contentOffset.roundToInt(), 0)
                }
            }
        ) {
            content()
        }

        val indicatorZIndex = if (state.currentIndicatorMode == IndicatorMode.Below) {
            -1f
        } else {
            1f
        }

        Box(
            modifier = Modifier.zIndex(indicatorZIndex)
        ) {
            CompositionLocalProvider(LocalFSwipeRefreshState provides state) {
                indicator()
            }
        }
    }
}

class FSwipeRefreshState internal constructor(
    val coroutineScope: CoroutineScope
) {
    /**
     * [OrientationMode]
     */
    var orientationMode: OrientationMode by mutableStateOf(OrientationMode.Vertical)
        private set

    /**
     * Size of the SwipeRefresh layout.
     */
    var layoutSize: IntSize by mutableStateOf(IntSize.Zero)
        internal set

    /**
     * The currently active direction.
     */
    var currentDirection: RefreshDirection? by mutableStateOf(null)
        private set

    /**
     * Shared offset between containers.
     */
    var sharedOffset: Float by mutableStateOf(0f)
        private set

    /**
     * [RefreshStateRecord]
     */
    var refreshStateRecord: RefreshStateRecord by mutableStateOf(RefreshStateRecord())
        private set

    /**
     * Current state.
     */
    val refreshState: RefreshState by derivedStateOf { refreshStateRecord.state }

    /**
     * This is a future state when [RefreshState.Fling] ends,
     * it should not be null when [refreshState] is [RefreshState.Fling].
     */
    var flingEndState: RefreshState? by mutableStateOf(null)
        private set

    /**
     * Whether this state is currently refreshing or not.
     */
    val isRefreshing: Boolean by derivedStateOf {
        refreshState == RefreshState.Refreshing || flingEndState == RefreshState.Refreshing
    }

    /**
     * Indicator mode for the start direction.
     */
    var startIndicatorMode: IndicatorMode by mutableStateOf(IndicatorMode.Above)
    /**
     * Indicator mode for the end direction.
     */
    var endIndicatorMode: IndicatorMode by mutableStateOf(IndicatorMode.Above)

    /**
     * Container api for the start direction.
     */
    var startContainerApi: ContainerApiForSwipeRefresh? by createContainerApiDelegate(RefreshDirection.Start)
    /**
     * Container api for the end direction.
     */
    var endContainerApi: ContainerApiForSwipeRefresh? by createContainerApiDelegate(RefreshDirection.End)

    internal val currentIndicatorMode by derivedStateOf {
        when (currentDirection) {
            RefreshDirection.Start -> startIndicatorMode
            RefreshDirection.End -> startIndicatorMode
            else -> null
        }
    }

    @Volatile
    private var _resetInProgress = false

    internal var _orientationMode by Delegates.observable(orientationMode) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            reset(newValue)
        }
    }
    internal var _onRefreshStart by mutableStateOf<(() -> Unit)?>(null)
    internal var _onRefreshEnd by mutableStateOf<(() -> Unit)?>(null)

    internal var contentOffset by mutableStateOf(0f)
        private set

    private val _containerJobs: MutableMap<Job, String> = ConcurrentHashMap()

    private val _animOffset = Animatable(0f)

    private var _internalOffset = 0f
        set(value) {
            val safeValue = when (currentDirection) {
                RefreshDirection.Start -> value.coerceAtLeast(0f)
                RefreshDirection.End -> value.coerceAtMost(0f)
                else -> 0f
            }
            if (field != safeValue) {
                field = safeValue
                sharedOffset = safeValue
                currentDirection?.containerApi()?.onOffsetChanged()
            }
        }

    private var _refreshState by Delegates.observable(RefreshState.None) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            refreshStateRecord = RefreshStateRecord(
                state = newValue,
                previous = oldValue,
            )

            if (flingEndState == newValue) {
                flingEndState = null
            }

            when (newValue) {
                RefreshState.None -> {
                    flingEndState = null
                    currentDirection = null
                }
                else -> checkNotNull(currentDirection) { "Direction is null." }
            }
        }
    }

    private var _orientationHandler = createOrientationHandler(_orientationMode)

    /**
     * Synchronize ui state in the start direction, 'onRefreshStart' this will not be called when [refresh] is true.
     */
    suspend fun refreshStart(refresh: Boolean) {
        if (refresh) {
            showRefreshing(RefreshDirection.Start)
        } else {
            hideRefreshing(RefreshDirection.Start, true)
        }
    }

    /**
     * Synchronize ui state in the end direction. 'onRefreshEnd' will not be called when [refresh] is true.
     */
    suspend fun refreshEnd(refresh: Boolean) {
        if (refresh) {
            showRefreshing(RefreshDirection.End)
        } else {
            hideRefreshing(RefreshDirection.End, true)
        }
    }

    internal suspend fun syncRefreshingState() {
        if (isRefreshing) {
            when (currentDirection) {
                RefreshDirection.Start -> refreshStart(true)
                RefreshDirection.End -> refreshEnd(true)
                else -> {}
            }
        }
    }

    internal fun handlePreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (_resetInProgress) return Offset.Zero
        return _orientationHandler.handlePreScroll(available, source)
    }

    internal fun handlePostScroll(available: Offset, source: NestedScrollSource): Offset {
        if (_resetInProgress) return Offset.Zero
        return _orientationHandler.handlePostScroll(available, source)
    }

    internal fun handleRelease(): Boolean {
        if (_resetInProgress) return false
        return _orientationHandler.handleRelease().also {
            if (_refreshState == RefreshState.Drag && _internalOffset == 0f) {
                _refreshState = RefreshState.None
            }
        }
    }

    private fun createContainerApiDelegate(direction: RefreshDirection): ReadWriteProperty<Any?, ContainerApiForSwipeRefresh?> {
        return Delegates.observable(null) { _, oldValue, newValue ->
            if (oldValue != newValue) {
                oldValue?.onDetach()
                newValue?.onAttach(createSwipeRefreshApiForContainer(direction))
            }
        }
    }

    private fun createSwipeRefreshApiForContainer(refreshDirection: RefreshDirection): SwipeRefreshApiForContainer {
        return object : SwipeRefreshApiForContainer {
            override fun setDirection() {
                currentDirection?.let { current ->
                    if (current != refreshDirection) {
                        error("Can not set direction $refreshDirection, current direction $current .")
                    }
                }
                currentDirection = refreshDirection
            }

            override fun appendOffset(offset: Float) {
                if (currentDirection != refreshDirection) return

                val oldOffset = _internalOffset
                _internalOffset += offset

                if (oldOffset == 0f && oldOffset != _internalOffset) {
                    _refreshState = RefreshState.Drag
                }
            }

            override fun setContentOffset(offset: Float) {
                if (currentDirection != refreshDirection) return
                contentOffset = offset
            }

            override fun hasCallback(): Boolean {
                return currentDirection?.refreshCallback() != null
            }

            override fun notifyCallback() {
                currentDirection?.refreshCallback()?.invoke()
            }

            override fun launch(cancellable: Boolean, block: suspend CoroutineScope.() -> Unit) {
                if (currentDirection != refreshDirection) return
                coroutineScope.launch { block() }.also {
                    if (cancellable) {
                        _containerJobs[it] = ""
                    }
                }
            }

            override suspend fun animateToOffset(offset: Float, flingEndState: RefreshState) {
                if (currentDirection != refreshDirection) return
                this@FSwipeRefreshState.animateToOffset(offset, flingEndState)
            }

            override suspend fun resetOffset(anim: Boolean) {
                if (currentDirection != refreshDirection) return
                this@FSwipeRefreshState.resetOffset(anim)
            }
        }
    }

    private suspend fun showRefreshing(direction: RefreshDirection) {
        cancelContainerJobs()

        currentDirection?.let { current ->
            if (current != direction) {
                hideRefreshingOrResetOffset(current, false)
            }
        }

        // Set the direction first.
        currentDirection = direction

        direction.containerApi()?.showRefreshing()
        _refreshState = RefreshState.Refreshing
    }

    private suspend fun hideRefreshing(direction: RefreshDirection, anim: Boolean) {
        if (isRefreshing && direction == currentDirection) {
            cancelContainerJobs()
            hideRefreshingOrResetOffset(direction, anim)
        }
    }

    private suspend fun hideRefreshingOrResetOffset(direction: RefreshDirection, anim: Boolean) {
        if (direction.containerApi()?.hideRefreshing(anim) == true) {
            // Ignore
        } else {
            resetOffset(anim)
        }
    }

    private suspend fun resetOffset(anim: Boolean) {
        if (anim) {
            animateToOffset(0f, RefreshState.None)
        } else {
            _animOffset.stop()
        }
        _internalOffset = 0f
        _refreshState = RefreshState.None
    }

    private suspend fun animateToOffset(offset: Float, futureState: RefreshState) {
        if (_internalOffset == offset) return

        if (_animOffset.isRunning && _animOffset.targetValue == offset
            && flingEndState == futureState
        ) {
            // Nothing changed.
            return
        }

        flingEndState = futureState
        _refreshState = RefreshState.Fling

        _animOffset.snapTo(_internalOffset)
        _animOffset.animateTo(offset) { _internalOffset = value }
    }

    private fun reset(mode: OrientationMode) {
        _resetInProgress = true
        cancelContainerJobs()

        coroutineScope.launch {
            try {
                currentDirection?.containerApi()?.hideRefreshing(false)
                resetOffset(false)
            } finally {
                if (orientationMode != mode) {
                    orientationMode = mode
                    _orientationHandler = createOrientationHandler(mode)
                }
                _resetInProgress = false
            }
        }
    }

    private fun createOrientationHandler(mode: OrientationMode): OrientationHandler {
        return when (mode) {
            OrientationMode.Vertical -> VerticalHandler()
            OrientationMode.Horizontal -> HorizontalHandler()
        }
    }

    private fun cancelContainerJobs() {
        _containerJobs.forEach {
            it.key.cancel()
            _containerJobs.remove(it.key)
        }
    }

    private abstract inner class OrientationHandler {
        abstract fun unpack(offset: Offset): Float
        abstract fun packConsume(offset: Float): Offset

        abstract fun handlePreScroll(available: Offset, source: NestedScrollSource): Offset
        abstract fun handlePostScroll(available: Offset, source: NestedScrollSource): Offset

        fun handleRelease(): Boolean {
            val direction = currentDirection ?: return false
            val api = direction.containerApi() ?: return false
            return api.onRelease()
        }
    }

    //-------------------- Default Handler --------------------

    private abstract inner class DefaultHandler : OrientationHandler() {

        override fun handlePreScroll(available: Offset, source: NestedScrollSource): Offset {
            val direction = currentDirection ?: return Offset.Zero
            val api = direction.containerApi() ?: return Offset.Zero

            val change = unpack(available)
            val consume = api.onPreScroll(change, source)

            return packConsume(consume)
        }

        override fun handlePostScroll(available: Offset, source: NestedScrollSource): Offset {
            val change = unpack(available)

            if (_onRefreshStart != null) {
                val startConsume = startContainerApi?.onPostScroll(change, source)
                if (startConsume != null && startConsume != 0f) {
                    return packConsume(startConsume)
                }
            }

            if (_onRefreshEnd != null) {
                val endConsume = endContainerApi?.onPostScroll(change, source)
                if (endConsume != null && endConsume != 0f) {
                    return packConsume(endConsume)
                }
            }

            return Offset.Zero
        }
    }

    private inner class VerticalHandler : DefaultHandler() {
        override fun unpack(offset: Offset): Float = offset.y
        override fun packConsume(offset: Float): Offset = Offset(0f, offset)
    }

    private inner class HorizontalHandler : DefaultHandler() {
        override fun unpack(offset: Offset): Float = offset.x
        override fun packConsume(offset: Float): Offset = Offset(offset, 0f)
    }

    private fun RefreshDirection.containerApi(): ContainerApiForSwipeRefresh? {
        return when (this) {
            RefreshDirection.Start -> startContainerApi
            RefreshDirection.End -> endContainerApi
        }
    }

    private fun RefreshDirection.refreshCallback(): (() -> Unit)? {
        return when (this) {
            RefreshDirection.Start -> _onRefreshStart
            RefreshDirection.End -> _onRefreshEnd
        }
    }
}

enum class OrientationMode {
    Vertical,
    Horizontal,
}

enum class IndicatorMode {
    Above,
    Drag,
    Below,
    Invisible,
}

enum class RefreshDirection {
    Start,
    End,
}

enum class RefreshState {
    None,
    Drag,
    Fling,
    Refreshing,
}

data class RefreshStateRecord internal constructor(
    val state: RefreshState = RefreshState.None,
    val previous: RefreshState = RefreshState.None,
)

interface SwipeRefreshApiForContainer {
    fun setDirection()

    fun appendOffset(offset: Float)

    fun setContentOffset(offset: Float)

    fun hasCallback(): Boolean

    fun notifyCallback()

    fun launch(cancellable: Boolean = true, block: suspend CoroutineScope.() -> Unit)

    suspend fun animateToOffset(offset: Float, flingEndState: RefreshState)

    suspend fun resetOffset(anim: Boolean)
}

internal fun logMsg(block: () -> String) {
    Log.i("FSwipeRefresh", block())
}