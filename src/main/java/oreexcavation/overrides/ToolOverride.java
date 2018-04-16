package oreexcavation.overrides;

import net.minecraft.item.ItemStack;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.groups.ItemEntry;

public class ToolOverride
{
	private ItemEntry itemType;
	
	private int speed;
	private int limit;
	private int range;
	private float exaustion;
	private int experience;
	
	public ToolOverride(ItemEntry itemType)
	{
		this.itemType = itemType;
		
		this.speed = ExcavationSettings.mineSpeed;
		this.limit = ExcavationSettings.mineLimit;
		this.range = ExcavationSettings.mineRange;
		this.exaustion = ExcavationSettings.exaustion;
		this.experience = ExcavationSettings.experience;
	}
	
	public boolean isApplicable(ItemStack stack)
	{
		if(stack == null)
		{
			return false;
		}
		
		return this.itemType.checkMatch(stack);
	}
	
	public void setSpeed(int value)
	{
		this.speed = value;
	}
	
	public int getSpeed()
	{
		return speed;
	}
	
	public void setLimit(int value)
	{
		this.limit = value;
	}
	
	public int getLimit()
	{
		return limit;
	}
	
	public void setRange(int value)
	{
		this.range = value;
	}
	
	public int getRange()
	{
		return range;
	}
	
	public void setExaustion(float value)
	{
		this.exaustion = value;
	}
	
	public float getExaustion()
	{
		return exaustion;
	}
	
	public void setExperience(int value)
	{
		this.experience = value;
	}
	
	public int getExperience()
	{
		return experience;
	}
	
	public static ToolOverride readFromString(String s)
	{
		ItemEntry entry = ItemEntry.readFromString(s);
		
		if(entry == null)
		{
			return null;
		}
		
		return new ToolOverride(entry);
	}
}
