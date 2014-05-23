package org.wikipathways.cytoscapeapp.internal.io;

import java.util.Map;
import java.util.HashMap;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;

/**
 * A wrapper for the kafka-esque annotations API.
 */
public class Annots {
  final AnnotationManager mgr;
  final AnnotationFactory<ArrowAnnotation> arrowFct;
  final AnnotationFactory<ShapeAnnotation> shapeFct;
  final AnnotationFactory<TextAnnotation> textFct;

  public Annots(
      final AnnotationManager mgr,
      final AnnotationFactory<ArrowAnnotation> arrowFct,
      final AnnotationFactory<ShapeAnnotation> shapeFct,
      final AnnotationFactory<TextAnnotation> textFct) {
    this.mgr = mgr;
    this.arrowFct = arrowFct;
    this.shapeFct = shapeFct;
    this.textFct = textFct;
  }


  public ArrowAnnotation newArrow(final CyNetworkView netView, final Map<String,String> args) {
    final ArrowAnnotation annot = arrowFct.createAnnotation(ArrowAnnotation.class, netView, args);
    mgr.addAnnotation(annot);
    return annot;
  }

  public ShapeAnnotation newShape(final CyNetworkView netView, final Map<String,String> args) {
    final ShapeAnnotation annot = shapeFct.createAnnotation(ShapeAnnotation.class, netView, args);
    mgr.addAnnotation(annot);
    return annot;
  }

  public TextAnnotation newText(final CyNetworkView netView, final Map<String,String> args) {
    final TextAnnotation annot = textFct.createAnnotation(TextAnnotation.class, netView, args);
    mgr.addAnnotation(annot);
    return annot;
  }
}
