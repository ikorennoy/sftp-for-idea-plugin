package com.github.ikorennoy.remotefileviewer.tree

import com.github.ikorennoy.remotefileviewer.remote.RemoteVirtualFile

class FileNode(
    private val file: RemoteVirtualFile,
) {

    fun getChildren(): Array<FileNode> {
        return emptyArray()
    }
}