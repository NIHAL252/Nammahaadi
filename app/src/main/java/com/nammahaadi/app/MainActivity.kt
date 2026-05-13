package com.nammahaadi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nammahaadi.app.navigation.NammaHaadiApp
import com.nammahaadi.app.ui.theme.NammaHaadiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NammaHaadiTheme {
                NammaHaadiApp()
            }
        }
    }
}