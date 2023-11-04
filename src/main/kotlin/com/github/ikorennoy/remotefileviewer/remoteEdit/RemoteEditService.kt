package com.github.ikorennoy.remotefileviewer.remoteEdit

import com.github.ikorennoy.remotefileviewer.filesystem.RemoteFileSystem
import com.github.ikorennoy.remotefileviewer.filesystem.RemoteVirtualFile
import com.github.ikorennoy.remotefileviewer.remote.RemoteOperationsNotifier
import com.github.ikorennoy.remotefileviewer.tree.RemoteFileSystemTree
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil

@Service
class RemoteEditService {

    fun downloadFile(project: Project, tree: RemoteFileSystemTree) {
        CommandProcessor.getInstance().executeCommand(project, {
            object : Task.Modal(project, "Downloading File", true) {
                override fun run(indicator: ProgressIndicator) {
                    val remoteFileToEdit = tree.getSelectedFile() as? RemoteVirtualFile ?: return
                    val remoteFileSize = remoteFileToEdit.getLength().toDouble()
                    val buffer = ByteArray(1024)

                    val localTempFile = FileUtil.createTempFile(remoteFileToEdit.getName(), ".tmp", false)

                    localTempFile.outputStream().use { localFileOutputStream ->
                        remoteFileToEdit.getInputStream().use { remoteFileInputStream ->
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
                    val localFs = LocalFileSystem.getInstance()
                    val localFileProjection =
                        localFs.wrapIntoTempFile(remoteFileToEdit, localTempFile)
                    ApplicationManager.getApplication().invokeLater {
                        OpenFileDescriptor(project, localFileProjection).navigate(true)
                    }
                }
            }.queue()
        }, "Downloading File", null)
    }

    fun uploadFileToRemote(project: Project, localTempFile: LocalVirtualFile) {
        val remoteFile = localTempFile.remoteFile
        CommandProcessor.getInstance().executeCommand(project, {
            object : Task.Backgroundable(project, "Uploading file", true) {
                override fun run(indicator: ProgressIndicator) {
                    if (remoteFile.isWritable()) {
                        val fs = remoteFile.getFileSystem()
                        // open a temp file and upload new content into it
                        val (tmpFileOutStream, tmpFileName) = fs.openTempFile(remoteFile)

                        val size = localTempFile.length.toDouble()
                        val buffer = ByteArray(1)
                        tmpFileOutStream.use { remoteFileOs ->
                            localTempFile.inputStream.use { localFileIs ->
                                var writtenTotal = 0.0
                                var readFromLocal = localFileIs.read(buffer)
                                while (readFromLocal != -1) {
                                    indicator.checkCanceled()
                                    remoteFileOs.write(buffer, 0, readFromLocal)
                                    writtenTotal += readFromLocal
                                    indicator.fraction = writtenTotal / size
                                    readFromLocal = localFileIs.read(buffer)
                                }
                            }
                        }
                        // because most sftp implementations don't support atomic rename
                        // we have to remove the original file and then do rename
                        fs.removeFile(remoteFile)
                        fs.renameTempFile(tmpFileName, remoteFile.getPath())
                        val notifications = project.service<RemoteOperationsNotifier>()
                        notifications.notifyFileUploaded()
                    }
                }
            }.queue()
        }, "Uploading File", null)
    }
}
