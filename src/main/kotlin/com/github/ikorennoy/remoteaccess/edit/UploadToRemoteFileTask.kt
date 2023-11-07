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
import java.io.IOException

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


    /**
     * This method unloads a file that the user edited and then closed the editor window or requested to be unloaded.
     * It creates a temporary (with a .tmp suffix) hidden (name starts with . if necessary) file in the
     * same directory as the file being edited, then loads the new content into the temporary file, deletes the
     * original file, and performs a rename operation. <p/>
     *
     * Because of the SFTP specification, renaming to an existing file doesn't work, so we have to delete
     * the original file. Also, we can't create a temporary file in a designated directory because,
     * according to the SFTP specification, the rename operation will fail if the src and dst files
     * are on different file systems.<p/>
     *
     * If something goes wrong during this operation at the deletion stage of the original file,
     * we want to keep our temporary file in the same directory as the file being edited so that the user has
     * a small chance of recovering the lost content.
     *
     */
    override fun run(indicator: ProgressIndicator) {
        val remoteOriginalFile = localTempVirtualFile.remoteFile
        val remoteOperations = RemoteOperations.getInstance(project)
        val notifier = RemoteOperationsNotifier.getInstance(project)
        var needToRemoveRemoteTempFile = false
        // find a name for a temp file and crate it
        indicator.checkCanceled()
        when (val prepareRemoteTempRes = remoteOperations.prepareTempFile(remoteOriginalFile)) {
            is Ok -> {
                val newRemoteTempFile = prepareRemoteTempRes.value
                remoteTempFile = newRemoteTempFile
                // open new temp file for writing, and write content into it
                indicator.checkCanceled()
                when (val openOutStreamRes = remoteOperations.fileOutputStream(newRemoteTempFile)) {
                    is Ok -> {
                        val remoteTempFileOutStream = openOutStreamRes.value
                        val size = localTempVirtualFile.length.toDouble()
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        indicator.text = remoteOriginalFile.getPresentablePath()
                        indicator.fraction = 0.0
                        indicator.isIndeterminate = false
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

                        indicator.checkCanceled()
                        // remove the original file
                        when (val removeRes = remoteOperations.remove(remoteOriginalFile)) {
                            is Ok -> {
                                // at this point we don't want to remove the tmp file
                                // if everything goes well, it will be renamed to the original file
                                // if something goes wrong, we want to keep a small chance for user
                                // to restore a content
                                needToRemoveRemoteTempFile = false

                                // rename the temp file into the original file
                                when (val renameRes = remoteOperations.rename(newRemoteTempFile, remoteOriginalFile)) {
                                    is Ok -> notifier.fileUploaded(remoteOriginalFile.getName())

                                    // fail on renaming the temp file the into original file branch
                                    is Er -> {
                                        notifier.cannotSaveFileToRemote(
                                            remoteOriginalFile.getName(),
                                            renameRes.error
                                        )
                                    }
                                }
                            }

                            // fail on removing the original file branch
                            is Er -> {
                                notifier.cannotSaveFileToRemote(remoteOriginalFile.getName(), removeRes.error)
                                needToRemoveRemoteTempFile = true
                            }
                        }
                    }

                    // fail on opening the temp file for writing branch
                    is Er -> {
                        notifier.cannotSaveFileToRemote(remoteOriginalFile.getName(), openOutStreamRes.error)
                        needToRemoveRemoteTempFile = true
                    }
                }
                if (needToRemoveRemoteTempFile) {
                    ProcessIOExecutorService.INSTANCE.execute {
                        remoteOperations.remove(newRemoteTempFile)
                    }
                }
            }

            // fail on creating a temp file branch
            is Er -> notifier.cannotSaveFileToRemote(remoteOriginalFile.getName(), prepareRemoteTempRes.error)
        }
    }

    /**
     * We're not allowing to cancel between remove and rename, so it's safe to remove a temp file here
     */
    override fun onCancel() {
        removeRemoteTempFile()
    }

    /**
     * We're processing most of the feasible exceptions from the sftp
     * We could end up here if we got an error while we were uploading content to a temp file, so it's safe to remove it.
     *
     * Or we could get an error that we can't handle in a reasonable way, so probably in that case it's better
     * to keep the temp file
     */
    override fun onThrowable(error: Throwable) {
        if (error is IOException) {
            removeRemoteTempFile()
        }
        super.onThrowable(error)
    }


    private fun removeRemoteTempFile() {
        if (remoteTempFile != null) {
            val remoteTempFile = remoteTempFile ?: return
            val remoteOperations = RemoteOperations.getInstance(project)
            ProcessIOExecutorService.INSTANCE.execute {
                remoteOperations.remove(remoteTempFile)
            }
        }
    }
}
