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

import com.android.ddmlib.Client;
import com.android.layoutinspectorv2.LayoutInspectorBridge;
import com.android.layoutinspectorv2.LayoutInspectorCaptureOptions;
import com.android.layoutinspectorv2.LayoutInspectorResult;
import com.android.layoutinspectorv2.ProtocolVersion;
import com.android.layoutinspectorv2.model.ClientWindow;
import com.android.tools.analytics.UsageTracker;
import com.android.tools.idea.stats.AndroidStudioUsageTracker;
import com.google.common.annotations.VisibleForTesting;
import com.google.wireless.android.sdk.stats.AndroidStudioEvent;
import com.google.wireless.android.sdk.stats.LayoutInspectorEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class LayoutInspectorCaptureTask extends Task.Backgroundable {
  private static final String TITLE = "Capture view hierarchy";

  @NotNull private final Client myClient;
  @NotNull private final ClientWindow myWindow;

  private String myError;
  private byte[] myData;

  private File file;

  public LayoutInspectorCaptureTask(@NotNull Project project, @NotNull Client client, @NotNull ClientWindow window) {
    super(project, TITLE);
    myClient = client;
    myWindow = window;
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    LayoutInspectorCaptureOptions options = new LayoutInspectorCaptureOptions();
    options.setTitle(myWindow.getDisplayName());
    ProtocolVersion version =
      determineProtocolVersion(myClient.getDevice().getVersion().getApiLevel());

    options.setVersion(version);

    // Capture view hierarchy
    indicator.setText(TITLE);
    indicator.setIndeterminate(false);

    long startTimeMs = System.currentTimeMillis();
    LayoutInspectorResult result = LayoutInspectorBridge.captureView(myWindow, options);
    long captureDurationMs = System.currentTimeMillis() - startTimeMs;
    
    if (!result.getError().isEmpty()) {
      myError = result.getError();
      return;
    }

    myData = result.getData();

    // write data to file
    file = LayoutInspectorFileHelper.saveToFile(myClient, this.myProject, myData);

  }

  @VisibleForTesting
  static ProtocolVersion determineProtocolVersion(int apiVersion) {
    return apiVersion >= LayoutInspectorBridge.getV2_MIN_API() ? ProtocolVersion.Version2 : ProtocolVersion.Version1;
  }

  @Override
  public void onSuccess() {
    if (myError != null) {
      Messages.showErrorDialog("Error obtaining view hierarchy: " + StringUtil.notNullize(myError), TITLE);
      return;
    }

    if (file == null) {
      Messages.showErrorDialog("Cannot save file", TITLE);
      return;
    }

    VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
    FileEditorManager.getInstance(myProject).openEditor(new OpenFileDescriptor(myProject, virtualFile), true);
  }
}
