package com.shiyun.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiyun.app.ShiyunApp
import com.shiyun.app.data.db.Poem
import com.shiyun.app.ui.theme.FadedInk

private val DYNASTIES = listOf("先秦", "汉", "魏晋", "唐", "宋", "元", "明清")
private val KINDS = listOf("诗", "词")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpenPoem: (Long) -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ShiyunApp
    val vm: LibraryViewModel = viewModel { LibraryViewModel(app.container) }
    val state by vm.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("搜索标题、诗句或作者") },
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DYNASTIES.forEach { dynasty ->
                FilterChip(
                    selected = state.dynasty == dynasty,
                    onClick = { vm.onDynastyChange(if (state.dynasty == dynasty) null else dynasty) },
                    label = { Text(dynasty) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KINDS.forEach { kind ->
                FilterChip(
                    selected = state.kind == kind,
                    onClick = { vm.onKindChange(if (state.kind == kind) null else kind) },
                    label = { Text(kind) },
                )
            }
        }

        if (state.results.isEmpty()) {
            Text(
                if (state.searched) "未找到匹配的诗词" else "加载中…",
                modifier = Modifier.padding(top = 32.dp),
                color = FadedInk,
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.results, key = { it.id }) { poem ->
                    PoemListItem(poem, onClick = { onOpenPoem(poem.id) })
                }
            }
        }
    }
}

@Composable
private fun PoemListItem(poem: Poem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(poem.title, style = MaterialTheme.typography.titleMedium)
        Text("${poem.dynasty} · ${poem.author} · ${poem.paragraphs.firstOrNull().orEmpty()}",
            style = MaterialTheme.typography.bodyMedium, color = FadedInk)
    }
}
