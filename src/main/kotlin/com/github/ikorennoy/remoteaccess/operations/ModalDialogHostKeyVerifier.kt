package com.github.ikorennoy.remoteaccess.operations

import com.intellij.CommonBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts
import java.io.File
import java.security.PublicKey

// todo support correct host file update
//  successfully updated notification
class ModalDialogHostKeyVerifier(
    private val project: Project,
    hostFile: File
) : OpenSSHKnownHosts(hostFile) {

    override fun hostKeyUnverifiableAction(hostname: String?, key: PublicKey?): Boolean {
        val type = KeyType.fromKey(key)
        val fingerPrint = SecurityUtils.getFingerprint(key)
        var result: Int? = null
        ApplicationManager.getApplication().invokeAndWait({
            result = Messages.showOkCancelDialog(
                project,
                """The authenticity of host '$hostname' can't be established. $type key fingerprint is $fingerPrint
                    
                        Are you sure you want to continue connecting?""",
                "Connecting To Remote Host",
                CommonBundle.getOkButtonText(),
                CommonBundle.getCancelButtonText(),
                Messages.getQuestionIcon()
            )
        }, ModalityState.any())

        if (result == null) return false

        return result == Messages.OK
    }
}
