package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.operations.RemoteOperationsNotifier
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class UploadToRemoteFileTask(
    private val project: Project,
    private val localTempVirtualFile: TempVirtualFile
) : Task.Backgroundable(
    project,
    RemoteFileAccessBundle.message("task.RemoteFileAccess.uploadFileToRemote.backgroundable.name"),
    true
) {

    override fun run(indicator: ProgressIndicator) {
        val remoteOriginalFile = localTempVirtualFile.remoteFile
        if (remoteOriginalFile.isWritable()) {
            // open a temp file and upload new content into it
            val remoteTempFile = remoteOriginalFile.prepareRemoteTempFile()

            if (remoteTempFile != null) {
                val remoteTempFileOutputStream = remoteTempFile.getOutputStream()
                if (remoteTempFileOutputStream != null) {
                    val size = localTempVirtualFile.length.toDouble()
                    val buffer = ByteArray(4096)
                    remoteTempFileOutputStream.use { remoteFileOs ->
                        localTempVirtualFile.inputStream.use { localFileIs ->
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
                    val ops = RemoteOperations.getInstance(project)
                    // rm original file
                    remoteOriginalFile.delete()
                    // move a file
                    ops.rename(remoteTempFile, remoteOriginalFile)
                    val notifications = RemoteOperationsNotifier.getInstance(project)
                    notifications.fileUploaded(remoteOriginalFile.getName())
                } else {
                    RemoteOperationsNotifier.getInstance(project).cannotSaveFile(remoteOriginalFile.getPath())
                }
            } else {
                RemoteOperationsNotifier.getInstance(project).cannotSaveFile(remoteOriginalFile.getPath())
            }
        }
    }
}
