package oreexcavation.groups;

import net.minecraft.block.BlockState;

import java.util.HashSet;
import java.util.Set;

public class BlockBlacklist
{
	public static final BlockBlacklist INSTANCE = new BlockBlacklist();
	
	private final Set<BlockEntry> banList = new HashSet<>();
	
	public boolean isBanned(BlockState state)
	{
	    return banList.parallelStream().anyMatch((val) -> val.checkMatch(state));
	}
	
	public void loadList(String[] list)
	{
		banList.clear();
		for(String s : list)
		{
			BlockEntry entry = BlockEntry.readFromString(s);
			if(entry != null) banList.add(entry);
		}
	}
}
