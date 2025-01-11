package com.kubectl;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

public class DeploymentEditorDialog extends JDialog {
    private RSyntaxTextArea editor;
    private final SSHConnection sshConnection;
    private final String podName;
    private boolean confirmed = false;
    private String updatedYaml;
    private static final Color ACCENT_COLOR = new Color(66, 133, 244);

    public DeploymentEditorDialog(Frame owner, SSHConnection sshConnection, String podName) {
        super(owner, "Edit Deployment - " + podName, true);
        this.sshConnection = sshConnection;
        this.podName = podName;
        
        setSize(1000, 700);
        setLocationRelativeTo(owner);
        
        initializeComponents();
        loadDeployment();
    }

    private void initializeComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);

        // Create editor
        editor = new RSyntaxTextArea(20, 60);
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
        editor.setCodeFoldingEnabled(true);
        editor.setAntiAliasingEnabled(true);
        editor.setAutoIndentEnabled(true);
        editor.setTabSize(2);
        
        // Add line numbers and wrap in scroll pane
        RTextScrollPane scrollPane = new RTextScrollPane(editor);
        scrollPane.setFoldIndicatorEnabled(true);
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        
        JButton saveButton = createStyledButton("Save", true);
        JButton cancelButton = createStyledButton("Cancel", false);
        
        saveButton.addActionListener(e -> saveDeployment());
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        
        // Add components to main panel
        mainPanel.add(new JLabel("Deployment YAML:"), BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add main panel to dialog
        add(mainPanel);
        
        // Add keyboard shortcuts
        KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
        editor.getInputMap().put(ctrlS, "save");
        editor.getActionMap().put("save", new AbstractAction("save") {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                saveDeployment();
            }
        });
    }

    private void loadDeployment() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new Thread(() -> {
            try {
                // Get deployment name from pod
                String result = sshConnection.executeCommand("kubectl get pod " + podName + " -o jsonpath='{.metadata.ownerReferences[0].name}'");
                String deploymentName = result.trim();
                
                // Get deployment YAML
                result = sshConnection.executeCommand("kubectl get deployment " + deploymentName + " -o yaml");
                
                final String yaml = result;
                SwingUtilities.invokeLater(() -> {
                    editor.setText(yaml);
                    editor.setCaretPosition(0);
                    setCursor(Cursor.getDefaultCursor());
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    JOptionPane.showMessageDialog(this,
                        "Error loading deployment: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    dispose();
                });
            }
        }).start();
    }

    private void saveDeployment() {
        try {
            updatedYaml = editor.getText();
            String command = String.format("echo '%s' | kubectl apply -f -", updatedYaml.replace("'", "'\\''"));
            String result = sshConnection.executeCommand(command);
            
            if (result.toLowerCase().contains("error") || result.toLowerCase().contains("failed")) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to apply deployment: " + result, 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            confirmed = true;
            dispose();
            JOptionPane.showMessageDialog(this, 
                "Deployment updated successfully!", 
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Failed to save deployment: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton createStyledButton(String text, boolean isPrimary) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(100, 30));
        if (isPrimary) {
            button.setBackground(ACCENT_COLOR);
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);
        } else {
            button.setBackground(Color.WHITE);
            button.setForeground(Color.BLACK);
            button.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        }
        button.setFocusPainted(false);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        return button;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getUpdatedYaml() {
        return updatedYaml;
    }

    public boolean isSaved() {
        return confirmed;
    }
}