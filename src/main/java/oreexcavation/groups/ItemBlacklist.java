package oreexcavation.groups;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.item.ItemStack;

public class ItemBlacklist
{
	public static final ItemBlacklist INSTANCE = new ItemBlacklist();
	
	private final List<ItemEntry> banList = new ArrayList<ItemEntry>();
	
	private ItemBlacklist()
	{
	}
	
	public boolean isBanned(ItemStack stack)
	{
		for(ItemEntry entry : banList)
		{
			if(entry.checkMatch(stack))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void loadList(String[] list)
	{
		banList.clear();
		
		for(String s : list)
		{
			ItemEntry entry = ItemEntry.readFromString(s);
			
			if(entry != null)
			{
				banList.add(entry);
			}
		}
	}
}
