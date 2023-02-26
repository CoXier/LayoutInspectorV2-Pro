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

import com.android.layoutinspectorv2.model.DisplayInfo;
import com.android.layoutinspectorv2.model.ViewNode;
import com.android.tools.idea.ui.MaterialColors;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.paint.LinePainter2D;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import org.intellij.images.options.GridOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A component to display a {@link ViewNode} with display boxes.
 * Renders the image scaled with a zoom factor and draws display boxes over the
 * image that listens to hover/click and fires events for listeners to respond.
 */
public class ViewNodeActiveDisplay extends JComponent {
  private static final double SHOW_GRID_LEVEL = 3;
  @VisibleForTesting
  static final float DEFAULT_OVERLAY_ALPHA = 0.5f;

  private static final Color DEFAULT_COLOR = Color.GRAY;
  private static final Color HOVER_COLOR = MaterialColors.DEEP_ORANGE_900;
  private static final Color SELECTED_COLOR = MaterialColors.LIGHT_BLUE_800;

  private static final Stroke DEFAULT_STROKE = new BasicStroke(1);
  private static final Stroke THICK_STROKE = new BasicStroke(2);

  @NotNull
  private ViewNode mRoot;
  @Nullable
  private Image mPreview;

  private final List<ViewNodeActiveDisplayListener> mListeners = new ArrayList<>();

  private float mZoomFactor = 1;

  // tracks size, recalculate node boundaries when size changes.
  private int mLastWidth;
  private int mLastHeight;
  // offset to center the image and boxes
  private int mDrawShiftX;
  private int mDrawShiftY;

  @Nullable
  private ViewNode mHoverNode;
  @Nullable
  private ViewNode mSelectedNode;
  private boolean mGridVisible = false;
  @Nullable
  private Image mOverlay;
  private float mOverlayAlpha = DEFAULT_OVERLAY_ALPHA;
  @Nullable
  private String myOverlayFileName;
  // flag to tell next render to update bound boxes
  private boolean updateBounds = false;

  public ViewNodeActiveDisplay(@NotNull ViewNode root, @Nullable Image preview) {
    mRoot = root;
    mPreview = preview;

    MyMouseAdapter adapter = new MyMouseAdapter();
    addMouseListener(adapter);
    addMouseMotionListener(adapter);
    addDoubleClickListener();
  }

  private void addDoubleClickListener() {
    new DoubleClickListener(){

      @Override
      protected boolean onDoubleClick(@NotNull MouseEvent event) {
        ViewNode clicked = getNode(event);
        if (clicked == null) return false;
        for (ViewNodeActiveDisplayListener listener : mListeners) {
          listener.onNodeDoubleClicked(clicked);
        }
        return true;
      }
    }.installOn(this);
  }

  public void addViewNodeActiveDisplayListener(ViewNodeActiveDisplayListener listener) {
    mListeners.add(listener);
  }

  public void removeViewNodeActiveDisplayListener(ViewNodeActiveDisplayListener listener) {
    mListeners.remove(listener);
  }

  public void setHoverNode(@Nullable ViewNode node) {
    if (!Objects.equal(node, mHoverNode)) {
      mHoverNode = node;
      repaint();

      for (ViewNodeActiveDisplayListener listener : mListeners) {
        listener.onViewNodeOver(mHoverNode);
      }
    }
  }

  public void setSelectedNode(@NotNull ViewNode node) {
    if (!Objects.equal(node, mSelectedNode)) {
      mSelectedNode = node;
      repaint();

      for (ViewNodeActiveDisplayListener listener : mListeners) {
        listener.onNodeSelected(mSelectedNode);
      }
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);

    // if size has changed, recalculate the the view node display boxes sizes/locations.
    if (mLastWidth != getWidth() || mLastHeight != getHeight() || updateBounds) {
      DisplayInfo info = mRoot.getDisplayInfo();
      float rootHeight = info.getHeight();
      float rootWidth = info.getWidth();

      // on first draw, calculate scale to fit image into panel
      if (mLastHeight == 0 && mLastWidth == 0) {
        mZoomFactor = calcDrawScale(rootWidth, rootHeight);
      }

      mLastHeight = getHeight();
      mLastWidth = getWidth();

      mDrawShiftX = (int) (getWidth() - mZoomFactor * rootWidth) / 2;
      mDrawShiftY = (int) (getHeight() - mZoomFactor * rootHeight) / 2;

      calculateNodeBounds(mRoot, info.getCopyAtOrigin(),0, 0, 1, 1, mZoomFactor);
      updateBounds = false;
    }

    paintPreview((Graphics2D)g);
  }

  private float calcDrawScale(float rootWidth, float rootHeight) {
    float width = getWidth() - 20;
    float height = getHeight() - 20;

    return Math.min(width / rootWidth, height / rootHeight);
  }

