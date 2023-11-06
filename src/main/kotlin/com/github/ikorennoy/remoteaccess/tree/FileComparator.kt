package com.github.ikorennoy.remoteaccess.tree

import com.intellij.ide.util.treeView.NodeDescriptor

internal class FileComparator : Comparator<NodeDescriptor<*>> {

    override fun compare(o1: NodeDescriptor<*>, o2: NodeDescriptor<*>): Int {
        val weight1 = getWeight(o1)
        val weight2 = getWeight(o2)
        if (weight1 != weight2) {
            return weight1 - weight2
        }

        val node1Text: String = o1.toString()
        val node2Text: String = o2.toString()
        val isNode1Unc = node1Text.startsWith("\\\\")
        val isNode2Unc = node2Text.startsWith("\\\\")
        if (isNode1Unc && !isNode2Unc) return 1
        return if (isNode2Unc && !isNode1Unc) -1 else node1Text.compareTo(node2Text, ignoreCase = true)
    }

    private fun getWeight(nodeDescriptor: NodeDescriptor<*>): Int {
        if (nodeDescriptor is RemoteFileSystemTreeNodeDescriptor) {
            val element = nodeDescriptor.element
            return if (element.isDirectory()) {
                0
            } else {
                1
            }
        }
        throw IllegalArgumentException("Not supported NodeDescriptor: ${nodeDescriptor::class.java}")
    }

    companion object {
        private val INSTANCE = FileComparator()

        fun getInstance(): FileComparator {
            return INSTANCE
        }
    }
}
