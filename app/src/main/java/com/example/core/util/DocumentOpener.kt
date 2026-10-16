package com.example.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast

object DocumentOpener {

    private const val GOOGLE_DOCS_PACKAGE = "com.google.android.apps.docs"

    fun openGoogleDocsLink(context: Context, url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return

        val webUri = try {
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                Uri.parse("https://$trimmed")
            } else {
                Uri.parse(trimmed)
            }
        } catch (_: Exception) {
            Toast.makeText(context, "Invalid Google Docs URL", Toast.LENGTH_SHORT).show()
            return
        }

        // 1. Try opening via Google Docs official app
        val isDocsAppInstalled = try {
            val pm = context.packageManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(GOOGLE_DOCS_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(GOOGLE_DOCS_PACKAGE, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

        if (isDocsAppInstalled) {
            try {
                val appIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    setPackage(GOOGLE_DOCS_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(appIntent)
                return
            } catch (_: Exception) {
                // Fallback to browser if intent dispatching failed
            }
        }

        // 2. Fallback to default web browser
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openLocalDocument(context: Context, fileUri: Uri, filename: String = "") {
        val lower = filename.lowercase()
        val mimeType = when {
            lower.endsWith(".doc") -> "application/msword"
            lower.endsWith(".docx") -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            lower.endsWith(".pdf") -> "application/pdf"
            else -> "application/octet-stream"
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(fileUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open with").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this document: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
