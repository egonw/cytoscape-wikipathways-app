package org.wikipathways.cytoscapeapp.internal.io;

import java.util.List;

class DelayedView {
  public final List<DelayedVizProp> vizProps;
  public final List<DelayedAnnot> annots;

  public DelayedView(
      final List<DelayedVizProp> vizProps,
      final List<DelayedAnnot> annots
    ) {
    this.vizProps = vizProps;
    this.annots = annots;
  }
}