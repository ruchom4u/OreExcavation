package oreexcavation.groups;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import oreexcavation.core.OreExcavation;
import oreexcavation.utils.TagIngredient;
import org.apache.logging.log4j.Level;

@SuppressWarnings("WeakerAccess")
public class ItemEntry
{
    public final boolean isWildcard;
	public final ResourceLocation idName;
	public final TagIngredient tagIng;
	
	public ItemEntry()
    {
        this.idName = null;
        this.tagIng = null;
        this.isWildcard = true;
    }
	
    public ItemEntry(ResourceLocation idName)
	{
		this.idName = idName;
		this.tagIng = null;
		this.isWildcard = false;
	}
	
	public ItemEntry(String tagName)
	{
		this.tagIng = StringUtils.isNullOrEmpty(tagName) ? null : new TagIngredient(tagName);
		this.idName = null;
		this.isWildcard = false;
	}
	
	public boolean checkMatch(ItemStack stack)
	{
		if(stack == null || stack.isEmpty()) return false;
		if(isWildcard) return true;
		if(tagIng != null && tagIng.apply(stack)) return true;
		return idName != null && idName.equals(stack.getItem().getRegistryName());
	}
	
	public static ItemEntry readFromString(String s)
	{
		if(s == null || s.length() <= 0) return null;
		if(s.equalsIgnoreCase("*")) return new ItemEntry();
		
		String[] split = s.split(":");
		
		if(split.length <= 1 || split.length > 3) // Invalid
		{
            OreExcavation.logger.log(Level.WARN, "Invalid Item Entry format: " + s);
			return null;
		} else if(split.length == 2) // Simple ID
		{
			return new ItemEntry(new ResourceLocation(split[0], split[1]));
		} else if(s.startsWith("tag:"))
		{
			return new ItemEntry(s.replaceFirst("tag:", ""));
		}
		
        OreExcavation.logger.log(Level.WARN, "Invalid Item Entry format: " + s);
		return null;
	}
}
