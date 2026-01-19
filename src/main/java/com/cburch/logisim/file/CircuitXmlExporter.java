/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.file;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to export Logisim circuit files to XML format.
 * This class is in the same package as LogisimFile to access package-private methods.
 */
public class CircuitXmlExporter {
  static final Logger logger = LoggerFactory.getLogger(CircuitXmlExporter.class);

  /**
   * Converts a LogisimFile to XML string format.
   *
   * @param file the LogisimFile to convert
   * @return XML string representation of the circuit, or null if conversion fails
   */
  public static String logisimFileToXmlString(LogisimFile file) {
    if (file == null) {
      logger.error("LogisimFile is null");
      return null;
    }

    try {
      // Create a ByteArrayOutputStream to capture the XML output
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      
      // Write the LogisimFile to the output stream as XML using XmlWriter directly
      XmlWriter.write(file, outputStream, file.getLoader(), null, null, false);
      
      // Convert the byte array to a string
      return outputStream.toString(StandardCharsets.UTF_8.name());
    } catch (IOException | TransformerException | ParserConfigurationException | LoadFailedException e) {
      logger.error("Error converting LogisimFile to XML string: {}", e.getMessage());
      return null;
    }
  }
}