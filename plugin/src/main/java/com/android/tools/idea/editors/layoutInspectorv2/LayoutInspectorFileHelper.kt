package com.android.tools.idea.editors.layoutInspectorv2

import com.android.ddmlib.Client
import com.intellij.openapi.project.Project
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object LayoutInspectorFileHelper {

    @JvmStatic
    fun saveToFile(client: Client, project: Project, data: ByteArray): File? {
        val fileName = getSuggestedName(client, project)
        project.basePath ?: return null
        val projectDir = File(project.basePath) ?: return null
        val capturesDir = File(projectDir, "captures")
        if (!capturesDir.exists()) {
            capturesDir.mkdirs()
        }

        val file = File(capturesDir, "$fileName${LayoutInspectorFileType.DOT_EXT_LAYOUT_INSPECTOR}")
        file.writeBytes(data)

        return file
    }

    private fun getSuggestedName(client: Client?, project: Project): String? {
        val format = "yyyy.MM.dd_HH.mm"
        val timestamp = SimpleDateFormat(format).format(Date())
        var suggestedName: String? = null
        if (client != null) {
            val name = client.clientData.clientDescription
            if (name != null && name.isNotEmpty()) {
                suggestedName = name + "_" + timestamp
            }
        }
        if (suggestedName == null) {
            suggestedName = project.name + "_" + timestamp
        }
        return suggestedName.replace("[^._A-Za-z0-9]".toRegex(), "")
    }

}