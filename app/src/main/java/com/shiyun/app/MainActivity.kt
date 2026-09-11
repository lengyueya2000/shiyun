package com.shiyun.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shiyun.app.ui.navigation.ShiyunNavHost
import com.shiyun.app.ui.theme.ShiyunTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShiyunTheme { ShiyunNavHost() } }
    }
}
