package com.example

import android.app.Application
import com.example.data.repository.ControllerRepository

class SalimApplication : Application() {

    lateinit var repository: ControllerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = ControllerRepository.getInstance(this)
    }
}
