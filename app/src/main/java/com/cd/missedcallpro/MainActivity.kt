package com.cd.missedcallpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.cd.missedcallpro.ui.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as App

        setContent {
            val nav = rememberNavController()
            val store = remember { app.store }
            val formStore = remember { app.formStore }

            MaterialTheme {
                Surface {
                    AppNavGraph(nav = nav, store = store, formStore = formStore)
                }
            }
        }
    }
}