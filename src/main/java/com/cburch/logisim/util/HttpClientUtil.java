/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.cburch.logisim.util.LlmResponse;
import com.cburch.logisim.util.StreamingCallback;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP client utility for calling large language model APIs.
 */
public class HttpClientUtil {
  static final Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);
  
  private static final Gson gson = new Gson();
  
  // API configuration
  private static String apiKey = ApiConfigUtil.getApiKey();
  private static String baseUrl = ApiConfigUtil.getBaseUrl();
  private static String model = ApiConfigUtil.getModel();
  
  /**
   * Calls the large language model API with the given prompt and circuit XML.
   *
   * @param prompt the user's prompt/question
   * @param circuitXml the XML representation of the circuit
   * @return the model's response with thinking process and final answer, or null if the call fails
   */
  public static LlmResponse callLlmApi(String prompt, String circuitXml) {
    // For backward compatibility, we'll create a callback that collects all content
    final StringBuilder thinkingProcess = new StringBuilder();
    final StringBuilder finalAnswer = new StringBuilder();
    
    boolean success = streamLlmApi(prompt, circuitXml, new StreamingCallback() {
      @Override
      public void onThinkingProcess(String content) {
        thinkingProcess.append(content);
      }
      
      @Override
      public void onFinalAnswer(String content) {
        finalAnswer.append(content);
      }
      
      @Override
      public void onComplete(String completeThinkingProcess, String completeFinalAnswer) {
        // Already handled by the string builders
      }
      
      @Override
      public void onError(String error) {
        logger.error("Error during streaming: {}", error);
      }
    });
    
    return success ? new LlmResponse(thinkingProcess.toString(), finalAnswer.toString()) : null;
  }
  
  /**
   * Calls the large language model API with the given prompt and circuit XML, providing real-time streaming updates.
   *
   * @param prompt the user's prompt/question
   * @param circuitXml the XML representation of the circuit
   * @param callback the callback to receive streaming updates
   * @return true if the call was successful, false otherwise
   */
  public static boolean streamLlmApi(String prompt, String circuitXml, StreamingCallback callback) {
    try {
      // Prepare the API endpoint URL
      URL url = new URI(baseUrl + "/chat/completions").toURL();
      
      // Create the HTTP connection
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("POST");
      connection.setRequestProperty("Content-Type", "application/json");
      connection.setRequestProperty("api-key", apiKey);
      connection.setDoOutput(true);
      connection.setDoInput(true);
      // Set connection timeouts
      connection.setConnectTimeout(30000); // 30 seconds
      connection.setReadTimeout(60000); // 60 seconds
      Gson gson = new Gson();
      logger.info("connection parmas: {}", gson.toJson(connection.getRequestProperties()));
      // Prepare the request payload
      JsonObject payload = new JsonObject();
      payload.addProperty("model", model);
      payload.addProperty("maxTokens", 2048);
      payload.addProperty("stream", true);
      
      // Add thinking process output
      JsonObject thinking = new JsonObject();
      thinking.addProperty("type", "enabled");
      payload.add("thinking", thinking);
      
      // Prepare messages array
      JsonArray messages = new JsonArray();
      
      // Add system message
      JsonObject systemMessage = new JsonObject();
      systemMessage.addProperty("role", "system");
      systemMessage.addProperty("content", "你是一个数字电路设计专家。用户会提供 logisim-evolution 的 circ 格式电路数据和问题。请分析电路并提供改进建议。");
      messages.add(systemMessage);
      
      // Add user message with circuit data
      JsonObject userMessage = new JsonObject();
      userMessage.addProperty("role", "user");
      
      // Combine prompt and circuit XML in the user message
      StringBuilder contentBuilder = new StringBuilder();
      contentBuilder.append("用户问题: ").append(prompt).append("\n\n");
      if (circuitXml != null && !circuitXml.isEmpty()) {
        contentBuilder.append("电路数据 (circ格式):\n").append(circuitXml);
      }
      
      userMessage.addProperty("content", contentBuilder.toString());
      messages.add(userMessage);
      
      payload.add("messages", messages);
      
      // Convert payload to JSON string
      String jsonPayload = gson.toJson(payload);
      
      // Send the request
      try (OutputStream os = connection.getOutputStream()) {
        byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      }
      
      // Get the response
      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
          StringBuilder thinkingProcess = new StringBuilder();
          StringBuilder finalAnswer = new StringBuilder();
          String line;
          while ((line = br.readLine()) != null) {
            // Handle streaming response
            logger.info("recv stream: {}", line);
            if (line.startsWith("data:")) {
              String jsonData = line.substring(5);
              if (!jsonData.equals("[DONE]")) {
                try {
                  JsonObject chunk = gson.fromJson(jsonData, JsonObject.class);
                  JsonArray choices = chunk.getAsJsonArray("choices");
                  if (choices != null && choices.size() > 0) {
                    JsonObject choice = choices.get(0).getAsJsonObject();
                    // Handle both old format (delta) and new format (message)
                    JsonObject messageObject = null;
                    if (choice.has("delta")) {
                      messageObject = choice.getAsJsonObject("delta");
                    } else if (choice.has("message")) {
                      messageObject = choice.getAsJsonObject("message");
                    }
                    
                    if (messageObject != null) {
                      // Check for reasoning_content (thinking process)
                      if (messageObject.has("reasoning_content") && !messageObject.get("reasoning_content").isJsonNull()) {
                        String reasoningContent = messageObject.get("reasoning_content").getAsString();
                        thinkingProcess.append(reasoningContent);
                        callback.onThinkingProcess(reasoningContent);
                      }
                      // Check for content (final answer)
                      else if (messageObject.has("content") && !messageObject.get("content").isJsonNull()) {
                        String content = messageObject.get("content").getAsString();
                        finalAnswer.append(content);
                        callback.onFinalAnswer(content);
                      }
                    }
                  }
                } catch (Exception e) {
                  // Ignore parsing errors for individual chunks
                  logger.debug("Error parsing streaming chunk: {}", e.getMessage());
                }
              }
            }
          }
          
          // Notify completion
          callback.onComplete(thinkingProcess.toString(), finalAnswer.toString());
          return true;
        }
      } else {
        logger.error("API call failed with response code: {}", responseCode);
        try (BufferedReader br = new BufferedReader(
            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
          StringBuilder errorResponse = new StringBuilder();
          String line;
          while ((line = br.readLine()) != null) {
            errorResponse.append(line);
          }
          logger.error("Error response: {}", errorResponse.toString());
          callback.onError("API call failed with response code: " + responseCode + ", error: " + errorResponse.toString());
        }
        return false;
      }
    } catch (IOException | URISyntaxException e) {
      logger.error("Error calling LLM API: {}", e.getMessage());
      callback.onError("Error calling LLM API: " + e.getMessage());
      return false;
    }
  }
  
  
    
  /**
   * Gets the current API configuration.
   *
   * @return a map containing the API configuration
   */
  public static Map<String, String> getApiConfig() {
    return ApiConfigUtil.getApiConfig();
  }
  
  /**
   * Sets the API configuration.
   *
   * @param newApiKey the new API key
   * @param newBaseUrl the new base URL
   * @param newModel the new model name
   */
  public static void setApiConfig(String newApiKey, String newBaseUrl, String newModel) {
    ApiConfigUtil.setApiConfig(newApiKey, newBaseUrl, newModel);
    
    // Update local variables
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