package com.kubectl;

import java.time.LocalDateTime;

public class PodInfo {
    private String name;
    private String status;
    private LocalDateTime startTime;

    public PodInfo() {
    }

    public PodInfo(String name, String status, LocalDateTime startTime) {
        this.name = name;
        this.status = status;
        this.startTime = startTime;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
}
