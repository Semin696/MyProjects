package aethereal.module.player;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Skeleton;
import aethereal.event.PacketEvent;
import aethereal.notification.Notification;
import aethereal.render.ColorUtil;
import aethereal.setting.BooleanSetting;
import aethereal.util.ChatUtil;
import aethereal.util.ServerUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

@ModuleRegister(name = "Totem Tracker", description = "Пишет в чат, кто потерял тотем и был ли он зачарован", category = Category.Player)
public class TotemTracker extends Module {
    private final BooleanSetting chat = new BooleanSetting("Сообщение в чат", true);
    private final BooleanSetting notify = new BooleanSetting("Уведомление", true);
    private final BooleanSetting self = new BooleanSetting("Свои тотемы", true);
    private long lastTotemTime;
    private String lastPlayerName = "";

    public TotemTracker() {
        a(this.chat, this.notify, this.self);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!event.isReceive() || mc.world == null) {
            return;
        }
        if (!(event.getPacket() instanceof EntityStatusS2CPacket status) || status.getStatus() != 35) {
            return;
        }
        Entity entity = status.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity player) || player.isSpectator()) {
            return;
        }
        if (player == mc.player && !this.self.c().booleanValue()) {
            return;
        }
        long now = System.currentTimeMillis();
        String name = player.getName().getString();
        if (now - this.lastTotemTime < 100L && name.equals(this.lastPlayerName)) {
            return;
        }
        this.lastTotemTime = now;
        this.lastPlayerName = name;

        ItemStack totem = player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING)
                ? player.getMainHandStack()
                : (player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING) ? player.getOffHandStack() : ItemStack.EMPTY);
        boolean enchanted = !totem.isEmpty() && (totem.hasGlint() || totem.getName().getString().startsWith("Талисман"));
        if (!totem.isEmpty() && ServerUtil.a.a$()) {
            String funName = ServerUtil.a.b(totem);
            enchanted = enchanted || (funName != null && funName.startsWith("Талисман"));
        }
        String who = player == mc.player ? "Вы" : name;
        String type = enchanted ? "&aзачарованный&7" : "&cне зачарованный&7";
        String text = who + " потерял " + type + " тотем";
        if (this.chat.c().booleanValue()) {
            ChatUtil.sendMessage("[" + j() + "]", text);
        }
        if (this.notify.c().booleanValue()) {
            ItemStack icon = totem.isEmpty() ? new ItemStack(Items.TOTEM_OF_UNDYING) : totem.copy();
            Skeleton.getInstance().getModuleProcessor().m().a(new Notification(icon,
                    ColorUtil.convertToARGB(enchanted ? 85 : 255, enchanted ? 255 : 85, 85, 255),
                    (player == mc.player ? "Вы потеряли тотем" : name + " потерял тотем") + (enchanted ? " (зачар)" : ""),
                    1800));
        }
    }
}
