package com.areka.app

import android.app.Application
import com.areka.app.core.di.AppContainer
import com.areka.app.data.repository.StudyRepository

class ArekaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContainer.getInstance(this)
        StudyRepository.initialize(this)
    }
}
