package com.areka.app.core.di

import android.content.Context
import com.areka.app.core.curriculum.DefaultQuestionBank
import com.areka.app.core.curriculum.QuestionBank
import com.areka.app.core.repository.AppFlashcardRepository
import com.areka.app.core.repository.AppMistakeRepository
import com.areka.app.core.repository.AppProfileRepository
import com.areka.app.core.repository.AppProgressRepository
import com.areka.app.core.repository.AppQuizRepository
import com.areka.app.core.repository.IFlashcardRepository
import com.areka.app.core.repository.IMistakeRepository
import com.areka.app.core.repository.IProfileRepository
import com.areka.app.core.repository.IProgressRepository
import com.areka.app.core.repository.IQuizRepository
import com.areka.app.core.sync.DefaultSyncRepository
import com.areka.app.core.sync.SyncRepository
import com.areka.app.data.local.AppDatabase
import com.areka.app.data.remote.SupabaseAuth
import com.areka.app.data.remote.SupabaseCloudSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Lightweight, production-grade service locator for V2 architecture.
 * Ensures clean dependency injection into ViewModels without heavy DI frameworks.
 */
class AppContainer(val context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database: AppDatabase by lazy { AppDatabase.getInstance(context) }

    val questionBank: QuestionBank by lazy { DefaultQuestionBank() }
    val syncRepository: SyncRepository by lazy { DefaultSyncRepository() }

    val profileRepository: IProfileRepository by lazy {
        AppProfileRepository({ database }, syncRepository, appScope)
    }

    val mistakeRepository: IMistakeRepository by lazy {
        AppMistakeRepository({ database }, appScope)
    }

    val flashcardRepository: IFlashcardRepository by lazy {
        AppFlashcardRepository({ database }, questionBank, appScope)
    }

    val quizRepository: IQuizRepository by lazy {
        AppQuizRepository({ database }, questionBank, syncRepository, appScope)
    }

    val progressRepository: IProgressRepository by lazy {
        AppProgressRepository(
            databaseProvider = { database },
            questionBank = questionBank,
            profileRepository = profileRepository,
            mistakeRepository = mistakeRepository,
            flashcardRepository = flashcardRepository
        )
    }

    init {
        SupabaseAuth.initialize(context)
        SupabaseCloudSync.initialize(context)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
