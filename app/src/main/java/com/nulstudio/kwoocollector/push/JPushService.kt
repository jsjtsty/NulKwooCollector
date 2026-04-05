package com.nulstudio.kwoocollector.push

import android.content.Context
import android.util.Log
import cn.jpush.android.api.JPushInterface
import com.nulstudio.kwoocollector.BuildConfig

object JPushService {
    private const val Tag = "JPushService"

    fun initialize(context: Context) {
        JPushInterface.setDebugMode(BuildConfig.DEBUG)
        JPushInterface.init(context)
    }

    fun bindAlias(context: Context, rawAlias: String) {
        val alias = sanitizeAlias(rawAlias) ?: return
        runCatching {
            JPushInterface.setAlias(context, alias.hashCode(), alias)
        }.onFailure {
            Log.w(Tag, "Failed to bind alias", it)
        }
    }

    fun clearAlias(context: Context) {
        runCatching {
            JPushInterface.deleteAlias(context, 0)
        }.onFailure {
            Log.w(Tag, "Failed to clear alias", it)
        }
    }

    private fun sanitizeAlias(rawAlias: String): String? {
        val alias = rawAlias.trim()
            .replace(Regex("[^A-Za-z0-9_\\-]"), "_")
            .take(40)
        return alias.ifBlank { null }
    }
}
