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
import com.intellij.openapi.util.io.FileUtil
import java.io.File

class DownloadAndOpenFileTask(
    project: Project,
    private val tree: RemoteFileSystemTree,
) : Task.Modal(
    project,
    RemoteFileAccessBundle.message("command.RemoteFileAccess.downloadAndOpenFile.name"),
    true
) {

    @Volatile
    var localTempFile: File? = null

    override fun run(indicator: ProgressIndicator) {
        val tempFs = TempVirtualFileSystem.Holder.getInstance()
        val remoteFileToEdit = tree.getSelectedFile() ?: return
        val remoteFileSize = remoteFileToEdit.getLength().toDouble()

        val possibleLocalTempFile = tempFs.findFileByPath(remoteFileToEdit.getPath())

        val toOpen = if (possibleLocalTempFile != null) {
            OpenFileDescriptor(project, possibleLocalTempFile)
        } else {
            val remoteOperations = RemoteOperations.getInstance(project)

            when (val result = remoteOperations.fileInputStream(remoteFileToEdit)) {
                is Ok -> {
                    val remoteFileInputStream = result.value
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    val newLocalTempFile = FileUtil.createTempFile(remoteFileToEdit.getName(), ".tmp", false)
                    this.localTempFile = newLocalTempFile

                    newLocalTempFile.outputStream().use { localFileOutputStream ->
                        remoteFileInputStream.use { remoteFileInputStream ->
                            var read = remoteFileInputStream.read(buffer)
                            var readTotal = read
                            indicator.text = remoteFileToEdit.getPath()
                            while (read != -1) {
                                indicator.checkCanceled()
                                localFileOutputStream.write(buffer, 0, read)
                                indicator.fraction = readTotal / remoteFileSize
                                read = remoteFileInputStream.read(buffer)
                                readTotal += read
                            }
                        }
                    }
                    val localTempVirtualFile = tempFs.wrapIntoTempFile(remoteFileToEdit, newLocalTempFile)
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

    override fun onCancel() {
        removeTempFile()
    }

    override fun onThrowable(error: Throwable) {
        removeTempFile()
    }

    private fun removeTempFile() {
        if (localTempFile != null) {
            val localTempFile = localTempFile ?: return
            val tempFs = TempVirtualFileSystem.Holder.getInstance()
            val localTempFileInFs = tempFs.findFileByPath(localTempFile.path)
            if (localTempFileInFs != null) {
                ProcessIOExecutorService.INSTANCE.execute {
                    localTempFileInFs.delete(this)
                }
            } else {
                ProcessIOExecutorService.INSTANCE.execute {
                    FileUtil.delete(localTempFile)
                }
            }
        }
    }
}
