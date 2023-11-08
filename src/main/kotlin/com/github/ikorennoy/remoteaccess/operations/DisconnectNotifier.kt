package com.github.ikorennoy.remoteaccess.operations

import com.github.ikorennoy.remoteaccess.notifyConnectionStatusChanged
import com.intellij.openapi.project.Project
import net.schmizz.sshj.common.DisconnectReason
import net.schmizz.sshj.transport.DisconnectListener

class DisconnectNotifier(private val project: Project) : DisconnectListener {

    override fun notifyDisconnect(reason: DisconnectReason, message: String) {
        if (reason != DisconnectReason.BY_APPLICATION) {
            RemoteOperationsNotifier.getInstance(project).disconnect(reason.name, message)
            notifyConnectionStatusChanged()
        }
    }
}
