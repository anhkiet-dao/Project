package com.example.do_an.core.constant;

public enum Language {
    VI("vi", 0, "Tiếng Việt"),
    EN("en", 1, "English");

    private final String code;
    private final int position;
    private final String displayName;

    Language(String code, int position, String displayName) {
        this.code = code;
        this.position = position;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public int getPosition() {
        return position;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Language fromCode(String code) {
        for (Language language : Language.values()) {
            if (language.code.equals(code)) {
                return language;
            }
        }
        return VI;
    }

    public static Language fromPosition(int position) {
        for (Language language : Language.values()) {
            if (language.position == position) {
                return language;
            }
        }
        return VI;
    }
}
