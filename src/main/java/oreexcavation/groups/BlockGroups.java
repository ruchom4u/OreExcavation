package oreexcavation.groups;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.block.state.IBlockState;
import oreexcavation.utils.JsonHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class BlockGroups
{
	public static final BlockGroups INSTANCE = new BlockGroups();
	
	private final Map<BlockEntry, String> staged = new HashMap<>();
	private final List<List<BlockEntry>> groupList = new ArrayList<>();
	private final List<BlockEntry> strictSubs = new ArrayList<>();
	
	private BlockGroups()
	{
	}
	
	public String getStage(IBlockState state)
	{
		for(Entry<BlockEntry, String> entry : staged.entrySet())
		{
			if(entry.getKey().checkMatch(state))
			{
				return entry.getValue();
			}
		}
		
		return null;
	}
	
	public List<BlockEntry> getGroup(IBlockState state)
	{
		List<BlockEntry> list = new ArrayList<>();
		
		for(List<BlockEntry> l2 : groupList)
		{
			for(BlockEntry e : l2)
			{
				if(e != null && e.checkMatch(state))
				{
					list.addAll(l2);
					break;
				}
			}
		}
		
		return list;
	}
	
	public boolean isStrict(IBlockState state)
	{
		return quickCheck(strictSubs, state);
	}
	
	public boolean quickCheck(List<BlockEntry> list, IBlockState state)
	{
		if(list == null || state == null)
		{
			return false;
		}
		
		for(BlockEntry e : list)
		{
			if(e != null && e.checkMatch(state))
			{
				return true;
			}
		}
		
		return false;
	}
	
	public void readFromJson(JsonObject json)
	{
		groupList.clear();
		
		for(JsonElement je1 : JsonHelper.GetArray(json, "blockGroups"))
		{
			if(je1 == null || !je1.isJsonArray())
			{
				continue;
			}
			
			List<BlockEntry> list = new ArrayList<>();
			
			for(JsonElement je2 : je1.getAsJsonArray())
			{
				if(je2 == null || !je2.isJsonPrimitive())
				{
					continue;
				}
				
				BlockEntry entry = BlockEntry.readFromString(je2.getAsString());
				
				if(entry != null)
				{
					list.add(entry);
				}
			}
			
			if(list.size() > 0)
			{
				groupList.add(list);
			}
		}
		
		strictSubs.clear();
		
		for(JsonElement je1 : JsonHelper.GetArray(json, "strictSubtypes"))
		{
			if(je1 == null || !je1.isJsonPrimitive())
			{
				continue;
			}
			
			BlockEntry entry = BlockEntry.readFromString(je1.getAsString());
			
			if(entry != null)
			{
				strictSubs.add(entry);
			}
		}
		
		staged.clear();
		
		for(Entry<String, JsonElement> entry : JsonHelper.GetObject(json, "gamestages").entrySet())
		{
			if(entry.getValue() == null || !entry.getValue().isJsonPrimitive())
			{
				continue;
			}
			
			BlockEntry be = BlockEntry.readFromString(entry.getKey());
			
			if(be != null)
			{
				staged.put(be, entry.getValue().getAsString());
			}
		}
	}
	
	public JsonObject getDefaultJson()
	{
		JsonObject json = new JsonObject();
		
		JsonArray ary1 = new JsonArray();
		
		JsonArray ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("treeLeaves"));
		ary1.add(ary2);
		
		ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("logWood"));
		ary1.add(ary2);
		
		json.add("blockGroups", ary1);
		
		ary1 = new JsonArray();
		ary1.add(new JsonPrimitive("cropWheat"));
		ary1.add(new JsonPrimitive("cropPotato"));
		ary1.add(new JsonPrimitive("cropCarrot"));
		ary1.add(new JsonPrimitive("cropNetherWart"));
		json.add("strictSubtypes", ary1);
		
		JsonObject obj2 = new JsonObject();
		obj2.addProperty("examplemod:example_block1", "example_stage1");
		obj2.addProperty("examplemod:example_block2", "example_stage2");
		json.add("gamestages", obj2);
		
		return json;
	}
}
