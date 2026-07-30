package org.nig.smp.settings;

import org.bukkit.Material;

public enum SettingType {
    NIGHT_VISION("night_vision", "Ночное зрение", Material.GOLDEN_CARROT, "Даёт эффект ночного зрения"),

    FLY_SPEED("fly_speed", "Скорость полёта", Material.FEATHER, "Увеличивает скорость полёта"),

    WALK_SPEED("walk_speed", "Скорость ходьбы", Material.SUGAR, "Увеличивает скорость ходьбы"),

    AUTO_FISH("auto_fish", "Авто-рыбалка", Material.FISHING_ROD, "Автоматически закидывает удочку");

    private final String key;
    private final String displayName;
    private final Material icon;
    private final String description;

    SettingType(String key, String displayName, Material icon, String description) {
        this.key = key;
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }
}
