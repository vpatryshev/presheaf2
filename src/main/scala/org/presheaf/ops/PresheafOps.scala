package org.presheaf.ops

import org.presheaf._

import java.io._

/**
 * Presheaf operations
 */

trait PresheafOps {
  def ref(file: File): String = "cache/" + file.getName

  def process(dir: File, diagram: String) : Diagram = {
    require (diagram != null && !diagram.isEmpty, "no diagram to render")
    OS.log("Rendering diagram \"" + diagram + "\"")
    DiagramRenderer(dir).process(diagram)
  }
}