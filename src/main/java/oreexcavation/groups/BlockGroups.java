package oreexcavation.groups;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.block.BlockState;
import oreexcavation.utils.JsonHelper;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BlockGroups
{
	public static final BlockGroups INSTANCE = new BlockGroups();
	
	private final Map<BlockEntry, String> staged = new HashMap<>();
	private final List<List<BlockEntry>> groupList = new ArrayList<>();
	private final List<BlockEntry> strictSubs = new ArrayList<>();
	
	public String getStage(BlockState state)
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
	
	public List<BlockEntry> getGroup(BlockState state)
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
	
	public boolean isStrict(@Nonnull BlockState state)
	{
		return quickCheck(strictSubs, state);
	}
	
	public boolean quickCheck(@Nonnull List<BlockEntry> list, @Nonnull BlockState state)
	{
	    return list.parallelStream().anyMatch(((val) -> val.checkMatch(state)));
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
				if(je2 == null || !je2.isJsonPrimitive()) continue;
				
				BlockEntry entry = BlockEntry.readFromString(je2.getAsString());
				if(entry != null) list.add(entry);
			}
			
			if(list.size() > 0) groupList.add(list);
		}
		
		strictSubs.clear();
		
		for(JsonElement je1 : JsonHelper.GetArray(json, "strictSubtypes"))
		{
			if(je1 == null || !je1.isJsonPrimitive()) continue;
			
			BlockEntry entry = BlockEntry.readFromString(je1.getAsString());
			if(entry != null) strictSubs.add(entry);
		}
		
		staged.clear();
		
		for(Entry<String, JsonElement> entry : JsonHelper.GetObject(json, "gamestages").entrySet())
		{
			if(entry.getValue() == null || !entry.getValue().isJsonPrimitive()) continue;
			
			BlockEntry be = BlockEntry.readFromString(entry.getKey());
			if(be != null) staged.put(be, entry.getValue().getAsString());
		}
	}
	
	public JsonObject getDefaultJson()
	{
		JsonObject json = new JsonObject();
		
		JsonArray ary1 = new JsonArray();
		
		JsonArray ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("tag:minecraft:leaves"));
		ary1.add(ary2);
		
		ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("tag:minecraft:dirt_like"));
		ary1.add(ary2);
		
		ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("tag:minecraft:logs"));
		ary1.add(ary2);
		
		ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("tag:minecraft:flower_pots"));
		ary1.add(ary2);
		
		ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("minecraft:stone"));
		ary2.add(new JsonPrimitive("minecraft:infested_stone"));
		ary2.add(new JsonPrimitive("minecraft:granite"));
		ary2.add(new JsonPrimitive("minecraft:diorite"));
		ary2.add(new JsonPrimitive("minecraft:andesite"));
		ary1.add(ary2);
		
		ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("minecraft:cobblestone"));
		ary2.add(new JsonPrimitive("minecraft:infested_cobblestone"));
		ary1.add(ary2);
		
		ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("minecraft:stone_bricks"));
		ary2.add(new JsonPrimitive("minecraft:infested_stone_bricks"));
		ary1.add(ary2);
		
		ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("minecraft:cracked_stone_bricks"));
		ary2.add(new JsonPrimitive("minecraft:infested_cracked_stone_bricks"));
		ary1.add(ary2);
		
		ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("minecraft:mossy_stone_bricks"));
		ary2.add(new JsonPrimitive("minecraft:infested_mossy_stone_bricks"));
		ary1.add(ary2);
		
		ary2 = new JsonArray();
		ary2.add(new JsonPrimitive("minecraft:chiseled_stone_bricks"));
		ary2.add(new JsonPrimitive("minecraft:infested_chiseled_stone_bricks"));
		ary1.add(ary2);
		
		json.add("blockGroups", ary1);
		
		ary1 = new JsonArray();
		ary1.add(new JsonPrimitive("tag:minecraft:crops")); // NOTE: Not actually valid until 1.15 but included for any mods using it early
		ary1.add(new JsonPrimitive("minecraft:beetroots"));
		ary1.add(new JsonPrimitive("minecraft:carrots"));
		ary1.add(new JsonPrimitive("minecraft:potatoes"));
		ary1.add(new JsonPrimitive("minecraft:wheat"));
		ary1.add(new JsonPrimitive("minecraft:melon_stem"));
		ary1.add(new JsonPrimitive("minecraft:pumpkin_stem"));
		json.add("strictSubtypes", ary1);
		
		JsonObject obj2 = new JsonObject();
		obj2.addProperty("examplemod:example_block1", "example_stage1");
		obj2.addProperty("examplemod:example_block2", "example_stage2");
		json.add("gamestages", obj2);
		
		return json;
	}
}
