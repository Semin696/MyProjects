package org.nig.smp.csp.manager;

import org.nig.smp.csp.model.BanEntry;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BanManager {

    private final File dataFolder;
    private Connection connection;

    public BanManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(dataFolder, "bans.db");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS bans (
                        ip TEXT PRIMARY KEY,
                        nick TEXT NOT NULL,
                        uuid TEXT NOT NULL,
                        reason TEXT NOT NULL,
                        banned_at INTEGER NOT NULL,
                        banned_by TEXT NOT NULL
                    )
                """);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize SQLite", e);
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }

    public boolean isBanned(String ip) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT 1 FROM bans WHERE ip = ?")) {
            stmt.setString(1, ip);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    public BanEntry getBan(String ip) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM bans WHERE ip = ?")) {
            stmt.setString(1, ip);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new BanEntry(
                        rs.getString("ip"),
                        rs.getString("nick"),
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("reason"),
                        rs.getLong("banned_at"),
                        rs.getString("banned_by")
                    );
                }
            }
        } catch (SQLException ignored) {}
        return null;
    }

    public void addBan(BanEntry entry) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT OR REPLACE INTO bans (ip, nick, uuid, reason, banned_at, banned_by) VALUES (?, ?, ?, ?, ?, ?)")) {
            stmt.setString(1, entry.getIp());
            stmt.setString(2, entry.getNick());
            stmt.setString(3, entry.getUuid().toString());
            stmt.setString(4, entry.getReason());
            stmt.setLong(5, entry.getBannedAt());
            stmt.setString(6, entry.getBannedBy());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeBan(String ipOrNick) {
        try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM bans WHERE ip = ? OR nick = ?")) {
            stmt.setString(1, ipOrNick);
            stmt.setString(2, ipOrNick);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<BanEntry> getAllBans() {
        List<BanEntry> bans = new ArrayList<>();
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM bans")) {
            while (rs.next()) {
                bans.add(new BanEntry(
                    rs.getString("ip"),
                    rs.getString("nick"),
                    UUID.fromString(rs.getString("uuid")),
                    rs.getString("reason"),
                    rs.getLong("banned_at"),
                    rs.getString("banned_by")
                ));
            }
        } catch (SQLException ignored) {}
        return bans;
    }
}
