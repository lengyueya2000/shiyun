package com.shiyun.app

import android.app.Application
import com.shiyun.app.data.assets.AssetsPoemImporter
import com.shiyun.app.data.db.ShiyunDatabase
import com.shiyun.app.data.repository.PoemRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: android.content.Context) {
    val database: ShiyunDatabase = ShiyunDatabase.build(context)
    val poemRepository = PoemRepository(database.poemDao(), AssetsPoemImporter(context))
}

class ShiyunApp : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch { container.poemRepository.importIfNeeded() }
    }
}
