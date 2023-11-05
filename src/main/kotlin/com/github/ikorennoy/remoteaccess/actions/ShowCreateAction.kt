package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory

class ShowCreateAction : DumbAwareAction(
    RemoteFileAccessBundle.messagePointer("action.RemoteFileAccess.showCreate.text")
) {

    override fun actionPerformed(e: AnActionEvent) {
        val newFile = ActionManager.getInstance().getAction("RemoteFileAccess.NewFileAction")
        val newFolder = ActionManager.getInstance().getAction("RemoteFileAccess.NewDirectoryAction")
        val group = DefaultActionGroup(newFile, newFolder)
        JBPopupFactory.getInstance()
            .createActionGroupPopup(
                RemoteFileAccessBundle.message("action.RemoteFileAccess.showCreate.title"), group, e.dataContext,
                false, true, false, null, 30, null
            )
            .showInBestPositionFor(e.dataContext)
    }
}
