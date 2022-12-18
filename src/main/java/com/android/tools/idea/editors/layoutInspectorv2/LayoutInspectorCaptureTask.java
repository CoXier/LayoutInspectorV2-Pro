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

import com.google.common.annotations.VisibleForTesting;
import com.android.ddmlib.Client;
import com.android.layoutinspectorv2.LayoutInspectorBridge;
import com.android.layoutinspectorv2.LayoutInspectorCaptureOptions;
import com.android.layoutinspectorv2.LayoutInspectorResult;
import com.android.layoutinspectorv2.ProtocolVersion;
import com.android.layoutinspectorv2.model.ClientWindow;
import com.android.tools.analytics.UsageTracker;
import com.android.tools.idea.flags.StudioFlags;
import com.android.tools.idea.profiling.capture.Capture;
import com.android.tools.idea.profiling.capture.CaptureService;
import com.android.tools.idea.stats.AndroidStudioUsageTracker;
import com.android.tools.idea.stats.UsageTrackerUtils;
import com.google.wireless.android.sdk.stats.AndroidStudioEvent;
import com.google.wireless.android.sdk.stats.LayoutInspectorEvent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

public class LayoutInspectorCaptureTask extends Task.Backgroundable {
  private static final String TITLE = "Capture View Hierarchy";

  @NotNull private final Client myClient;
  @NotNull private final ClientWindow myWindow;

  private String myError;
  private byte[] myData;

  public LayoutInspectorCaptureTask(@NotNull Project project, @NotNull Client client, @NotNull ClientWindow window) {
    super(project, "Capturing View Hierarchy");
    myClient = client;
    myWindow = window;
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    LayoutInspectorCaptureOptions options = new LayoutInspectorCaptureOptions();
    options.setTitle(myWindow.getDisplayName());
    ProtocolVersion version =
      determineProtocolVersion(myClient.getDevice().getVersion().getApiLevel(), true);
    options.setVersion(
      version);

    // Capture view hierarchy
    indicator.setText("Capturing View Hierarchy");
    indicator.setIndeterminate(false);

    long startTimeMs = System.currentTimeMillis();
    LayoutInspectorResult result = LayoutInspectorBridge.captureView(myWindow, options);
    long captureDurationMs = System.currentTimeMillis() - startTimeMs;
    UsageTracker.log(UsageTrackerUtils.withProjectId(
      AndroidStudioEvent.newBuilder().setKind(AndroidStudioEvent.EventKind.LAYOUT_INSPECTOR_EVENT)
        .setDeviceInfo(AndroidStudioUsageTracker.deviceToDeviceInfo(myClient.getDevice()))
        .setLayoutInspectorEvent(LayoutInspectorEvent.newBuilder()
          .setType(LayoutInspectorEvent.LayoutInspectorEventType.CAPTURE)
          .setDurationInMs(captureDurationMs)
          .setDataSize(result.getError().isEmpty() ? result.getData().length : 0)),
      myProject));

    UsageTracker.log(UsageTrackerUtils.withProjectId(
      AndroidStudioEvent.newBuilder().setKind(AndroidStudioEvent.EventKind.LAYOUT_INSPECTOR_EVENT)
        .setDeviceInfo(AndroidStudioUsageTracker.deviceToDeviceInfo(myClient.getDevice()))
        .setLayoutInspectorEvent(LayoutInspectorEvent.newBuilder()
          .setType(LayoutInspectorEvent.LayoutInspectorEventType.CAPTURE)
          .setDurationInMs(captureDurationMs)
          .setVersion(version.ordinal() + 1)
          .setDataSize(result.getError().isEmpty()
                       ? result.getData().length
                       : 0)),
        myProject));
    
    if (!result.getError().isEmpty()) {
      myError = result.getError();
      return;
    }

    myData = result.getData();
  }

  @VisibleForTesting
  static ProtocolVersion determineProtocolVersion(int apiVersion, boolean v2Enabled) {
    return apiVersion >= LayoutInspectorBridge.getV2_MIN_API() && v2Enabled ? ProtocolVersion.Version2 : ProtocolVersion.Version1;
  }

  @Override
  public void onSuccess() {
    if (myError != null) {
      Messages.showErrorDialog("Error obtaining view hierarchy: " + StringUtil.notNullize(myError), TITLE);
      return;
    }

    CaptureService service = CaptureService.getInstance(myProject);
    try {
      Capture capture = service.createCapture(LayoutInspectorCaptureType.class, myData, service.getSuggestedName(myClient));
      final VirtualFile file = capture.getFile();
      file.refresh(true, false, () -> UIUtil.invokeLaterIfNeeded(() -> {
        OpenFileDescriptor descriptor = new OpenFileDescriptor(myProject, file);
        List<FileEditor> editors = FileEditorManager.getInstance(myProject).openEditor(descriptor, true);

        editors.stream().filter(e -> e instanceof LayoutInspectorEditor).findFirst().ifPresent((editor) -> {
          ((LayoutInspectorEditor)editor).setSources(myClient, myWindow);
        });
      }));
    }
    catch (IOException e) {
      Messages.showErrorDialog("Error creating hierarchy view capture: " + e, TITLE);
    }
  }
}
