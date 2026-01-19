/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

/**
 * Test class for streaming response functionality.
 */
public class StreamingResponseTest {
  /**
   * Main method to test streaming response functionality.
   * @param args command line arguments
   */
  public static void main(String[] args) {
    System.out.println("Testing streaming response functionality...");
    
    // Test the API configuration
    System.out.println("API Configuration:");
    System.out.println("API Key: " + ApiConfigUtil.getApiKey());
    System.out.println("Base URL: " + ApiConfigUtil.getBaseUrl());
    System.out.println("Model: " + ApiConfigUtil.getModel());
    
    // Test streaming API call
    try {
      System.out.println("\nMaking streaming API call...");
      boolean success = HttpClientUtil.streamLlmApi("1+1=?", null, new StreamingCallback() {
        @Override
        public void onThinkingProcess(String content) {
          System.out.print("[Thinking Process Chunk]: " + content);
        }
        
        @Override
        public void onFinalAnswer(String content) {
          System.out.print("[Final Answer Chunk]: " + content);
        }
        
        @Override
        public void onComplete(String thinkingProcess, String finalAnswer) {
          System.out.println("\n\nStreaming completed.");
          if (thinkingProcess != null && !thinkingProcess.isEmpty()) {
            System.out.println("[Complete Thinking Process]");
            System.out.println(thinkingProcess);
          }
          if (finalAnswer != null && !finalAnswer.isEmpty()) {
            System.out.println("[Complete Final Answer]");
            System.out.println(finalAnswer);
          }
        }
        
        @Override
        public void onError(String error) {
          System.err.println("Error during streaming: " + error);
        }
      });
      
      if (!success) {
        System.out.println("Streaming API call failed.");
      }
    } catch (Exception e) {
      System.err.println("Error during streaming API call: " + e.getMessage());
      e.printStackTrace();
    }
    
    // Also test the traditional call for comparison
    try {
      System.out.println("\nMaking traditional API call for comparison...");
      LlmResponse response = HttpClientUtil.callLlmApi("1+1=?", null);
      
      if (response != null) {
        System.out.println("Traditional response received:");
        if (response.hasThinkingProcess()) {
          System.out.println("[Thinking Process]");
          System.out.println(response.getThinkingProcess());
        }
        if (response.hasFinalAnswer()) {
          System.out.println("[Final Answer]");
          System.out.println(response.getFinalAnswer());
        }
      } else {
        System.out.println("No traditional response received.");
      }
    } catch (Exception e) {
      System.err.println("Error during traditional API call: " + e.getMessage());
      e.printStackTrace();
    }
    
    System.out.println("Test completed.");
  }
}