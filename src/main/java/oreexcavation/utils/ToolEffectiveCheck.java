package oreexcavation.utils;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.IShearable;
import oreexcavation.core.ExcavationSettings;

public class ToolEffectiveCheck
{
	public static boolean canHarvestBlock(World world, Block block, int metadata, BlockPos pos, EntityPlayer player)
	{
		if(world == null || block == null || pos == null || player == null)
		{
			return false;
		}
		
		ItemStack held = player.getHeldItem();
		
		if(ExcavationSettings.openHand && held == null)
		{
			return block.canHarvestBlock(player, metadata);
		} else if(ExcavationSettings.toolClass)
		{
			if(held == null)
			{
				return false;
			}
			
			if(held.getItem() instanceof ItemShears && block instanceof IShearable)
			{
				return true;
			}
			
			for(String type : held.getItem().getToolClasses(held))
			{
				if(type.equalsIgnoreCase(block.getHarvestTool(metadata)) && held.getItem().getHarvestLevel(held, type) >= block.getHarvestLevel(metadata))
				{
					return true;
				} else if(block.isToolEffective(type, metadata)) // Demoted to a fallback check because it's dumb with redstone/obsidian
				{
					return true;
				}
			}
			
			return false;
		} else if(ExcavationSettings.altTools)
		{
			if(held != null && held.getItem().func_150893_a(held, block) > 1F)
			{
				return true;
			}
		}
		
		if(held != null && held.getItem() instanceof ItemShears && block instanceof IShearable)
		{
			return true;
		}
		
		return block.canHarvestBlock(player, metadata);
	}
}
