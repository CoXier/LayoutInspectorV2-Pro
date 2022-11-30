/*
 * Copyright (C) 2017 The Android Open Source Project
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
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.TableModel

class ViewNodeTableModel : TableModel {

    private val mListeners = Lists.newArrayList<TableModelListener>()

    private val mEntries = Lists.newArrayList<ViewProperty>()

    private val mGroupedProperties = Maps.newHashMap<String, List<ViewProperty>>()

    val groupedProperties: Map<String, List<ViewProperty>>
        get() = mGroupedProperties

    fun setNode(node: ViewNode) {
        // Go through the properties, filtering the favorites properties first
        mEntries.clear()
        mEntries.addAll(node.properties)
        mGroupedProperties.clear()
        mGroupedProperties.putAll(node.groupedProperties)
        notifyChange(TableModelEvent(this))
    }

    override fun getRowCount(): Int {
        return mEntries.size
    }

    override fun getColumnCount(): Int {
        return 2
    }

    override fun getColumnName(columnIndex: Int): String {
        return if (columnIndex == 0) "Property" else "Value"
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return String::class.java
    }

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
        return false
    }

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
        val p = mEntries[rowIndex]
        return if (columnIndex == 0) p.name else p.value
    }

    override fun setValueAt(aValue: Any, rowIndex: Int, columnIndex: Int) {
        // Not supported
    }

    override fun addTableModelListener(l: TableModelListener) {
        mListeners.add(l)
    }

    override fun removeTableModelListener(l: TableModelListener) {
        mListeners.remove(l)
    }

    private fun notifyChange(event: TableModelEvent) {
        for (l in mListeners) {
            l.tableChanged(event)
        }
    }
}
