package com.sd.demo.compose_swiperefresh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.compose_swiperefresh.ui.theme.AppTheme
import com.sd.lib.compose.swiperefresh.*

class SampleCustomStyleActivity : ComponentActivity() {
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
        // Set 'Drag' mode for the start direction.
        it.startIndicatorMode = IndicatorMode.Drag

        // Set 'Drag' mode for the end direction.
        it.endIndicatorMode = IndicatorMode.Drag
    }

    LaunchedEffect(Unit) {
        viewModel.refresh(20)
    }

    FSwipeRefresh(
        state = state,
        isRefreshingStart = uiState.isRefreshing,
        isRefreshingEnd = uiState.isLoadingMore,
        onRefreshStart = {
            logMsg { "onRefreshStart" }
            viewModel.refresh(20)
        },
        onRefreshEnd = {
            logMsg { "onRefreshEnd" }
            viewModel.loadMore()
        },
        indicatorStart = {
            // Custom indicator style for the start direction.
            CustomizedIndicator()
        },
        indicatorEnd = {
            // Custom indicator style for the end direction.
            CustomizedIndicator()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        ColumnView(list = uiState.list)
    }

    LaunchedEffect(state) {
        snapshotFlow { state.refreshState }
            .collect {
                logMsg { "$it ${state.flingEndState} ${state.currentDirection}" }
            }
    }
}


@Composable
private fun CustomizedIndicator() {
    // Get the FSwipeRefreshState.
    val state = checkNotNull(LocalFSwipeRefreshState.current)

    // Get the container api which is provided to the indicator.
    val containerApi = checkNotNull(LocalContainerApiForIndicator.current)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(50.dp),
        contentAlignment = Alignment.Center
    ) {

        val text = if (state.isRefreshing) {
            "Refreshing..."
        } else {
            if (containerApi.reachRefreshDistance && state.refreshState == RefreshState.Drag) {
                "Release to refresh"
            } else {
                "Pull to refresh"
            }
        }

        Text(text)
    }
}