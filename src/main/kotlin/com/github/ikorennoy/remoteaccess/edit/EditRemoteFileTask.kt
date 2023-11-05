package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project


class EditRemoteFileTask(
    private val project: Project,
    private val tree: RemoteFileSystemTree,
) : Runnable {

    override fun run() {
        val remoteEditService = service<RemoteEditService>()
        remoteEditService.downloadAndOpenFile(project, tree)
    }
}
