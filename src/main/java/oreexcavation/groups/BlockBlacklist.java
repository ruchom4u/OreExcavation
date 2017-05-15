package oreexcavation.groups;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.Block;

public class BlockBlacklist
{
	public static final BlockBlacklist INSTANCE = new BlockBlacklist();
	
	private final List<BlockEntry> banList = new ArrayList<BlockEntry>();
	
	private BlockBlacklist()
	{
	}
	
	public boolean isBanned(Block block, int metadata)
	{
		for(BlockEntry entry : banList)
		{
			if(entry.checkMatch(block, metadata))
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
			BlockEntry entry = BlockEntry.readFromString(s);
			
			if(entry != null)
			{
				banList.add(entry);
			}
		}
	}
}
