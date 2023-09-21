package com.github.reoseah.catwalksinc.part;

import alexiil.mc.lib.multipart.api.*;
import alexiil.mc.lib.multipart.api.event.NeighbourStateUpdateEvent;
import alexiil.mc.lib.multipart.api.render.PartModelKey;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public abstract class CatwalksIncPart extends AbstractPart {
    public CatwalksIncPart(PartDefinition definition, MultipartHolder holder) {
        super(definition, holder);
    }

    protected World getWorld() {
        return this.holder.getContainer().getMultipartWorld();
    }

    protected BlockPos getPos() {
        return this.holder.getContainer().getMultipartPos();
    }

    protected void updateListeners() {
        BlockPos pos = this.getPos();
        BlockState multipartState = this.getWorld().getBlockState(pos);
        this.getWorld().updateListeners(this.getPos(), multipartState, multipartState, 3);
    }

    @Override
    public void onAdded(MultipartEventBus bus) {
        bus.addListener(this, NeighbourStateUpdateEvent.class, event -> {
            MultipartContainer container = this.holder.getContainer();
            World world = container.getMultipartWorld();
            if (!world.isClient) {
                BlockPos pos = container.getMultipartPos();
                if (!this.getClosestBlockState().canPlaceAt(world, pos)) {
                    this.breakPart();
                }
            }

            this.onNeighborUpdate(event.pos);
        });
    }

    protected void breakPart() {
        this.holder.remove(MultipartHolder.PartRemoval.DROP_ITEMS, MultipartHolder.PartRemoval.BREAK_PARTICLES, MultipartHolder.PartRemoval.BREAK_SOUND);
    }

    protected void onNeighborUpdate(BlockPos neighborPos) {
    }

    @Override
    public PartModelKey getModelKey() {
        return new BlockModelKey(this.getClosestBlockState());
    }

    @Override
    public ItemStack getPickStack(@Nullable BlockHitResult hitResult) {
        return new ItemStack(this.getClosestBlockState().getBlock());
    }

    @Override
    public void addDrops(ItemDropTarget target, LootContextParameterSet params) {
        target.dropAll(this.getClosestBlockState().getDroppedStacks(new LootContextParameterSet.Builder(params.getWorld())
                .luck(params.getLuck()) //
                .add(LootContextParameters.BLOCK_STATE, params.get(LootContextParameters.BLOCK_STATE)) //
                .add(LootContextParameters.ORIGIN, params.get(LootContextParameters.ORIGIN)) //
                .add(LootContextParameters.TOOL, params.get(LootContextParameters.TOOL)) //
                .addOptional(LootContextParameters.THIS_ENTITY, params.get(LootContextParameters.THIS_ENTITY)) //
                .addOptional(LootContextParameters.BLOCK_ENTITY, params.get(LootContextParameters.BLOCK_ENTITY)) //
                .addOptional(LootContextParameters.EXPLOSION_RADIUS, params.get(LootContextParameters.EXPLOSION_RADIUS))));
    }
}
