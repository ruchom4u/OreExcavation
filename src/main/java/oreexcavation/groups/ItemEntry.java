package oreexcavation.groups;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.oredict.OreDictionary;
import oreexcavation.core.OreExcavation;
import org.apache.logging.log4j.Level;

public class ItemEntry
{
	public final ResourceLocation idName;
	public final String oreDict;
	public final int subType;
	
	public ItemEntry(ResourceLocation idName, int subType)
	{
		this.idName = idName;
		this.subType = subType;
		
		this.oreDict = null;
	}
	
	public ItemEntry(String oreDict)
	{
		this.oreDict = oreDict;
		
		this.idName = null;
		this.subType = -1;
	}
	
	public boolean checkMatch(ItemStack stack)
	{
		if(stack == null)
		{
			return false;
		} else if(idName == null)
		{
			return checkOre(stack);
		}
		
		ResourceLocation r = Item.REGISTRY.getNameForObject(stack.getItem());
		
		return r != null && r.equals(idName) && (subType < 0 || subType == OreDictionary.WILDCARD_VALUE || subType == stack.getMetadata());
	}
	
	private boolean checkOre(ItemStack stack)
	{
		if(oreDict == null)
		{
			return false;
		} else if(oreDict.equals("*"))
		{
			return true;
		}
		
		for(int id : OreDictionary.getOreIDs(stack))
		{
			if(OreDictionary.getOreName(id).equals(oreDict))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public static ItemEntry readFromString(String s)
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
			return new ItemEntry(split[0]);
		} else if(split.length == 2) // Simple ID
		{
			return new ItemEntry(new ResourceLocation(split[0], split[1]), -1);
		} else // ID and Subtype
		{
			int meta;
			
			try
			{
				meta = Integer.parseInt(split[2]);
			} catch(Exception e)
			{
				OreExcavation.logger.log(Level.ERROR, "Unable to read metadata value for Item Entry \"" + s + "\":", e);
				return null;
			}
			
			return new ItemEntry(new ResourceLocation(split[0], split[1]), meta);
		}
	}
}
