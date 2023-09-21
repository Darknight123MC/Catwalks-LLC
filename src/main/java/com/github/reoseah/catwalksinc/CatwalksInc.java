package com.github.reoseah.catwalksinc;

import alexiil.mc.lib.multipart.api.MultipartContainer;
import alexiil.mc.lib.multipart.api.MultipartUtil;
import alexiil.mc.lib.multipart.api.NativeMultipart;
import alexiil.mc.lib.multipart.api.render.PartStaticModelRegisterEvent;
import alexiil.mc.lib.multipart.impl.LibMultiPart;
import com.github.reoseah.catwalksinc.block.*;
import com.github.reoseah.catwalksinc.item.WrenchItem;
import com.github.reoseah.catwalksinc.part.*;
import net.fabricmc.api.*;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;

@EnvironmentInterface(value = EnvType.CLIENT, itf = ClientModInitializer.class)
public class CatwalksInc implements ModInitializer, ClientModInitializer {
    public static final String ID = "catwalksinc";

    public static final Item IRON_ROD = new Item(new FabricItemSettings());

    public static final ItemGroup ITEM_GROUP = FabricItemGroup.builder().
            displayName(Text.translatable("itemGroup.catwalksinc.main"))
            .icon(() -> new ItemStack(CatwalkBlock.ITEM))
            .entries((displayContext, entries) -> {
                entries.add(CatwalkBlock.ITEM);
                entries.add(CageLampBlock.ITEM);
                entries.add(CrankWheelBlock.ITEM);
                entries.add(WrenchItem.INSTANCE);
                entries.add(IRON_ROD);
            })
            .build();

    public static Identifier id(String name) {
        return new Identifier(ID, name);
    }

