package com.rkjewellery.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.rkjewellery.app.ui.navigation.RKNavHost
import com.rkjewellery.app.ui.theme.RKJewelleryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RKJewelleryTheme {
                Root()
            }
        }
    }
}

@Composable
private fun Root() {
    val nav = rememberNavController()
    RKNavHost(nav)
}
