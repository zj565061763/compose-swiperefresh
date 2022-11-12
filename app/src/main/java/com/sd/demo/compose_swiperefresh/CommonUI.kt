package com.sd.demo.compose_swiperefresh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension


@Composable
fun ColumnView(list: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth()
    ) {
        items(list) { item ->
            ColumnViewItem(text = item)
        }
    }
}

@Composable
fun ColumnViewItem(
    modifier: Modifier = Modifier,
    text: String,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(color = Color(0xFFCCCCCC))
    ) {
        val (refText, refDivider) = createRefs()

        Text(
            text = text,
            modifier = Modifier.constrainAs(refText) {
                centerTo(parent)
            },
        )

        Box(
            modifier
                .constrainAs(refDivider) {
                    width = Dimension.matchParent
                    height = Dimension.value(1.dp)
                    bottom.linkTo(parent.bottom)
                }
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}


@Composable
fun RowView(list: List<String>) {
    LazyRow(
        modifier = Modifier.fillMaxSize()
    ) {
        items(list) { item ->
            RowViewItem(text = item)
        }
    }
}

@Composable
fun RowViewItem(
    modifier: Modifier = Modifier,
    text: String,
) {
    ConstraintLayout(
        modifier = modifier
            .fillMaxHeight()
            .width(50.dp)
            .background(color = Color(0xFFCCCCCC))
    ) {
        val (refText, refDivider) = createRefs()

        Text(
            text = text,
            modifier = Modifier.constrainAs(refText) {
                centerTo(parent)
            },
        )

        Box(
            modifier
                .constrainAs(refDivider) {
                    height = Dimension.matchParent
                    width = Dimension.value(1.dp)
                    end.linkTo(parent.end)
                }
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}