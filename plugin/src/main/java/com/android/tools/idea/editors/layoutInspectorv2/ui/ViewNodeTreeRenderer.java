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

import com.android.layoutinspectorv2.model.ViewNode;
import com.android.tools.adtui.font.FontUtil;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.android.dom.AndroidDomElementDescriptorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class ViewNodeTreeRenderer extends ColoredTreeCellRenderer {
  private static final Icon DEFAULT_VIEW_ICON = AndroidDomElementDescriptorProvider.getIconForViewTag("View");
  private static final String EMPTY_ID = "NO_ID";

  private String myHighlight;

  @Override
  public void customizeCellRenderer(@NotNull JTree tree, Object nodeValue, boolean selected,
                                    boolean expanded, boolean leaf, int row, boolean hasFocus) {
    if (!(nodeValue instanceof ViewNode)) {
      return;
    }

    StringBuilder cellTextBuilder = new StringBuilder();
    ViewNode node = (ViewNode)nodeValue;
    SimpleTextAttributes attr = node.isDrawn() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAYED_ATTRIBUTES;

    String id = getId(node);
    String elementName = getName(node);
    if (id != null) {
      cellTextBuilder.append(id + " (" + elementName + ") ");
    } else {
      cellTextBuilder.append(elementName + " ");
    }
    setIcon(findIconForNode(elementName));

    displayText(attr, cellTextBuilder.toString());

    if (node.getDisplayInfo().getContentDesc() != null) {
      Font currentFont = getFont();
      Font f = FontUtil.getFontAbleToDisplay(node.getDisplayInfo().getContentDesc(), currentFont);
      if (f != currentFont) {
        setFont(f);
      }
      displayText(node.isDrawn() ? new SimpleTextAttributes(Font.PLAIN, Color.GRAY) : attr, "- \"" + node.getDisplayInfo().getContentDesc() + "\"");
    }
  }

  @Nullable
  public static String getId(@NotNull ViewNode node) {
    String id = node.getId();
    if (id == null || id.equals(EMPTY_ID)) return null;
    return id;
  }

  @NotNull
  public static String getName(@NotNull ViewNode node) {
    String[] name = node.getName().split("\\.");
    return name[name.length - 1];
  }

  public static List<String> splitKeepDelimiter(@NotNull String string, @NotNull String delimiter) {
    return Arrays.asList(string.split(String.format("((?<=%1$s)|(?=%1$s))", delimiter)));
  }

  private void displayText(SimpleTextAttributes attr, String cellText) {
    if (myHighlight == null || myHighlight.isEmpty()) {
      append(cellText, attr);
    }
    else { // we want to highlight text that matches by changing the attribute to use a foreground colour
      List<String> outputs = splitKeepDelimiter(cellText, myHighlight);
      SimpleTextAttributes highlightAttr = SimpleTextAttributes.SYNTHETIC_ATTRIBUTES;
      for (String s : outputs) {
        if (s.equals(myHighlight)) {
          append(s, highlightAttr);
        }
        else {
          append(s, attr);
        }
      }
    }
  }

  /**
   * Determine the icon to use given a node. First try full element name.
   * If there is no matching icon, then try a subset of the name (i.e. map AppCompatEditText to EditText)
   *
   * @param elementName the elementName name we want to find icon for
   * @return Icon for the node
   */
  @Nullable
  private static Icon findIconForNode(@NotNull String elementName) {
    Icon icon = null;
    String[] words = elementName.split("(?=\\p{Upper})");

    int index = 0;
    StringBuilder builder;
    while (icon == null && index < words.length) {
      builder = new StringBuilder();
      for (int i = index; i < words.length; i++) {
        builder.append(words[i]);
      }
      icon = AndroidDomElementDescriptorProvider.getIconForViewTag(builder.toString());
      index++;
    }

    return icon != null ? icon : DEFAULT_VIEW_ICON;
  }

  public void setHighlight(@Nullable String highlight) {
    myHighlight = highlight;
  }

  @Nullable
  public String getHighlight() {
    return myHighlight;
  }
}
