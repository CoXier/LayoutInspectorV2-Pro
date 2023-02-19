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
package com.android.tools.idea.editors.layoutInspectorv2.ui;

import com.android.tools.adtui.workbench.*;
import com.android.tools.idea.editors.layoutInspectorv2.LayoutInspectorContext;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class LayoutTreeDefinition extends ToolWindowDefinition<LayoutInspectorContext> {
  public LayoutTreeDefinition(@NotNull Side side, @NotNull Split split, @NotNull AutoHide autoHide) {
    super("View Tree", AllIcons.Toolwindows.WebToolWindow, "LI_VIEW_TREE", side, split, autoHide, DEFAULT_SIDE_WIDTH,
          DEFAULT_BUTTON_SIZE, ALLOW_SPLIT_MODE, LayoutTreePanel::new);
  }
}
