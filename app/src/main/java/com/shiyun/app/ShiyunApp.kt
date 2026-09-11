package com.shiyun.app

import android.app.Application
import android.util.Log
import com.shiyun.app.data.assets.AssetsPoemImporter
import com.shiyun.app.data.db.ShiyunDatabase
import com.shiyun.app.data.repository.DailyRepository
import com.shiyun.app.data.repository.FavoriteRepository
import com.shiyun.app.data.repository.PoemRepository
import com.shiyun.app.data.repository.QuizRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: android.content.Context) {
    val database: ShiyunDatabase = ShiyunDatabase.build(context)
    val poemRepository = PoemRepository(database.poemDao(), AssetsPoemImporter(context))
    val favoriteRepository = FavoriteRepository(database.favoriteDao())
    val dailyRepository = DailyRepository(database.dailyRecordDao())
    val quizRepository = QuizRepository(database.quizStateDao())
}

class ShiyunApp : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            try {
                container.poemRepository.importIfNeeded()
            } catch (e: Exception) {
                Log.w("ShiyunApp", "首次导入诗词数据失败,待 UI 层重试", e)
            }
        }
    }
}
