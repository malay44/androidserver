package com.example.androidserver

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Start the foreground service
        val serviceIntent = Intent(this, BackgroundServer::class.java)
        startService(serviceIntent)
    }
}