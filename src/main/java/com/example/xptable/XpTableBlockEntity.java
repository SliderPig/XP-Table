package com.example.xptable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Holds the XP (in points, no limit) and the claim. */
public class XpTableBlockEntity extends BlockEntity {
    private long xp;
    private UUID owner;
    private String ownerName = "";

    // Book animation, client only (same idea as the vanilla enchanting table).
    public int time;
    public float flip, oFlip, flipT, flipA, open, oOpen, rot, oRot, tRot;

    public XpTableBlockEntity(BlockPos pos, BlockState state) {
        super(XpTableMod.XP_TABLE_BE.get(), pos, state);
    }

    public long xp() { return xp; }
    public boolean claimed() { return owner != null; }
    public String ownerName() { return ownerName; }
    public boolean isOwner(Player p) { return owner != null && owner.equals(p.getUUID()); }

    /** Unclaimed tables are free for everyone; claimed ones only for the owner. */
    public boolean mayUse(Player p) { return owner == null || isOwner(p); }
    /** Ops can always break or unclaim, in case something goes wrong. */
    public boolean mayBreak(ServerPlayer p) { return mayUse(p) || p.hasPermissions(2); }

    public void setXp(long v) { xp = Math.max(0, v); changed(); }

    public void claim(Player p) {
        owner = p.getUUID();
        ownerName = p.getGameProfile().getName();
        changed();
    }

    public void unclaim() {
        owner = null;
        ownerName = "";
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeShared(tag);
    }

    private void writeShared(CompoundTag tag) {
        tag.putLong("Xp", xp);
        if (owner != null) {
            tag.putUUID("Owner", owner);
            tag.putString("OwnerName", ownerName);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        xp = Math.max(0, tag.getLong("Xp"));
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        ownerName = owner == null ? "" : tag.getString("OwnerName");
    }

    // The client needs the owner's name (floating label) and the amount.
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeShared(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, XpTableBlockEntity be) {
        be.oOpen = be.open;
        be.oRot = be.rot;
        Player p = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0, false);
        if (p != null) {
            double dx = p.getX() - (pos.getX() + 0.5);
            double dz = p.getZ() - (pos.getZ() + 0.5);
            be.tRot = (float) Mth.atan2(dz, dx);
            be.open += 0.1F;
            if (be.open < 0.5F || level.random.nextInt(40) == 0) {
                float f = be.flipT;
                do { be.flipT += (float) (level.random.nextInt(4) - level.random.nextInt(4)); } while (f == be.flipT);
            }
        } else {
            be.tRot += 0.02F;
            be.open -= 0.1F;
        }
        while (be.rot >= (float) Math.PI) be.rot -= (float) Math.PI * 2F;
        while (be.rot < -(float) Math.PI) be.rot += (float) Math.PI * 2F;
        while (be.tRot >= (float) Math.PI) be.tRot -= (float) Math.PI * 2F;
        while (be.tRot < -(float) Math.PI) be.tRot += (float) Math.PI * 2F;
        float d = be.tRot - be.rot;
        while (d >= (float) Math.PI) d -= (float) Math.PI * 2F;
        while (d < -(float) Math.PI) d += (float) Math.PI * 2F;
        be.rot += d * 0.4F;
        be.open = Mth.clamp(be.open, 0.0F, 1.0F);
        ++be.time;
        be.oFlip = be.flip;
        float f1 = (be.flipT - be.flip) * 0.4F;
        f1 = Mth.clamp(f1, -0.2F, 0.2F);
        be.flipA += (f1 - be.flipA) * 0.9F;
        be.flip += be.flipA;
    }
}
