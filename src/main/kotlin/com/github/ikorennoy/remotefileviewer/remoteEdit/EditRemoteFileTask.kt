package com.github.ikorennoy.remotefileviewer.remoteEdit

import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
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
