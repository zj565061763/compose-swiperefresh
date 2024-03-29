package com.sd.lib.compose.swiperefresh

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

val LocalFSwipeRefreshState = staticCompositionLocalOf<FSwipeRefreshState?> { null }

@Composable
fun rememberFSwipeRefreshState(
    onCreate: ((FSwipeRefreshState) -> Unit)? = null,
): FSwipeRefreshState {
    val coroutineScope = rememberCoroutineScope()
    return remember {
        FSwipeRefreshState(coroutineScope).also {
            onCreate?.invoke(it)
        }
    }
}

@Composable
fun FSwipeRefresh(
    modifier: Modifier = Modifier,
    state: FSwipeRefreshState = rememberFSwipeRefreshState(),
    isRefreshingStart: Boolean?,
    isRefreshingEnd: Boolean?,
    onRefreshStart: (() -> Unit)?,
    onRefreshEnd: (() -> Unit)?,
    enableSwipeStart: Boolean = onRefreshStart != null,
    enableSwipeEnd: Boolean = onRefreshEnd != null,
    orientationMode: OrientationMode = OrientationMode.Vertical,
    indicatorStart: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
    indicatorEnd: @Composable () -> Unit = { DefaultSwipeRefreshIndicator() },
    indicator: @Composable () -> Unit = {
        StartIndicatorContainer {
            indicatorStart()
        }
        EndIndicatorContainer {
            indicatorEnd()
        }
    },
    content: @Composable () -> Unit,
) {
    SwipeRefresh(
        modifier = modifier,
        state = state,
        isRefreshingStart = isRefreshingStart,
        isRefreshingEnd = isRefreshingEnd,
        orientationMode = orientationMode,
        onRefreshStart = onRefreshStart ?: {},
        onRefreshEnd = onRefreshEnd ?: {},
        enableSwipeStart = enableSwipeStart,
        enableSwipeEnd = enableSwipeEnd,
        indicator = indicator,
        content = content,
    )
}

@Composable
private fun SwipeRefresh(
    modifier: Modifier = Modifier,
    state: FSwipeRefreshState,
    isRefreshingStart: Boolean?,
    isRefreshingEnd: Boolean?,
    onRefreshStart: () -> Unit,
    onRefreshEnd: () -> Unit,
    enableSwipeStart: Boolean,
    enableSwipeEnd: Boolean,
    orientationMode: OrientationMode,
    indicator: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var layoutSize by remember { mutableStateOf(IntSize.Zero) }

    state._orientationMode = orientationMode
    state._onRefreshStart = onRefreshStart
    state._onRefreshEnd = onRefreshEnd
    state._enableSwipeStart = enableSwipeStart
    state._enableSwipeEnd = enableSwipeEnd
    state.layoutSize = layoutSize

    if (isRefreshingStart != null) {
        LaunchedEffect(state, isRefreshingStart) {
            state.refreshStart(isRefreshingStart)
        }
    }

    if (isRefreshingEnd != null) {
        LaunchedEffect(state, isRefreshingEnd) {
            state.refreshEnd(isRefreshingEnd)
        }
    }

    Box(
        modifier = modifier
            .nestedScroll(state.nestedScrollConnection)
            .onSizeChanged { layoutSize = it }
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .zIndex(0f)
                .offset {
                    when (state.orientationMode) {
                        OrientationMode.Vertical -> IntOffset(0, state.contentOffset.roundToInt())
                        OrientationMode.Horizontal -> IntOffset(state.contentOffset.roundToInt(), 0)
                    }
                },
        ) {
            content()
        }

        val containerApi = when (state.currentDirection) {
            RefreshDirection.Start -> state.startContainerApi
            RefreshDirection.End -> state.endContainerApi
            else -> null
        }

        Box(
            modifier = Modifier.zIndex(containerApi?.zIndex ?: 1f),
        ) {
            CompositionLocalProvider(LocalFSwipeRefreshState provides state) {
                indicator()
            }
        }
    }
}

