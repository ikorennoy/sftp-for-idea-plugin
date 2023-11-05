package com.github.ikorennoy.remoteaccess.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction

class ShowSettingsAction : DumbAwareAction(
    { "Show Settings" },
    AllIcons.General.Settings
) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            "com.github.ikorennoy.remotefileviewer.settings.ui.RemoteFilePluginConfigurable"
        )
    }
}
