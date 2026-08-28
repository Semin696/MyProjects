package aethereal.module.misc;

import aethereal.core.Category;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Processor;
import aethereal.core.Skeleton;
import aethereal.discord.DiscordProcessor;

@ModuleRegister(name = "Discord RPC", description = "Показывает статус Malice Visuals в Discord", category = Category.Misc)
public class DiscordRPC extends Module {
    @Override
    public void b() {
        super.b();
        DiscordProcessor discord = discord();
        if (discord != null) {
            discord.b(true);
        }
    }

    @Override
    public void c() {
        super.c();
        DiscordProcessor discord = discord();
        if (discord != null) {
            discord.b(false);
        }
    }

    private static DiscordProcessor discord() {
        Processor processor = Skeleton.getInstance().getModuleProcessor();
        return processor == null ? null : processor.g();
    }
}
