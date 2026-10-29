package com.tpa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tpa.app.notify.NotifChannels
import com.tpa.app.notify.setupTrendWorker
import com.tpa.app.ui.TpaApp
import com.tpa.app.ui.theme.TechnicalPatternAnalysisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotifChannels.create(this)
        setupTrendWorker(this)
        val container = (application as TpaApplication).container
        setContent {
            TechnicalPatternAnalysisTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
                    TpaApp(container)
                }
            }
        }
    }
}
