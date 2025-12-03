package com.run.peakflow.presentation.components

import com.arkivanov.decompose.ComponentContext

class RootComponentFactory {
    fun create(componentContext: ComponentContext): RootComponent {
        return RootComponent(componentContext)
    }
}