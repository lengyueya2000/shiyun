package com.shiyun.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shiyun.app.AppContainer
import com.shiyun.app.ShiyunApp
import com.shiyun.app.ui.theme.FadedInk

@Composable
fun HomeScreen(onOpenPoem: (Long) -> Unit) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ShiyunApp
    val vm: HomeViewModel = viewModel { HomeViewModel(app.container) }
    val state by vm.state.collectAsStateWithLifecycle()

    when {
        state.loading -> Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { CircularProgressIndicator() }

        else -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("今日一诗", style = MaterialTheme.typography.headlineMedium)
                Text("连续打卡 ${state.streak} 天", style = MaterialTheme.typography.bodyMedium, color = FadedInk)
            }

            val poem = state.poem
            if (poem != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenPoem(poem.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(poem.title, style = MaterialTheme.typography.titleLarge)
                        Text("${poem.dynasty} · ${poem.author}", style = MaterialTheme.typography.bodyMedium, color = FadedInk)
                        poem.paragraphs.take(2).forEach { line ->
                            Text(line, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

                Button(
                    onClick = vm::checkIn,
                    enabled = !state.checkedIn,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(if (state.checkedIn) "今日已打卡" else "打卡")
                }
            }
        }
    }
}
