package com.example.ui;

public final class TodoUiText {
    private TodoUiText() {
    }

    public static String breakAnywhere(String s) {
        if (s == null)
            return "";
        return s.replace("", "\u200B");
    }
}
