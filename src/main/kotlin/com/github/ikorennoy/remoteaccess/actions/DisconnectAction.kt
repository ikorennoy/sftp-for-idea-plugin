package com.github.ikorennoy.remoteaccess.actions

import com.github.ikorennoy.remoteaccess.notifyUpdateFullTree
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class DisconnectAction : DumbAwareAction(
    RemoteFileAccessBundle.messagePointer("action.RemoteFileAccess.disconnect.text"),
    AllIcons.CodeWithMe.CwmTerminate
) {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        e.presentation.isEnabled = RemoteOperations.getInstance(project).isInitializedAndConnected()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ProcessIOExecutorService.INSTANCE.execute {
            RemoteOperations.getInstance(project).close()
            notifyUpdateFullTree()
        }
    }
}
