package aethereal.config;

import aethereal.ambience.Ambience;
import aethereal.command.CommandProcessor;
import aethereal.command.LayoutCommand;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.Processor;
import aethereal.core.Skeleton;
import aethereal.event.KeyEvent;
import aethereal.lib.json.JSONArray;
import aethereal.lib.json.JSONObject;
import aethereal.module.misc.*;
import aethereal.module.movement.*;
import aethereal.module.player.*;
import aethereal.module.render.*;
import aethereal.render.Animations;
import aethereal.setting.BindSetting;
import aethereal.setting.Setting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.DefaultedRegistry;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ModuleProcessor extends ConfigProcessor<Module> {
    private final Sprint o = new Sprint();
    private final AutoAuth k = new AutoAuth();
    private final LockSlot p = new LockSlot();
    private final ClickPearl clickPearl = new ClickPearl();
    private final ShiftTap shiftTap = new ShiftTap();
    private final NoDelay noDelay = new NoDelay();
    private final CustomFog customFog = new CustomFog();
    private final KillEffect killEffect = new KillEffect();
    private final CustomHitbox customHitbox = new CustomHitbox();
    private final TotemTracker totemTracker = new TotemTracker();
    private final SoundReducer r = new SoundReducer();
    private final ItemScroller w = new ItemScroller();
    private final BoardSpoofer x = new BoardSpoofer();
    private final Communication y = new Communication();
    private final UseTracker z = new UseTracker();
    private final DeathCoords J = new DeathCoords();
    private final AutoAccept K = new AutoAccept();
    private final ThirdPerson M = new ThirdPerson();
    private final Zoom zoom = new Zoom();
    private final AutoRespawn P = new AutoRespawn();
    private final Animations Q = new Animations();
    private final SwingAnimation R = new SwingAnimation();
    private final ItemPhysic ag = new ItemPhysic();
    private final Removals ai = new Removals();
    private final NoCommands am = new NoCommands();
    private final ServerJoiner an = new ServerJoiner();
    private final ViewModel ao = new ViewModel();
    private final ChatHelper as = new ChatHelper();
    private final Sounds at = new Sounds();
    private final Crosshair au = new Crosshair();
    private final ShulkerPreview aw = new ShulkerPreview();
    private final ChinaHat ay = new ChinaHat();
    private final AspectRatio aB = new AspectRatio();
    private final StreamerMode aE = new StreamerMode();
    private final Ambience aF = new Ambience();
    private final FastLoad aN = new FastLoad();
    private final FullBright aO = new FullBright();
    private final HandsShader aT = new HandsShader();
    private final BlockOverlay blockOverlay = new BlockOverlay();
    private final TargetESP targetEsp = new TargetESP();
    private final WorldParticles worldParticles = new WorldParticles();
    private final PlayerCosmetics cosmetics = new PlayerCosmetics();
    private final CustomTotem customTotem = new CustomTotem();
    private final Hotbar hotbar = new Hotbar();
    private final Optimization optimization = new Optimization();
    private final DiscordRPC discordRpc = new DiscordRPC();
    private Interface bd;
    private String currentConfig = "default";

    public static void a(JSONObject obj, Module module) {
        module.a(obj.a("activated", false));
        module.a(obj.a("bind", -1));
        if (obj.m("settings")) {
            JSONObject settingsObj = obj.j("settings");
            for (Setting<?> setting : module.e()) {
                if (settingsObj.m(setting.i())) {
                    ConverterUtil.a(setting, settingsObj.a(setting.i()));
                }
            }
        }
    }

    @Override
    public void setup() {
        this.bd = new Interface();
        a(this.o, this.k, this.p, this.clickPearl, this.shiftTap, this.noDelay, this.r, this.w, this.x, this.y, this.z, this.totemTracker, this.J, this.K,
                this.M, this.zoom, this.P, this.Q, this.R, this.ag, this.ai, this.am, this.an, this.ao,
                this.as, this.at, this.au, this.aw, this.ay, this.aB, this.aE, this.aF, this.customFog, this.aN,
                this.aO, this.aT, this.blockOverlay, this.targetEsp, this.worldParticles, this.cosmetics, this.customTotem, this.customHitbox, this.killEffect, this.hotbar, this.optimization, this.discordRpc, this.bd);
        boolean discordRpcKnown = discordRpcSaved();
        boolean cosmeticsKnown = cosmeticsSaved();
        super.setup();
        if (!discordRpcKnown && !this.discordRpc.m()) {
            this.discordRpc.a(true);
        }
        if (!cosmeticsKnown && !this.cosmetics.m()) {
            this.cosmetics.a(true);
        }
        if (this.bd != null) {
            this.bd.applyBrandColor();
        }
    }

    private boolean cosmeticsSaved() {
        try {
            java.io.File file = new java.io.File(d(), getConfigFileName());
            if (!file.exists()) {
                return false;
            }
            return java.nio.file.Files.readString(file.toPath()).contains("\"Косметика\"");
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean discordRpcSaved() {
        try {
            java.io.File file = new java.io.File(d(), getConfigFileName());
            if (!file.exists()) {
                return false;
            }
            return java.nio.file.Files.readString(file.toPath()).contains("\"Discord RPC\"");
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected List<Module> loadConfig(String json) {
        if (json == null || json.isBlank() || json.trim().startsWith("[")) {
            return null;
        }
        JSONObject jSONObject = new JSONObject(json);
        JSONArray jSONArrayI = jSONObject.i("modules");
        if (jSONArrayI != null) {
            for (int i = 0; i < jSONArrayI.a(); i++) {
                final JSONObject jSONObjectJ = jSONArrayI.j(i);
                if (jSONObjectJ != null) {
                    final String strL = jSONObjectJ.l("name");
                    List<Module> listE = e();
                    if (listE != null) {
                        Stream<Module> stream = listE.stream();
                        Predicate<? super Module> predicate = obj -> obj.j().equalsIgnoreCase(strL);
                        Optional<Module> optionalFindFirst = stream.filter(predicate).findFirst();
                        Consumer<? super Module> consumer = obj -> ModuleProcessor.a(jSONObjectJ, obj);
                        optionalFindFirst.ifPresent(consumer);
                    }
                }
            }
        }
        JSONArray jSONArrayY = jSONObject.y("layouts");
        if (jSONArrayY != null) {
            Skeleton skeleton = Skeleton.getInstance();
            if (skeleton != null) {
                Processor processor = skeleton.getModuleProcessor();
                if (processor != null) {
                    CommandProcessor commandProcessor = processor.u();
                    if (commandProcessor != null) {
                        LayoutCommand layoutCommand = commandProcessor.e();
                        if (layoutCommand != null) {
                            List<LayoutCommand.a> listC = layoutCommand.c();
                            if (listC != null) {
                                listC.clear();
                                for (int i2 = 0; i2 < jSONArrayY.a(); i2++) {
                                    JSONObject jSONObjectJ2 = jSONArrayY.j(i2);
                                    if (jSONObjectJ2 != null) {
                                        DefaultedRegistry<Item> itemRegistry = Registries.ITEM;
                                        Identifier itemId = Identifier.of(jSONObjectJ2.l("item"));
                                        Item item = itemRegistry.get(itemId);
                                        if (item != null && item != Items.AIR) {
                                            listC.add(new LayoutCommand.a(jSONObjectJ2.l("name"), new ItemStack(item), jSONObjectJ2.h("slot")));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return new ArrayList<>(e());
    }

    @Override
    protected String saveConfig(List<Module> data) {
        JSONArray jSONArray = new JSONArray();
        if (data != null) {
            for (Module module : data) {
                if (module != null) {
                    JSONObject jSONObject = new JSONObject();
                    jSONObject.c("name", module.j());
                    jSONObject.b("activated", module.m());
                    jSONObject.b("bind", module.p());
                    JSONObject jSONObject2 = new JSONObject();
                    List<Setting<?>> listE = module.e();
                    if (listE != null) {
                        for (Setting<?> setting : listE) {
                            if (setting != null && setting.j()) {
                                jSONObject2.c(setting.i(), ConverterUtil.a(setting));
                            }
                        }
                    }
                    jSONObject.c("settings", jSONObject2);
                    jSONArray.a(jSONObject);
                }
            }
        }
        JSONArray jSONArray2 = new JSONArray();
        Skeleton skeleton = Skeleton.getInstance();
        if (skeleton != null) {
            Processor processor = skeleton.getModuleProcessor();
            if (processor != null) {
                CommandProcessor commandProcessor = processor.u();
                if (commandProcessor != null) {
                    LayoutCommand layoutCommand = commandProcessor.e();
                    if (layoutCommand != null) {
                        List<LayoutCommand.a> listC = layoutCommand.c();
                        if (listC != null) {
                            for (LayoutCommand.a layoutItem : listC) {
                                if (layoutItem != null) {
                                    JSONObject jSONObject3 = new JSONObject();
                                    jSONObject3.c("name", layoutItem.a());
                                    DefaultedRegistry<Item> itemRegistry = Registries.ITEM;
                                    ItemStack itemStack = layoutItem.b();
                                    if (itemStack != null) {
                                        Item item = itemStack.getItem();
                                        Identifier itemId = itemRegistry.getId(item);
                                        if (itemId != null) {
                                            jSONObject3.c("item", itemId.toString());
                                            jSONObject3.b("slot", layoutItem.c());
                                            jSONArray2.a(jSONObject3);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        JSONObject jSONObject4 = new JSONObject();
        jSONObject4.c("modules", jSONArray);
        jSONObject4.c("layouts", jSONArray2);
        return jSONObject4.a(2);
    }

    public Sprint o() {
        return this.o;
    }

    public AutoAuth k() {
        return this.k;
    }

    public LockSlot p() {
        return this.p;
    }

    public ClickPearl getClickPearl() {
        return this.clickPearl;
    }

    public ShiftTap getShiftTap() {
        return this.shiftTap;
    }

    public NoDelay getNoDelay() {
        return this.noDelay;
    }

    public CustomFog getCustomFog() {
        return this.customFog;
    }

    public KillEffect getKillEffect() {
        return this.killEffect;
    }

    public CustomHitbox getCustomHitbox() {
        return this.customHitbox;
    }

    public TotemTracker getTotemTracker() {
        return this.totemTracker;
    }

    public SoundReducer r() {
        return this.r;
    }

    public ItemScroller w() {
        return this.w;
    }

    public BoardSpoofer x() {
        return this.x;
    }

    public Communication y() {
        return this.y;
    }

    public UseTracker z() {
        return this.z;
    }

    public DeathCoords J() {
        return this.J;
    }

    public AutoAccept K() {
        return this.K;
    }

    public ThirdPerson M() {
        return this.M;
    }

    public AutoRespawn P() {
        return this.P;
    }

    public Animations Q() {
        return this.Q;
    }

    public SwingAnimation R() {
        return this.R;
    }

    public ItemPhysic ag() {
        return this.ag;
    }

    public Removals ai() {
        return this.ai;
    }

    public NoCommands am() {
        return this.am;
    }

    public ServerJoiner an() {
        return this.an;
    }

    public ViewModel ao() {
        return this.ao;
    }

    public ChatHelper as() {
        return this.as;
    }

    public Sounds at() {
        return this.at;
    }

    public Crosshair au() {
        return this.au;
    }

    public ShulkerPreview aw() {
        return this.aw;
    }

    public ChinaHat ay() {
        return this.ay;
    }

    public AspectRatio aB() {
        return this.aB;
    }

    public StreamerMode aE() {
        return this.aE;
    }

    public Ambience aF() {
        return this.aF;
    }

    public FastLoad aN() {
        return this.aN;
    }

    public FullBright aO() {
        return this.aO;
    }

    public HandsShader aT() {
        return this.aT;
    }

    public Interface bd() {
        return this.bd;
    }

    public BlockOverlay getBlockOverlay() {
        return this.blockOverlay;
    }

    public DiscordRPC getDiscordRpc() {
        return this.discordRpc;
    }

    public Optimization getOptimization() {
        return this.optimization;
    }

    public WorldParticles getWorldParticles() {
        return this.worldParticles;
    }

    public PlayerCosmetics getCosmetics() {
        return this.cosmetics;
    }

    public CustomTotem getCustomTotem() {
        return this.customTotem;
    }

    public Hotbar getHotbar() {
        return this.hotbar;
    }

    public Zoom getZoom() {
        return this.zoom;
    }

    @Override
    public void unSetup() {
        super.unSetup();
    }

    @Override
    public File d() {
        return this.b;
    }

    @Override
    protected String getConfigFileName() {
        return "default.json";
    }

    @EventTarget
    public void a(KeyEvent event) {
        int action = event.getAction();
        int key = event.getKey();
        for (Module module : e()) {
            if (module.p() != -1 && module.p() == key && action == 1) {
                module.a();
                event.a(true);
            }
            if (module.m()) {
                for (Setting<?> setting : module.e()) {
                    if (setting instanceof BindSetting bind) {
                        if (bind.e().get().booleanValue() && bind.c().intValue() != -1 && bind.c().intValue() == key) {
                            if (action == 1 && bind.k() != null) {
                                bind.k().execute();
                                event.a(true);
                            } else if (action == 0 && bind.m() == 0 && bind.l() != null) {
                                bind.l().execute();
                                event.a(true);
                            }
                        }
                    }
                }
            }
        }
    }

    public void b(String configName) {
        try {
            File dir = d();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String name = sanitizeConfigName(configName);
            if (name.isEmpty()) {
                return;
            }
            Files.writeString(new File(dir, name + ".json").toPath(), saveConfig((List<Module>) this.d));
            this.currentConfig = name;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean c(String str) {
        try {
            String name = sanitizeConfigName(str);
            File file = new File(d(), name + ".json");
            if (!file.exists()) {
                return false;
            }
            List<Module> listA = loadConfig(Files.readString(file.toPath()));
            if (listA != null) {
                this.d.clear();
                this.d.addAll(listA);
            }
            this.currentConfig = name;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean d(String configName) {
        String name = sanitizeConfigName(configName);
        File configFile = new File(d(), name + ".json");
        if (configFile.exists() && !name.equals("default")) {
            boolean deleted = configFile.delete();
            if (deleted && name.equals(this.currentConfig)) {
                this.currentConfig = "default";
            }
            return deleted;
        }
        return false;
    }

    public String currentConfig() {
        return this.currentConfig;
    }

    public List<File> configFiles() {
        File dir = d();
        if (dir == null || !dir.exists()) {
            return List.of();
        }
        File[] files = dir.listFiles((folder, filename) -> filename.endsWith(".json") && new File(folder, filename).isFile());
        if (files == null || files.length == 0) {
            return List.of();
        }
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        return Arrays.asList(files);
    }

    public void openConfigFolder() {
        File dir = d();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        net.minecraft.util.Util.getOperatingSystem().open(dir);
    }

    public static String configDisplayName(File file) {
        String name = file.getName();
        return name.endsWith(".json") ? name.substring(0, name.length() - 5) : name;
    }

    public static String sanitizeConfigName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim().replaceAll("[\\\\/:*?\"<>|]", "").replaceAll("\\s+", " ");
        if (trimmed.toLowerCase(Locale.ROOT).endsWith(".json")) {
            trimmed = trimmed.substring(0, trimmed.length() - 5);
        }
        return trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
    }

    private void a(Module... modules) {
        Collections.addAll(this.d, modules);
    }
}

