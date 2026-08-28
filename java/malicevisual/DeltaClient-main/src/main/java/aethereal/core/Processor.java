package aethereal.core;

import aethereal.command.CommandProcessor;
import aethereal.config.BaseProcessor;
import aethereal.config.ModuleProcessor;
import aethereal.config.ResourcePacksProcessor;
import aethereal.config.ThemeProcessor;
import aethereal.cosmetic.CosmeticsProcessor;
import aethereal.discord.DiscordProcessor;
import aethereal.friend.FriendProcessor;
import aethereal.mark.MarksProcessor;
import aethereal.handler.HandlerProcessor;
import aethereal.lib.log4j.LoggerFactory;
import aethereal.macro.MacrosProcessor;
import aethereal.network.AccountProcessor;
import aethereal.notification.NotificationProcessor;
import aethereal.render.Draw2DProcessor;
import aethereal.render.Draw3DProcessor;
import aethereal.staff.StaffProcessor;
import aethereal.ui.element.DragProcessor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class Processor implements Interface {

    static {
        LoggerFactory.a(Processor.class);
    }

    private final List<BaseProcessor> processors;
    private final MacrosProcessor macrosProcessor;
    private final FriendProcessor friendProcessor;
    private final MarksProcessor marksProcessor;
    private final StaffProcessor staffProcessor;
    private final DiscordProcessor discordProcessor;
    private final AccountProcessor accountProcessor;
    private final Draw2DProcessor draw2DProcessor;
    private final Draw3DProcessor draw3DProcessor;
    private final NotificationProcessor notificationProcessor;
    private final ResourcePacksProcessor resourcePacksProcessor;
    private final ThemeProcessor themeProcessor;
    private final CosmeticsProcessor cosmeticsProcessor;
    private final DragProcessor dragProcessor;
    private final ModuleProcessor moduleProcessor;
    private final CommandProcessor commandProcessor;
    private final HandlerProcessor handlerProcessor;

    public Processor() {
        Skeleton.getInstance().a(this);
        this.processors = new ArrayList<>();
        this.macrosProcessor = new MacrosProcessor();
        this.friendProcessor = new FriendProcessor();
        this.marksProcessor = new MarksProcessor();
        this.staffProcessor = new StaffProcessor();
        this.discordProcessor = new DiscordProcessor();
        this.accountProcessor = new AccountProcessor();
        this.draw2DProcessor = new Draw2DProcessor();
        this.draw3DProcessor = new Draw3DProcessor();
        this.notificationProcessor = new NotificationProcessor();
        this.resourcePacksProcessor = new ResourcePacksProcessor();
        this.themeProcessor = new ThemeProcessor();
        this.cosmeticsProcessor = new CosmeticsProcessor();
        this.dragProcessor = new DragProcessor();
        this.moduleProcessor = new ModuleProcessor();
        this.commandProcessor = new CommandProcessor();
        this.handlerProcessor = new HandlerProcessor();
    }

    public void a() {
        Collections.addAll(this.processors, this.macrosProcessor, this.cosmeticsProcessor, this.friendProcessor, this.marksProcessor, this.notificationProcessor, this.resourcePacksProcessor, this.staffProcessor, this.themeProcessor, this.accountProcessor,
                this.moduleProcessor, this.discordProcessor, this.draw2DProcessor, this.dragProcessor, this.draw3DProcessor, this.commandProcessor, this.handlerProcessor);
        this.processors.forEach(new Consumer<BaseProcessor>() {
            @Override
            public void accept(BaseProcessor obj) {
                obj.setup();
            }
        });
        System.out.println("setup - ".concat(String.valueOf(this.processors.stream().map(new Function<BaseProcessor, String>() {
            @Override
            public String apply(BaseProcessor obj) {
                return obj.getClass().getSimpleName();
            }
        }).toList())));
    }

    public List<BaseProcessor> c() {
        return this.processors;
    }

    public MacrosProcessor d() {
        return this.macrosProcessor;
    }

    public FriendProcessor e() {
        return this.friendProcessor;
    }

    public MarksProcessor getMarks() {
        return this.marksProcessor;
    }

    public StaffProcessor f() {
        return this.staffProcessor;
    }

    public DiscordProcessor g() {
        return this.discordProcessor;
    }

    public AccountProcessor h() {
        return this.accountProcessor;
    }

    public Draw2DProcessor i() {
        return this.draw2DProcessor;
    }

    public Draw3DProcessor j() {
        return this.draw3DProcessor;
    }

    public NotificationProcessor m() {
        return this.notificationProcessor;
    }

    public ResourcePacksProcessor n() {
        return this.resourcePacksProcessor;
    }

    public ThemeProcessor o() {
        return this.themeProcessor;
    }

    public CosmeticsProcessor r() {
        return this.cosmeticsProcessor;
    }

    public DragProcessor s() {
        return this.dragProcessor;
    }

    public ModuleProcessor t() {
        return this.moduleProcessor;
    }

    public CommandProcessor u() {
        return this.commandProcessor;
    }

    public HandlerProcessor v() {
        return this.handlerProcessor;
    }

    public void b() {
        this.processors.forEach(processor -> {
            try {
                processor.unSetup();
            } catch (Throwable th) {
            }
        });
        System.out.println("unSetup - " + this.processors.stream().map(processor2 -> {
            return processor2.getClass().getSimpleName();
        }).toList());
    }
}
