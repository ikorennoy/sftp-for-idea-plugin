package com.github.ikorennoy.remotefileviewer.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil

class EditSettingsAction : AnAction({"Show Settings"}, AllIcons.General.Settings) {


    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(project, "com.github.ikorennoy.remotefileviewer.settings.ui.RemoteFilePluginConfigurable")
    }
}