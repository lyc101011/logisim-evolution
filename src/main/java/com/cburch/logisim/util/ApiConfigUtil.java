/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to read environment variables, configuration files, and .env
 * files for API configuration.
 */
public class ApiConfigUtil {
  static final Logger logger = LoggerFactory.getLogger(ApiConfigUtil.class);

  // Default values are now generic or empty to avoid hardcoding secrets
  private static final String DEFAULT_API_KEY = "";
  private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
  private static final String DEFAULT_MODEL = "gpt-3.5-turbo";

  private static String apiKey = DEFAULT_API_KEY;
  private static String baseUrl = DEFAULT_BASE_URL;
  private static String model = DEFAULT_MODEL;

  static {
    // 1. Initialize with defaults (done above)

    // 2. Load from logisim-evolution.properties (if exists)
    loadFromPropertiesFile();

    // 3. Load from .env file (legacy/dev support) - overrides properties if present
    loadFromEnvFile();

    // 4. Check System Environment variables - overrides all
    loadFromSystemEnv();
  }

  private static void loadFromPropertiesFile() {
    File configFile = new File("logisim-evolution.properties");
    if (!configFile.exists()) {
      // Try user home directory
      configFile = new File(System.getProperty("user.home"), "logisim-evolution.properties");
    }

    if (configFile.exists()) {
      try (InputStream input = new FileInputStream(configFile)) {
        Properties prop = new Properties();
        prop.load(input);

        if (prop.getProperty("api.key") != null && !prop.getProperty("api.key").isEmpty()) {
          apiKey = prop.getProperty("api.key");
        }
        if (prop.getProperty("api.base.url") != null && !prop.getProperty("api.base.url").isEmpty()) {
          baseUrl = prop.getProperty("api.base.url");
        }
        if (prop.getProperty("api.model") != null && !prop.getProperty("api.model").isEmpty()) {
          model = prop.getProperty("api.model");
        }
      } catch (IOException ex) {
        logger.error("Error reading configuration file", ex);
      }
    }
  }

  private static void loadFromSystemEnv() {
    String envApiKey = System.getenv("OPENAI_API_KEY");
    if (envApiKey != null && !envApiKey.isEmpty()) {
      apiKey = envApiKey;
    }

    String envBaseUrl = System.getenv("OPENAI_BASE_URL");
    if (envBaseUrl != null && !envBaseUrl.isEmpty()) {
      baseUrl = envBaseUrl;
    }

    String envModel = System.getenv("OPENAI_MODEL");
    if (envModel != null && !envModel.isEmpty()) {
      model = envModel;
    }
  }

  /**
   * Loads API configuration from .env file.
   */
  private static void loadFromEnvFile() {
    try (BufferedReader reader = new BufferedReader(new FileReader(".env"))) {
      String line;
      while ((line = reader.readLine()) != null) {
        // Skip comments and empty lines
        if (line.trim().startsWith("#") || line.trim().isEmpty()) {
          continue;
        }

        // Remove "export " prefix if present
        if (line.startsWith("export ")) {
          line = line.substring(7);
        }

        // Split on '='
        final String[] parts = line.split("=", 2);
        if (parts.length == 2) {
          final String key = parts[0].trim();
          final String value = parts[1].trim();

          switch (key) {
            case "OPENAI_API_KEY":
              if (!value.isEmpty()) {
                apiKey = value;
              }
              break;
            case "OPENAI_BASE_URL":
              if (!value.isEmpty()) {
                baseUrl = value;
              }
              break;
            case "OPENAI_MODEL":
              if (!value.isEmpty()) {
                model = value;
              }
              break;
            default:
              // Ignore unknown keys
              break;
          }
        }
      }
    } catch (IOException e) {
      // It's okay if .env file doesn't exist
      logger.debug("Could not read .env file: {}", e.getMessage());
    }
  }

  /**
   * Gets the current API configuration.
   *
   * @return a map containing the API configuration
   */
  public static Map<String, String> getApiConfig() {
    Map<String, String> config = new HashMap<>();
    config.put("apiKey", apiKey);
    config.put("baseUrl", baseUrl);
    config.put("model", model);
    return config;
  }

  /**
   * Gets the API key.
   *
   * @return the API key
   */
  public static String getApiKey() {
    return apiKey;
  }

  /**
   * Gets the base URL.
   *
   * @return the base URL
   */
  public static String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Gets the model name.
   *
   * @return the model name
   */
  public static String getModel() {
    return model;
  }

  /**
   * Sets the API configuration.
   *
   * @param newApiKey  the new API key
   * @param newBaseUrl the new base URL
   * @param newModel   the new model name
   */
  public static void setApiConfig(String newApiKey, String newBaseUrl, String newModel) {
    if (newApiKey != null && !newApiKey.isEmpty()) {
      apiKey = newApiKey;
    }
    if (newBaseUrl != null && !newBaseUrl.isEmpty()) {
      baseUrl = newBaseUrl;
    }
    if (newModel != null && !newModel.isEmpty()) {
      model = newModel;
    }
  }
}