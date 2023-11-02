package com.github.ikorennoy.remotefileviewer.remote

import com.intellij.util.messages.Topic
import java.util.EventListener

interface RemoteConnectionListener: EventListener {

    companion object {
        @Topic.AppLevel
        val TOPIC = Topic(
            RemoteConnectionListener::class.java, Topic.BroadcastDirection.NONE
        )
    }

    fun connectionEstablished()
}