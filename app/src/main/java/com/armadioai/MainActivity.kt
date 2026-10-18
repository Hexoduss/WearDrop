package com.armadioai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.armadioai.ui.ArmadioRoot
import com.armadioai.ui.theme.ArmadioTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ArmadioApplication).container
        setContent {
            ArmadioTheme {
                ArmadioRoot(container)
            }
        }
    }
}
