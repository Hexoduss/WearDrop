package com.weardrop

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.weardrop.ui.WearDropRoot
import com.weardrop.ui.theme.WearDropTheme

// AppCompatActivity is needed for the in-app language switch (per-app locales).
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as WearDropApplication).container
        setContent {
            WearDropTheme {
                WearDropRoot(container)
            }
        }
    }
}