    @Override
    public void onInitialize() {
        Registry.register(Registries.BLOCK, "catwalksinc:catwalk", CatwalkBlock.INSTANCE);
        Registry.register(Registries.ITEM, "catwalksinc:catwalk", CatwalkBlock.ITEM);
        CatwalkPart.DEFINITION.register();

        Registry.register(Registries.BLOCK, "catwalksinc:catwalk_stairs", CatwalkStairsBlock.INSTANCE);
        Registry.register(Registries.BLOCK_ENTITY_TYPE, "catwalksinc:catwalk_stairs", CatwalkStairsBlockEntity.TYPE);

        Registry.register(Registries.BLOCK, "catwalksinc:caged_ladder", CagedLadderBlock.INSTANCE);

        Registry.register(Registries.BLOCK, "catwalksinc:cage_lamp", CageLampBlock.INSTANCE);
        Registry.register(Registries.ITEM, "catwalksinc:cage_lamp", CageLampBlock.ITEM);
        CageLampPart.DEFINITION.register();

        Registry.register(Registries.BLOCK, "catwalksinc:crank_wheel", CrankWheelBlock.INSTANCE);
        Registry.register(Registries.ITEM, "catwalksinc:crank_wheel", CrankWheelBlock.ITEM);
        CrankWheelPart.DEFINITION.register();

        Registry.register(Registries.ITEM, "catwalksinc:wrench", WrenchItem.INSTANCE);
        Registry.register(Registries.SOUND_EVENT, "catwalksinc:wrench_use", WrenchItem.USE_SOUND);

        Registry.register(Registries.ITEM, "catwalksinc:iron_rod", IRON_ROD);

        Registry.register(Registries.ITEM_GROUP, id("main"), ITEM_GROUP);

        UseBlockCallback.EVENT.register(CatwalksInc::interact);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void onInitializeClient() {
        BlockRenderLayerMap.INSTANCE.putBlock(CatwalkBlock.INSTANCE, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(CatwalkStairsBlock.INSTANCE, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(CagedLadderBlock.INSTANCE, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(CageLampBlock.INSTANCE, RenderLayer.getCutoutMipped());
        BlockRenderLayerMap.INSTANCE.putBlock(CrankWheelBlock.INSTANCE, RenderLayer.getCutoutMipped());

        PartStaticModelRegisterEvent.EVENT.register(renderer -> renderer.register(BlockModelKey.class, new BlockPartModel()));

        WorldRenderEvents.BLOCK_OUTLINE.register(CatwalksInc::onBlockOutline);
    }

    private static ActionResult interact(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (!player.canModifyBlocks()) {
            return ActionResult.PASS;
        }
        ItemStack stack = player.getStackInHand(hand);
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            BlockPos pos = hitResult.getBlockPos().offset(hitResult.getSide());
            ItemPlacementContext placementCtx = new ItemPlacementContext(new ItemUsageContext(player, hand, hitResult));
            BlockState currentState = world.getBlockState(pos);
            if (currentState.canReplace(placementCtx) && !currentState.isOf(LibMultiPart.BLOCK) && !(currentState.getBlock() instanceof NativeMultipart) && NativeMultipart.LOOKUP.getProvider(currentState.getBlock()) == null) {
                // just place normally
                return ActionResult.PASS;
            }
            // try placing as multipart
            BlockState placementState = block.getPlacementState(placementCtx);
            if (placementState != null && placementState.canPlaceAt(world, pos)) {
                if (block instanceof NativeMultipart nativeMultipart && block instanceof CatwalksIncBlock) {
                    boolean success = false;
                    for (MultipartContainer.MultipartCreator creator : nativeMultipart.getMultipartConversion(world, pos, placementState)) {
                        MultipartContainer.PartOffer offer = MultipartUtil.offerNewPart(world, pos, creator);
                        if (offer != null) {
                            if (!world.isClient) {
                                offer.apply();
                            }
                            success = true;
                        }
                    }
                    if (success) {
                        if (!player.getAbilities().creativeMode) {
                            stack.decrement(1);
                        }
                        BlockSoundGroup sounds = block.getDefaultState().getSoundGroup();
                        world.playSound(player, pos, sounds.getPlaceSound(), SoundCategory.BLOCKS, (sounds.getVolume() + 1f) / 2f, sounds.getPitch() * 0.8f);
                        return ActionResult.SUCCESS;
                    }
                }
            }
        }
        return ActionResult.PASS;
    }

    @Environment(EnvType.CLIENT)
    private static boolean onBlockOutline(WorldRenderContext wrc, WorldRenderContext.BlockOutlineContext boc) {
        BlockState state = boc.blockState();
        Block block = state.getBlock();
        if (MinecraftClient.getInstance().player.getMainHandStack().isIn(WrenchItem.COMPATIBILITY_TAG)) {
            wrc.matrixStack().push();

            BlockPos pos = boc.blockPos();
            double x = pos.getX() - boc.cameraX();
            double y = pos.getY() - boc.cameraY();
            double z = pos.getZ() - boc.cameraZ();

            wrc.matrixStack().translate(x, y, z);

            VertexConsumer lines = wrc.consumers().getBuffer(RenderLayer.getLines());

            if (block instanceof CatwalkBlock) {
                drawLine(wrc.matrixStack(), lines, 0, 1 / 16F, 0, 1, 1 / 16F, 1);
                drawLine(wrc.matrixStack(), lines, 0, 1 / 16F, 1, 1, 1 / 16F, 0);
            } else if (block instanceof CatwalkStairsBlock) {
                Direction facing = state.get(CatwalkStairsBlock.FACING);

                wrc.matrixStack().translate(0.5F, 0, 0.5F);
                wrc.matrixStack().multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(180 + facing.asRotation()));
                wrc.matrixStack().translate(-0.5F, 0, -0.5F);

                if (state.get(CatwalkStairsBlock.HALF) == DoubleBlockHalf.UPPER) {
                    wrc.matrixStack().translate(0, -1, 0);
                }

                drawLine(wrc.matrixStack(), lines, 0.5F, 9 / 16F, 0, 0.5F, 9 / 16F, 0.5F);
                drawLine(wrc.matrixStack(), lines, 0.5F, 17 / 16F, 0.5F, 0.5F, 17 / 16F, 1);
            } else {
                MultipartContainer container = MultipartUtil.get(wrc.world(), boc.blockPos());
                if (container != null && container.getFirstPart(CatwalkPart.class) != null) {
                    drawLine(wrc.matrixStack(), lines, 0, 1 / 16F, 0, 1, 1 / 16F, 1);
                    drawLine(wrc.matrixStack(), lines, 0, 1 / 16F, 1, 1, 1 / 16F, 0);
                }
            }
            wrc.matrixStack().pop();
        }
        return true;
    }

    @Environment(EnvType.CLIENT)
    private static void drawLine(MatrixStack matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2) {
        MatrixStack.Entry entry = matrices.peek();

        float dx = x2 - x1;
        float dy = y2 - y1;
        float dz = z2 - z1;
        float d = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);

        vertexConsumer.vertex(entry.getPositionMatrix(), x1, y1, z1).color(0, 0, 0, 0.4F).normal(entry.getNormalMatrix(), dx /= d, dy /= d, dz /= d).next();
        vertexConsumer.vertex(entry.getPositionMatrix(), x2, y2, z2).color(0, 0, 0, 0.4F).normal(entry.getNormalMatrix(), dx, dy, dz).next();
    }
}
