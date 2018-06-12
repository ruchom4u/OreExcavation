package oreexcavation.overrides;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.utils.JsonHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ToolOverrideHandler
{
	public static ToolOverrideHandler INSTANCE = new ToolOverrideHandler();
	private List<ToolOverride> list = new ArrayList<>();
	
	private ToolOverrideHandler()
	{
	}
	
	public void loadOverrides(JsonObject json)
	{
		list.clear();
		for(JsonElement je : JsonHelper.GetArray(json, "overrides"))
		{
			if(je == null || !je.isJsonObject())
			{
				continue;
			}
			
			JsonObject jo = je.getAsJsonObject();
			ToolOverride to = ToolOverride.readFromString(JsonHelper.GetString(jo, "itemID", ""));
			
			if(to == null)
			{
				continue;
			}
			
			to.setSpeed(JsonHelper.GetNumber(jo, "speed", ExcavationSettings.mineSpeed).intValue());
			to.setLimit(JsonHelper.GetNumber(jo, "limit", ExcavationSettings.mineLimit).intValue());
			to.setRange(JsonHelper.GetNumber(jo, "range", ExcavationSettings.mineRange).intValue());
			to.setExaustion(JsonHelper.GetNumber(jo, "exaustion", ExcavationSettings.exaustion).floatValue());
			to.setExperience(JsonHelper.GetNumber(jo, "experience", ExcavationSettings.experience).intValue());
			to.setGameStage(JsonHelper.GetString(jo, "gamestage", ExcavationSettings.gamestage));
			list.add(to);
		}
	}
	
	public ToolOverride getOverride(ItemStack stack)
	{
		for(ToolOverride o : list)
		{
			if(o.isApplicable(stack))
			{
				return o;
			}
		}
		
		return null;
	}
	
	public JsonObject getDefaultOverrides()
	{
		JsonObject json = new JsonObject();
		JsonArray jAry = new JsonArray();
		
		JsonObject jo = new JsonObject();
		jo.addProperty("itemID", "examplemod:nerfed_pickaxe:0");
		jo.addProperty("speed", 1);
		jo.addProperty("limit", 0);
		jo.addProperty("range", 0);
		jo.addProperty("exaustion", 0.1F);
		jo.addProperty("experience", 0);
		jo.addProperty("gamestage", "");
		jAry.add(jo);
		
		json.add("overrides", jAry);
		return json;
	}
}
