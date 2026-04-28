package com.abplua.qiitare.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.abplua.qiitare.data.models.Item

@Composable fun ItemItem(item: Item) {
    Column {
        Row {
            UserIcon(
                item.user.profile_image_url,
            )

        }

        Text(
            text = item.title,
            modifier = Modifier.padding(start = 8.dp)
        )


    }
}
