package com.run.peakflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.retainedComponent
import com.run.peakflow.presentation.components.RootComponent

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Use retainedComponent to keep the root component alive across configuration changes
        val rootComponent = retainedComponent { componentContext ->
            RootComponent(componentContext)
        }

        enableEdgeToEdge()

        setContent {
            App(rootComponent)
        }
    }
}
