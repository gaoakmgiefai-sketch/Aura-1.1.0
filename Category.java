package com.example.aura.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    VISUAL("Visual");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
