package org.wikipathways.cytoscapeapp.internal.io;

import java.util.Map;
import org.cytoscape.view.presentation.annotations.Annotation;

class DelayedAnnot {
  public final Class<? extends Annotation> type;
  public final Map<String,String> argMap;

  public DelayedAnnot(
      final Class<? extends Annotation> type,
      final Map<String,String> argMap
    ) {
    this.type = type;
    this.argMap = argMap;
  }
}