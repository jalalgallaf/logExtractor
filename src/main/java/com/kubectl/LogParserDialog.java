package com.kubectl;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import com.kubectl.logParser.LogParser;
import com.kubectl.logParser.LogParser.SearchGroup;

public class LogParserDialog extends JDialog {
    private final JTextArea resultArea;
    private final List<SearchGroupPanel> searchGroupPanels;
    private final JPanel searchGroupsContainer;
    private final String logFilePath;
    private final LogParser logParser;
    private final JCheckBox includeAfterMatchCheckbox;
    private final Preferences prefs;
    private static final String LAST_EXPORT_PATH = "lastExportPath";

    public LogParserDialog(JFrame parent, String logFilePath) {
        super(parent, "Log Parser", true);
        this.logFilePath = logFilePath;
        this.logParser = new LogParser();
        this.searchGroupPanels = new ArrayList<>();
        this.prefs = Preferences.userNodeForPackage(LogParserDialog.class);

        setLayout(new BorderLayout());
        setSize(800, 600);
        setLocationRelativeTo(parent);

        // Create search panel
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create search groups container
        searchGroupsContainer = new JPanel();
        searchGroupsContainer.setLayout(new BoxLayout(searchGroupsContainer, BoxLayout.Y_AXIS));
        
        // Add initial search group
        addSearchGroup();

        // Create button panel for search groups
        JPanel searchButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addGroupButton = new JButton("Add Search Group");
        addGroupButton.addActionListener(e -> addSearchGroup());
        searchButtonPanel.add(addGroupButton);

        // Include after match checkbox
        includeAfterMatchCheckbox = new JCheckBox("Include all lines after matches");
        searchButtonPanel.add(includeAfterMatchCheckbox);

        // Add search components to search panel
        searchPanel.add(searchGroupsContainer, BorderLayout.CENTER);
        searchPanel.add(searchButtonPanel, BorderLayout.SOUTH);

        // Create results panel
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospace", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(resultArea);
        resultsPanel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton searchButton = new JButton("Search");
        JButton exportButton = new JButton("Export Results");
        JButton closeButton = new JButton("Close");

        searchButton.addActionListener(e -> performSearch());
        exportButton.addActionListener(e -> exportResults());
        closeButton.addActionListener(e -> dispose());

        buttonPanel.add(searchButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(closeButton);

        // Add all components to dialog
        add(searchPanel, BorderLayout.NORTH);
        add(resultsPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addSearchGroup() {
        SearchGroupPanel panel = new SearchGroupPanel(searchGroupPanels.isEmpty());
        searchGroupPanels.add(panel);
        searchGroupsContainer.add(panel);
        searchGroupsContainer.revalidate();
        searchGroupsContainer.repaint();
    }

    private void performSearch() {
        List<SearchGroup> searchGroups = new ArrayList<>();
        
        for (SearchGroupPanel panel : searchGroupPanels) {
            if (!panel.getText().isEmpty()) {
                try {
                    searchGroups.add(panel.createSearchGroup());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this,
                        "Invalid regex pattern: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
        }

        logParser.searchInLogFile(logFilePath, searchGroups, resultArea, includeAfterMatchCheckbox.isSelected());
    }

    private void exportResults() {
        String lastPath = prefs.get(LAST_EXPORT_PATH, System.getProperty("user.home"));
        JFileChooser fileChooser = new JFileChooser(lastPath);
        fileChooser.setSelectedFile(new File("log_search_results.txt"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            prefs.put(LAST_EXPORT_PATH, file.getParent());
            
            logParser.exportResults(file.getAbsolutePath(), resultArea.getText());
            JOptionPane.showMessageDialog(this,
                "Results exported successfully to " + file.getAbsolutePath(),
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private class SearchGroupPanel extends JPanel {
        private final JTextField searchText;
        private final JComboBox<String> operationCombo;
        private final JCheckBox caseSensitiveCheck;
        private final JCheckBox regexCheck;

        public SearchGroupPanel(boolean isFirst) {
            setBorder(BorderFactory.createEtchedBorder());
            setLayout(new FlowLayout(FlowLayout.LEFT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            if (!isFirst) {
                operationCombo = new JComboBox<>(new String[]{"AND", "OR"});
                add(operationCombo);
            } else {
                operationCombo = null;
            }

            searchText = new JTextField(20);
            add(new JLabel("Search:"));
            add(searchText);

            caseSensitiveCheck = new JCheckBox("Case Sensitive");
            regexCheck = new JCheckBox("Use Regex");
            add(caseSensitiveCheck);
            add(regexCheck);

            JButton removeButton = new JButton("X");
            removeButton.addActionListener(e -> {
                if (searchGroupPanels.size() > 1) {
                    searchGroupPanels.remove(this);
                    searchGroupsContainer.remove(this);
                    searchGroupsContainer.revalidate();
                    searchGroupsContainer.repaint();
                }
            });
            add(removeButton);
        }

        public String getText() {
            return searchText.getText().trim();
        }

        public SearchGroup createSearchGroup() {
            String operation = operationCombo != null ? (String) operationCombo.getSelectedItem() : "";
            return new SearchGroup(
                searchText.getText(),
                operation,
                caseSensitiveCheck.isSelected(),
                regexCheck.isSelected()
            );
        }
    }
}
