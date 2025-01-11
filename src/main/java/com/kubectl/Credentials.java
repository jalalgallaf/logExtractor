package com.kubectl;

public class Credentials {
    private String host;
    private String username;
    private String password;
    private String environment;
    private boolean rememberMe;

    public Credentials() {
    }

    public Credentials(String host, String username, String password, String environment, boolean rememberMe) {
        this.host = host;
        this.username = username;
        this.password = password;
        this.environment = environment;
        this.rememberMe = rememberMe;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public boolean isRememberMe() {
        return rememberMe;
    }

    public void setRememberMe(boolean rememberMe) {
        this.rememberMe = rememberMe;
    }
}
