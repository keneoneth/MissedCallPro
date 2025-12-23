package com.example.missedcallpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.example.missedcallpro.data.AppStateStore
import com.example.missedcallpro.ui.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val nav = rememberNavController()
            val store = remember { AppStateStore(this) }

            MaterialTheme {
                Surface {
                    AppNavGraph(nav = nav, store = store)
                }
            }
        }
    }
}