package com.github.ikorennoy.remotefileviewer.remoteEdit

import com.github.ikorennoy.remotefileviewer.remote.RemoteOperations
import com.github.ikorennoy.remotefileviewer.remote.RemoteOperationsNotifier
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class UploadToRemoteFileTask(
    project: Project,
    private val localTempVirtualFile: TempVirtualFile
): Task.Backgroundable(project, "Uploading file") {

    override fun run(indicator: ProgressIndicator) {
        val remoteFile = localTempVirtualFile.remoteFile
        if (remoteFile.isWritable()) {
            // open a temp file and upload new content into it
            val (tmpFileOutStream, tmpFileName) = remoteFile.openTempFile()

            val size = localTempVirtualFile.length.toDouble()
            val buffer = ByteArray(1)
            tmpFileOutStream.use { remoteFileOs ->
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

            val ops = service<RemoteOperations>()
            // rm original file
            remoteFile.delete()
            ops.rename(tmpFileName, remoteFile.getPath())
            val notifications = project.service<RemoteOperationsNotifier>()
            notifications.notifyFileUploaded()
        }
    }
}