class FSwipeRefreshState internal constructor(
    val coroutineScope: CoroutineScope,
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
    var sharedOffset: Float by mutableFloatStateOf(0f)
        private set

    /**
     * The current offset for the content.
     */
    var contentOffset: Float by mutableFloatStateOf(0f)
        private set

    /**
     * [RefreshStateRecord]
     */
    var refreshStateRecord: RefreshStateRecord by mutableStateOf(RefreshStateRecord())
        private set

    /**
     * Current state.
     */
    val refreshState: RefreshState by derivedStateOf { refreshStateRecord.current }

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

    internal var startContainerApi: ContainerApiForSwipeRefresh? by createContainerApiDelegate(RefreshDirection.Start)
    internal var endContainerApi: ContainerApiForSwipeRefresh? by createContainerApiDelegate(RefreshDirection.End)

    private var _resetInProgress = false
    internal var _orientationMode by Delegates.observable(orientationMode) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            reset {
                _orientationHandler = null
                orientationMode = newValue
            }
        }
    }
    internal var _onRefreshStart: () -> Unit = {}
    internal var _onRefreshEnd: () -> Unit = {}

    internal var _enableSwipeStart = true
    internal var _enableSwipeEnd = true

    private val _containerJobs: MutableMap<Job, String> = hashMapOf()

    private val _animOffset = Animatable(0f)

    private var _internalOffset = 0f
        set(value) {
            val newValue = safeOffset(value)
            val oldValue = field
            if (oldValue != newValue) {
                field = newValue

                if (oldValue == 0f && newValue != 0f) {
                    // Set the direction first.
                    if (currentDirection == null) {
                        currentDirection = if (newValue > 0) RefreshDirection.Start else RefreshDirection.End
                    }
                    if (_refreshState == RefreshState.None) {
                        _refreshState = RefreshState.Drag
                    }
                }

                sharedOffset = newValue
                currentDirection?.containerApi()?.onOffsetChanged()

                if (_refreshState == RefreshState.Drag && newValue == 0f) {
                    _refreshState = RefreshState.None
                }
            }
        }

    private var _refreshState by Delegates.observable(RefreshState.None) { _, oldValue, newValue ->
        if (oldValue != newValue) {
            refreshStateRecord = RefreshStateRecord(
                current = newValue,
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

    private var _orientationHandler: OrientationHandler? = null
    private val orientationHandler: OrientationHandler
        get() = _orientationHandler ?: when (_orientationMode) {
            OrientationMode.Vertical -> VerticalHandler()
            OrientationMode.Horizontal -> HorizontalHandler()
        }.also {
            _orientationHandler = it
        }

    /**
     * Sync ui state in the start direction, 'onRefreshStart' this will not be called when [refresh] is true.
     */
    suspend fun refreshStart(refresh: Boolean) {
        if (refresh) {
            showRefreshing(RefreshDirection.Start)
        } else {
            hideRefreshing(RefreshDirection.Start, true)
        }
    }

    /**
     * Sync ui state in the end direction. 'onRefreshEnd' will not be called when [refresh] is true.
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

    internal val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            return this@FSwipeRefreshState.handlePreScroll(available, source)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            return this@FSwipeRefreshState.handlePostScroll(available, source)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            return this@FSwipeRefreshState.handlePreFling(available)
        }
    }

    private fun handlePreScroll(available: Offset, source: NestedScrollSource): Offset {
        if (_resetInProgress) return Offset.Zero
        return orientationHandler.handlePreScroll(available, source)
    }

    private fun handlePostScroll(available: Offset, source: NestedScrollSource): Offset {
        if (_resetInProgress) return Offset.Zero
        return orientationHandler.handlePostScroll(available, source)
    }

    private fun handlePreFling(available: Velocity): Velocity {
        if (_resetInProgress) return Velocity.Zero
        return orientationHandler.handlePreFling(available).also {
            if (_refreshState == RefreshState.Drag && _internalOffset == 0f) {
                _refreshState = RefreshState.None
            }
        }
    }

    private fun safeOffset(value: Float): Float {
        return when (currentDirection) {
            RefreshDirection.Start -> value.coerceAtLeast(0f)
            RefreshDirection.End -> value.coerceAtMost(0f)
            else -> value
        }
    }

    private fun createContainerApiDelegate(direction: RefreshDirection): ReadWriteProperty<Any?, ContainerApiForSwipeRefresh?> {
        return Delegates.observable(null) { _, oldValue, newValue ->
            if (oldValue != newValue) {
                oldValue?.onDetach()
                if (currentDirection == direction) {
                    reset {
                        newValue?.onAttach(createSwipeRefreshApiForContainer(direction))
                    }
                } else {
                    newValue?.onAttach(createSwipeRefreshApiForContainer(direction))
                }
            }
        }
    }

    private fun createSwipeRefreshApiForContainer(refreshDirection: RefreshDirection): SwipeRefreshApiForContainer {
        return object : SwipeRefreshApiForContainer {

            override fun appendOffset(offset: Float) {
                val current = currentDirection
                if (current != null && current != refreshDirection) return

                _internalOffset += offset
            }

            override fun setContentOffset(offset: Float) {
                if (currentDirection != refreshDirection) return
                contentOffset = offset
            }

            override fun notifyCallback() {
                refreshDirection.refreshCallback().invoke()
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
        if (_resetInProgress) return
        cancelContainerJobs()

        val current = currentDirection
        if (current != null && current != direction) {
            hideRefreshingOrResetOffset(current, false)
        }

        // Set the direction first.
        currentDirection = direction

        direction.containerApi()?.showRefreshing()
        _refreshState = RefreshState.Refreshing
    }

    private suspend fun hideRefreshing(direction: RefreshDirection, anim: Boolean) {
        if (isRefreshing && currentDirection == direction) {
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
        try {
            if (anim) {
                animateToOffset(0f, RefreshState.None)
            } else {
                _animOffset.stop()
            }
        } finally {
            _internalOffset = 0f
            contentOffset = 0f
            _refreshState = RefreshState.None
        }
    }

    private suspend fun animateToOffset(offset: Float, futureState: RefreshState) {
        if (_internalOffset == offset) return

        if (_animOffset.isRunning && _animOffset.targetValue == offset && flingEndState == futureState) {
            // Nothing changed.
            return
        }

        flingEndState = futureState
        _refreshState = RefreshState.Fling

        _animOffset.snapTo(_internalOffset)
        _animOffset.animateTo(offset) { _internalOffset = value }
    }

    private var _resetJob: Job? = null

    private fun reset(onFinish: (() -> Unit)? = null) {
        _resetJob?.cancel()
        _resetInProgress = true
        cancelContainerJobs()
        coroutineScope.launch {
            try {
                currentDirection?.containerApi()?.hideRefreshing(false)
                resetOffset(false)
                onFinish?.invoke()
            } finally {
                _resetInProgress = false
            }
        }.also {
            _resetJob = it
        }
    }

    private fun cancelContainerJobs() {
        while (true) {
            if (_containerJobs.isEmpty()) break
            val holder = _containerJobs.keys.toTypedArray()
            holder.forEach {
                it.cancel()
                _containerJobs.remove(it)
            }
        }
    }

    private abstract inner class OrientationHandler {
        abstract fun unpack(offset: Offset): Float
        abstract fun packConsume(offset: Float): Offset

        abstract fun unpackVelocity(velocity: Velocity): Float
        abstract fun packConsumeVelocity(velocity: Float): Velocity

        abstract fun handlePreScroll(available: Offset, source: NestedScrollSource): Offset
        abstract fun handlePostScroll(available: Offset, source: NestedScrollSource): Offset

        fun handlePreFling(available: Velocity): Velocity {
            val direction = currentDirection ?: return Velocity.Zero
            val api = direction.containerApi() ?: return Velocity.Zero

            val velocity = unpackVelocity(available)
            val consume = api.onPreFling(velocity)

            return packConsumeVelocity(consume)
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

            if (_enableSwipeStart) {
                val startConsume = startContainerApi?.onPostScroll(change, source)
                if (startConsume != null && startConsume != 0f) {
                    return packConsume(startConsume)
                }
            }

            if (_enableSwipeEnd) {
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

        override fun unpackVelocity(velocity: Velocity): Float = velocity.y
        override fun packConsumeVelocity(velocity: Float): Velocity = Velocity(0f, velocity)
    }

    private inner class HorizontalHandler : DefaultHandler() {
        override fun unpack(offset: Offset): Float = offset.x
        override fun packConsume(offset: Float): Offset = Offset(offset, 0f)

        override fun unpackVelocity(velocity: Velocity): Float = velocity.x
        override fun packConsumeVelocity(velocity: Float): Velocity = Velocity(velocity, 0f)
    }

    private fun RefreshDirection.containerApi(): ContainerApiForSwipeRefresh? {
        return when (this) {
            RefreshDirection.Start -> startContainerApi
            RefreshDirection.End -> endContainerApi
        }
    }

    private fun RefreshDirection.refreshCallback(): (() -> Unit) {
        return when (this) {
            RefreshDirection.Start -> _onRefreshStart
            RefreshDirection.End -> _onRefreshEnd
        }
    }
}

enum class OrientationMode {
    Vertical, Horizontal,
}

enum class IndicatorMode {
    Above, Drag, Below, Boundary,
}

enum class RefreshDirection {
    Start, End,
}

enum class RefreshState {
    None, Drag, Fling, Refreshing,
}

data class RefreshStateRecord internal constructor(
    val current: RefreshState = RefreshState.None,
    val previous: RefreshState = RefreshState.None,
)

interface SwipeRefreshApiForContainer {
    fun appendOffset(offset: Float)

    fun setContentOffset(offset: Float)

    fun notifyCallback()

    fun launch(cancellable: Boolean = true, block: suspend CoroutineScope.() -> Unit)

    suspend fun animateToOffset(offset: Float, flingEndState: RefreshState)

    suspend fun resetOffset(anim: Boolean)
}

internal fun logMsg(block: () -> String) {
    Log.i("FSwipeRefresh", block())
}