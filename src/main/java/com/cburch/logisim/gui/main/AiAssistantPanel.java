/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.CircuitXmlConverter;
import com.cburch.logisim.util.HttpClientUtil;
import com.cburch.logisim.util.LlmResponse;
import com.cburch.logisim.util.StreamingCallback;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiAssistantPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  static final Logger logger = LoggerFactory.getLogger(AiAssistantPanel.class);
  
  // Markdown processor
  private final Parser markdownParser;
  private final HtmlRenderer htmlRenderer;
  
  private final Project project;
  private final LFrame parent;
  private final JTextPane conversationArea;
  private final JTextField inputField;
  private final JButton sendButton;
  private final JButton clearButton;
  private final ArrayList<String> conversationHistory;
  
  public AiAssistantPanel(LFrame parent, Project project) {
    this.parent = parent;
    this.project = project;
    this.conversationHistory = new ArrayList<>();
    
    setLayout(new BorderLayout());
    
    // Create conversation area
    conversationArea = new JTextPane();
    conversationArea.setEditable(false);
    
    // Initialize markdown processor
    MutableDataSet options = new MutableDataSet();
    markdownParser = Parser.builder(options).build();
    htmlRenderer = HtmlRenderer.builder(options).build();
    
    // Set up styles for different types of content
    StyledDocument doc = conversationArea.getStyledDocument();
    
    // Default style
    Style defaultStyle = conversationArea.addStyle("Default", null);
    StyleConstants.setFontFamily(defaultStyle, Font.MONOSPACED);
    StyleConstants.setFontSize(defaultStyle, 12);
    
    // User message style
    Style userStyle = conversationArea.addStyle("User", null);
    StyleConstants.setFontFamily(userStyle, Font.MONOSPACED);
    StyleConstants.setFontSize(userStyle, 12);
    StyleConstants.setBold(userStyle, true);
    
    // AI assistant style
    Style aiStyle = conversationArea.addStyle("AI", null);
    StyleConstants.setFontFamily(aiStyle, Font.MONOSPACED);
    StyleConstants.setFontSize(aiStyle, 12);
    
    // Thinking process style
    Style thinkingStyle = conversationArea.addStyle("Thinking", null);
    StyleConstants.setFontFamily(thinkingStyle, Font.MONOSPACED);
    StyleConstants.setFontSize(thinkingStyle, 12);
    StyleConstants.setForeground(thinkingStyle, Color.GRAY);
    StyleConstants.setItalic(thinkingStyle, true);
    
    // Final answer style
    Style answerStyle = conversationArea.addStyle("Answer", null);
    StyleConstants.setFontFamily(answerStyle, Font.MONOSPACED);
    StyleConstants.setFontSize(answerStyle, 12);
    StyleConstants.setBold(answerStyle, true);
    
    final var conversationScrollPane = new JScrollPane(conversationArea);
    conversationScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    conversationScrollPane.setPreferredSize(new Dimension(300, 400));
    
    final var conversationPanel = new JPanel(new BorderLayout());
    conversationPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), "Conversation", TitledBorder.CENTER, TitledBorder.TOP));
    conversationPanel.add(conversationScrollPane, BorderLayout.CENTER);
    
    // Create input area
    final var inputPanel = new JPanel(new BorderLayout());
    inputPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createEtchedBorder(), "Your Message", TitledBorder.CENTER, TitledBorder.TOP));
    
    inputField = new JTextField();
    inputField.addActionListener(new InputFieldListener());
    
    sendButton = new JButton("Send");
    sendButton.addActionListener(new SendButtonListener());
    
    clearButton = new JButton("Clear");
    clearButton.addActionListener(new ClearButtonListener());
    
    final var buttonPanel = new JPanel();
    buttonPanel.add(sendButton);
    buttonPanel.add(clearButton);
    
    inputPanel.add(inputField, BorderLayout.CENTER);
    inputPanel.add(buttonPanel, BorderLayout.EAST);
    
    // Add components to main panel
    add(new JLabel("AI Assistant", JLabel.CENTER), BorderLayout.NORTH);
    add(conversationPanel, BorderLayout.CENTER);
    add(inputPanel, BorderLayout.SOUTH);
    
    // Add welcome message
    addMessage("AI Assistant", "Hello! I'm your AI assistant. How can I help you with your digital logic design?");
  }
  
  private void addMessage(String sender, String message) {
    try {
      StyledDocument doc = conversationArea.getStyledDocument();
      
      if ("You".equals(sender)) {
        doc.insertString(doc.getLength(), sender + ": ", conversationArea.getStyle("User"));
        doc.insertString(doc.getLength(), message + "\n\n", conversationArea.getStyle("Default"));
      } else if ("AI Assistant".equals(sender)) {
        doc.insertString(doc.getLength(), sender + ": ", conversationArea.getStyle("AI"));
        doc.insertString(doc.getLength(), message + "\n\n", conversationArea.getStyle("Default"));
      } else {
        doc.insertString(doc.getLength(), sender + ": " + message + "\n\n", conversationArea.getStyle("Default"));
      }
      
      conversationArea.setCaretPosition(doc.getLength());
    } catch (Exception e) {
      // Fallback to simple append if styling fails
      conversationArea.setText(conversationArea.getText() + sender + ": " + message + "\n\n");
      conversationArea.setCaretPosition(conversationArea.getDocument().getLength());
    }
  }
  
  private void sendToAiModel(String message) {
    addMessage("You", message);
    
    // Get the circuit XML data
    final var circuitXml = CircuitXmlConverter.circuitToXmlString(project);
    
    // Call the AI model API in a separate thread to avoid blocking the UI
    new Thread(() -> {
      // Track message positions for real-time updates
      final AtomicInteger thinkingStartPos = new AtomicInteger(-1);
      final AtomicInteger answerStartPos = new AtomicInteger(-1);
      final StringBuilder thinkingBuffer = new StringBuilder();
      final StringBuilder answerBuffer = new StringBuilder();
      
      boolean success = HttpClientUtil.streamLlmApi(message, circuitXml, new StreamingCallback() {
        @Override
        public void onThinkingProcess(String content) {
          SwingUtilities.invokeLater(() -> {
            try {
              StyledDocument doc = conversationArea.getStyledDocument();
              
              // If this is the first thinking content, add the header
              if (thinkingStartPos.get() == -1) {
                // Add initial message if not already present
                if (doc.getLength() == 0 || !conversationArea.getText().endsWith("\n\n")) {
                  doc.insertString(doc.getLength(), "AI Assistant: ", conversationArea.getStyle("AI"));
                }
                thinkingStartPos.set(doc.getLength());
                doc.insertString(doc.getLength(), "[Thinking Process]\n", conversationArea.getStyle("Thinking"));
              }
              
              // Process and append the content with markdown support
              insertMarkdownText(content, "Thinking");
              thinkingBuffer.append(content);
              
              // Scroll to the end
              conversationArea.setCaretPosition(doc.getLength());
            } catch (Exception e) {
              logger.error("Error updating thinking process: {}", e.getMessage());
            }
          });
        }
        
        @Override
        public void onFinalAnswer(String content) {
          SwingUtilities.invokeLater(() -> {
            try {
              StyledDocument doc = conversationArea.getStyledDocument();
              
              // If this is the first answer content and we have thinking content, add a separator
              if (answerStartPos.get() == -1 && thinkingStartPos.get() != -1) {
                doc.insertString(doc.getLength(), "\n\n", conversationArea.getStyle("Default"));
              }
              
              // If this is the first answer content, add the header
              if (answerStartPos.get() == -1) {
                // Add initial message if not already present
                if (doc.getLength() == 0 || !conversationArea.getText().endsWith("\n\n")) {
                  doc.insertString(doc.getLength(), "AI Assistant: ", conversationArea.getStyle("AI"));
                }
                answerStartPos.set(doc.getLength());
                doc.insertString(doc.getLength(), "[Final Answer]\n", conversationArea.getStyle("Answer"));
              }
              
              // Process and append the content with markdown support
              insertMarkdownText(content, "Answer");
              answerBuffer.append(content);
              
              // Scroll to the end
              conversationArea.setCaretPosition(doc.getLength());
            } catch (Exception e) {
              logger.error("Error updating final answer: {}", e.getMessage());
            }
          });
        }
        
        @Override
        public void onComplete(String completeThinkingProcess, String completeFinalAnswer) {
          SwingUtilities.invokeLater(() -> {
            try {
              // Add final newlines
              StyledDocument doc = conversationArea.getStyledDocument();
              doc.insertString(doc.getLength(), "\n\n", conversationArea.getStyle("Default"));
              conversationArea.setCaretPosition(doc.getLength());
            } catch (Exception e) {
              logger.error("Error finalizing response: {}", e.getMessage());
            }
          });
        }
        
        @Override
        public void onError(String error) {
          SwingUtilities.invokeLater(() -> {
            addMessage("AI Assistant", "Sorry, there was an error processing your request: " + error);
          });
        }
      });
      
      if (!success) {
        SwingUtilities.invokeLater(() -> {
          addMessage("AI Assistant", "Sorry, I couldn't generate a response. Please try again.");
        });
      }
    }).start();
  }
  
  
    
  private class InputFieldListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      sendMessage();
    }
  }
  
  private class SendButtonListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      sendMessage();
    }
  }
  
  private class ClearButtonListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      try {
        StyledDocument doc = conversationArea.getStyledDocument();
        doc.remove(0, doc.getLength());
      } catch (Exception ex) {
        conversationArea.setText("");
      }
      conversationHistory.clear();
      addMessage("AI Assistant", "Conversation cleared. How can I help you?");
    }
  }
  
  /**
   * Converts markdown text to styled document content with basic formatting.
   * @param markdownText the markdown text to convert
   * @param baseStyle the base style to apply to the converted text
   */
  private void insertMarkdownText(String markdownText, String baseStyle) {
    try {
      StyledDocument doc = conversationArea.getStyledDocument();
      
      // Simple markdown formatting for common elements
      String formattedText = markdownText;
      
      // Handle bold text (**text** or __text__)
      formattedText = formattedText.replaceAll("\\*\\*(.*?)\\*\\*", "$1"); // Remove bold markers
      formattedText = formattedText.replaceAll("__(.*?)__", "$1"); // Remove bold markers
      
      // Handle italic text (*text* or _text_)
      formattedText = formattedText.replaceAll("\\*(.*?)\\*", "$1"); // Remove italic markers
      formattedText = formattedText.replaceAll("_(.*?)_", "$1"); // Remove italic markers
      
      // Handle code blocks (`text`)
      formattedText = formattedText.replaceAll("`([^`]*?)`", "$1"); // Remove code markers
      
      // Handle headers (# Header)
      formattedText = formattedText.replaceAll("^#{1,6}\\s*(.*?)\\s*$", "$1"); // Remove header markers
      
      // Insert the formatted text
      doc.insertString(doc.getLength(), formattedText, conversationArea.getStyle(baseStyle));
    } catch (Exception e) {
      // Fallback to simple text insertion if markdown processing fails
      try {
        StyledDocument doc = conversationArea.getStyledDocument();
        doc.insertString(doc.getLength(), markdownText, conversationArea.getStyle(baseStyle));
      } catch (Exception ex) {
        logger.error("Error inserting markdown text: {}", ex.getMessage());
      }
    }
  }
  
  private void sendMessage() {
    final var message = inputField.getText().trim();
    if (!message.isEmpty()) {
      inputField.setText("");
      sendToAiModel(message);
    }
  }
}