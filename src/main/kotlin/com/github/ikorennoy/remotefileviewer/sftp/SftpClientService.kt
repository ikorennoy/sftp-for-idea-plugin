package com.github.ikorennoy.remotefileviewer.sftp

import com.github.ikorennoy.remotefileviewer.settings.RemoteFileViewerSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import net.schmizz.sshj.SSHClient

@Service
class SftpClientService {

    private val configuration = service<RemoteFileViewerSettingsState>()

    private val sshClient = SSHClient()


}