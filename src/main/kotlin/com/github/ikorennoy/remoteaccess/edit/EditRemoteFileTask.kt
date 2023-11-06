package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.openapi.project.Project

class EditRemoteFileTask(
    private val project: Project,
    private val tree: RemoteFileSystemTree,
) : Runnable {

    override fun run() {
        RemoteEditService.getInstance(project).downloadAndOpenFile(tree)
    }
}