  public float getZoomFactor() {
    return mZoomFactor;
  }

  @Nullable
  public Image getPreview() {
    return mPreview;
  }

  private void calculateNodeBounds(
    @NotNull ViewNode node, @NotNull DisplayInfo info, float leftShift, float topShift,
    float scaleX, float scaleY, float drawScale) {

    float newScaleX = scaleX * info.getScaleX();
    float newScaleY = scaleY * info.getScaleY();

    float l =
      leftShift + (info.getLeft() + info.getTranslateX()) * scaleX + info.getWidth() * (scaleX - newScaleX) / 2;
    float t =
      topShift + (info.getTop() + info.getTranslateY()) * scaleY + info.getHeight() * (scaleY - newScaleY) / 2;

    node.getPreviewBox().setBounds(
      (int)(l * drawScale),
      (int)(t * drawScale),
      (int)(info.getWidth() * newScaleX * drawScale),
      (int)(info.getHeight() * newScaleY * drawScale)
    );

    if (!node.isLeaf()) {
      for (ViewNode child : node.getChildren()) {
        calculateNodeBounds(child, child.displayInfo, l, t, newScaleX, newScaleY, drawScale);
      }
    }
  }

  private void paintPreview(Graphics2D g) {
    // move the coordinate so we draw in the center of the canvas instead of top left.
    g.translate(mDrawShiftX, mDrawShiftY);

    if (mPreview != null) {
      RenderingHints oldHints = g.getRenderingHints();
      g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      if (Float.compare(mZoomFactor, 1.0f) < 0) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      }
      else {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
      }
      g.drawImage(mPreview, 0, 0, mRoot.getPreviewBox().width, mRoot.getPreviewBox().height,
                  0, 0, mPreview.getWidth(null), mPreview.getHeight(null), null);

      if (isGridVisible() && mZoomFactor >= SHOW_GRID_LEVEL) {
        paintGrid(g, mPreview);
      }

      drawOverlay(g);

      g.setRenderingHints(oldHints);
    }

    g.clipRect(0, 0, mRoot.getPreviewBox().width, mRoot.getPreviewBox().height);
    g.setColor(DEFAULT_COLOR);
    g.setStroke(DEFAULT_STROKE);

    paintNode(mRoot, g);

