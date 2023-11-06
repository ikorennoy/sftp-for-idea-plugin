package com.github.ikorennoy.remoteaccess.edit

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class CleanupTempFsListener : FileEditorManagerListener {

    override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        if (file is TempVirtualFile) {
            ProcessIOExecutorService.INSTANCE.execute {
                file.delete(this)
            }
        }
    }
}
