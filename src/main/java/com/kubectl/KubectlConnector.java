package com.kubectl;

import com.formdev.flatlaf.FlatLightLaf;
import com.kubectl.logParser.LogParser;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class KubectlConnector extends JFrame {
    // Fields
    private SSHConnection sshConnection;
    private JTextField hostField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> environmentCombo;
    private JComboBox<String> podListCombo;
    private JTextArea outputArea;
    private JTextField commandField;
    private JButton saveLogsButton;
    private JButton getLogsButton;
    private JToggleButton watchLogsButton;
    private JButton refreshPodsButton;
    private JButton editDeploymentButton;
    private JButton parseLogsButton;
    private JButton searchButton;
    private JTextField searchField;
    private JComboBox<String> operationCombo;
    private JCheckBox caseSensitiveCheck;
    private JCheckBox regexCheck;
    private JCheckBox includeAfterMatchCheck;
    private JList<LogSearchCriteria> criteriaList;
    private DefaultListModel<LogSearchCriteria> criteriaListModel;
    private JButton exportButton;
    private Color accentColor = new Color(66, 133, 244);
    private CredentialsManager credentialsManager;
    private JCheckBox rememberCredentialsCheckbox;
    private HashMap<String, String> environmentCommands;
    private List<LogSearchCriteria> currentSearchGroups;
    private boolean includeAfterMatch = false;
    private JButton resetSearchButton;
    private Map<String, PodInfo> podInfoMap;
    private javax.swing.Timer logWatchTimer;
    private String originalLogContent;
    private SearchDialog searchDialog;
    private JButton openInVSCodeButton; // New Button

    public KubectlConnector() {
        FlatLightLaf.setup();
        setTitle("Kubectl Log Extractor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);

        // Initialize fields
        podInfoMap = new HashMap<>();
        currentSearchGroups = new ArrayList<>();
        credentialsManager = new CredentialsManager();
        sshConnection = new SSHConnection();
        initializeEnvironmentCommands();

        // Create components
        createComponents();
        setVisible(true);
    }

    private void initializeEnvironmentCommands() {
        environmentCommands = new LinkedHashMap<>();
        environmentCommands.put("Dev", "connect-aws-dev-kubectl");
        environmentCommands.put("Test", "connect-aws-test-kubectl");
        environmentCommands.put("Production", "connect-aws-production-kubectl");
        environmentCommands.put("UAT", "connect-aws-uat-kubectl");
    }

    private void createComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(Color.WHITE);

        mainPanel.add(createConnectionPanel(), BorderLayout.NORTH);
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER);

        add(mainPanel);
    }

    private JPanel createConnectionPanel() {
        JPanel connectionPanel = new JPanel(new GridBagLayout());
        connectionPanel.setBackground(Color.WHITE);
        connectionPanel.setBorder(BorderFactory.createTitledBorder("Connection Settings"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Username field
        gbc.gridx = 0; gbc.gridy = 0;
        connectionPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        usernameField = new JTextField(20);
        connectionPanel.add(usernameField, gbc);

        // Password field
        gbc.gridx = 0; gbc.gridy = 1;
        connectionPanel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        connectionPanel.add(passwordField, gbc);

        // Host field
        gbc.gridx = 0; gbc.gridy = 2;
        connectionPanel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        hostField = new JTextField(20);
        connectionPanel.add(hostField, gbc);

        // Environment dropdown
        gbc.gridx = 0; gbc.gridy = 3;
        connectionPanel.add(new JLabel("Environment:"), gbc);
        gbc.gridx = 1;
        environmentCombo = new JComboBox<>(environmentCommands.keySet().toArray(new String[0]));
        environmentCombo.setPreferredSize(new Dimension(200, 30));
        connectionPanel.add(environmentCombo, gbc);

        // Remember credentials checkbox
        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 2;
        rememberCredentialsCheckbox = new JCheckBox("Remember Credentials");
        connectionPanel.add(rememberCredentialsCheckbox, gbc);

        // Connect button
        gbc.gridx = 0; gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.EAST;
        JButton connectButton = createStyledButton("Connect");
        connectButton.addActionListener(e -> onConnect());
        connectionPanel.add(connectButton, gbc);

        // Load saved credentials after creating the fields
        loadSavedCredentials();

        return connectionPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create pod panel
        JPanel podPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        podPanel.setBackground(Color.WHITE);
        podPanel.setBorder(BorderFactory.createTitledBorder("Pod Selection"));

        // Pod selection with refresh button
        JPanel podSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        podSelectionPanel.setBackground(Color.WHITE);

        // Create a filtered combo box for pods
        podListCombo = new JComboBox<String>() {
            private final JTextField textField = (JTextField) getEditor().getEditorComponent();
            private boolean isFiltering = false;
            private boolean firstKeyTyped = false;
            {
                setEditable(true);
                setMaximumRowCount(20);

                textField.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyPressed(KeyEvent e) {
                        // Clear text on first keypress that's not a control key
                        if (!firstKeyTyped &&
                                e.getKeyCode() != KeyEvent.VK_ENTER &&
                                e.getKeyCode() != KeyEvent.VK_UP &&
                                e.getKeyCode() != KeyEvent.VK_DOWN &&
                                e.getKeyCode() != KeyEvent.VK_LEFT &&
                                e.getKeyCode() != KeyEvent.VK_RIGHT &&
                                e.getKeyCode() != KeyEvent.VK_BACK_SPACE &&
                                e.getKeyCode() != KeyEvent.VK_DELETE) {
                            textField.setText("");
                            firstKeyTyped = true;
                        }
                    }

                    @Override
                    public void keyReleased(KeyEvent e) {
                        if (e.getKeyCode() != KeyEvent.VK_ENTER &&
                                e.getKeyCode() != KeyEvent.VK_UP &&
                                e.getKeyCode() != KeyEvent.VK_DOWN) {
                            String text = textField.getText();
                            isFiltering = true;
                            filterPodList(text);
                            isFiltering = false;
                        }
                    }
                });

                // Reset firstKeyTyped when focus is gained
                textField.addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        firstKeyTyped = false;
                    }
                });

                // Handle selection changes
                addActionListener(e -> {
                    if (!isFiltering && getSelectedItem() != null) {
                        textField.setText(getSelectedItem().toString());
                        firstKeyTyped = false;
                    }
                });
            }

            // Override to make dropdown wider
            @Override
            public Dimension getPreferredSize() {
                Dimension size = super.getPreferredSize();
                return new Dimension(400, size.height);
            }
        };

        // Set a custom popup menu listener to adjust the dropdown width
        podListCombo.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                JComboBox<?> combo = (JComboBox<?>) e.getSource();
                Object comp = combo.getUI().getAccessibleChild(combo, 0);
                if (!(comp instanceof JPopupMenu)) return;

                JPopupMenu popup = (JPopupMenu) comp;
                JScrollPane scrollPane = (JScrollPane) popup.getComponent(0);
                scrollPane.setPreferredSize(new Dimension(400, 400));
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        podListCombo.setPreferredSize(new Dimension(400, 30));
        podListCombo.setRenderer(new PodListCellRenderer());
        podSelectionPanel.add(podListCombo);

        refreshPodsButton = createStyledButton("⟳");
        refreshPodsButton.setPreferredSize(new Dimension(30, 30));
        refreshPodsButton.setToolTipText("Refresh Pods List");
        refreshPodsButton.addActionListener(e -> refreshPods());
        podSelectionPanel.add(refreshPodsButton);

        podPanel.add(podSelectionPanel);

        getLogsButton = createStyledButton("Get Logs");
        getLogsButton.addActionListener(e -> getLogs());

        watchLogsButton = new JToggleButton("Watch Logs");
        styleButton(watchLogsButton);
        watchLogsButton.addActionListener(this::watchLogs);

        editDeploymentButton = createStyledButton("Edit Deployment");
        editDeploymentButton.addActionListener(e -> editDeployment());

        podPanel.add(getLogsButton);
        podPanel.add(watchLogsButton);
        podPanel.add(editDeploymentButton);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonPanel.setBackground(Color.WHITE);

        searchButton = createStyledButton("Search & Filter");
        searchButton.addActionListener(e -> showSearchDialog());

        exportButton = createStyledButton("Export Logs");
        exportButton.addActionListener(e -> exportLogs());

        resetSearchButton = createStyledButton("Reset Search");
        resetSearchButton.addActionListener(e -> resetSearch());

        // **Add the "Open in VS Code" button**
        openInVSCodeButton = createStyledButton("Open in VS Code");
        openInVSCodeButton.addActionListener(e -> openInVSCode());

        buttonPanel.add(searchButton);
        buttonPanel.add(resetSearchButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(openInVSCodeButton); // Add the new button here

        // Output area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Output"));

        // Add components to center panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(Color.WHITE);
        topPanel.add(podPanel, BorderLayout.NORTH);
        topPanel.add(buttonPanel, BorderLayout.CENTER);

        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        return centerPanel;
    }

    private void showSearchDialog() {
        if (searchDialog == null) {
            searchDialog = new SearchDialog(this);
        }
        searchDialog.setVisible(true);
    }

    private class SearchDialog extends JDialog {
        private JTextField searchField;
        private JComboBox<String> operationCombo;
        private JCheckBox caseSensitiveCheck;
        private JCheckBox regexCheck;
        private JCheckBox includeAfterMatchCheck;
        private JList<LogSearchCriteria> criteriaList;
        private DefaultListModel<LogSearchCriteria> criteriaListModel;
        private List<LogSearchCriteria> currentSearchGroups;

        public SearchDialog(JFrame parent) {
            super(parent, "Search & Filter", false);
            setSize(500, 600);
            setLocationRelativeTo(parent);
            setResizable(true);

            currentSearchGroups = new ArrayList<>();
            initializeComponents();
        }

        private void initializeComponents() {
            JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            mainPanel.setBackground(Color.WHITE);

            // Input Panel
            JPanel inputPanel = new JPanel(new GridBagLayout());
            inputPanel.setBackground(Color.WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(2, 2, 2, 2);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            // Search field
            gbc.gridx = 0; gbc.gridy = 0;
            inputPanel.add(new JLabel("Search:"), gbc);
            gbc.gridx = 1; gbc.gridwidth = 2;
            searchField = new JTextField(20);
            inputPanel.add(searchField, gbc);

            // Operation combo
            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 1;
            inputPanel.add(new JLabel("Operation:"), gbc);
            gbc.gridx = 1;
            operationCombo = new JComboBox<>(new String[]{"AND", "OR"});
            inputPanel.add(operationCombo, gbc);

            // Checkboxes
            gbc.gridy = 2;
            caseSensitiveCheck = new JCheckBox("Case Sensitive");
            inputPanel.add(caseSensitiveCheck, gbc);

            gbc.gridy = 3;
            regexCheck = new JCheckBox("Use Regex");
            inputPanel.add(regexCheck, gbc);

            gbc.gridy = 4;
            includeAfterMatchCheck = new JCheckBox("Include Lines After Match");
            inputPanel.add(includeAfterMatchCheck, gbc);

            // Criteria List
            criteriaListModel = new DefaultListModel<>();
            criteriaList = new JList<>(criteriaListModel);
            criteriaList.setCellRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList<?> list, Object value,
                                                              int index, boolean isSelected, boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof LogSearchCriteria) {
                        setText(value.toString());
                    }
                    return this;
                }
            });

            JScrollPane criteriaScroll = new JScrollPane(criteriaList);
            criteriaScroll.setBorder(BorderFactory.createTitledBorder("Search Criteria"));

            // Buttons Panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
            buttonPanel.setBackground(Color.WHITE);

            JButton addButton = createStyledButton("Add Criteria");
            JButton removeButton = createStyledButton("Remove Selected");
            JButton searchButton = createStyledButton("Search");
            JButton resetButton = createStyledButton("Reset");
            JButton closeButton = createStyledButton("Close");

            addButton.addActionListener(e -> addSearchCriteria());
            removeButton.addActionListener(e -> removeSelectedCriteria());
            searchButton.addActionListener(e -> {
                performSearch();
                if (criteriaListModel.size() > 0) {
                    setVisible(false);
                }
            });
            resetButton.addActionListener(e -> {
                resetSearch();
                criteriaListModel.clear();
                currentSearchGroups.clear();
            });
            closeButton.addActionListener(e -> setVisible(false));

            buttonPanel.add(addButton);
            buttonPanel.add(removeButton);
            buttonPanel.add(searchButton);
            buttonPanel.add(resetButton);
            buttonPanel.add(closeButton);

            // Add all panels to main panel
            mainPanel.add(inputPanel, BorderLayout.NORTH);
            mainPanel.add(criteriaScroll, BorderLayout.CENTER);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            setContentPane(mainPanel);
        }

        public void clearSearchCriteria() {
            criteriaListModel.clear();
            currentSearchGroups.clear();
            searchField.setText("");
            caseSensitiveCheck.setSelected(false);
            regexCheck.setSelected(false);
            includeAfterMatchCheck.setSelected(false);
            operationCombo.setSelectedIndex(0);
        }

        private void addSearchCriteria() {
            String searchText = searchField.getText().trim();
            if (searchText.isEmpty()) {
                showError("Please enter search text");
                return;
            }

            try {
                if (regexCheck.isSelected()) {
                    // Validate regex pattern
                    Pattern.compile(searchText);
                }

                LogSearchCriteria criteria = new LogSearchCriteria(
                        searchText,
                        (String) operationCombo.getSelectedItem(),
                        caseSensitiveCheck.isSelected(),
                        regexCheck.isSelected()
                );

                criteriaListModel.addElement(criteria);
                currentSearchGroups.add(criteria);
                searchField.setText("");
            } catch (Exception e) {
                showError("Invalid regex pattern: " + e.getMessage());
            }
        }

        private void removeSelectedCriteria() {
            int[] indices = criteriaList.getSelectedIndices();
            for (int i = indices.length - 1; i >= 0; i--) {
                criteriaListModel.remove(indices[i]);
                currentSearchGroups.remove(indices[i]);
            }
        }

        private void performSearch() {
            if (currentSearchGroups.isEmpty()) {
                showError("Please add at least one search criteria");
                return;
            }

            String content = outputArea.getText();
            String[] lines = content.split("\n");
            StringBuilder result = new StringBuilder();
            boolean includeNext = false;

            for (String line : lines) {
                boolean matches = evaluateSearchCriteria(line);
                if (matches || includeNext) {
                    result.append(line).append("\n");
                    includeNext = includeAfterMatchCheck.isSelected() && matches;
                }
            }

            if (result.length() > 0) {
                outputArea.setText(result.toString());
                outputArea.setCaretPosition(0);
            } else {
                showInfo("No matches found");
                resetSearch();
            }
        }

        private boolean evaluateSearchCriteria(String line) {
            if (currentSearchGroups.isEmpty()) return true;

            boolean isAnd = currentSearchGroups.get(0).operation.equals("AND");
            boolean result = isAnd;

            for (LogSearchCriteria criteria : currentSearchGroups) {
                boolean matches = criteria.matches(line);
                if (isAnd) {
                    result &= matches;
                } else {
                    result |= matches;
                }
            }

            return result;
        }
    }

    private void watchLogs(ActionEvent e) {
        String selectedPod = getPodName(podListCombo.getSelectedItem().toString());
        if (selectedPod == null || selectedPod.isEmpty()) {
            showError("Please select a pod first");
            watchLogsButton.setSelected(false);
            return;
        }

        if (!sshConnection.isConnected()) {
            showError("Not connected to server. Please connect first.");
            watchLogsButton.setSelected(false);
            return;
        }

        if (watchLogsButton.isSelected()) {
            if (logWatchTimer != null) {
                logWatchTimer.stop();
            }

            // Use javax.swing.Timer explicitly
            logWatchTimer = new javax.swing.Timer(2000, evt -> {
                try {
                    String command = String.format("kubectl logs --tail=50 %s", selectedPod);
                    String logs = sshConnection.executeCommand(command);
                    outputArea.setText(logs);
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());
                    originalLogContent = logs;
                } catch (Exception ex) {
                    showError("Failed to watch logs: " + ex.getMessage());
                    watchLogsButton.setSelected(false);
                    logWatchTimer.stop();
                }
            });
            logWatchTimer.start();
            showInfo("Log watching started");
        } else {
            if (logWatchTimer != null) {
                logWatchTimer.stop();
            }
            showInfo("Log watching stopped");
        }
    }

    private void getLogs() {
        String selectedPod = getPodName(podListCombo.getSelectedItem().toString());
        if (selectedPod == null || selectedPod.isEmpty()) {
            showError("Please select a pod first");
            return;
        }

        try {
            if (!sshConnection.isConnected()) {
                showError("Not connected to server. Please connect first.");
                return;
            }

            String command = String.format("kubectl logs %s", selectedPod);
            String logs = sshConnection.executeCommand(command);

            outputArea.setText(logs);
            outputArea.setCaretPosition(0);
            originalLogContent = logs;

        } catch (Exception e) {
            showError("Failed to get logs: " + e.getMessage());
        }
    }

    private void editDeployment() {
        String podName = (String) podListCombo.getSelectedItem();
        if (podName == null || podName.isEmpty()) {
            showError("Please select a pod first");
            return;
        }

        if (!sshConnection.isConnected()) {
            showError("Not connected to server. Please connect first.");
            return;
        }

        try {
            String command = String.format("kubectl get deployment -o yaml");
            String yaml = sshConnection.executeCommand(command);
            DeploymentEditorDialog editor = new DeploymentEditorDialog(this, sshConnection, podName);
            editor.setVisible(true);
        } catch (Exception e) {
            showError("Failed to get deployment: " + e.getMessage());
        }
    }

    private void refreshPods() {
        try {
            if (!sshConnection.isConnected()) {
                showError("Not connected to server. Please connect first.");
                return;
            }

            String result = sshConnection.executeCommand("kubectl get pods");
            podInfoMap.clear();

            String[] lines = result.split("\n");
            for (int i = 1; i < lines.length; i++) { // Skip header line
                String[] parts = lines[i].trim().split("\\s+");
                if (parts.length >= 5) {
                    String name = parts[0];
                    String status = parts[2];
                    LocalDateTime startTime = parseAgeToStartTime(parts[4]);
                    PodInfo podInfo = new PodInfo(name, status, startTime);
                    podInfoMap.put(name, podInfo);
                }
            }
            refreshPodListCombo();
            showInfo("Pods refreshed successfully");
        } catch (Exception e) {
            showError("Failed to refresh pods: " + e.getMessage());
        }
    }

    private LocalDateTime parseAgeToStartTime(String age) {
        LocalDateTime now = LocalDateTime.now();
        if (age.endsWith("d")) {
            int days = Integer.parseInt(age.substring(0, age.length() - 1));
            return now.minusDays(days);
        } else if (age.endsWith("h")) {
            int hours = Integer.parseInt(age.substring(0, age.length() - 1));
            return now.minusHours(hours);
        } else if (age.endsWith("m")) {
            int minutes = Integer.parseInt(age.substring(0, age.length() - 1));
            return now.minusMinutes(minutes);
        }
        return now;
    }

    private void onConnect() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String host = hostField.getText();
        String selectedEnv = (String) environmentCombo.getSelectedItem();

        // Validate input on EDT
        if (username.isEmpty() || password.isEmpty() || host.isEmpty()) {
            showError("Please fill in all connection fields");
            return;
        }

        // Do connection in background thread
        new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                });

                if (sshConnection.isConnected()) {
                    sshConnection.disconnect();
                }

                // First connect to jump server
                boolean connected = sshConnection.connect(host, username, password);
                if (!connected) {
                    SwingUtilities.invokeLater(() ->
                            showError("Failed to connect to server. Please check your credentials and try again."));
                    return;
                }

                // Then execute the environment command
                String envCommand = environmentCommands.get(selectedEnv);
                if (envCommand == null) {
                    SwingUtilities.invokeLater(() ->
                            showError("No command found for environment: " + selectedEnv));
                    return;
                }

                String result = sshConnection.executeCommand(envCommand);
                if (result.toLowerCase().contains("error") || result.toLowerCase().contains("failed")) {
                    SwingUtilities.invokeLater(() ->
                            showError("Failed to connect to environment: " + result));
                    return;
                }

                if (rememberCredentialsCheckbox.isSelected()) {
                    Credentials creds = new Credentials(host, username, password, selectedEnv, true);
                    credentialsManager.saveCredentials(creds);
                } else {
                    credentialsManager.clearCredentials();
                }

                SwingUtilities.invokeLater(() -> {
                    showInfo("Connected to " + selectedEnv + " environment");
                    refreshPods();
                });

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    showError("Connection failed: " + e.getMessage());
                    e.printStackTrace();
                });
            } finally {
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                });
            }
        }).start();
    }

    private void loadSavedCredentials() {
        if (credentialsManager != null) {
            Credentials savedCredentials = credentialsManager.loadCredentials();
            if (savedCredentials != null) {
                usernameField.setText(savedCredentials.getUsername());
                passwordField.setText(savedCredentials.getPassword());
                hostField.setText(savedCredentials.getHost());
                environmentCombo.setSelectedItem(savedCredentials.getEnvironment());
                rememberCredentialsCheckbox.setSelected(savedCredentials.isRememberMe());
            }
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        styleButton(button);
        return button;
    }

    private void styleButton(AbstractButton button) {
        button.setBackground(accentColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private String getPodName(String podInfo) {
        if (podInfo == null) return null;
        return podInfo.split(" - ")[0];
    }

    private void resetSearch() {
        if (originalLogContent != null) {
            outputArea.setText(originalLogContent);
            outputArea.setCaretPosition(0);
        }
        if (searchDialog != null) {
            searchDialog.clearSearchCriteria();
        }
        showInfo("Search reset successfully");
    }

    private void exportLogs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Logs");
        fileChooser.setSelectedFile(new File("kubectl_logs.txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (PrintWriter writer = new PrintWriter(fileChooser.getSelectedFile())) {
                writer.print(outputArea.getText());
                showInfo("Logs exported successfully to: " + fileChooser.getSelectedFile().getAbsolutePath());
            } catch (IOException e) {
                showError("Failed to export logs: " + e.getMessage());
            }
        }
    }

    private void filterPodList(String searchText) {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) podListCombo.getModel();
        model.removeAllElements();

        if (searchText == null || searchText.isEmpty()) {
            // Show all pods
            for (PodInfo podInfo : podInfoMap.values()) {
                model.addElement(podInfo.toString());
            }
        } else {
            // Show only matching pods
            searchText = searchText.toLowerCase();
            for (PodInfo podInfo : podInfoMap.values()) {
                String podDisplay = podInfo.toString().toLowerCase();
                if (podDisplay.contains(searchText)) {
                    model.addElement(podInfo.toString());
                }
            }
        }

        // Keep the typed text and show dropdown
        JTextField textField = (JTextField) podListCombo.getEditor().getEditorComponent();
        textField.setText(searchText);
        if (model.getSize() > 0) {
            podListCombo.showPopup();
        }
    }

    private void refreshPodListCombo() {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) podListCombo.getModel();
        model.removeAllElements();
        for (PodInfo podInfo : podInfoMap.values()) {
            model.addElement(podInfo.toString());
        }
    }

    private static class LogSearchCriteria {
        private final String text;
        private final String operation;
        private final boolean caseSensitive;
        private final boolean useRegex;
        private Pattern pattern;

        public LogSearchCriteria(String text, String operation, boolean caseSensitive, boolean useRegex) {
            this.text = text;
            this.operation = operation;
            this.caseSensitive = caseSensitive;
            this.useRegex = useRegex;

            if (useRegex) {
                int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
                pattern = Pattern.compile(text, flags);
            }
        }

        public boolean matches(String line) {
            if (useRegex) {
                return pattern.matcher(line).find();
            } else if (caseSensitive) {
                return line.contains(text);
            } else {
                return line.toLowerCase().contains(text.toLowerCase());
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("\"").append(text).append("\"");
            if (useRegex) sb.append(" (regex)");
            if (caseSensitive) sb.append(" (case sensitive)");
            sb.append(" [").append(operation).append("]");
            return sb.toString();
        }
    }

    // **New Method to Open VS Code**
    private void openInVSCode() {
        try {
            // Default VS Code installation paths for Windows
            String[] possiblePaths = {
                    System.getenv("LOCALAPPDATA") + "\\Programs\\Microsoft VS Code\\Code.exe",
                    System.getenv("ProgramFiles") + "\\Microsoft VS Code\\Code.exe",
                    System.getenv("ProgramFiles(x86)") + "\\Microsoft VS Code\\Code.exe"
            };

            String vsCodePath = null;
            for (String path : possiblePaths) {
                if (new File(path).exists()) {
                    vsCodePath = path;
                    break;
                }
            }

            if (vsCodePath == null) {
                showError("VS Code not found in common installation locations. Please ensure VS Code is installed.");
                return;
            }

            // Create a temporary file with the log content
            String tempDir = System.getProperty("java.io.tmpdir");
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = "pod_logs_" + timestamp + ".log";
            File tempFile = new File(tempDir, fileName);

            // Write the log content to the file
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(outputArea.getText());
            }

            // Open VS Code with the file
            ProcessBuilder pb = new ProcessBuilder(vsCodePath, tempFile.getAbsolutePath());
            pb.start();

            showInfo("Opening logs in VS Code...");
        } catch (Exception e) {
            showError("Failed to open VS Code: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new KubectlConnector());
    }
}

class PodListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof PodInfo) {
            PodInfo podInfo = (PodInfo) value;
            setText(String.format("%s (%s) - %s",
                    podInfo.getName(),
                    podInfo.getStatus(),
                    formatDuration(Duration.between(podInfo.getStartTime(), LocalDateTime.now()))));
        } else if (value instanceof String) {
            setText((String) value);
        }
        return this;
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return String.format("%dd %dh", days, hours);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }
}
