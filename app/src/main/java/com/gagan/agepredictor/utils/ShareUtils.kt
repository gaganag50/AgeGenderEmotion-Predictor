package com.gagan.agepredictor.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.gagan.agepredictor.R

object ShareUtils {

    fun openUrlInBrowser(context: Context, url: String) {
        val defaultBrowserPackageName = getDefaultBrowserPackageName(context)
        if (defaultBrowserPackageName == "android") {
            // no browser set as default

            openInDefaultApp(context, url)
        } else {

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .setPackage(defaultBrowserPackageName)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }


    private fun openInDefaultApp(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(
            Intent.createChooser(
                intent, context.getString(R.string.share_dialog_title)
            )
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }


    private fun getDefaultBrowserPackageName(context: Context): String {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://"))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val resolveInfo = context.packageManager.resolveActivity(
            intent, PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo!!.activityInfo.packageName
    }


}
