package com.github.ikorennoy.remoteaccess.edit

import com.github.ikorennoy.remoteaccess.Er
import com.github.ikorennoy.remoteaccess.Ok
import com.github.ikorennoy.remoteaccess.operations.RemoteFileInformation
import com.github.ikorennoy.remoteaccess.operations.RemoteOperations
import com.github.ikorennoy.remoteaccess.operations.RemoteOperationsNotifier
import com.github.ikorennoy.remoteaccess.template.RemoteFileAccessBundle
import com.intellij.execution.process.ProcessIOExecutorService
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

    @Volatile
    var remoteTempFile: RemoteFileInformation? = null

    override fun run(indicator: ProgressIndicator) {
        val remoteOriginalFile = localTempVirtualFile.remoteFile
        val ops = RemoteOperations.getInstance(project)
        val notifier = RemoteOperationsNotifier.getInstance(project)
        // open a temp file and upload new content into it
        when (val prepareRemoteTempRes = ops.prepareTempFile(remoteOriginalFile)) {
            is Ok -> {
                val newRemoteTempFile = prepareRemoteTempRes.value
                remoteTempFile = newRemoteTempFile
                when (val openOutStreamRes = ops.fileOutputStream(newRemoteTempFile)) {
                    is Ok -> {
                        val remoteTempFileOutStream = openOutStreamRes.value
                        val size = localTempVirtualFile.length.toDouble()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

                        remoteTempFileOutStream.use { remoteFileOs ->
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
                        // rm original file
                        indicator.checkCanceled()
                        when (val removeResult = ops.remove(remoteOriginalFile)) {
                            is Ok -> {
                                // move a file
                                when (val renameResult = ops.rename(newRemoteTempFile, remoteOriginalFile)) {
                                    is Ok -> notifier.fileUploaded(remoteOriginalFile.getName())
                                    is Er -> {
                                        notifier.cannotSaveFileToRemote(
                                            remoteOriginalFile.getName(),
                                            renameResult.error
                                        )
                                        ops.remove(newRemoteTempFile)
                                    }
                                }
                            }

                            is Er -> {
                                notifier.cannotSaveFileToRemote(remoteOriginalFile.getName(), removeResult.error)
                                ops.remove(newRemoteTempFile)
                            }
                        }
                    }

                    is Er -> {
                        notifier.cannotSaveFileToRemote(remoteOriginalFile.getName(), openOutStreamRes.error)
                        ops.remove(newRemoteTempFile)
                    }
                }
            }

            is Er -> notifier.cannotSaveFileToRemote(remoteOriginalFile.getName(), prepareRemoteTempRes.error)
        }
    }

    override fun onCancel() {
        if (remoteTempFile != null) {
            val remoteTempFile = remoteTempFile ?: return
            val ops = RemoteOperations.getInstance(project)
            ProcessIOExecutorService.INSTANCE.execute {
                ops.remove(remoteTempFile)
            }
        }
    }
}
