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
 * Callback interface for streaming responses from the LLM API.
 */
public interface StreamingCallback {
  /**
   * Called when a chunk of thinking process content is received.
   * @param content the content chunk
   */
  void onThinkingProcess(String content);
  
  /**
   * Called when a chunk of final answer content is received.
   * @param content the content chunk
   */
  void onFinalAnswer(String content);
  
  /**
   * Called when the streaming is complete.
   * @param thinkingProcess the complete thinking process
   * @param finalAnswer the complete final answer
   */
  void onComplete(String thinkingProcess, String finalAnswer);
  
  /**
   * Called when an error occurs during streaming.
   * @param error the error message
   */
  void onError(String error);
}