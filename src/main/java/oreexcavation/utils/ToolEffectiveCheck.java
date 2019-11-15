package oreexcavation.utils;

import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import net.minecraftforge.common.ToolType;
import oreexcavation.core.ExcavationSettings;

public class ToolEffectiveCheck
{
	public static boolean canHarvestBlock(World world, BlockState state, BlockPos pos, PlayerEntity player)
	{
		if(world == null || state == null || pos == null || player == null)
		{
			return false;
		}
		
		ItemStack held = player.getHeldItem(Hand.MAIN_HAND);
		
		if(ExcavationSettings.openHand && held.isEmpty())
		{
			return state.getBlock().canHarvestBlock(state, world, pos, player);
		} else if(ExcavationSettings.toolClass)
		{
			if(held.isEmpty())
			{
				return false;
			}
			
			if(held.getItem() instanceof ShearsItem && state.getBlock() instanceof IShearable)
			{
				return true;
			}
			
			for(ToolType type : held.getItem().getToolTypes(held))
			{
				if(type == state.getBlock().getHarvestTool(state) && held.getItem().getHarvestLevel(held, type, player, state) >= state.getBlock().getHarvestLevel(state))
				{
					return true;
				} else if(state.getBlock().isToolEffective(state, type))
				{
					return true;
				}
			}
			
			return false;
		} else if(ExcavationSettings.altTools)
		{
			if(!held.isEmpty() && held.getDestroySpeed(state) > 1F)
			{
				return true;
			}
		}
		
		if(!held.isEmpty() && held.getItem() instanceof ShearsItem && state.getBlock() instanceof IShearable)
		{
			return true;
		}
		
		return state.getBlock().canHarvestBlock(state, world, pos, player);
	}
}
