package org.wikipathways.cytoscapeapp.internal.cmd;

import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.ObservableTask;

import org.wikipathways.cytoscapeapp.WPClient;
import org.wikipathways.cytoscapeapp.WPPathway;
import org.wikipathways.cytoscapeapp.ResultTask;

public class WPSearchCmdTask extends AbstractTask {
  @Tunable
  public String query;

  @Tunable
  public String species = null;

  final WPClient client;
  public WPSearchCmdTask(
      final WPClient client
    ) {
    this.client = client;
  }

  public void run(TaskMonitor monitor) {
    if (query == null || query.length() == 0) {
      throw new IllegalArgumentException("query must be specified");
    }
    final ResultTask<List<WPPathway>> searchTask = client.newFreeTextSearchTask(query, species);
    insertTasksAfterCurrentTask(searchTask, new ObservableTask() {
      public void run(TaskMonitor monitor) {}
      public void cancel() {}
      public <R> R getResults(Class<? extends R> type) {
        if (List.class.equals(type)) {
          return (R) searchTask.get();
        } else if (String.class.equals(type)) {
          return (R) searchTask.get().toString();
        } else {
          return null;
        }
      }
    });
  }
}