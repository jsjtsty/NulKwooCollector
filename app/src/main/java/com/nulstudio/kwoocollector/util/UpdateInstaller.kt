package com.nulstudio.kwoocollector.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings

data class UpdateDownloadSnapshot(
    val status: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val reason: Int
) {
    val progress: Float?
        get() = if (totalBytes > 0) {
            downloadedBytes.toFloat() / totalBytes.toFloat()
        } else {
            null
        }
}

object UpdateInstaller {
    private const val ApkMimeType = "application/vnd.android.package-archive"

    fun enqueueApkDownload(
        context: Context,
        url: String,
        versionName: String
    ): Long? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return null
        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType(ApkMimeType)
            .setTitle("开悟数据收集平台")
            .setDescription("正在下载版本 $versionName")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "kwoo-collector-${sanitizeVersionName(versionName)}.apk"
            )

        return runCatching { downloadManager.enqueue(request) }.getOrNull()
    }

    fun queryDownloadSnapshot(context: Context, downloadId: Long): UpdateDownloadSnapshot? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return null
        val query = DownloadManager.Query().setFilterById(downloadId)

        return downloadManager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null

            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloadedBytes =
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val totalBytes =
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))

            UpdateDownloadSnapshot(
                status = status,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                reason = reason
            )
        }
    }

    fun resolveDownloadedApkUri(context: Context, downloadId: Long): Uri? {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return null
        return runCatching { downloadManager.getUriForDownloadedFile(downloadId) }.getOrNull()
    }

    fun canInstallPackages(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()
    }

    fun buildUnknownSourcesIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        )
    }

    fun installApk(context: Context, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, ApkMimeType)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun describeFailure(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "下载中断，无法继续"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "未找到可用存储设备"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "更新包文件已存在"
            DownloadManager.ERROR_FILE_ERROR -> "无法写入更新包"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "下载数据异常"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "存储空间不足"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "下载链接重定向过多"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "服务器返回了无效响应"
            DownloadManager.ERROR_UNKNOWN -> "更新下载失败"
            else -> "更新下载失败"
        }
    }

    private fun sanitizeVersionName(versionName: String): String {
        return versionName.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}
