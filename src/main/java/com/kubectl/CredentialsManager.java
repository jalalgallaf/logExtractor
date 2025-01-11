package com.kubectl;

import com.google.gson.Gson;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.PropertyValueEncryptionUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CredentialsManager {
    private static final String CREDENTIALS_FILE = System.getProperty("user.home") + File.separator + ".kubectl-connector" + File.separator + "credentials.json";
    private static final String ENCRYPTION_PASSWORD = "kubectl-connector-secret-key";
    private final StandardPBEStringEncryptor encryptor;
    private final Gson gson;

    public CredentialsManager() {
        this.encryptor = new StandardPBEStringEncryptor();
        this.encryptor.setPassword(ENCRYPTION_PASSWORD);
        this.gson = new Gson();
        createCredentialsDirectory();
    }

    private void createCredentialsDirectory() {
        Path credentialsDir = Paths.get(CREDENTIALS_FILE).getParent();
        try {
            Files.createDirectories(credentialsDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveCredentials(com.kubectl.Credentials credentials) {
        try (Writer writer = new FileWriter(CREDENTIALS_FILE)) {
            String json = gson.toJson(credentials);
            String encrypted = PropertyValueEncryptionUtils.encrypt(json, encryptor);
            writer.write(encrypted);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public com.kubectl.Credentials loadCredentials() {
        File file = new File(CREDENTIALS_FILE);
        if (!file.exists()) {
            return null;
        }

        try (Reader reader = new FileReader(file)) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }

            String encrypted = content.toString();
            if (PropertyValueEncryptionUtils.isEncryptedValue(encrypted)) {
                String decrypted = PropertyValueEncryptionUtils.decrypt(encrypted, encryptor);
                return gson.fromJson(decrypted, com.kubectl.Credentials.class);
            }
            return gson.fromJson(encrypted, com.kubectl.Credentials.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void clearCredentials() {
        try {
            Files.deleteIfExists(Paths.get(CREDENTIALS_FILE));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}