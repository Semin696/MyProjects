package aethereal.mark;

import aethereal.command.GPSCommand;
import aethereal.config.ConfigProcessor;
import aethereal.core.Interface;
import aethereal.core.Skeleton;
import aethereal.lib.json.JSONArray;
import aethereal.lib.json.JSONObject;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MarksProcessor extends ConfigProcessor<MarkConstructor> implements Interface {
    @Override
    protected List<MarkConstructor> loadConfig(String json) throws Exception {
        ArrayList<MarkConstructor> list = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return list;
        }
        JSONArray array = new JSONArray(json.trim());
        for (int i = 0; i < array.a(); i++) {
            try {
                JSONObject obj = array.j(i);
                String name = obj.a("name", "");
                if (name == null || name.isBlank()) {
                    continue;
                }
                list.add(new MarkConstructor(name.trim(), obj.e("x"), obj.e("y"), obj.e("z"), obj.a("dimension", "")));
            } catch (Exception ignored) {
            }
        }
        return list;
    }

    @Override
    protected String saveConfig(List<MarkConstructor> data) throws Exception {
        JSONArray array = new JSONArray();
        for (MarkConstructor mark : data) {
            JSONObject obj = new JSONObject();
            obj.c("name", mark.name());
            obj.b("x", mark.x());
            obj.b("y", mark.y());
            obj.b("z", mark.z());
            obj.c("dimension", mark.dimension());
            array.a(obj);
        }
        return array.E(2);
    }

    @Override
    protected String getConfigFileName() {
        return "marks.json";
    }

    public List<MarkConstructor> a() {
        return new ArrayList<>(this.d);
    }

    public MarkConstructor find(String name) {
        if (name == null) {
            return null;
        }
        return this.d.stream().filter(mark -> mark.name().equalsIgnoreCase(name.trim())).findFirst().orElse(null);
    }

    public boolean exists(String name) {
        return find(name) != null;
    }

    public MarkConstructor add(String name, Vec3d pos) {
        if (mc.player == null && pos == null) {
            return null;
        }
        Vec3d at = pos != null ? pos : mc.player.getPos();
        String unique = uniqueName(name);
        MarkConstructor mark = new MarkConstructor(unique, at.x, at.y, at.z, currentDimension());
        this.d.add(mark);
        unSetup();
        return mark;
    }

    public boolean remove(String name) {
        MarkConstructor mark = find(name);
        if (mark == null) {
            return false;
        }
        this.d.removeIf(item -> item.name().equalsIgnoreCase(name));
        unSetup();
        GPSCommand gps = gps();
        if (gps != null) {
            gps.clearForMark(mark.name(), mark.pos());
        }
        return true;
    }

    public boolean rename(String oldName, String newName) {
        MarkConstructor mark = find(oldName);
        if (mark == null) {
            return false;
        }
        String trimmed = sanitize(newName);
        if (trimmed.isEmpty()) {
            return false;
        }
        MarkConstructor other = find(trimmed);
        if (other != null && other != mark) {
            trimmed = uniqueName(trimmed);
        }
        mark.name(trimmed);
        unSetup();
        GPSCommand gps = gps();
        if (gps != null) {
            gps.renameSource(oldName, trimmed);
        }
        return true;
    }

    public boolean move(String name, Vec3d pos) {
        MarkConstructor mark = find(name);
        if (mark == null || pos == null) {
            return false;
        }
        mark.pos(pos.x, pos.y, pos.z);
        mark.dimension(currentDimension());
        unSetup();
        GPSCommand gps = gps();
        if (gps != null) {
            gps.retargetIfMark(mark.name(), pos);
        }
        return true;
    }

    private GPSCommand gps() {
        Skeleton client = Skeleton.getInstance();
        if (client == null || client.getModuleProcessor() == null || client.getModuleProcessor().u() == null) {
            return null;
        }
        return client.getModuleProcessor().u().d();
    }

    public void clear() {
        this.d.clear();
        unSetup();
    }

    public double distance(MarkConstructor mark) {
        if (mark == null || mc.player == null) {
            return -1.0d;
        }
        return mc.player.getPos().distanceTo(mark.pos());
    }

    public String currentCoords() {
        if (mc.player == null) {
            return "—";
        }
        return ((int) Math.floor(mc.player.getX())) + "  " + ((int) Math.floor(mc.player.getY())) + "  " + ((int) Math.floor(mc.player.getZ()));
    }

    public String currentDimension() {
        if (mc.world == null) {
            return "";
        }
        Identifier id = mc.world.getRegistryKey().getValue();
        return id == null ? "" : id.toString();
    }

    public static String dimensionLabel(String dimension) {
        if (dimension == null) {
            return "";
        }
        String value = dimension.toLowerCase(Locale.ROOT);
        if (value.contains("nether")) {
            return "Ад";
        }
        if (value.contains("the_end") || value.endsWith(":end")) {
            return "Энд";
        }
        if (value.contains("overworld")) {
            return "Обычный";
        }
        return dimension.isBlank() ? "" : dimension;
    }

    private String uniqueName(String name) {
        String base = sanitize(name);
        if (base.isEmpty()) {
            base = "Метка";
        }
        if (!exists(base)) {
            return base;
        }
        int index = 2;
        while (exists(base + " " + index)) {
            index++;
        }
        return base + " " + index;
    }

    public static String sanitize(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim().replaceAll("\\s+", " ");
        return trimmed.length() > 32 ? trimmed.substring(0, 32) : trimmed;
    }
}
