package com.abplua.qiitare.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abplua.qiitare.data.models.Item
import com.abplua.qiitare.data.models.Tag

@Composable fun ItemItem(item: Item) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserIcon(
                    item.user.profile_image_url,
                )
                Column {
                    Text(
                        text = item.user.name ?: "",
                        modifier = Modifier.padding(start = 8.dp),
                        color = Color.Gray
                    )
                    Text(
                        text = item.updated_at.substring(0, 10),
                        modifier = Modifier.padding(start = 8.dp),
                        color = Color.Gray,
                        fontSize = 9.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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
}
