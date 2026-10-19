package com.example.xptable;

import net.minecraft.server.level.ServerPlayer;

/** Exact XP point maths (1.20.1 formulas), so storing and taking back never loses a point. */
public final class Xp {
    private Xp() {}

    /** Points needed to go from level L to L+1. */
    public static long needed(long level) {
        if (level >= 30) return 112 + (level - 30) * 9;
        if (level >= 15) return 37 + (level - 15) * 5;
        return 7 + level * 2;
    }

    /** Total points to reach level L from 0. */
    public static long totalFor(long l) {
        if (l <= 16) return l * l + 6 * l;
        if (l <= 31) return (5 * l * l - 81 * l + 720) / 2;
        return (9 * l * l - 325 * l + 4440) / 2;
    }

    /** The level someone with this many points would be. */
    public static long levelFor(long points) {
        long lo = 0, hi = 1;
        while (totalFor(hi) <= points) hi *= 2;
        while (lo < hi - 1) {
            long mid = (lo + hi) / 2;
            if (totalFor(mid) <= points) lo = mid; else hi = mid;
        }
        return lo;
    }

    /** The player's exact points right now. */
    public static long points(ServerPlayer p) {
        long base = totalFor(p.experienceLevel);
        long into = Math.round(p.experienceProgress * needed(p.experienceLevel));
        return base + Math.max(0, Math.min(into, needed(p.experienceLevel) - 1));
    }

    /** Highest total we hand a player: vanilla keeps total XP in an int. */
    public static final long PLAYER_MAX = Integer.MAX_VALUE;

    /** Sets the player to exactly this many points. */
    public static void set(ServerPlayer p, long total) {
        total = Math.max(0, Math.min(total, PLAYER_MAX));
        int level = (int) levelFor(total);
        p.setExperienceLevels(level);
        p.setExperiencePoints((int) (total - totalFor(level)));
        p.totalExperience = (int) total;
    }
}
