package com.hhgaq.drivelog

import android.app.Application

class DriveLogApp : Application() {
    lateinit var repository: DriveRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = DriveRepository(this)
    }
}
