package org.wikipathways.cytoscapeapp.internal.io;

import java.util.Map;
import java.util.HashMap;
import java.awt.Shape;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;

class DelayedAnnot {
  enum Type {
    SHAPE
  }

  final Type type;
  final Map<String,String> argMap;
  final Shape shape;

  public static DelayedAnnot newShape(final Shape shape, final Map<String,String> argMap) {
    return new DelayedAnnot(Type.SHAPE, shape, argMap);
  }

  private DelayedAnnot(final Type type, final Shape shape, final Map<String,String> argMap) {
    this.type = type;
    this.shape = shape;
    this.argMap = argMap;
  }

  public static void applyAll(final CyNetworkView view, final Annots annots, final Iterable<DelayedAnnot> delayedAnnots) {
    for (final DelayedAnnot delayedAnnot : delayedAnnots) {
      switch(delayedAnnot.type) {
      case SHAPE:
        final ShapeAnnotation annot = annots.newShape(view, delayedAnnot.argMap);
        if (delayedAnnot.shape != null) {
          annot.setCustomShape(delayedAnnot.shape);
        }
        break;
      }
    }
  }

  static Map<String,String> ezMap(Object[] elems) {
    final Map<String,String> map = new HashMap<String,String>();
    for (int i = 0; i < elems.length; i += 2) {
      map.put(elems[i].toString(), elems[i+1].toString());
    }
    return map;
  }
}