package com.sd.demo.compose_swiperefresh

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sd.demo.compose_swiperefresh.ui.theme.AppTheme
import com.sd.lib.compose.swiperefresh.FSwipeRefresh
import com.sd.lib.compose.swiperefresh.IndicatorMode
import com.sd.lib.compose.swiperefresh.rememberFSwipeRefreshState

class SampleBoundaryModeActivity : ComponentActivity() {
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
    viewModel: MainViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val state = rememberFSwipeRefreshState().apply {
        // Set 'Boundary' mode for the end direction.
        this.endIndicatorMode = IndicatorMode.Boundary
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
        modifier = Modifier.fillMaxWidth()
    ) {
        ListView(list = uiState.list, isLoadingMore = uiState.isLoadingMore)
    }

    LaunchedEffect(state) {
        snapshotFlow { state.refreshState }
            .collect {
                logMsg { "$it ${state.flingEndState}" }
            }
    }
}

@Composable
private fun ListView(list: List<String>, isLoadingMore: Boolean) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(list) { item ->
            ColumnViewItem(text = item)
        }

        item(contentType = "footer") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            )
        }
    }
}