package com.buildsmart.finance.entity.enums;

public enum AssignedTaskStatus {
    PENDING("Pending"),
    COMPLETED("Completed");

    private final String displayName;

    AssignedTaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
