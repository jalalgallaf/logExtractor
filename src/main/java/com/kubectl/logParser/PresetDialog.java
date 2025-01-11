package com.kubectl.logParser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;

public class PresetDialog extends JDialog {
    private JList<SearchPreset> presetList;
    private DefaultListModel<SearchPreset> listModel;
    private boolean loadRequested = false;
    private SearchPreset selectedPreset = null;

    public PresetDialog(Frame owner, List<SearchPreset> presets, boolean saveMode) {
        super(owner, saveMode ? "Save Preset" : "Load Preset", true);
        setSize(400, 300);
        setLocationRelativeTo(owner);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create list model and populate it
        listModel = new DefaultListModel<>();
        for (SearchPreset preset : presets) {
            listModel.addElement(preset);
        }

        // Create list with single selection
        presetList = new JList<>(listModel);
        presetList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(presetList);

        // Create buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        if (saveMode) {
            // Add name input for save mode
            JPanel namePanel = new JPanel(new BorderLayout(5, 5));
            namePanel.add(new JLabel("Preset Name:"), BorderLayout.WEST);
            JTextField nameField = new JTextField(20);
            namePanel.add(nameField, BorderLayout.CENTER);
            mainPanel.add(namePanel, BorderLayout.NORTH);

            JButton saveButton = new JButton("Save");
            saveButton.addActionListener(e -> {
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    JOptionPane.showMessageDialog(this, 
                        "Please enter a preset name", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                selectedPreset = new SearchPreset(name, null, false); // Actual groups will be set by caller
                dispose();
            });
            buttonPanel.add(saveButton);
        } else {
            // Load mode buttons
            JButton loadButton = new JButton("Load");
            JButton deleteButton = new JButton("Delete");
            
            loadButton.addActionListener(e -> {
                selectedPreset = presetList.getSelectedValue();
                if (selectedPreset == null) {
                    JOptionPane.showMessageDialog(this,
                        "Please select a preset to load",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                loadRequested = true;
                dispose();
            });

            deleteButton.addActionListener(e -> {
                SearchPreset toDelete = presetList.getSelectedValue();
                if (toDelete == null) {
                    JOptionPane.showMessageDialog(this,
                        "Please select a preset to delete",
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                    return;
                }
                listModel.removeElement(toDelete);
            });

            buttonPanel.add(loadButton);
            buttonPanel.add(deleteButton);
        }

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    public boolean isLoadRequested() {
        return loadRequested;
    }

    public SearchPreset getSelectedPreset() {
        return selectedPreset;
    }

    public List<SearchPreset> getUpdatedPresets() {
        List<SearchPreset> presets = new ArrayList<>();
        for (int i = 0; i < listModel.size(); i++) {
            presets.add(listModel.getElementAt(i));
        }
        return presets;
    }
}
