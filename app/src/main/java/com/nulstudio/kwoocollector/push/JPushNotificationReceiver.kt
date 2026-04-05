package com.nulstudio.kwoocollector.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nulstudio.kwoocollector.MainActivity
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JPushNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ActionNotificationOpened, ActionNotificationClickAction, ActionMessageReceived -> {
                val formId = extractFormId(intent)
                if (formId > 0) {
                    PushNavigationCenter.publish(FormPushTarget(formId))

                    if (intent.action == ActionNotificationOpened || intent.action == ActionNotificationClickAction) {
                        context.startActivity(
                            Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra(PushNavigationCenter.ExtraFormId, formId)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun extractFormId(intent: Intent): Int {
        val extrasJson = intent.getStringExtra(ExtraPayload).orEmpty()
        if (extrasJson.isBlank()) return -1

        return runCatching {
            Json.parseToJsonElement(extrasJson)
                .jsonObject["formId"]
                ?.jsonPrimitive
                ?.content
                ?.toInt()
                ?: -1
        }.getOrElse {
            Log.w(Tag, "Failed to parse JPush payload: $extrasJson", it)
            -1
        }
    }

    private companion object {
        const val Tag = "JPushReceiver"
        const val ExtraPayload = "cn.jpush.android.EXTRA"
        const val ActionMessageReceived = "cn.jpush.android.intent.MESSAGE_RECEIVED"
        const val ActionNotificationOpened = "cn.jpush.android.intent.NOTIFICATION_OPENED"
        const val ActionNotificationClickAction = "cn.jpush.android.intent.NOTIFICATION_CLICK_ACTION"
    }
}
