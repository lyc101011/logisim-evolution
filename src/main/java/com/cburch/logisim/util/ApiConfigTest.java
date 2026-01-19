/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.util.Map;

/**
 * Simple test class to verify the API configuration functionality.
 */
public class ApiConfigTest {
  public static void main(String[] args) {
    System.out.println("Testing API Configuration...");
    
    // Get the current API configuration
    Map<String, String> config = ApiConfigUtil.getApiConfig();
    
    System.out.println("API Key: " + config.get("apiKey"));
    System.out.println("Base URL: " + config.get("baseUrl"));
    System.out.println("Model: " + config.get("model"));
    
    System.out.println("Test completed.");
  }
}