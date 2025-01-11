package com.kubectl.logParser;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SearchPreset implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private List<LogParser.SearchGroup> searchGroups;
    private boolean includeAfterMatch;

    public SearchPreset(String name, List<LogParser.SearchGroup> searchGroups, boolean includeAfterMatch) {
        this.name = name;
        this.searchGroups = searchGroups != null ? new ArrayList<>(searchGroups) : new ArrayList<>();
        this.includeAfterMatch = includeAfterMatch;
    }

    public String getName() {
        return name;
    }

    public List<LogParser.SearchGroup> getSearchGroups() {
        return new ArrayList<>(searchGroups);
    }

    public boolean isIncludeAfterMatch() {
        return includeAfterMatch;
    }

    @Override
    public String toString() {
        return name;
    }

    public static void savePresets(List<SearchPreset> presets, String filePath) {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        System.out.println("Attempting to save presets to: " + file.getAbsolutePath());
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            List<SearchPreset> toSave = presets != null ? new ArrayList<>(presets) : new ArrayList<>();
            System.out.println("Saving " + toSave.size() + " presets");
            oos.writeObject(toSave);
            System.out.println("Successfully saved presets");
        } catch (IOException e) {
            System.err.println("Error saving presets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static List<SearchPreset> loadPresets(String filePath) {
        File file = new File(filePath);
        System.out.println("Attempting to load presets from: " + file.getAbsolutePath());
        
        if (!file.exists()) {
            System.out.println("Presets file does not exist, returning empty list");
            return new ArrayList<>();
        }
        
        if (!file.canRead()) {
            System.out.println("Cannot read presets file, returning empty list");
            return new ArrayList<>();
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            List<SearchPreset> presets = (List<SearchPreset>) ois.readObject();
            System.out.println("Successfully loaded " + (presets != null ? presets.size() : 0) + " presets");
            return presets != null ? new ArrayList<>(presets) : new ArrayList<>();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading presets: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
