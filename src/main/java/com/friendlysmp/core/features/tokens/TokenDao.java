package com.friendlysmp.core.features.tokens;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.UUID;

public final class TokenDao {
    private final DataSource ds;

    public TokenDao(DataSource ds) {
        this.ds = ds;
    }

    public void init() throws Exception {
        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS friendlycore_tokens (
                  uuid TEXT PRIMARY KEY,
                  last_rewarded_year INTEGER,
                  last_rewarded_month INTEGER,
                  pending_tokens INTEGER NOT NULL DEFAULT 0
                )
            """);
        }
    }

    public int getPendingTokens(UUID uuid) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 SELECT pending_tokens
                 FROM friendlycore_tokens
                 WHERE uuid = ?
             """)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return 0;
                return rs.getInt("pending_tokens");
            }
        }
    }

    public void setPendingTokens(UUID uuid, int amount) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO friendlycore_tokens (uuid, pending_tokens)
                 VALUES (?, ?)
                 ON CONFLICT(uuid) DO UPDATE SET pending_tokens = excluded.pending_tokens
             """)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, Math.max(0, amount));
            ps.executeUpdate();
        }
    }

    public void addPendingTokens(UUID uuid, int amount) throws Exception {
        int current = getPendingTokens(uuid);
        setPendingTokens(uuid, current + Math.max(0, amount));
    }

    public boolean hasReceivedForPeriod(UUID uuid, int year, int month) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 SELECT last_rewarded_year, last_rewarded_month
                 FROM friendlycore_tokens
                 WHERE uuid = ?
             """)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return rs.getInt("last_rewarded_year") == year
                        && rs.getInt("last_rewarded_month") == month;
            }
        }
    }

    public void setLastRewarded(UUID uuid, int year, int month) throws Exception {
        try (Connection c = ds.getConnection();
             PreparedStatement ps = c.prepareStatement("""
                 INSERT INTO friendlycore_tokens (uuid, last_rewarded_year, last_rewarded_month)
                 VALUES (?, ?, ?)
                 ON CONFLICT(uuid) DO UPDATE SET
                   last_rewarded_year = excluded.last_rewarded_year,
                   last_rewarded_month = excluded.last_rewarded_month
             """)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, year);
            ps.setInt(3, month);
            ps.executeUpdate();
        }
    }
}