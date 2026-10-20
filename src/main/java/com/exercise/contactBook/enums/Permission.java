package com.exercise.contactBook.enums;

public enum Permission {
    CONTACT_COMMON_DIRECT("Add, edit and delete common contacts without approval"),
    REQUEST_APPROVE("Review and approve contact requests"),
    USER_APPROVE("Approve or reject people who join the organization"),
    ROLE_MANAGE("create and edit roles"),
    WORKFLOW_MANAGE("Create and edit approval workflows");

    private final String description;

    Permission(String description){
        this.description = description;
    }
    public String getDescription(){
        return description;
    }
}
