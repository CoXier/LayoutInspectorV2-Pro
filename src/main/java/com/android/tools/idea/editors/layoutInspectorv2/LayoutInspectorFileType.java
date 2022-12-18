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
package com.android.tools.idea.editors.layoutInspectorv2;

import com.android.layoutinspectorv2.Icons;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vfs.VirtualFile;
import icons.StudioIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class LayoutInspectorFileType implements FileType {
  public static final LayoutInspectorFileType INSTANCE = new LayoutInspectorFileType();
  public static final String EXT_LAYOUT_INSPECTOR = "liv2";
  public static final String DOT_EXT_LAYOUT_INSPECTOR = ".liv2";

  @NotNull
  @Override
  public String getName() {
    return "Layout Inspector V2";
  }

  @NotNull
  @Override
  public String getDescription() {
    return "Layout Inspector Snapshot";
  }

  @NotNull
  @Override
  public String getDefaultExtension() {
    return EXT_LAYOUT_INSPECTOR;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return Icons.getLogo();
  }

  @Override
  public boolean isBinary() {
    return true;
  }

  @Override
  public boolean isReadOnly() {
    return true;
  }

  @Nullable
  @Override
  public String getCharset(@NotNull VirtualFile file, @NotNull byte[] content) {
    return null;
  }
}
