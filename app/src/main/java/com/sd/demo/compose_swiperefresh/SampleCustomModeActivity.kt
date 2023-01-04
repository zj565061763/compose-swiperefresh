package com.sd.demo.compose_swiperefresh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.compose_swiperefresh.ui.theme.AppTheme
import com.sd.lib.compose.swiperefresh.*
import com.sd.lib.compose.swiperefresh.indicator.DefaultSwipeRefreshIndicator
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

class SampleCustomModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box {
                        Sample()
                    }
                }
            }
        }
    }
}

@Composable
private fun Sample(
    viewModel: MainViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    val state = rememberFSwipeRefreshState {
        it.startIndicatorMode = IndicatorMode.Drag
    }

    LaunchedEffect(uiState.isRefreshing) {
        state.refreshStart(uiState.isRefreshing)
    }
    LaunchedEffect(uiState.isLoadingMore) {
        state.refreshEnd(uiState.isLoadingMore)
    }

    LaunchedEffect(Unit) {
        viewModel.refresh(20)
    }

    FSwipeRefresh(
        state = state,
        onRefreshStart = {
            logMsg { "onRefreshStart" }
            viewModel.refresh(20)
        },
        onRefreshEnd = {
            logMsg { "onRefreshEnd" }
            viewModel.loadMore()
        },
        indicator = {
            SwipeRefreshIndicator()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        ColumnView(
            list = uiState.list,
        )
    }

    LaunchedEffect(state) {
        snapshotFlow { state.refreshState }
            .collect {
                logMsg { "$it ${state.flingEndState} ${state.currentDirection}" }
            }
    }
}

@Composable
private fun SwipeRefreshIndicator() {
    val state = checkNotNull(LocalFSwipeRefreshState.current)
    val startContainerState = remember(state) {
        CustomizedIndicatorContainerState(state, RefreshDirection.Start)
    }

    // Indicator container for the start direction.
    StartIndicatorContainer(startContainerState) {
        StartIndicatorContent(startContainerState.isReachBounds)
    }

    // Indicator container for the end direction.
    EndIndicatorContainer()
}

@Composable
private fun StartIndicatorContent(isReachBounds: Boolean) {

    val state = checkNotNull(LocalFSwipeRefreshState.current)
    val containerApi = checkNotNull(LocalContainerApiForIndicator.current)

    var indicatorSize by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = state.sharedOffset.absoluteValue / containerApi.containerSize
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.image),
                contentDescription = "new ui",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            Button(
                onClick = {
                    state.coroutineScope.launch {
                        state.refreshStart(false)
                    }
                }
            ) {
                Text(text = "Go back.")
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            val showTipsText = isReachBounds && state.refreshState == RefreshState.Drag
            if (showTipsText) {
                Text(text = "Try release drag.")
            }

            DefaultSwipeRefreshIndicator(
                modifier = Modifier
                    .onSizeChanged { indicatorSize = it }
                    .graphicsLayer {
                        alpha = if (isReachBounds) 0f else 1f
                    },
            )
        }
    }

    val indicatorHeight = indicatorSize.height
    if (indicatorHeight > 0) {
        LaunchedEffect(containerApi, indicatorHeight) {
            containerApi.setRefreshingDistance(indicatorHeight)
        }
    }
}

private class CustomizedIndicatorContainerState(
    swipeRefreshState: FSwipeRefreshState,
    direction: RefreshDirection,
) : IndicatorContainerStateDrag(swipeRefreshState, direction) {

    override val maxDragDistance: Int
        get() = containerSize

    val isReachBounds by derivedStateOf {
        swipeRefreshState.sharedOffset.absoluteValue > refreshTriggerDistance * 2f
    }

    override fun onPreFling(available: Float): Float {
        if (swipeRefreshState.refreshState != RefreshState.Drag) return 0f
        val swipeRefreshApi = swipeRefreshApi ?: return 0f

        val offset = swipeRefreshState.sharedOffset
        if (offset == 0f) return 0f

        val openNewUi = isReachBounds && available >= 0f
        if (openNewUi) {
            swipeRefreshApi.launch {
                swipeRefreshApi.animateToOffset(containerSize.toFloat(), RefreshState.Refreshing)
            }
        }

        return if (openNewUi) available else super.onPreFling(available)
    }
}