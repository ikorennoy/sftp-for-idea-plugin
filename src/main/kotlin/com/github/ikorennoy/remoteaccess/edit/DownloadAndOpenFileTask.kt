package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.Er
import com.github.ikorennoy.remoteaccess.Ok
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.operations.RemoteOperationsNotifier
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.github.ikorennoy.remoteaccess.tree.RemoteFileSystemTree
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.UIBundle

class DownloadAndOpenFileTask(
    project: Project,
    private val tree: RemoteFileSystemTree,
) : Task.Modal(
    project,
    RemoteFileAccessBundle.message("command.RemoteFileAccess.downloadAndOpenFile.name"),
    true
) {

    @Volatile
    var localTempVirtualFile: TempVirtualFile? = null

    override fun run(indicator: ProgressIndicator) {
        val tempFs = TempVirtualFileSystem.Holder.getInstance()
        val remoteFileToEdit = tree.getSelectedFile() ?: return
        val remoteFileSize = remoteFileToEdit.getLength().toDouble()
        val remoteOperations = RemoteOperations.getInstance(project)

        val possibleLocalTempFile = tempFs.findFileByPath(remoteFileToEdit.getUri())

        val toOpen = if (possibleLocalTempFile != null) {
            OpenFileDescriptor(project, possibleLocalTempFile)
        } else {
            if (remoteFileToEdit.isSpecial()) {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        RemoteFileAccessBundle.message("dialog.RemoteFileAccess.cannotOpenSpecialFile.message"),
                        UIBundle.message("error.dialog.title"),
                    )
                }
                null
            } else {
                indicator.checkCanceled()
                when (val res = remoteOperations.fileInputStream(remoteFileToEdit)) {
                    is Ok -> {
                        val remoteFileInputStream = res.value
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                        indicator.checkCanceled()
                        val newLocalTempFile = FileUtil.createTempFile(remoteFileToEdit.getName(), ".tmp", false)
                        val newLocalTempVirtualFile = tempFs.wrapIntoTempFile(remoteFileToEdit, newLocalTempFile)

                        this.localTempVirtualFile = newLocalTempVirtualFile

                        newLocalTempFile.outputStream().use { localFileOutputStream ->
                            remoteFileInputStream.use { remoteFileInputStream ->
                                indicator.fraction = 0.0
                                indicator.text = remoteFileToEdit.getUri()
                                indicator.isIndeterminate = false
                                var read = remoteFileInputStream.read(buffer)
                                var readTotal = read
                                while (read != -1) {
                                    indicator.checkCanceled()
                                    localFileOutputStream.write(buffer, 0, read)
                                    indicator.fraction = readTotal / remoteFileSize
                                    read = remoteFileInputStream.read(buffer)
                                    readTotal += read
                                }
                            }
                        }

                        OpenFileDescriptor(project, newLocalTempVirtualFile)
                    }

                    is Er -> {
                        RemoteOperationsNotifier.getInstance(project)
                            .cannotOpenFile(remoteFileToEdit.getPathFromRemoteRoot(), res.error)
                        null
                    }
                }
            }
        }

        if (toOpen != null) {
            // to check if file is writable we need to make one more request to the server
            // we have to do it here because otherwise it will be done on EDT during file opening
            toOpen.file.isWritable
            ApplicationManager.getApplication().invokeLater {
                toOpen.navigate(true)
            }
        }
    }

    override fun onCancel() {
        removeTempFile()
    }

    override fun onThrowable(error: Throwable) {
        RemoteOperationsNotifier.getInstance(project)
            .genericDownloadError(error)
        removeTempFile()
        super.onThrowable(error)
    }

    private fun removeTempFile() {
        if (localTempVirtualFile != null) {
            val localTempFile = localTempVirtualFile ?: return
            ProcessIOExecutorService.INSTANCE.execute {
                localTempFile.delete(this)
            }
        }
    }
}
