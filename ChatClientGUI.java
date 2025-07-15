import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClientGUI extends JFrame {

    private JTextPane chatPane;
    private JTextField inputField;
    private JButton sendButton;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String username;
    private static final String HISTORY_FILE = "chat_history.txt";

    public ChatClientGUI(String serverAddress, int port) {
        username = JOptionPane.showInputDialog(this, "Enter your username:", "Login", JOptionPane.PLAIN_MESSAGE);
        if (username == null || username.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username is required.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

        setTitle("Chat - " + username);
        setSize(500, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        chatPane.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(new Color(245, 245, 245));
        inputField.setBorder(new RoundedBorder(15));
        bottomPanel.add(inputField, BorderLayout.CENTER);

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        sendButton.setForeground(Color.WHITE);
        sendButton.setBackground(new Color(0, 153, 76));
        sendButton.setFocusPainted(false);
        sendButton.setBorder(new RoundedBorder(15));
        bottomPanel.add(sendButton, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        try {
            socket = new Socket(serverAddress, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            appendStyledMessage("Connected to chat server as " + username, false);

            out.println(username + " has joined the chat.");

            new Thread(new IncomingReader()).start();

        } catch (IOException ex) {
            appendStyledMessage("Connection error: " + ex.getMessage(), false);
        }

        loadChatHistory();

        setVisible(true);
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            String fullMessage = username + ": " + message;
            out.println(fullMessage);
            inputField.setText("");
            saveMessageToFile(fullMessage);
            appendStyledMessage(fullMessage, true);
        }
    }

    private void appendStyledMessage(String message, boolean isSentByUser) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = chatPane.getStyledDocument();
                Style style = chatPane.addStyle("MessageStyle", null);

                StyleConstants.setFontFamily(style, "Segoe UI");
                StyleConstants.setFontSize(style, 14);
                StyleConstants.setForeground(style, Color.BLACK);

                if (isSentByUser) {
                    StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT);
                    StyleConstants.setLeftIndent(style, 50);
                    StyleConstants.setRightIndent(style, 10);
                } else {
                    StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
                    StyleConstants.setLeftIndent(style, 10);
                    StyleConstants.setRightIndent(style, 50);
                }

                StyleConstants.setSpaceAbove(style, 6);
                StyleConstants.setSpaceBelow(style, 6);
                StyleConstants.setLineSpacing(style, 0.2f);

                int len = doc.getLength();
                doc.insertString(len, message + "\n", style);
                doc.setParagraphAttributes(len, message.length(), style, true);
                chatPane.setCaretPosition(doc.getLength());

            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private class IncomingReader implements Runnable {
        public void run() {
            String message;
            try {
                while ((message = in.readLine()) != null) {
                    boolean isSentByMe = message.startsWith(username + ":");
                    appendStyledMessage(message, isSentByMe);
                    saveMessageToFile(message);
                }
            } catch (IOException ex) {
                appendStyledMessage("Disconnected from server.", false);
            }
        }
    }

    private void saveMessageToFile(String message) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(HISTORY_FILE, true))) {
            writer.println(message);
        } catch (IOException e) {
            System.err.println("Could not save chat history.");
        }
    }

    private void loadChatHistory() {
        File file = new File(HISTORY_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    boolean isSentByMe = line.startsWith(username + ":");
                    appendStyledMessage(line, isSentByMe);
                }
            } catch (IOException e) {
                System.err.println("Could not load chat history.");
            }
        }
    }

    // Custom rounded border for inputs and buttons
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;

        RoundedBorder(int radius) {
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 153, 76));
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.top = insets.right = insets.bottom = radius / 2;
            return insets;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClientGUI("localhost", 1234));
    }
}
