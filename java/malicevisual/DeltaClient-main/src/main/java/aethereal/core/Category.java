package aethereal.core;


public enum Category {
    Movement("I", "Movement"),
    Render("t", "Render"),
    Player("L", "Player"),
    Misc("D", "Misc"),
    Friends("F", "Друзья"),
    Marks("G", "Метки"),
    Configs("C", "Конфиги");

    private final String icon;
    private final String label;

    Category(String icon, String label) {
        this.icon = icon;
        this.label = label;
    }

    public String a() {
        return this.icon;
    }

    public String b() {
        return this.label;
    }

    public boolean isFriends() {
        return this == Friends;
    }

    public boolean isMarks() {
        return this == Marks;
    }

    public boolean isConfigs() {
        return this == Configs;
    }

    public boolean isCustomPanel() {
        return this == Friends || this == Marks || this == Configs;
    }
}
