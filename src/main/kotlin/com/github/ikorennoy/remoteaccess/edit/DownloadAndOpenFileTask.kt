package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.Er
import com.github.ikorennoy.remoteaccess.Ok
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.operations.RemoteOperationsNotifier
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil

class DownloadAndOpenFileTask(
    project: Project,
    private val tree: RemoteFileSystemTree,
) : Task.Modal(
    project,
    RemoteFileAccessBundle.message("command.RemoteFileAccess.downloadAndOpenFile.name"),
    true
) {

    override fun run(indicator: ProgressIndicator) {
        val tempFs = TempVirtualFileSystem.getInstance()
        val remoteFileToEdit = tree.getSelectedFile() ?: return
        val remoteFileSize = remoteFileToEdit.getLength().toDouble()

        val possibleLocalTempFile = tempFs.findFileByPath(remoteFileToEdit.getPath())

        val toOpen = if (possibleLocalTempFile != null) {
            OpenFileDescriptor(project, possibleLocalTempFile)
        } else {
            val operations = RemoteOperations.getInstance(project)

            when (val result = operations.fileInputStream(remoteFileToEdit)) {
                is Ok -> {
                    val remoteFileInputStream = result.value
                    val buffer = ByteArray(4096)
                    val localTempFile = FileUtil.createTempFile(remoteFileToEdit.getName(), ".tmp", false)
                    localTempFile.outputStream().use { localFileOutputStream ->
                        remoteFileInputStream.use { remoteFileInputStream ->
                            var read = remoteFileInputStream.read(buffer)
                            var readTotal = read
                            indicator.text = remoteFileToEdit.getPath()
                            while (read != -1) {
                                // todo we need to remove local temp file if cancelled
                                indicator.checkCanceled()
                                localFileOutputStream.write(buffer, 0, read)
                                indicator.fraction = readTotal / remoteFileSize
                                read = remoteFileInputStream.read(buffer)
                                readTotal += read
                            }
                        }
                    }
                    val localTempVirtualFile = tempFs.wrapIntoTempFile(remoteFileToEdit, localTempFile)
                    OpenFileDescriptor(project, localTempVirtualFile)
                }

                is Er -> {
                    RemoteOperationsNotifier.getInstance(project)
                        .cannotOpenFile(remoteFileToEdit.getPath(), result.error)
                    null
                }
            }
        }

        if (toOpen != null) {
            ApplicationManager.getApplication().invokeLater {
                toOpen.navigate(true)
            }
        }
    }
}
