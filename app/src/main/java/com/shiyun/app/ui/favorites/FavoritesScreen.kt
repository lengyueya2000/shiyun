package com.shiyun.app.ui.favorites

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiyun.app.ShiyunApp
import com.shiyun.app.ui.theme.FadedInk

@Composable
fun FavoritesScreen(onOpenPoem: (Long) -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ShiyunApp
    val vm: FavoritesViewModel = viewModel { FavoritesViewModel(app.container) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("收藏", style = MaterialTheme.typography.headlineMedium)
        if (state.poems.isEmpty()) {
            Text("还没有收藏,去诗词库逛逛吧", color = FadedInk)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.poems, key = { it.id }) { poem ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenPoem(poem.id) }.padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(poem.title, style = MaterialTheme.typography.titleMedium)
                        Text("${poem.dynasty} · ${poem.author}", style = MaterialTheme.typography.bodyMedium, color = FadedInk)
                    }
                }
            }
        }
    }
}
