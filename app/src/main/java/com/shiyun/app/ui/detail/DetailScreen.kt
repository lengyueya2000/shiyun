package com.shiyun.app.ui.detail

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun DetailScreen(poemId: Long, onBack: () -> Unit) {
    Text("详情 $poemId")
}
