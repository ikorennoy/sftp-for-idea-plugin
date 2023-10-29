package com.github.ikorennoy.remotefileviewer.sftp

import net.schmizz.sshj.SSHClient

class SftpClient {

    private val client = SSHClient()

    init {
        client.loadKnownHosts()

    }

    companion object {

        fun checkAndConnect() {

        }
    }
}