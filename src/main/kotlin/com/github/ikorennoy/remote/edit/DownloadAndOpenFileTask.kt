package com.github.ikorennoy.remote.edit

import com.github.ikorennoy.remote.tree.RemoteFileSystemTree
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil


class DownloadAndOpenFileTask(
    project: Project,
    private val tree: RemoteFileSystemTree,
) : Task.Modal(project, "Downloading File", false) {


    override fun run(indicator: ProgressIndicator) {
        val tempFs = TempVirtualFileSystem.getInstance()
        val remoteFileToEdit = tree.getSelectedFile() ?: return
        val remoteFileSize = remoteFileToEdit.getLength().toDouble()

        val possibleLocalTempFile = tempFs.findFileByPath(remoteFileToEdit.getPath())

        val toOpen = if (possibleLocalTempFile != null) {
            OpenFileDescriptor(project, possibleLocalTempFile)
        } else {
            val remoteFileInputStream = remoteFileToEdit.getInputStream() ?: return

            val buffer = ByteArray(1024)
            val localTempFile = FileUtil.createTempFile(remoteFileToEdit.getName(), ".tmp", false)

            localTempFile.outputStream().use { localFileOutputStream ->
                remoteFileInputStream.use { remoteFileInputStream ->
                    var read = remoteFileInputStream.read(buffer)
                    var readTotal = read
                    indicator.text = remoteFileToEdit.getPath()
                    while (read != -1) {
                        localFileOutputStream.write(buffer, 0, read)
                        indicator.checkCanceled()
                        indicator.fraction = readTotal / remoteFileSize
                        read = remoteFileInputStream.read(buffer)
                        readTotal += read
                    }
                }
            }

            val localTempVirtualFile = tempFs.wrapIntoTempFile(remoteFileToEdit, localTempFile)
            OpenFileDescriptor(project, localTempVirtualFile)
        }

        ApplicationManager.getApplication().invokeLater {
            toOpen.navigate(true)
        }
    }
}