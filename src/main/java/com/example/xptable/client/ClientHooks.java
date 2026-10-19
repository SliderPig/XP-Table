package com.example.xptable.client;

import com.example.xptable.Net;
import com.example.xptable.XpTableMenu;
import com.example.xptable.XpTableMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = XpTableMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientHooks {
    private ClientHooks() {}

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent e) {
        e.enqueueWork(() -> MenuScreens.register(XpTableMod.XP_TABLE_MENU.get(), XpTableScreen::new));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerBlockEntityRenderer(XpTableMod.XP_TABLE_BE.get(), XpTableRenderer::new);
    }

    public static void onState(Net.State s) {
        var p = Minecraft.getInstance().player;
        if (p != null && p.containerMenu instanceof XpTableMenu m) m.state = s;
    }
}
