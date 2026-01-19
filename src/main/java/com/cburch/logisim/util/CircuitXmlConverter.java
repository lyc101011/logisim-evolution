/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.logisim.file.CircuitXmlExporter;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to convert Logisim circuit files to XML format.
 */
public class CircuitXmlConverter {
  static final Logger logger = LoggerFactory.getLogger(CircuitXmlConverter.class);

  /**
   * Converts the current project's Logisim file to XML string format.
   *
   * @param project the current Logisim project
   * @return XML string representation of the circuit, or null if conversion fails
   */
  public static String circuitToXmlString(Project project) {
    if (project == null) {
      logger.error("Project is null");
      return null;
    }

    LogisimFile file = project.getLogisimFile();
    if (file == null) {
      logger.error("LogisimFile is null");
      return null;
    }

    return CircuitXmlExporter.logisimFileToXmlString(file);
  }
}