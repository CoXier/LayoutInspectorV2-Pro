package com.android.tools.idea.editors.layoutInspectorv2.actions;

import com.android.ddmlib.Client;
import com.android.layoutinspectorv2.model.ClientWindow;
import com.android.tools.idea.editors.layoutInspectorv2.LayoutInspectorCaptureTask;
import com.android.tools.idea.editors.layoutInspectorv2.WindowPickerDialog;
import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class GetClientWindowsTask extends Task.Backgroundable {
  private final Client myClient;
  private List<ClientWindow> myWindows;
  private String myError;

  @NotNull
  private final ClientWindowRetriever myClientWindowRetriever;

  @VisibleForTesting
  interface ClientWindowRetriever {
    default List<ClientWindow> getAllWindows(@NotNull Client client, long timeout, @NotNull TimeUnit timeoutUnits) throws IOException {
      return ClientWindow.getAll(client, timeout, timeoutUnits);
    }
  }

  @VisibleForTesting
  GetClientWindowsTask(@Nullable Project project, @NotNull Client client, @NotNull ClientWindowRetriever windowRetriever) {
    super(project, "Obtaining Windows");
    myClient = client;
    myError = null;

    myClientWindowRetriever = windowRetriever;
  }

  public GetClientWindowsTask(@Nullable Project project, @NotNull Client client) {
    this(project, client, new ClientWindowRetriever() {});
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    indicator.setIndeterminate(true);

    try {
      myWindows = myClientWindowRetriever.getAllWindows(myClient, 5, TimeUnit.SECONDS);

      if (myWindows == null) {
        myError = "Unable to obtain list of windows used by " +
                myClient.getClientData().getPackageName() +
                "\nLayout Inspector requires device API version to be 18 or greater.";
      }
      else if (myWindows.isEmpty()) {
        myError = "No active windows displayed by " + myClient.getClientData().getPackageName();
      }
    }
    catch (IOException e) {
      myError = "Unable to obtain list of windows used by " + myClient.getClientData().getPackageName() + "\nError: " + e.getMessage();
    }
  }

  @Override
  public void onSuccess() {
    if (myError != null) {
      Messages.showErrorDialog(myError, "Capture View Hierarchy");
      return;
    }

    ClientWindow window;
    if (myWindows.size() == 1) {
      window = myWindows.get(0);
    }
    else { // prompt user if there are more than 1 windows displayed by this application
      WindowPickerDialog pickerDialog = new WindowPickerDialog(myProject, myClient, myWindows);
      if (!pickerDialog.showAndGet()) {
        return;
      }

      window = pickerDialog.getSelectedWindow();
      if (window == null) {
        return;
      }
    }

    LayoutInspectorCaptureTask captureTask = new LayoutInspectorCaptureTask(myProject, myClient, window);
    captureTask.queue();
  }
}
