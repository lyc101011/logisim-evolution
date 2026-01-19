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
 * Class to hold the response from the large language model,
 * including both the thinking process and the final answer.
 */
public class LlmResponse {
  private String thinkingProcess;
  private String finalAnswer;
  
  /**
   * Constructor.
   * @param thinkingProcess the thinking process from the model
   * @param finalAnswer the final answer from the model
   */
  public LlmResponse(String thinkingProcess, String finalAnswer) {
    this.thinkingProcess = thinkingProcess;
    this.finalAnswer = finalAnswer;
  }
  
  /**
   * Gets the thinking process.
   * @return the thinking process
   */
  public String getThinkingProcess() {
    return thinkingProcess;
  }
  
  /**
   * Sets the thinking process.
   * @param thinkingProcess the thinking process to set
   */
  public void setThinkingProcess(String thinkingProcess) {
    this.thinkingProcess = thinkingProcess;
  }
  
  /**
   * Gets the final answer.
   * @return the final answer
   */
  public String getFinalAnswer() {
    return finalAnswer;
  }
  
  /**
   * Sets the final answer.
   * @param finalAnswer the final answer to set
   */
  public void setFinalAnswer(String finalAnswer) {
    this.finalAnswer = finalAnswer;
  }
  
  /**
   * Checks if the response has a thinking process.
   * @return true if the response has a thinking process, false otherwise
   */
  public boolean hasThinkingProcess() {
    return thinkingProcess != null && !thinkingProcess.isEmpty();
  }
  
  /**
   * Checks if the response has a final answer.
   * @return true if the response has a final answer, false otherwise
   */
  public boolean hasFinalAnswer() {
    return finalAnswer != null && !finalAnswer.isEmpty();
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (hasThinkingProcess()) {
      sb.append("[Thinking Process]\n").append(thinkingProcess).append("\n\n");
    }
    if (hasFinalAnswer()) {
      sb.append("[Final Answer]\n").append(finalAnswer);
    }
    return sb.toString();
  }
}