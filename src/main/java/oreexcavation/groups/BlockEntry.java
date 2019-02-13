package oreexcavation.groups;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.oredict.OreDictionary;
import oreexcavation.core.OreExcavation;
import oreexcavation.utils.OreIngredient;
import org.apache.logging.log4j.Level;

public class BlockEntry
{
	public final ResourceLocation idName;
	public final OreIngredient oreDict;
	public final int subType;
	
	public BlockEntry(ResourceLocation idName, int subType)
	{
		this.idName = idName;
		this.subType = subType;
		
		this.oreDict = null;
	}
	
	public BlockEntry(String oreDict)
	{
		this.oreDict = StringUtils.isNullOrEmpty(oreDict) ? null : new OreIngredient(oreDict);
		
		this.idName = null;
		this.subType = -1;
	}
	
	public boolean checkMatch(IBlockState state)
	{
		return state != null && checkMatch(state.getBlock(), state.getBlock().getMetaFromState(state));
	}
	
	public boolean checkMatch(Block block, int metadata)
	{
		if(block == null || block == Blocks.AIR)
		{
			return false;
		} else if(idName == null)
		{
			return checkOre(block, metadata);
		}
		
		return idName.equals(block.getRegistryName()) && (subType < 0 || subType == OreDictionary.WILDCARD_VALUE || subType == metadata);
	}
	
	private boolean checkOre(Block block, int metadata)
	{
	    Item iBlock = Item.getItemFromBlock(block);
	    return oreDict != null && iBlock != null && oreDict.apply(new ItemStack(iBlock, 1, metadata));
	}
	
	public static BlockEntry readFromString(String s)
	{
		if(s == null || s.length() <= 0) return null;
		
		String[] split = s.split(":");
		
		if(split.length <= 0 || split.length > 3) // Invalid
		{
			return null;
		} else if(split.length == 1) // Ore Dictionary
		{
			return new BlockEntry(split[0]);
		} else if(split.length == 2) // Simple ID
		{
			return new BlockEntry(new ResourceLocation(split[0], split[1]), -1);
		} else // ID and Subtype
		{
			try
			{
				return new BlockEntry(new ResourceLocation(split[0], split[1]), Integer.parseInt(split[2]));
			} catch(Exception e)
            {
                OreExcavation.logger.log(Level.ERROR, "Unable to read metadata value for Block Entry \"" + s + "\":", e);
                return null;
            }
		}
	}
}
