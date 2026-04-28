package com.abplua.qiitare.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abplua.qiitare.data.models.Item
import com.abplua.qiitare.data.models.Tag

@Composable fun ItemItem(item: Item) {
    Column {
        Row {
            UserIcon(
                item.user.profile_image_url,
            )
            Column {
                Text(
                    text = item.user.name ?: "",
                    modifier = Modifier.padding(start = 8.dp)
                )
                Text(
                    text = item.updated_at,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Text(
            text = item.title,
            modifier = Modifier.padding(start = 8.dp)
        )

        Row {
            item.tags.forEach { tag ->
                Tag(tag.name)
            }
        }
    }
}
