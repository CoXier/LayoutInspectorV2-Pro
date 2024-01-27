package com.android.layoutinspectorv2

import com.intellij.openapi.util.IconLoader

object Icons {
    @JvmStatic
    val LOGO = IconLoader.findIcon("/icons/icon.svg", this::class.java.classLoader)

    @JvmStatic
    val LOAD_OVERLAY = IconLoader.findIcon("/icons/load-overlay.svg", this::class.java.classLoader)

    @JvmStatic
    val CLEAR_OVERLAY = IconLoader.findIcon("/icons/clear-overlay.svg", this::class.java.classLoader)
}