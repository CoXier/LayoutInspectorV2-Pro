/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.layoutinspectorv2.model

import com.google.common.collect.Lists
import com.google.common.collect.Maps
import java.awt.Rectangle
import java.util.Collections
import java.util.Enumeration
import java.util.LinkedList
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

/**
 * Represents an Android View object. Holds properties and a previewBox that contains the display area
 * of the object on screen.
 * Created by parsing view dumps using [com.android.layoutinspector.parser.ViewNodeParser].
 */
// make parent private because it's the same as the getParent method from TreeNode
data class ViewNode internal constructor(private val parent: ViewNode?, val name: String, val hash: String) :
    TreeNode {
    // If the force state is set, the preview tries to render/hide the view
    // (depending on the parent's state)
    enum class ForcedState {
        NONE,
        VISIBLE,
        INVISIBLE
    }

    val groupedProperties: MutableMap<String, MutableList<ViewProperty>> = Maps.newHashMap()
    val namedProperties: MutableMap<String, ViewProperty> = Maps.newHashMap()
    val properties: MutableList<ViewProperty> = Lists.newArrayList()
    val children: MutableList<ViewNode> = Lists.newArrayList()
    val previewBox: Rectangle = Rectangle()

    // default in case properties are not available
    var index: Int = 0
    var id: String? = null
    // TODO(kelvinhanma) get rid of lateinit by refactoring creation of DisplayInfo
    lateinit var displayInfo: DisplayInfo

    var isParentVisible: Boolean = false
        private set
    var isDrawn: Boolean = false
        private set
    var forcedState: ForcedState = ForcedState.NONE

    fun addPropertyToGroup(property: ViewProperty) {
        val key = getKey(property)
        val propertiesList = groupedProperties.getOrDefault(
            key,
            LinkedList()
        )
        propertiesList.add(property)
        groupedProperties[key] = propertiesList
    }

    private fun getKey(property: ViewProperty): String {
        return property.category ?: if (property.fullName.endsWith("()")) {
            "methods"
        } else {
            "properties"
        }
    }

    fun getProperty(name: String, vararg altNames: String): ViewProperty? {
        var property: ViewProperty? = namedProperties[name]
        var i = 0
        while (property == null && i < altNames.size) {
            property = namedProperties[altNames[i]]
            i++
        }
        return property
    }

    /** Recursively updates all the visibility parameter of the nodes.  */
    fun updateNodeDrawn() {
        updateNodeDrawn(isParentVisible)
    }

    fun updateNodeDrawn(parentVisible: Boolean) {
        var parentVisible = parentVisible
        isParentVisible = parentVisible
        if (forcedState == ForcedState.NONE) {
            isDrawn = !displayInfo.willNotDraw && parentVisible && displayInfo.isVisible
            parentVisible = parentVisible and displayInfo.isVisible
        } else {
            isDrawn = forcedState == ForcedState.VISIBLE && parentVisible
            parentVisible = isDrawn
        }
        for (child in children) {
            child.updateNodeDrawn(parentVisible)
            isDrawn = isDrawn or (child.isDrawn && child.displayInfo.isVisible)
        }
    }

    override fun toString(): String {
        return "$name@$hash"
    }

    override fun getChildAt(childIndex: Int): ViewNode {
        return children[childIndex]
    }

    override fun getChildCount(): Int {
        return children.size
    }

    override fun getParent(): ViewNode? {
        return parent
    }

    override fun getIndex(node: TreeNode): Int {
        return children.indexOf(node as ViewNode)
    }

    override fun getAllowsChildren(): Boolean {
        return true
    }

    override fun isLeaf(): Boolean {
        return childCount == 0
    }

    override fun children(): Enumeration<ViewNode> {
        return Collections.enumeration(children)
    }

    companion object {
        /** Finds the path from node to the root.  */
        @JvmStatic
        fun getPath(node: ViewNode): TreePath {
            return getPathImpl(node, null)
        }

        /** Finds the path from node to the parent.  */
        @JvmStatic
        fun getPathFromParent(node: ViewNode, root: ViewNode): TreePath {
            return getPathImpl(node, root)
        }

        private fun getPathImpl(node: ViewNode, root: ViewNode?): TreePath {
            var node: ViewNode? = node
            val nodes = Lists.newArrayList<Any>()
            do {
                nodes.add(0, node)
                node = node?.parent
            } while (node != null && node !== root)
            if (root != null && node === root) {
                nodes.add(0, root)
            }
            return TreePath(nodes.toTypedArray())
        }
    }
}
