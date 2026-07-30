package org.nig.smp.csp.model;

import java.time.Instant;
import java.util.UUID;

public class BanEntry {

    private final String ip;
    private final String nick;
    private final UUID uuid;
    private final String reason;
    private final long bannedAt;
    private final String bannedBy;

    public BanEntry(String ip, String nick, UUID uuid, String reason, String bannedBy) {
        this.ip = ip;
        this.nick = nick;
        this.uuid = uuid;
        this.reason = reason;
        this.bannedAt = Instant.now().toEpochMilli();
        this.bannedBy = bannedBy;
    }

    public BanEntry(String ip, String nick, UUID uuid, String reason, long bannedAt, String bannedBy) {
        this.ip = ip;
        this.nick = nick;
        this.uuid = uuid;
        this.reason = reason;
        this.bannedAt = bannedAt;
        this.bannedBy = bannedBy;
    }

    public String getIp() { return ip; }
    public String getNick() { return nick; }
    public UUID getUuid() { return uuid; }
    public String getReason() { return reason; }
    public long getBannedAt() { return bannedAt; }
    public String getBannedBy() { return bannedBy; }
}
