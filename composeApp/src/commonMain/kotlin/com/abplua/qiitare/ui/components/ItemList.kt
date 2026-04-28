package com.abplua.qiitare.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abplua.qiitare.data.models.Item
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.StateFlow

@Composable
fun ItemList(
    itemFlow: StateFlow<List<Item>>,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit = {},
) {
    val items by itemFlow.collectAsState()
    val gridState = rememberLazyStaggeredGridState()

    LaunchedEffect(gridState, items.size) {
        snapshotFlow {
            val lastVisibleIndex = gridState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index }
                ?: return@snapshotFlow false
            lastVisibleIndex >= items.lastIndex - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onLoadMore()
            }
    }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        state = gridState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
    ) {
        items(
            items = items,
            key = { item -> item.id },
        ) { item ->
            ItemItem(item)
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 3
