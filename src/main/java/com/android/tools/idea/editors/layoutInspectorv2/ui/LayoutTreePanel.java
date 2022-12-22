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

import com.android.annotations.Nullable;
import com.android.tools.idea.editors.layoutInspectorv2.LayoutInspectorContext;
import com.google.common.annotations.VisibleForTesting;
import com.android.layoutinspectorv2.model.ViewNode;
import com.android.tools.adtui.workbench.ToolContent;
import com.android.tools.idea.observable.InvalidationListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class LayoutTreePanel extends JPanel implements ToolContent<LayoutInspectorContext>, InvalidationListener {
  @NotNull private final JScrollPane myTreePanel;
  @Nullable private RollOverTree myTree;
  @Nullable private ViewNodeTreeRenderer myTreeCellRenderer;
  @Nullable private JPanel myBackPanel;
  @Nullable private JLabel myBackLabel;
  @Nullable private LayoutInspectorContext myContext;

  public LayoutTreePanel(@NotNull Disposable parentDisposable) {
    setLayout(new BorderLayout());
    Disposer.register(parentDisposable, this);
    myTreePanel = new JBScrollPane();
    myTreePanel.setBackground(JBColor.WHITE);
    add(myTreePanel, BorderLayout.CENTER);
    add(createBackPanel(), BorderLayout.NORTH);
  }

  @Override
  public void setToolContext(@Nullable LayoutInspectorContext toolContext) {
    if (toolContext != null) {
      myContext = toolContext;
      myContext.getSubviewList().addListener(this);
      setBackground(JBColor.WHITE);
      myTree = toolContext.getNodeTree();
      if (myTree == null) return;
      myTreeCellRenderer = (ViewNodeTreeRenderer)myTree.getCellRenderer();
      myTreePanel.setViewportView(myTree);
      myTreePanel.getViewport().setBackground(JBColor.WHITE);
    }
  }

  @NotNull
  private JComponent createBackPanel() {
    // TODO(kelvinhanma b/69255011) refactor to be a common component with DestinationList's back panel.
    myBackPanel = new JPanel(new BorderLayout());
    myBackLabel = new JLabel("Back", AllIcons.Actions.Back, SwingConstants.LEFT);
    myBackLabel.setBorder(new EmptyBorder(8, 6, 8, 0));
    myBackPanel.setBackground(JBColor.WHITE);
    myBackPanel.setVisible(false);
    myBackPanel.add(myBackLabel, BorderLayout.WEST);
    myBackLabel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        goBack();
      }
    });

    myBackPanel.add(new JSeparator(), BorderLayout.SOUTH);
    return myBackPanel;
  }

  private void goBack() {
    myContext.goBackSubView();
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public boolean supportsFiltering() {
    return true;
  }

  @Override
  public void setFilter(@Nullable String filter) {
    if (myTreeCellRenderer != null) {
      myTreeCellRenderer.setHighlight(filter);
      myTree.repaint();
    }
  }

  @Override
  public void dispose() {
    myContext.getSubviewList().removeListener(this);
  }

  @Override
  public void onInvalidated() {
    if (!myContext.getSubviewList().isEmpty()) {
      ViewNode parentNode = myContext.getSubviewList().get(myContext.getSubviewList().size() - 1);
      if (parentNode == null) return;

      myBackPanel.setVisible(true);
      String id = ViewNodeTreeRenderer.getId(parentNode);
      myBackLabel.setText(id != null ? id : ViewNodeTreeRenderer.getName(parentNode));
    }
    else {
      myBackPanel.setVisible(false);
    }
    myTree = myContext.getNodeTree();
    myTreePanel.setViewportView(myTree);
  }

  @Nullable
  @VisibleForTesting
  JPanel getBackPanel() {
    return myBackPanel;
  }
}
