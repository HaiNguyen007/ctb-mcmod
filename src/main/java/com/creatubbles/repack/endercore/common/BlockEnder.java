package com.creatubbles.repack.endercore.common;

import java.util.List;

import javax.annotation.Nullable;

import lombok.Setter;
import lombok.SneakyThrows;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.GameRegistry;

import com.creatubbles.ctbmod.CTBMod;
import com.google.common.collect.Lists;

public abstract class BlockEnder<T extends TileEntityBase> extends Block {

    protected final Class<? extends T> teClass;
    protected final String name;

    @Setter
    private ItemBlock itemBlock;

    protected BlockEnder(String name, Class<? extends T> teClass) {
        this(name, teClass, new Material(MapColor.IRON));
    }

    protected BlockEnder(String name, Class<? extends T> teClass, Material mat) {
        super(mat);
        this.teClass = teClass;
        this.name = name;
        setHardness(0.5F);
        setUnlocalizedName(name);
        setRegistryName(name);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
    }

    @SneakyThrows
    protected void init() {
        GameRegistry.register(this);
        if (itemBlock == null) {
            itemBlock = new ItemBlock(this);
        }
        itemBlock.setRegistryName(this.getRegistryName());
        GameRegistry.register(itemBlock);
        
        if (teClass != null) {
            GameRegistry.registerTileEntity(teClass, name + "TileEntity");
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return teClass != null;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        if (teClass != null) {
            try {
                T te = teClass.newInstance();
                te.init();
                return te;
            } catch (Exception e) {
                CTBMod.logger.error("Could not create tile entity for block " + name + " for class " + teClass);
            }
        }
        return null;
    }

    /* Subclass Helpers */

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (playerIn.isSneaking()) {
            return false;
        }
        return openGui(worldIn, pos, playerIn, side);
    }

    protected boolean openGui(World world, BlockPos pos, EntityPlayer entityPlayer, EnumFacing side) {
        return false;
    }

    public boolean doNormalDrops(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (willHarvest) {
            return true;
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te, ItemStack stack) {
        super.harvestBlock(worldIn, player, pos, state, te, stack);
        worldIn.setBlockToAir(pos);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        if (doNormalDrops(world, pos)) {
            return super.getDrops(world, pos, state, fortune);
        }
        return Lists.newArrayList(getNBTDrop(world, pos, (T) world.getTileEntity(pos)));
    }

    public ItemStack getNBTDrop(IBlockAccess world, BlockPos pos, T te) {
        int meta = damageDropped(world.getBlockState(pos));
        ItemStack itemStack = new ItemStack(this, 1, meta);
        processDrop(world, pos, te, itemStack);
        return itemStack;
    }

    protected void processDrop(IBlockAccess world, BlockPos pos, @Nullable T te, ItemStack drop) {}

    @SuppressWarnings("unchecked")
    protected T getTileEntity(World world, BlockPos pos) {
        TileEntity te = world.getTileEntity(pos);
        if (teClass.isInstance(te)) {
            return (T) te;
        }
        return null;
    }

    protected boolean shouldDoWorkThisTick(World world, BlockPos pos, int interval) {
        T te = getTileEntity(world, pos);
        if (te == null) {
            return world.getTotalWorldTime() % interval == 0;
        } else {
            return te.shouldDoWorkThisTick(interval);
        }
    }

    protected boolean shouldDoWorkThisTick(World world, BlockPos pos, int interval, int offset) {
        T te = getTileEntity(world, pos);
        if (te == null) {
            return (world.getTotalWorldTime() + offset) % interval == 0;
        } else {
            return te.shouldDoWorkThisTick(interval, offset);
        }
    }
}
