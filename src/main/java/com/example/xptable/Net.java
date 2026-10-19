package com.example.xptable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.Supplier;

/** Two messages: the screen's button presses (to the server) and the table's state (to the screen). */
public final class Net {
    private Net() {}

    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(XpTableMod.MODID, "main"), () -> VERSION, VERSION::equals, VERSION::equals);

    /** A button press. amount = {@link XpTableMenu#ALL} for "All". */
    public record Action(int action, long amount, boolean levels) {
        void write(FriendlyByteBuf b) { b.writeVarInt(action); b.writeLong(amount); b.writeBoolean(levels); }
        static Action read(FriendlyByteBuf b) { return new Action(b.readVarInt(), b.readLong(), b.readBoolean()); }
    }

    /** What the screen shows. moved = points stored (+) or taken (-) since the menu opened. */
    public record State(long xp, String ownerName, boolean claimed, boolean canUse, boolean isOwner,
                        boolean canUnclaim, long moved) {
        void write(FriendlyByteBuf b) {
            b.writeLong(xp); b.writeUtf(ownerName, 64); b.writeBoolean(claimed); b.writeBoolean(canUse);
            b.writeBoolean(isOwner); b.writeBoolean(canUnclaim); b.writeLong(moved);
        }
        static State read(FriendlyByteBuf b) {
            return new State(b.readLong(), b.readUtf(64), b.readBoolean(), b.readBoolean(), b.readBoolean(),
                    b.readBoolean(), b.readLong());
        }
    }

    static void register() {
        CHANNEL.registerMessage(0, Action.class, Action::write, Action::read, Net::onAction,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, State.class, State::write, State::read, Net::onState,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void toServer(Action a) { CHANNEL.sendToServer(a); }

    static void toClient(ServerPlayer p, State s) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), s); }

    private static void onAction(Action a, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp != null && sp.containerMenu instanceof XpTableMenu m)
                m.handle(sp, a.action(), a.amount(), a.levels());
        });
        c.setPacketHandled(true);
    }

    private static void onState(State s, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.example.xptable.client.ClientHooks.onState(s)));
        c.setPacketHandled(true);
    }
}
