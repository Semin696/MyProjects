package aethereal.config;

import aethereal.core.InterfaceC0020Opcode;

public enum ThemeInfo {
    PRIMARY(new ThemeConstructor("primary", 224, 92, 208, 255), new ThemeConstructor("primary", 196, 72, 188, 255)),
    BACKGROUND_HUD(new ThemeConstructor("background_hud", 16, 8, 22, InterfaceC0020Opcode.cY), new ThemeConstructor("background_hud", 246, 236, 246, InterfaceC0020Opcode.cY)),
    BACKGROUND_GUI(new ThemeConstructor("background_gui", 12, 6, 16, 255), new ThemeConstructor("background_gui", 253, 248, 252, 255)),
    OUTLINE_SMALL(new ThemeConstructor("outline_small", 255, 255, 255, 5), new ThemeConstructor("outline_small", 17, 18, 22, 5)),
    OUTLINE_MEDIUM(new ThemeConstructor("outline_medium", 255, 255, 255, 10), new ThemeConstructor("outline_medium", 17, 18, 22, 5)),
    TEXT(new ThemeConstructor("typography_text", 255, 255, 255, 255), new ThemeConstructor("typography_text", 17, 18, 22, 255)),
    TEXT_DISABLED(new ThemeConstructor("typography_disabled", 67, 70, 81, 255), new ThemeConstructor("typography_disabled", InterfaceC0020Opcode.bv, InterfaceC0020Opcode.aD, InterfaceC0020Opcode.C, 255));

    private final ThemeConstructor dark;
    private final ThemeConstructor light;

    ThemeInfo(ThemeConstructor dark, ThemeConstructor light) {
        this.dark = dark;
        this.light = light;
    }

    public ThemeConstructor a(ThemeType theme) {
        return theme == ThemeType.LIGHT ? this.light : this.dark;
    }

    public ThemeConstructor a() {
        return this.light;
    }
}
