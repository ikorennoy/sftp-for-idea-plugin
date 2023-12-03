package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class RemoteEditService(private val project: Project) {

    fun downloadAndOpenFile(tree: RemoteFileSystemTree) {
        val downloadAndOpenTask = DownloadAndOpenFileTask(project, tree)
        CommandProcessor.getInstance().executeCommand(project, {
            downloadAndOpenTask.queue()
        }, RemoteFileAccessBundle.message("command.RemoteFileAccess.downloadAndOpenFile.name"), null)
    }

    fun uploadFileToRemote(localTempFile: TempVirtualFile, localTempFileCanBeRemoved: Boolean = false) {
        val uploadTask = UploadToRemoteFileTask(project, localTempFile, localTempFileCanBeRemoved)
        CommandProcessor.getInstance().executeCommand(project, {
            uploadTask.queue()
        }, RemoteFileAccessBundle.message("command.RemoteFileAccess.uploadFileToRemote.name"), null)
    }

    companion object {
        fun getInstance(project: Project): RemoteEditService = project.service()
    }
}