    g.setStroke(THICK_STROKE);
    if (mHoverNode != null && mSelectedNode != mHoverNode) {
      g.setColor(HOVER_COLOR);
      paintBox(mHoverNode.getPreviewBox(), g);
    }
    if (mSelectedNode != null) {
      g.setColor(SELECTED_COLOR);
      paintBox(mSelectedNode.getPreviewBox(), g);
    }
  }

  /**
   * If there is an overlay set, draw overlay on top of the preview for Pixel Perfect feature.
   * Otherwise does nothing.
   * The overlay is drawn with an alpha for transparancy. After drawing returns
   * the composite to it's previous state.
   */
  private void drawOverlay(Graphics2D g) {
    if (mOverlay == null) {
      return;
    }
    Composite oldComposite = g.getComposite();
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, mOverlayAlpha));
    g.drawImage(mOverlay, 0, 0, mRoot.getPreviewBox().width, mRoot.getPreviewBox().height,
                0, 0, mOverlay.getWidth(null), mOverlay.getHeight(null), null);
    g.setComposite(oldComposite);
  }

  private void paintNode(ViewNode node, Graphics2D g) {
    if (node != mHoverNode && node != mSelectedNode) {
      // Hover node & selected node are drawn last
      paintBox(node.getPreviewBox(), g);
    }
    if (!node.isLeaf()) {
      for (ViewNode child : node.getChildren()) {
        if (child.isDrawn()) {
          paintNode(child, g);
        }
      }
    }
  }

  private void paintBox(Rectangle box, Graphics2D g) {
    g.drawRect(box.x, box.y, box.width, box.height);
  }

  @Nullable
  private ViewNode getNode(MouseEvent e) {
    int x = e.getX() - mDrawShiftX;
    int y = e.getY() - mDrawShiftY;

    if (!mRoot.getPreviewBox().contains(x, y)) {
      return null;
    }
    return updateSelection(mRoot, x, y, new ViewNode[1], 0, 0,
                           mRoot.getPreviewBox().width, mRoot.getPreviewBox().height);
  }

  @Nullable
  private ViewNode updateSelection(@NotNull ViewNode node, int x, int y, @NotNull ViewNode[] firstNoDrawChild,
                                   int clipX1, int clipY1, int clipX2, int clipY2) {
    if (!node.isDrawn()) {
      return null;
    }
    boolean wasFirstNoDrawChildNull = firstNoDrawChild[0] == null;
    Rectangle boxpos = node.getPreviewBox();

    int boxRight = boxpos.x + boxpos.width;
    int boxBottom = boxpos.y + boxpos.height;
    int newClipX1 = clipX1;
    int newClipY1 = clipY1;
    int newClipX2 = clipX2;
    int newClipY2 = clipY2;
    if (node.getDisplayInfo().getClipChildren()) {
      newClipX1 = Math.max(clipX1, boxpos.x);
      newClipY1 = Math.max(clipY1, boxpos.y);
      newClipX2 = Math.min(clipX2, boxRight);
      newClipY2 = Math.min(clipY2, boxBottom);
    }
    if (newClipX1 < x && newClipX2 > x && newClipY1 < y && newClipY2 > y) {
      ViewNode result = null;
      for (int i = node.getChildren().size() - 1; i >= 0; i--) {
        ViewNode child = node.getChildAt(i);
        ViewNode ret = updateSelection(child, x, y, firstNoDrawChild, newClipX1, newClipY1, newClipX2, newClipY2);
        if (ret != null) {

          if (result == null) {
            result = ret;
            continue;
          }

          int resultArea = result.getPreviewBox().width * result.getPreviewBox().height;
          int retArea = ret.getPreviewBox().width * ret.getPreviewBox().height;

          if (retArea < resultArea) {
            result = ret;
          }
        }
      }

      if (result != null) {
        return result;
      }

    }
    if (boxpos.x < x && boxRight > x && boxpos.y < y && boxBottom > y) {
      if (node.getDisplayInfo().getWillNotDraw()) {
        if (firstNoDrawChild[0] == null) {
          firstNoDrawChild[0] = node;
        }
        return null;
      }
      else {
        if (wasFirstNoDrawChildNull && firstNoDrawChild[0] != null) {
          return firstNoDrawChild[0];
        }
        return node;
      }
    }
    return null;
  }

  public void setZoomFactor(float zoomFactor) {
    mZoomFactor = zoomFactor;
    float rootHeight = mRoot.getDisplayInfo().getHeight();
    float rootWidth = mRoot.getDisplayInfo().getWidth();
    // when zoom factor changes, change the size. more zoomed in = bigger size and vice versa.
    setPreferredSize(new Dimension((int)(rootWidth * mZoomFactor), (int)(rootHeight * mZoomFactor)));
    revalidate();
  }

  public void setGridVisible(boolean gridVisible) {
    mGridVisible = gridVisible;
    repaint();
  }

  public boolean isGridVisible() {
    return mGridVisible;
  }

  private void paintGrid(@NotNull Graphics g, @NotNull Image image) {
    Dimension size = getSize();
    int imageWidth = image.getWidth(null);
    int imageHeight = image.getHeight(null);
    double zoomX = (double)size.width / (double)imageWidth;
    double zoomY = (double)size.height / (double)imageHeight;

    g.setColor(GridOptions.DEFAULT_LINE_COLOR);
    int lineSpan = GridOptions.DEFAULT_LINE_SPAN;
    for (int dx = lineSpan; dx < imageWidth; dx += lineSpan) {
      LinePainter2D.paint((Graphics2D)g, (int)((double)dx * zoomX), 0, (int)((double)dx * zoomX), size.height);
    }
    for (int dy = lineSpan; dy < imageHeight; dy += lineSpan) {
      LinePainter2D.paint((Graphics2D)g, 0, (int)((double)dy * zoomY), size.width, (int)((double)dy * zoomY));
    }
  }

  public void setOverLay(@Nullable Image overlay, @Nullable String fileName) {
    mOverlay = overlay;
    myOverlayFileName = fileName;
    mOverlayAlpha = DEFAULT_OVERLAY_ALPHA; // reset on selecting new image
    repaint();
  }

  public boolean hasOverlay() {
    return mOverlay != null;
  }

  @Nullable
  public String getOverlayFileName() {
    return myOverlayFileName;
  }

  public float getOverlayAlpha() {
    return mOverlayAlpha;
  }

  public void setOverlayAlpha(float mOverlayAlpha) {
    this.mOverlayAlpha = mOverlayAlpha;
    repaint();
  }

  public void setPreview(@NotNull BufferedImage preview, ViewNode root) {
    mPreview = preview;
    mRoot = root;
    updateBounds = true;
    repaint();
  }

  private class MyMouseAdapter extends MouseAdapter {

    @Override
    public void mouseEntered(MouseEvent e) {
      setHoverNode(getNode(e));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      setHoverNode(getNode(e));
    }

    @Override
    public void mouseExited(MouseEvent e) {
      setHoverNode(null);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      ViewNode clicked = getNode(e);
      // Prevent unselecting a node.
      if (clicked != null) {
        setSelectedNode(clicked);
      }
    }
  }

  public interface ViewNodeActiveDisplayListener {

    void onViewNodeOver(@Nullable ViewNode node);

    void onNodeSelected(@NotNull ViewNode node);

    void onNodeDoubleClicked(@NotNull ViewNode node);
  }
}
