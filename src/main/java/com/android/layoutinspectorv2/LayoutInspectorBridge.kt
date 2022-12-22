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
package com.android.layoutinspectorv2

import com.android.layoutinspectorv2.model.ClientWindow
import com.android.layoutinspectorv2.model.ViewNode
import com.android.layoutinspectorv2.parser.ViewNodeParser
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.util.concurrent.TimeUnit

object LayoutInspectorBridge {
    @JvmStatic
    val V2_MIN_API = 23

    @JvmStatic
    fun captureView(
        window: ClientWindow, options: LayoutInspectorCaptureOptions
    ): LayoutInspectorResult {
        val hierarchy =
            window.loadWindowData(options, 20, TimeUnit.SECONDS) ?: return LayoutInspectorResult(
                null,
                "There was a timeout error capturing the layout data from the device.\n" +
                "The device may be too slow, the captured view may be too complex, or the view may contain animations.\n\n" +
                "Please retry with a simplified view and ensure the device is responsive."
            )

        var root: ViewNode?
        try {
            root = ViewNodeParser.parse(hierarchy, options.version)
        } catch (e: StringIndexOutOfBoundsException) {
            return LayoutInspectorResult(null, "Unexpected error: $e")
        } catch (e: IOException) {
            return LayoutInspectorResult(null, "Unexpected error: $e")
        }

        if (root == null) {
            return LayoutInspectorResult(
                null,
                "Unable to parse view hierarchy"
            )
        }

        //  Get the preview of the root node
        val preview = window.loadViewImage(
            root,
            10,
            TimeUnit.SECONDS
        ) ?: return LayoutInspectorResult(
            null,
            "Unable to obtain preview image"
        )

        val bytes = ByteArrayOutputStream(4096)
        var output: ObjectOutputStream? = null

        try {
            output = ObjectOutputStream(bytes)
            output.writeUTF(options.toString())

            output.writeInt(hierarchy.size)
            output.write(hierarchy)

            output.writeInt(preview.size)
            output.write(preview)
        } catch (e: IOException) {
            return LayoutInspectorResult(
                null,
                "Unexpected error while saving hierarchy snapshot: $e"
            )
        } finally {
            try {
                if (output != null) {
                    output.close()
                }
            } catch (e: IOException) {
                return LayoutInspectorResult(
                    null,
                    "Unexpected error while closing hierarchy snapshot: $e"
                )
            }

        }

        return LayoutInspectorResult(bytes.toByteArray(), "")
    }
}
