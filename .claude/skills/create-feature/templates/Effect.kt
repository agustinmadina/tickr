package com.example.app.feature.{{FEATURE_PACKAGE}}.ui

internal sealed interface {{PASCAL}}Effect {
    data class ShowError(val message: String) : {{PASCAL}}Effect
}
