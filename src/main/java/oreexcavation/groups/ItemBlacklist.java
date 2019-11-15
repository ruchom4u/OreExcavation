package oreexcavation.groups;

import net.minecraft.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class ItemBlacklist
{
	public static final ItemBlacklist INSTANCE = new ItemBlacklist();
	
	private final Set<ItemEntry> banList = new HashSet<>();
	
	public boolean isBanned(ItemStack stack)
	{
	    return banList.parallelStream().anyMatch((val) -> val.checkMatch(stack));
	}
	
	public void loadList(String[] list)
	{
		banList.clear();
		for(String s : list)
		{
			ItemEntry entry = ItemEntry.readFromString(s);
			if(entry != null) banList.add(entry);
		}
	}
}
