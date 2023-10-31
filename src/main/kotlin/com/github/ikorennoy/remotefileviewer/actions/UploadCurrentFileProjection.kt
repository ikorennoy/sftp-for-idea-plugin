package com.github.ikorennoy.remotefileviewer.actions

import com.github.ikorennoy.remotefileviewer.remoteEdit.FileProjectionStateService
import com.github.ikorennoy.remotefileviewer.remoteEdit.LocalVirtualFile
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ShortcutSet
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction

class UploadCurrentFileProjection : DumbAwareAction({ "Upload Current Remote File" }, AllIcons.Actions.MenuSaveall) {

    init {
        val set = ActionManager.getInstance().getAction("SaveAll").shortcutSet
        println(set)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val files = FileEditorManager.getInstance(project).selectedFiles
            for (file in files) {
                if (file is LocalVirtualFile) {
                    e.presentation.isEnabled = true
                    return
                }
            }
        }
        e.presentation.isEnabled = false
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val documentManager = FileDocumentManager.getInstance()
        val files = FileEditorManager.getInstance(project).selectedFiles
        for (file in files) {
            if (file is LocalVirtualFile) {
                val document = documentManager.getCachedDocument(file)
                if (document != null) {
                    documentManager.saveDocument(document)
                }
                val syncService = service<FileProjectionStateService>()
                syncService.uploadFileToRemote(project, file)
            }
        }
    }
}
