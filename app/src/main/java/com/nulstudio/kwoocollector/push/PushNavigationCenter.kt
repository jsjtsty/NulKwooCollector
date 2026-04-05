package com.nulstudio.kwoocollector.push

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FormPushTarget(
    val formId: Int
)

object PushNavigationCenter {
    const val ExtraFormId = "push_form_id"

    private val _pendingTarget = MutableStateFlow<FormPushTarget?>(null)
    val pendingTarget: StateFlow<FormPushTarget?> = _pendingTarget.asStateFlow()

    fun publish(target: FormPushTarget) {
        _pendingTarget.value = target
    }

    fun consume() {
        _pendingTarget.value = null
    }

    fun handleIntent(intent: Intent?) {
        val formId = intent?.getIntExtra(ExtraFormId, -1) ?: -1
        if (formId > 0) {
            publish(FormPushTarget(formId = formId))
        }
    }
}
