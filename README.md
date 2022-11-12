# Gradle

[![](https://jitpack.io/v/zj565061763/compose-swiperefresh.svg)](https://jitpack.io/#zj565061763/comopse-swiperefresh)

# Sample

|                               Vertical                               |                              Horizontal                              |                           Custom behavior                            |
|:--------------------------------------------------------------------:|:--------------------------------------------------------------------:|:--------------------------------------------------------------------:|
| <img src="https://thumbsnap.com/i/dzmQ8ztV.gif?1112" width="240px"/> | <img src="https://thumbsnap.com/i/mm53qzRT.gif?1112" width="240px"/> | <img src="https://thumbsnap.com/i/z2YcGXim.gif?1112" width="240px"/> |

```kotlin
// Your ui state.
val uiState by viewModel.uiState.collectAsState()

// Remember FSwipeRefreshState.
val state = rememberFSwipeRefreshState()

LaunchedEffect(uiState.isRefreshing) {
    // Synchronize ui state in the start direction, 'onRefreshStart' this will not be called when 'isRefreshing' is true.
    state.refreshStart(uiState.isRefreshing)
}
LaunchedEffect(uiState.isLoadingMore) {
    // Synchronize ui state in the end direction. 'onRefreshEnd' will not be called when 'isLoadingMore' is true.
    state.refreshEnd(uiState.isLoadingMore)
}

FSwipeRefresh(
    state = state,
    onRefreshStart = {
        // Refresh in the start direction.
    },
    onRefreshEnd = {
        // Refresh in the end direction.
    },
    // ...
) {
    LazyColumn {
        //...
    }
}

// Horizontal orientation.
FSwipeRefresh(
    orientationMode = OrientationMode.Horizontal,
    // ...
) {
    LazyRow {
        //...
    }
}
```

# Indicator style

It is easy to customize the indicator style, here is a quick sample:

![](https://thumbsnap.com/i/GBcgB2gr.gif?1112)

```kotlin

FSwipeRefresh(
    indicatorStart = {
        // Custom indicator style for the start direction.
        CustomizedIndicator()
    },
    //...
) {
    //...
}

@Composable
private fun CustomizedIndicator() {
    // Get the FSwipeRefreshState.
    val state = checkNotNull(LocalFSwipeRefreshState.current)

    // Get the container api which is provided to the indicator.
    val containerApi = checkNotNull(LocalContainerApiForIndicator.current)

    Box(
        modifier = Modifier.fillMaxWidth().height(50.dp),
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
```

Well that is it, if you want to learn more about how to customize the indicator style check the
[`DefaultSwipeRefreshIndicator`](https://github.com/zj565061763/compose-swiperefresh/blob/master/lib/src/main/java/com/sd/lib/compose/swiperefresh/indicator/DefaultSwipeRefreshIndicator.kt)
[`ContainerApiForIndicator`](https://github.com/zj565061763/compose-swiperefresh/blob/master/lib/src/main/java/com/sd/lib/compose/swiperefresh/IndicatorContainerState.kt)

# Indicator mode

|                                Above                                 |                                 Drag                                 |
|:--------------------------------------------------------------------:|:--------------------------------------------------------------------:|
| <img src="https://thumbsnap.com/i/oqvD6znE.gif?1112" width="320px"/> | <img src="https://thumbsnap.com/i/dKZ7i7dt.gif?1112" width="320px"/> |

|                                Below                                 |                              Invisible                               |  
|:--------------------------------------------------------------------:|:--------------------------------------------------------------------:|
| <img src="https://thumbsnap.com/i/dmKAyDcX.gif?1112" width="320px"/> | <img src="https://thumbsnap.com/i/MrdmMTgY.gif?1110" width="320px"/> |

```kotlin

val state = rememberFSwipeRefreshState().apply {
    // Set indicator mode for the start direction. 'Above' is the default indicator mode.
    startIndicatorMode = IndicatorMode.Above

    // Set indicator mode for the end direction.
    endIndicatorMode = IndicatorMode.Above
}

FSwipeRefresh(
    state = state,
    // ...
) {
    //...
}

```

The behavior of `IndicatorMode` is controlled by `ContainerApiForSwipeRefresh`. You can customize this behavior by provide the implementation of `ContainerApiForSwipeRefresh` to the `FSwipeRefreshState`. Check
[`IndicatorContainerState`](https://github.com/zj565061763/compose-swiperefresh/blob/master/lib/src/main/java/com/sd/lib/compose/swiperefresh/IndicatorContainerState.kt)
and
[`IndicatorContainerStateImpl`](https://github.com/zj565061763/compose-swiperefresh/blob/master/lib/src/main/java/com/sd/lib/compose/swiperefresh/IndicatorContainerStateImpl.kt)
for more information.

<br>

In `IndicatorMode.Invisible` mode, `onRefreshXXX` will be called when the scrollable layout is scrolled to the bounds, you should add a refreshing indicator to scrollable layout manually.
[click here](https://github.com/zj565061763/compose-swiperefresh/blob/master/app/src/main/java/com/sd/demo/compose_swiperefresh/SampleIndicatorModeActivity.kt)
for details.

Here is an example of how to customize the indicator mode behavior,
[click here](https://github.com/zj565061763/compose-swiperefresh/blob/master/app/src/main/java/com/sd/demo/compose_swiperefresh/SampleCustomModeActivity.kt)
for details.
![](https://thumbsnap.com/i/z2YcGXim.gif?1112)