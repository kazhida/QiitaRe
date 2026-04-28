package com.abplua.qiitare.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
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
    val listState = rememberLazyListState()

    LaunchedEffect(listState, items.size) {
        snapshotFlow {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@snapshotFlow false
            lastVisibleIndex >= items.lastIndex - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect {
                onLoadMore()
            }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
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
