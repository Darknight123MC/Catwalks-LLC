package com.github.reoseah.catwalksinc.blocks;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.github.reoseah.catwalksinc.CIBlocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

public class PaintedCatwalkBlock extends CatwalkBlock implements PaintScrapableBlock {
	protected static final Map<DyeColor, Block> INSTANCES = new EnumMap<>(DyeColor.class);

	protected final DyeColor color;

	public PaintedCatwalkBlock(DyeColor color, Block.Settings settings) {
		super(settings);
		this.color = color;
		INSTANCES.put(color, this);
	}

	public static Block ofColor(DyeColor color) {
		return INSTANCES.get(color);
	}

	@Override
	public String getTranslationKey() {
		return CIBlocks.CATWALK.getTranslationKey();
	}

	@Override
	public void appendTooltip(ItemStack stack, BlockView world, List<Text> tooltip, TooltipContext options) {
		super.appendTooltip(stack, world, tooltip, options);
		tooltip.add(new TranslatableText("misc.catwalksinc." + this.color.asString()).formatted(Formatting.GRAY));
	}

	@Override
	protected BlockState convertToStairs(Direction facing) {
		return PaintedCatwalkStairsBlock.ofColor(this.color).getDefaultState() //
				.with(CatwalkStairsBlock.FACING, facing.getOpposite());
	}

	@Override
	public boolean canPaintBlock(DyeColor color, BlockState state, BlockView world, BlockPos pos) {
		return false;
	}

	@Override
	public void scrapPaint(BlockState state, WorldAccess world, BlockPos pos) {
		world.setBlockState(pos, CIBlocks.CATWALK.getDefaultState() //
				.with(NORTH_RAIL, state.get(NORTH_RAIL)) //
				.with(SOUTH_RAIL, state.get(SOUTH_RAIL)) //
				.with(WEST_RAIL, state.get(WEST_RAIL)) //
				.with(EAST_RAIL, state.get(EAST_RAIL)) //
				.with(WATERLOGGED, state.get(WATERLOGGED)), //
				3);
	}
}
