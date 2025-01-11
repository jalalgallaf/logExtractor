package com.kubectl.logParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchGroupDialog extends JDialog {
    private boolean approved = false;
    private String searchText;
    private String operation;
    private boolean caseSensitive;
    private boolean useRegex;
    private final JTextField searchField;
    private final JComboBox<String> operationCombo;
    private final JCheckBox caseSensitiveCheck;
    private final JCheckBox regexCheck;

    public SearchGroupDialog(Frame owner) {
        this(owner, null);
    }

    public SearchGroupDialog(Frame owner, LogParser.SearchGroup groupToEdit) {
        super(owner, (groupToEdit != null ? "Edit" : "Add") + " Search Group", true);
        setSize(400, 250);
        setLocationRelativeTo(owner);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Operation:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        operationCombo = new JComboBox<>(new String[]{"AND", "OR"});
        inputPanel.add(operationCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        inputPanel.add(new JLabel("Search Text:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        searchField = new JTextField(20);
        inputPanel.add(searchField, gbc);

        JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        caseSensitiveCheck = new JCheckBox("Case Sensitive");
        regexCheck = new JCheckBox("Use Regex");
        checkboxPanel.add(caseSensitiveCheck);
        checkboxPanel.add(regexCheck);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        inputPanel.add(checkboxPanel, gbc);

        JTextArea helpText = new JTextArea(
            "Regex Examples:\n" +
            "  \\d+         - Match one or more digits\n" +
            "  \\[.*?\\]    - Match text in square brackets\n" +
            "  error|warn  - Match 'error' or 'warn'\n" +
            "  ^\\[\\d+\\]  - Match '[digits]' at start of line"
        );
        helpText.setEditable(false);
        helpText.setBackground(new Color(245, 245, 245));
        helpText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        helpText.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        inputPanel.add(helpText, gbc);

        if (groupToEdit != null) {
            searchField.setText(groupToEdit.getText());
            operationCombo.setSelectedItem(groupToEdit.getOperation());
            caseSensitiveCheck.setSelected(groupToEdit.isCaseSensitive());
            regexCheck.setSelected(groupToEdit.isUseRegex());
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (searchField.getText().trim().isEmpty()) {
                    JOptionPane.showMessageDialog(SearchGroupDialog.this,
                        "Please enter search text",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (regexCheck.isSelected()) {
                    try {
                        Pattern.compile(searchField.getText().trim());
                    } catch (PatternSyntaxException ex) {
                        JOptionPane.showMessageDialog(SearchGroupDialog.this,
                            "Invalid regular expression: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                approved = true;
                searchText = searchField.getText().trim();
                operation = (String) operationCombo.getSelectedItem();
                caseSensitive = caseSensitiveCheck.isSelected();
                useRegex = regexCheck.isSelected();
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        getRootPane().setDefaultButton(okButton);
    }

    public boolean isApproved() {
        return approved;
    }

    public String getSearchText() {
        return searchText;
    }

    public String getOperation() {
        return operation;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public boolean isUseRegex() {
        return useRegex;
    }
}
