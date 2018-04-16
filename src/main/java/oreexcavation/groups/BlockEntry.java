package oreexcavation.groups;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import oreexcavation.core.OreExcavation;
import org.apache.logging.log4j.Level;

public class BlockEntry
{
	public final ResourceLocation idName;
	public final String oreDict;
	public final int subType;
	
	public BlockEntry(ResourceLocation idName, int subType)
	{
		this.idName = idName;
		this.subType = subType;
		
		this.oreDict = null;
	}
	
	public BlockEntry(String oreDict)
	{
		this.oreDict = oreDict;
		
		this.idName = null;
		this.subType = -1;
	}
	
	public boolean checkMatch(IBlockState state)
	{
		if(state == null)
		{
			return false;
		}
		
		return checkMatch(state.getBlock(), state.getBlock().getMetaFromState(state));
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
		
		ResourceLocation r = Block.REGISTRY.getNameForObject(block);
		
		return r.equals(idName) && (subType < 0 || subType == OreDictionary.WILDCARD_VALUE || subType == metadata);
	}
	
	private boolean checkOre(Block block, int metadata)
	{
		if(oreDict == null)
		{
			return false;
		} else if(oreDict.equals("*"))
		{
			return true; // For the morbidly curious
		}
		
		Item itemBlock = Item.getItemFromBlock(block);
		
		if(itemBlock == null)
		{
			return false;
		}
		
		for(int id : OreDictionary.getOreIDs(new ItemStack(block)))
		{
			if(OreDictionary.getOreName(id).equals(oreDict))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public static BlockEntry readFromString(String s)
	{
		if(s == null || s.length() <= 0)
		{
			return null;
		}
		
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
			int meta;
			
			try
			{
				meta = Integer.parseInt(split[2]);
			} catch(Exception e)
			{
				OreExcavation.logger.log(Level.ERROR, "Unable to read metadata value for Block Entry \"" + s + "\":", e);
				return null;
			}
			
			return new BlockEntry(new ResourceLocation(split[0], split[1]), meta);
		}
	}
}
