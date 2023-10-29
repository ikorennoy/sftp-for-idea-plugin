package com.github.ikorennoy.remotefileviewer.sftp

import net.schmizz.sshj.SSHClient

class SftpClient {

    private val client = SSHClient()

    init {
        client.loadKnownHosts()

    }


    fun tryConnect(host: String, ) {

    }
}