package com.example.xptable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The table as an item: the tooltip shows the XP (and claim) it carries. */
public class XpTableItem extends BlockItem {
    public XpTableItem(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        CompoundTag tag = getBlockEntityData(stack);
        long xp = tag == null ? 0 : tag.getLong("Xp");
        tip.add(Component.literal("Stored: " + XpTableMod.describe(xp)).withStyle(ChatFormatting.GREEN));
        if (tag != null && tag.hasUUID("Owner"))
            tip.add(Component.literal("Claimed by " + tag.getString("OwnerName")).withStyle(ChatFormatting.DARK_GREEN));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CompoundTag tag = getBlockEntityData(stack);
        return tag != null && tag.getLong("Xp") > 0;
    }
}
