package com.sd.demo.compose_swiperefresh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.compose_swiperefresh.ui.theme.AppTheme
import com.sd.lib.compose.swiperefresh.FSwipeRefresh
import com.sd.lib.compose.swiperefresh.IndicatorMode
import com.sd.lib.compose.swiperefresh.rememberFSwipeRefreshState

class SampleVerticalActivity : ComponentActivity() {
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
    // Your ui state.
    val uiState by viewModel.uiState.collectAsState()

    // Remember FSwipeRefreshState.
    val state = rememberFSwipeRefreshState().apply {
        // Set indicator mode for the start direction.
        startIndicatorMode = IndicatorMode.Above

        // Set indicator mode for the end direction.
        endIndicatorMode = IndicatorMode.Above
    }

    LaunchedEffect(uiState.isRefreshing) {
        // Synchronize ui state in the start direction, 'onRefreshStart' this will not be called when 'isRefreshing' is true.
        state.refreshStart(uiState.isRefreshing)
    }
    LaunchedEffect(uiState.isLoadingMore) {
        // Synchronize ui state in the end direction. 'onRefreshEnd' will not be called when 'isLoadingMore' is true.
        state.refreshEnd(uiState.isLoadingMore)
    }

    LaunchedEffect(Unit) {
        viewModel.refresh(20)
    }

    FSwipeRefresh(
        state = state,
        onRefreshStart = {
            // Refresh in the start direction.
            logMsg { "onRefreshStart" }
            viewModel.refresh(20)
        },
        onRefreshEnd = {
            // Refresh in the end direction.
            logMsg { "onRefreshEnd" }
            viewModel.loadMore()
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        ColumnView(list = uiState.list)
    }

    LaunchedEffect(state) {
        snapshotFlow { state.refreshState }
            .collect {
                logMsg { "refreshState $it ${state.flingEndState}" }
            }
    }
}