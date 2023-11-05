package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service
class RemoteEditService {

    fun downloadAndOpenFile(project: Project, tree: RemoteFileSystemTree) {
        val downloadAndOpen = DownloadAndOpenFileTask(project, tree)
        CommandProcessor.getInstance().executeCommand(project, {
            downloadAndOpen.queue()
        }, RemoteFileAccessBundle.message("command.RemoteFileAccess.downloadAndOpenFile.name"), null)
    }

    fun uploadFileToRemote(project: Project, localTempFile: TempVirtualFile) {
        val upload = UploadToRemoteFileTask(project, localTempFile)
        CommandProcessor.getInstance().executeCommand(project, {
            upload.queue()
        }, RemoteFileAccessBundle.message("command.RemoteFileAccess.uploadFileToRemote.name"), null)
    }
}
