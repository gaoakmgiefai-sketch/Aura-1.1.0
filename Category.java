package com.example.aura.module;

public enum Category {
    COMBAT("Combat"),
    MOVEMENT("Movement"),
    VISUAL("Visuals"),
    PLAYER("Player"),
    MISC("Miscellaneous");

    public final String label;

    Category(String label) {
        this.label = label;
    }
}
