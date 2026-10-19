package com.example.xptable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** XP Table: a green enchanting table that stores XP, can be claimed, and keeps its XP when broken. */
@Mod(XpTableMod.MODID)
public class XpTableMod {
    public static final String MODID = "xptable";

    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    public static final RegistryObject<Block> XP_TABLE = BLOCKS.register("xp_table", () -> new XpTableBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GREEN)
                    .strength(5.0F, 1200.0F) // always drops (no tool needed) so the XP inside is never lost
                    .lightLevel(s -> 7)));
    public static final RegistryObject<Item> XP_TABLE_ITEM = ITEMS.register("xp_table",
            () -> new XpTableItem(XP_TABLE.get(), new Item.Properties().rarity(Rarity.RARE)));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<XpTableBlockEntity>> XP_TABLE_BE = BLOCK_ENTITIES.register("xp_table",
            () -> BlockEntityType.Builder.of(XpTableBlockEntity::new, XP_TABLE.get()).build(null));
    public static final RegistryObject<MenuType<XpTableMenu>> XP_TABLE_MENU = MENUS.register("xp_table",
            () -> IForgeMenuType.create(XpTableMenu::new));

    public XpTableMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(this::creativeTab);
        Net.register();
        MinecraftForge.EVENT_BUS.addListener(XpTableMod::onBreak);
    }

    private void creativeTab(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) e.accept(XP_TABLE_ITEM);
    }

    /** Only the owner (or an op) can break a claimed table. */
    private static void onBreak(BlockEvent.BreakEvent e) {
        if (!(e.getLevel().getBlockEntity(e.getPos()) instanceof XpTableBlockEntity be)) return;
        if (!(e.getPlayer() instanceof ServerPlayer sp)) return;
        if (be.mayBreak(sp)) return;
        e.setCanceled(true);
        sp.displayClientMessage(Component.literal("This XP Table is claimed by " + be.ownerName())
                .withStyle(ChatFormatting.GREEN), true);
    }

    /** Shared by BlockItem tooltips: "1,234 points (Level 25)". */
    public static String describe(long points) {
        return String.format("%,d points (Level %,d)", points, Xp.levelFor(points));
    }
}
