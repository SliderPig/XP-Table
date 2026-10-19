package com.example.xptable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * No item slots: the server does all the XP moving and claim checks, then sends the
 * screen a {@link Net.State} whenever something changed (also when someone else uses the table).
 */
public class XpTableMenu extends AbstractContainerMenu {
    public static final int DEPOSIT = 0, WITHDRAW = 1, CLAIM = 2, UNCLAIM = 3;
    /** amount = -1 means "All". */
    public static final long ALL = -1;

    public final BlockPos pos;
    private final Player player;

    // Server side: what we last told the client, and how much moved since the menu opened.
    private Net.State lastSent;
    private long moved;

    // Client side: the latest state from the server.
    public Net.State state = new Net.State(0, "", false, false, false, false, 0);

    /** Client constructor. */
    public XpTableMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readBlockPos());
    }

    public XpTableMenu(int id, Inventory inv, BlockPos pos) {
        super(XpTableMod.XP_TABLE_MENU.get(), id);
        this.pos = pos;
        this.player = inv.player;
    }

    private XpTableBlockEntity table() {
        return player.level().getBlockEntity(pos) instanceof XpTableBlockEntity be ? be : null;
    }

    @Override
    public boolean stillValid(Player p) {
        return table() != null && p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!(player instanceof ServerPlayer sp)) return;
        XpTableBlockEntity be = table();
        if (be == null) return;
        Net.State now = new Net.State(be.xp(), be.ownerName(), be.claimed(), be.mayUse(sp), be.isOwner(sp),
                be.claimed() && be.mayBreak(sp), moved);
        if (!now.equals(lastSent)) {
            lastSent = now;
            Net.toClient(sp, now);
        }
    }

    /** Called on the server when the screen sends a button press. */
    public void handle(ServerPlayer sp, int action, long amount, boolean levels) {
        XpTableBlockEntity be = table();
        if (be == null || !stillValid(sp)) return;
        switch (action) {
            case CLAIM -> { if (!be.claimed()) be.claim(sp); }
            case UNCLAIM -> { if (be.claimed() && be.mayBreak(sp)) be.unclaim(); }
            case DEPOSIT, WITHDRAW -> {
                if (!be.mayUse(sp)) return;
                long have = Xp.points(sp);
                long pts;
                if (action == DEPOSIT) {
                    if (amount == ALL) pts = have;
                    else if (levels) pts = have - Xp.totalFor(Math.max(0, sp.experienceLevel - amount));
                    else pts = amount;
                    pts = Math.min(pts, have);
                    pts = Math.min(pts, Long.MAX_VALUE - be.xp());
                } else {
                    long room = Xp.PLAYER_MAX - have;
                    if (amount == ALL) pts = be.xp();
                    else if (levels) pts = Xp.totalFor(Math.min(sp.experienceLevel + amount, 100_000L)) - Xp.totalFor(sp.experienceLevel);
                    else pts = amount;
                    pts = Math.min(Math.min(pts, be.xp()), room);
                }
                if (pts <= 0) return;
                if (action == DEPOSIT) {
                    Xp.set(sp, have - pts);
                    be.setXp(be.xp() + pts);
                    moved += pts;
                } else {
                    Xp.set(sp, have + pts);
                    be.setXp(be.xp() - pts);
                    moved -= pts;
                }
                sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                        SoundSource.PLAYERS, 0.4F, 0.9F + sp.getRandom().nextFloat() * 0.3F);
            }
            default -> { }
        }
        broadcastChanges();
    }
}
