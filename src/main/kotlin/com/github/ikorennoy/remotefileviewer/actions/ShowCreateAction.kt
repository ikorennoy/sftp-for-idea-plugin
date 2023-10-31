package com.github.ikorennoy.remotefileviewer.actions

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory

class ShowCreateAction: DumbAwareAction("Create New...") {

    override fun actionPerformed(e: AnActionEvent) {
        val newFile = ActionManager.getInstance().getAction("RemoteFileSystem.NewFileAction")
        val newFolder = ActionManager.getInstance().getAction("FileChooser.NewFolder")
        val group = DefaultActionGroup(newFile, newFolder)
        JBPopupFactory.getInstance()
            .createActionGroupPopup("New", group, e.dataContext,
                false, true, false, null, 30, null)
            .showInBestPositionFor(e.dataContext)
    }
}