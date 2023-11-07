package com.github.ikorennoy.remoteaccess.tree

import com.intellij.util.messages.Topic
import java.util.*

interface TreeStateListener : EventListener {

    companion object {
        @Topic.AppLevel
        val TOPIC = Topic(TreeStateListener::class.java, Topic.BroadcastDirection.NONE)
    }

    fun updateTree()
}
