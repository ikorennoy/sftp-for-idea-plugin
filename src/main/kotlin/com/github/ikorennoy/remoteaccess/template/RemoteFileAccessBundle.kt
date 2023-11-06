package com.github.ikorennoy.remoteaccess.template

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.RemoteFileAccess"

object RemoteFileAccessBundle : DynamicBundle(BUNDLE) {

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    @Suppress("unused")
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)

    fun unknownReason(): String {
        return message("dialog.RemoteFileAccess.reason.unknown")
    }
}
