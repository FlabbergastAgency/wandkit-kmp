package com.flabbergast.wandkit.core.feedback

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient

/**
 * Normalises file-picker results for [WebChromeClient.onShowFileChooser].
 *
 * [WebChromeClient.FileChooserParams.parseResult] alone is not enough on
 * modern Android: the system photo picker often puts the selection in
 * [Intent.getClipData] rather than [Intent.getData], and the deprecated
 * [Activity.startActivityForResult] path can report [Activity.RESULT_CANCELED]
 * on Android 14+ even when the user picked a file.
 */
internal object FileChooserResultParser {
    internal fun parse(resultCode: Int, data: Intent?): Array<Uri>? {
        if (resultCode != Activity.RESULT_OK) return null

        data?.clipData?.toUriArray()?.let { return it }
        data?.data?.let { return arrayOf(it) }

        return WebChromeClient.FileChooserParams.parseResult(resultCode, data)
    }

    internal fun chooserIntent(base: Intent): Intent =
        base.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    private fun ClipData.toUriArray(): Array<Uri>? {
        if (itemCount == 0) return null
        return Array(itemCount) { index -> getItemAt(index).uri }
    }
}
