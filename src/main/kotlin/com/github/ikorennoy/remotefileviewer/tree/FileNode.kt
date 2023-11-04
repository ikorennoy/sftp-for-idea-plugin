package com.github.ikorennoy.remotefileviewer.tree

import com.github.ikorennoy.remotefileviewer.filesystem.RemoteVirtualFile

class FileNode(
    private val file: RemoteVirtualFile,
) {

    fun getChildren(): Array<FileNode> {
        return emptyArray()
    }
}