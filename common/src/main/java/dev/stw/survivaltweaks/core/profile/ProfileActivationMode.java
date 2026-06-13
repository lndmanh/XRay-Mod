package dev.stw.survivaltweaks.core.profile;

public enum ProfileActivationMode {
    NONE,
    DEFAULT,
    SINGLEPLAYER,
    MULTIPLAYER;

    public String jsonName() { return name().toLowerCase(); }

    public static ProfileActivationMode fromJsonName(String value) {
        if (value == null || value.isBlank()) return NONE;
        for (var mode : values()) if (mode.jsonName().equals(value)) return mode;
        return NONE;
    }
}
