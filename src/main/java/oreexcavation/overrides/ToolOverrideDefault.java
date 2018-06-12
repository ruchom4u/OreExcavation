package oreexcavation.overrides;

import oreexcavation.core.ExcavationSettings;
import oreexcavation.groups.ItemEntry;

public class ToolOverrideDefault extends ToolOverride
{
	public static final ToolOverride DEFAULT = new ToolOverrideDefault();
	
	private ToolOverrideDefault()
	{
		super(ItemEntry.readFromString("*"));
	}
	
	@Override
	public void setSpeed(int value)
	{
	}
	
	@Override
	public int getSpeed()
	{
		return ExcavationSettings.mineSpeed;
	}
	
	@Override
	public void setLimit(int value)
	{
	}
	
	@Override
	public int getLimit()
	{
		return ExcavationSettings.mineLimit;
	}
	
	@Override
	public void setRange(int value)
	{
	}
	
	@Override
	public int getRange()
	{
		return ExcavationSettings.mineRange;
	}
	
	@Override
	public void setExaustion(float value)
	{
	}
	
	@Override
	public float getExaustion()
	{
		return ExcavationSettings.exaustion;
	}
	
	@Override
	public void setExperience(int value)
	{
	}
	
	@Override
	public int getExperience()
	{
		return ExcavationSettings.experience;
	}
	
	@Override
	public String getGameStage()
	{
		return ExcavationSettings.gamestage;
	}
}
