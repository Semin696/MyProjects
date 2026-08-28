package aethereal.friend;


import aethereal.config.ConfigProcessor;
import aethereal.core.EventTarget;
import aethereal.core.Interface;
import aethereal.event.AttackEvent;
import aethereal.event.BackendEvent;
import aethereal.lib.json.JSONArray;
import aethereal.lib.json.JSONObject;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;

import java.util.ArrayList;
import java.util.List;

public class FriendProcessor extends ConfigProcessor<FriendConstructor> implements Interface {
    private boolean noDamage = true;
    private boolean highlight = true;
    private boolean noPush = true;
    private boolean skipEsp = true;

    @Override

    protected List<FriendConstructor> loadConfig(String json) throws Exception {
        this.noDamage = true;
        this.highlight = true;
        this.noPush = true;
        this.skipEsp = true;
        ArrayList<FriendConstructor> arrayList = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return arrayList;
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("{")) {
            JSONObject object = new JSONObject(trimmed);
            this.noDamage = object.a("noDamage", true);
            this.highlight = object.a("highlight", true);
            this.noPush = object.a("noPush", true);
            this.skipEsp = object.a("skipEsp", true);
            JSONArray friends = object.i("friends");
            if (friends != null) {
                readFriends(friends, arrayList);
            }
            return arrayList;
        }
        readFriends(new JSONArray(trimmed), arrayList);
        return arrayList;
    }

    private static void readFriends(JSONArray array, List<FriendConstructor> out) {
        for (int i = 0; i < array.a(); i++) {
            try {
                JSONObject obj = array.j(i);
                String name = obj.l("name");
                if (name != null && !name.isBlank()) {
                    out.add(new FriendConstructor(name.trim(), obj.a("note", ""), obj.a("favorite", false)));
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override

    protected String saveConfig(List<FriendConstructor> data) throws Exception {
        JSONArray friends = new JSONArray();
        for (FriendConstructor friendConstructor : data) {
            JSONObject friend = new JSONObject();
            friend.c("name", friendConstructor.a());
            friend.c("note", friendConstructor.note());
            friend.b("favorite", friendConstructor.favorite());
            friends.a(friend);
        }
        JSONObject object = new JSONObject();
        object.b("noDamage", this.noDamage);
        object.b("highlight", this.highlight);
        object.b("noPush", this.noPush);
        object.b("skipEsp", this.skipEsp);
        object.c("friends", friends);
        return object.a(2);
    }

    @Override
    protected String getConfigFileName() {
        return "friends.json";
    }

    @EventTarget
    public void onBackend(BackendEvent event) {
        String minecraft;
        if (event.isReceive() && "friend".equals(event.getPacket().getId())) {
            String payload = event.getPacket().getPayload();
            if ("rename".equals(event.getPacket().getSecurity().extractString(payload, "type")) && (minecraft = event.getPacket().getSecurity().extractString(payload, "minecraft")) != null) {
                b(minecraft);
                unSetup();
            }
        }
    }

    @EventTarget(a = 0)
    public void onAttack(AttackEvent event) {
        if (!this.noDamage) {
            return;
        }
        Entity target = event.b();
        if (target instanceof PlayerEntity player && d(player.getName().getString())) {
            event.a(true);
        }
    }

    public List<FriendConstructor> a() {
        return new ArrayList<>(this.d);
    }

    public FriendConstructor find(String name) {
        return this.d.stream().filter(friend -> friend.a().equalsIgnoreCase(name)).findFirst().orElse(null);
    }

    public void b(String str) {
        if (str == null) {
            return;
        }
        String name = str.trim();
        if (!name.isEmpty() && !d(name)) {
            this.d.add(new FriendConstructor(name));
        }
    }

    public void c(String str) {
        this.d.removeIf(friend -> friend.a().equalsIgnoreCase(str));
    }

    public boolean d(String name) {
        return this.d.stream().anyMatch(friend -> friend.a().equalsIgnoreCase(name));
    }

    public void f() {
        this.d.clear();
    }

    public boolean g() {
        return this.noDamage;
    }

    public void a(boolean noDamage) {
        this.noDamage = noDamage;
        unSetup();
    }

    public boolean highlight() {
        return this.highlight;
    }

    public void highlight(boolean highlight) {
        this.highlight = highlight;
        unSetup();
    }

    public boolean noPush() {
        return this.noPush;
    }

    public void noPush(boolean noPush) {
        this.noPush = noPush;
        unSetup();
    }

    public boolean skipEsp() {
        return this.skipEsp;
    }

    public void skipEsp(boolean skipEsp) {
        this.skipEsp = skipEsp;
        unSetup();
    }

    public boolean isOnline(String name) {
        return getEntry(name) != null || getWorldPlayer(name) != null;
    }

    public PlayerListEntry getEntry(String name) {
        if (name == null || mc.getNetworkHandler() == null) {
            return null;
        }
        return mc.getNetworkHandler().getPlayerList().stream()
                .filter(entry -> entry.getProfile().getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public PlayerEntity getWorldPlayer(String name) {
        if (name == null || mc.world == null) {
            return null;
        }
        return mc.world.getPlayers().stream()
                .filter(player -> player.getName().getString().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public long onlineCount() {
        return this.d.stream().filter(friend -> isOnline(friend.a())).count();
    }

    public int ping(String name) {
        PlayerListEntry entry = getEntry(name);
        return entry == null ? -1 : entry.getLatency();
    }

    public double distance(String name) {
        PlayerEntity player = getWorldPlayer(name);
        if (player == null || mc.player == null) {
            return -1.0d;
        }
        return mc.player.distanceTo(player);
    }

    public PlayerEntity lookingAtPlayer() {
        if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof PlayerEntity player) {
            if (mc.player != null && player != mc.player) {
                return player;
            }
        }
        return null;
    }

    public boolean shouldProtect(Entity entity) {
        return entity instanceof PlayerEntity player && d(player.getName().getString());
    }
}
