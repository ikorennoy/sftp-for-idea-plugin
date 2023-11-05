package com.github.ikorennoy.remoteaccess.edit

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
        }, "Downloading File", null)
    }

    fun uploadFileToRemote(project: Project, localTempFile: TempVirtualFile) {
        val upload = UploadToRemoteFileTask(project, localTempFile)
        CommandProcessor.getInstance().executeCommand(project, {
            upload.queue()
        }, "Uploading File", null)
    }
}
