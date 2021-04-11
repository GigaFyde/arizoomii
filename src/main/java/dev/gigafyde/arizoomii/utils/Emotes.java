package dev.gigafyde.arizoomii.utils;

public enum Emotes {
    HEARTBEAT("\uD83D\uDC93"),
    PINGPONG("\uD83C\uDFD3"),
    SUCCESS("\u2705"),
    ERROR("\u274C"),
    EDIT("\uD83D\uDDD2"),
    WARN("\u26A0\uFE0F"),
    CHANGE("\uD83C\uDFF7"),
    INFO("\u2139"),
    REGION("\uD83C\uDF10"),
    LEAVE("\uD83D\uDEAA"),
    KICK("\uD83D\uDC62"),
    URL("\uD83D\uDD17"),
    BAN("\uD83D\uDD28");

    private final String emote;

    Emotes(String emote) {
        this.emote = emote;
    }

    @Override
    public String toString() {
        return emote;
    }
}
