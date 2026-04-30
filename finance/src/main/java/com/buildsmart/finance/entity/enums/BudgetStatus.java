package com.buildsmart.finance.entity.enums;

public enum BudgetStatus {
    DRAFT("Draft"),
    APPROVED("Approved"),
    REJECTED("Rejected");

    private final String displayName;

    BudgetStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
