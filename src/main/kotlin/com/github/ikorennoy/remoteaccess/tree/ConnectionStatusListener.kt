package com.github.ikorennoy.remoteaccess.tree

import com.intellij.util.messages.Topic
import java.util.*

interface ConnectionStatusListener : EventListener {

    companion object {
        @Topic.AppLevel
        val TOPIC = Topic(ConnectionStatusListener::class.java, Topic.BroadcastDirection.NONE)
    }

    fun connectionStatusChanged()
}
