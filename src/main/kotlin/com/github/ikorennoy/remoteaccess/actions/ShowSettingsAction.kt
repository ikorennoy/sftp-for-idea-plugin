package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.settings.RemoteFileAccessConfigurable
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction

class ShowSettingsAction : DumbAwareAction(
    RemoteFileAccessBundle.messagePointer("action.RemoteFileAccess.showSettings.text"),
    AllIcons.General.Settings
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            RemoteFileAccessConfigurable::class.java
        )
    }
}
