package aethereal.module.player;

import aethereal.core.*;
import aethereal.core.Module;
import aethereal.event.KeyEvent;
import aethereal.event.TickEvent;
import aethereal.lib.javassist.TokenId;
import aethereal.setting.BindSetting;
import aethereal.setting.ModeSetting;
import aethereal.util.Look;
import aethereal.util.MathUtil;
import aethereal.util.Rotation;
import net.minecraft.client.option.Perspective;

@ModuleRegister(name = "Third Person", description = "Свободный обзор от третьего лица без изменения направления движения", category = Category.Player)
public class ThirdPerson extends Module {
    private final ModeSetting b = new ModeSetting("Режим активации осмотра", "По нажатию", "По нажатию", "По зажатию");
    private boolean isActive;
    private Rotation rotation;

    public ThirdPerson() {
        BindSetting d = new BindSetting("Кнопка осмотра", Integer.valueOf(TokenId.Q_), 0).a(() -> {
            if (this.b.l("По зажатию")) {
                d(true);
            } else {
                d(!this.isActive);
            }
        }).b(() -> {
            if (this.isActive && this.b.l("По зажатию")) {
                d(false);
            }
        });
        a(this.b, d);
    }

    @EventTarget
    public void a(KeyEvent event) {
        if (this.isActive && event.getKey() == mc.options.togglePerspectiveKey.getDefaultKey().getCode()) {
            event.a(true);
        }
    }

    @EventTarget
    public void a(TickEvent event) {
        if (this.isActive && (mc.currentScreen != null || mc.player == null)) {
            d(false);
        }
    }

    @Override
    public void c() {
        if (this.isActive) {
            d(false);
        }
        super.c();
    }

    private void d(boolean active) {
        if (active) {
            if (mc.player == null) {
                return;
            }
            this.rotation = new Rotation(mc.player);
            Look.getInstance().a(true);
        } else {
            if (this.rotation == null) {
                return;
            }
            Look.a(this.rotation.c());
            Look.b(this.rotation.d());
            Look.getInstance().a(false);
        }
        mc.options.setPerspective(active ? Perspective.THIRD_PERSON_BACK : Perspective.FIRST_PERSON);
        this.isActive = active;
    }
}
