package com.github.ikorennoy.remoteaccess.edit

import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class TemporaryFilesEditorManagerListener: FileEditorManagerListener.Before {

    override fun beforeFileClosed(source: FileEditorManager, file: VirtualFile) {
        if (file is TempVirtualFile) {
            val documentManager = FileDocumentManager.getInstance()
            if (documentManager.isFileModified(file)) {
                val res = Messages.showOkCancelDialog(
                    "Do you want to save the changes you made to ${file.name}?",
                    "Save Changes",
                    "Save Changes",
                    "Cancel",
                    Messages.getQuestionIcon()
                )
                if (res == Messages.OK) {
                    val syncService = service<RemoteEditService>()
                    val document = documentManager.getCachedDocument(file)
                    if (document != null) {
                        documentManager.saveDocument(document)
                    }
                    syncService.uploadFileToRemote(source.project, file)
                }
            }
        }
    }
}