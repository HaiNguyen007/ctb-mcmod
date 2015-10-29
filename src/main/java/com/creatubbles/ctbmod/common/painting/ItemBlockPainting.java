package com.creatubbles.ctbmod.common.painting;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.creatubbles.api.core.Creation;
import com.creatubbles.ctbmod.common.util.NBTUtil;


public class ItemBlockPainting extends ItemBlock {

    public ItemBlockPainting(Block block) {
        super(block);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void addInformation(ItemStack stack, EntityPlayer playerIn, List tooltip, boolean advanced) {
        super.addInformation(stack, playerIn, tooltip, advanced);
        Creation c = NBTUtil.readJsonFromNBT(Creation.class, stack.getTagCompound());
        tooltip.add(EnumChatFormatting.ITALIC.toString().concat(c.name));
        tooltip.add(c.creators[0].name);
    }
}