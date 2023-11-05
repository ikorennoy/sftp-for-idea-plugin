package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class UnsavedChangesListener : FileEditorManagerListener.Before {

    override fun beforeFileClosed(source: FileEditorManager, file: VirtualFile) {
        if (file is TempVirtualFile) {
            val documentManager = FileDocumentManager.getInstance()
            if (documentManager.isFileModified(file)) {
                val res = Messages.showOkCancelDialog(
                    RemoteFileAccessBundle.message("dialog.RemoteFileAccess.beforeFileClosed.message", file.name),
                    RemoteFileAccessBundle.message("dialog.RemoteFileAccess.beforeFileClosed.title"),
                    RemoteFileAccessBundle.message("dialog.RemoteFileAccess.beforeFileClosed.okText"),
                    RemoteFileAccessBundle.message("dialog.RemoteFileAccess.beforeFileClosed.cancelText"),
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