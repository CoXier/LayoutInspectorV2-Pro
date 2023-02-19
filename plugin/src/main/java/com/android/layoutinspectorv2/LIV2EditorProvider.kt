package com.android.layoutinspectorv2

import com.android.tools.idea.editors.layoutInspectorv2.LayoutInspectorEditor
import com.android.tools.idea.editors.layoutInspectorv2.LayoutInspectorFileType
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class LIV2EditorProvider: FileEditorProvider, DumbAware {
    override fun accept(p0: Project, p1: VirtualFile): Boolean {
        return p1.extension == LayoutInspectorFileType.EXT_LAYOUT_INSPECTOR
    }

    override fun createEditor(p0: Project, p1: VirtualFile): FileEditor =
        LayoutInspectorEditor(
            p0,
            p1
        )

    override fun getEditorTypeId(): String = "LayoutInspectorV2"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}