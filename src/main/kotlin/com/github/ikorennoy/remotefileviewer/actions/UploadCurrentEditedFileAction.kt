package com.github.ikorennoy.remotefileviewer.actions

import com.github.ikorennoy.remotefileviewer.remote.RemoteFileInformation
import com.github.ikorennoy.remotefileviewer.remoteEdit.RemoteEditService
import com.github.ikorennoy.remotefileviewer.remoteEdit.TempVirtualFile
import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.ui.UIBundle

open class UploadCurrentEditedFileAction : DumbAwareAction({ "Upload Current Remote File" }, AllIcons.Actions.MenuSaveall) {

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val files = FileEditorManager.getInstance(project).selectedFiles
            for (file in files) {
                if (file is TempVirtualFile) {
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
            if (file is TempVirtualFile) {
                val confirmationMessage = createConfirmationMessage(file.remoteFile)
                val returnValue = Messages.showOkCancelDialog(
                    confirmationMessage,
                    "Confirmation",
                    "Yes",
                    CommonBundle.getCancelButtonText(), Messages.getQuestionIcon()
                )
                if (returnValue == Messages.OK) {
                    val document = documentManager.getCachedDocument(file)
                    if (document != null) {
                        documentManager.saveDocument(document)
                    }
                    val syncService = service<RemoteEditService>()
                    syncService.uploadFileToRemote(project, file)
                }
            }
        }
    }

    private fun createConfirmationMessage(file: RemoteFileInformation): String {
        return "Do you want to upload ${file.getName()}"
    }
}
