package com.github.ikorennoy.remoteaccess.operations

import com.intellij.util.messages.Topic
import java.util.EventListener

interface ConnectionListener : EventListener {

    companion object {
        @Topic.AppLevel
        val TOPIC = Topic(ConnectionListener::class.java, Topic.BroadcastDirection.NONE)
    }

    fun connectionStatusChanged()
}
