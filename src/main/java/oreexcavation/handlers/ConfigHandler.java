package oreexcavation.handlers;

import com.google.gson.JsonObject;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.config.ModConfig.ConfigReloading;
import net.minecraftforge.fml.config.ModConfig.Loading;
import net.minecraftforge.fml.config.ModConfig.Type;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.groups.BlockBlacklist;
import oreexcavation.groups.BlockGroups;
import oreexcavation.groups.ItemBlacklist;
import oreexcavation.overrides.ToolOverrideHandler;
import oreexcavation.shapes.ShapeRegistry;
import oreexcavation.utils.JsonHelper;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"WeakerAccess", "SameParameterValue", "unused"})
public class ConfigHandler
{
    public static final ForgeConfigSpec commonSpec;
    public static final ForgeConfigSpec clientSpec;
    
    public static final ConfigCommon COMMON;
    public static final ConfigClient CLIENT;
    
    static
    {
        final Pair<ConfigClient, ForgeConfigSpec> clientSpecPair = new ForgeConfigSpec.Builder().configure(ConfigClient::new);
        clientSpec = clientSpecPair.getRight();
        CLIENT = clientSpecPair.getLeft();
        
        final Pair<ConfigCommon, ForgeConfigSpec> commonSpecPair = new ForgeConfigSpec.Builder().configure(ConfigCommon::new);
        commonSpec = commonSpecPair.getRight();
        COMMON = commonSpecPair.getLeft();
    }
    
    public static class ConfigCommon
    {
        public final IntValue mineSpeed;
        public final IntValue mineLimit;
        public final IntValue mineRange;
        public final DoubleValue exaustion;
        public final IntValue experience;
        public final IntValue maxUndos;
        
        public final BooleanValue invertTBlacklist;
        public final BooleanValue invertBBlacklist;
        
        public final ConfigValue<List<? extends String>> toolBlacklist;
        public final ConfigValue<List<? extends String>> blockBlacklist;
        
        public final BooleanValue openHand;
        public final BooleanValue ignoreTools;
        public final BooleanValue toolClass;
        public final BooleanValue altTools;
        public final BooleanValue tpsGuard;
        public final BooleanValue autoPickup;
        public final BooleanValue allowShapes;
        
        public final ConfigValue<String> gamestage;
        
        private ConfigCommon(Builder builder)
        {
            builder.comment("Common settings").push("common");
            
            mineLimit = lazyInt(builder, "Limit", 128, 1, Integer.MAX_VALUE, "The maximum number of blocks that can be excavated at once");
            mineSpeed = lazyInt(builder, "Speed", 64, 1, Integer.MAX_VALUE, "How many blocks per tick can be excavated");
            mineRange = lazyInt(builder, "Range", 16, 1, Integer.MAX_VALUE, "How far from the origin an excavation can travel");
            exaustion = lazyDouble(builder, "Exaustion", 0.1D, 0D, Double.MAX_VALUE, "Amount of exaustion per block excavated");
            experience = lazyInt(builder, "Experience", 0, 0, Integer.MAX_VALUE, "Experience cost per block excavated");
            openHand = lazyBool(builder, "Open Hand", true, "Allow excavation with an open hand");
            invertTBlacklist = lazyBool(builder, "Invert Tool Blacklist", false, "Inverts the tool blacklist to function as a whitelist");
		    invertBBlacklist = lazyBool(builder, "Invert Block Blacklist", false, "Inverts the block blacklist to function as a whitelist");
            ignoreTools = lazyBool(builder, "Ignore Tool", false, "Ignores whether or not the held tool is valid");
            altTools = lazyBool(builder, "Alt Tools", false, "Use alternate check for tool validity (e.g. swords on webs)");
            toolClass = lazyBool(builder, "Only Standard Types", false, "Limit excavation to standard tool types (Picks, Shoves, Axes & Shears)");
            tpsGuard = lazyBool(builder, "TPS Guard", true, "Temporarily reduces excavation speed if TPS begins to slow down");
            autoPickup = lazyBool(builder, "Auto Pickup", false, "Skips spawning drops in world adding them directly to your inventory");
            allowShapes = lazyBool(builder, "Allow Shapes", true, "Allow players to use shape mining");
            maxUndos = lazyInt(builder, "Max Undos", 3, 0, Integer.MAX_VALUE, "How many excavations should be kept in undo history (may lead to exploits or instability)");
            gamestage = lazyString(builder, "Game Stage", "", "The default game stage required to unlock excavations (requires gamestages to be installed)");
            
            toolBlacklist = lazyList(builder, "Tool Blacklist", Collections.emptyList(), "Tools blacklisted from excavating");
            blockBlacklist = lazyList(builder, "Block Blacklist", Collections.emptyList(), "Blocks blacklisted from being excavated");
            
            builder.pop();
        }
        
        private void apply()
        {
            ExcavationSettings.mineLimit = mineLimit.get();
            ExcavationSettings.mineSpeed = mineSpeed.get();
            ExcavationSettings.mineRange = mineRange.get();
            ExcavationSettings.exaustion = exaustion.get().floatValue();
            ExcavationSettings.experience = experience.get();
            ExcavationSettings.openHand = openHand.get();
            ExcavationSettings.invertTBlacklist = invertTBlacklist.get();
            ExcavationSettings.invertBBlacklist = invertBBlacklist.get();
            ExcavationSettings.ignoreTools = ignoreTools.get();
            ExcavationSettings.altTools = altTools.get();
            ExcavationSettings.toolClass = toolClass.get();
            ExcavationSettings.tpsGuard = tpsGuard.get();
            ExcavationSettings.autoPickup = autoPickup.get();
            ExcavationSettings.allowShapes = allowShapes.get();
            ExcavationSettings.maxUndos = maxUndos.get();
            ExcavationSettings.gamestage = gamestage.get();
    
            BlockBlacklist.INSTANCE.loadList(blockBlacklist.get().toArray(new String[0]));
            ItemBlacklist.INSTANCE.loadList(toolBlacklist.get().toArray(new String[0]));
        }
    }
    
    public static class ConfigClient
    {
        public final IntValue mineMode;
        public final BooleanValue mustHold;
        public final BooleanValue useSideHit;
        
        private ConfigClient(ForgeConfigSpec.Builder builder)
        {
            builder.push("client");
            
            mineMode = lazyInt(builder, "Mode", 0, -1, 2, "Excavation mode (-1 Disabled, 0 = Keybind, 1 = Sneak, 2 = Always)");
            mustHold = lazyBool(builder, "Must Hold", true, "Allows players to cancel excavation by releasing the keys");
            useSideHit = lazyBool(builder, "Use Side Hit", true, "Use the side of the block hit to determine shape mining direction");
            
            builder.pop();
        }
        
        private void apply()
        {
            ExcavationSettings.mineMode = mineMode.get();
            ExcavationSettings.mustHold = mustHold.get();
            ExcavationSettings.useSideHit = useSideHit.get();
        }
    }
    
    private static IntValue lazyInt(Builder builder, String var, int def, int min, int max, String com)
    {
        builder.comment(com);
        builder.translation(OreExcavation.MODID + ".config." + var.replaceAll(" ", "_"));
        return builder.defineInRange(var, def, min, max);
    }
    
    private static DoubleValue lazyDouble(Builder builder, String var, double def, double min, double max, String com)
    {
        builder.comment(com);
        builder.translation(OreExcavation.MODID + ".config." + var.replaceAll(" ", "_"));
        return builder.defineInRange(var, def, min, max);
    }
    
    private static BooleanValue lazyBool(Builder builder, String var, boolean def, String com)
    {
        builder.comment(com);
        builder.translation(OreExcavation.MODID + ".config." + var.replaceAll(" ", "_"));
        return builder.define(var, def);
    }
    
    private static ConfigValue<String> lazyString(Builder builder, String var, String def, String com)
    {
        builder.comment(com);
        builder.translation(OreExcavation.MODID + ".config." + var.replaceAll(" ", "_"));
        return builder.define(var, def);
    }
    
    private static ConfigValue<List<? extends String>> lazyList(Builder builder, String var, List<String> def, String com)
    {
        builder.comment(com);
        builder.translation(OreExcavation.MODID + ".config." + var.replaceAll(" ", "_"));
        return builder.defineList(var, def, (o) -> o instanceof String);
    }
    
    @SubscribeEvent
    public static void onLoad(final Loading event)
    {
        if(OreExcavation.MODID.equalsIgnoreCase(event.getConfig().getModId()))
        {
            if(event.getConfig().getType() == Type.CLIENT)
            {
                CLIENT.apply();
            } else if(event.getConfig().getType() == Type.COMMON)
            {
                COMMON.apply();
                loadJsonFiles();
            }
        }
    }
    
    @SubscribeEvent
    public static void onFileChanged(ConfigReloading event)
    {
        if(OreExcavation.MODID.equalsIgnoreCase(event.getConfig().getModId()))
        {
            if(event.getConfig().getType() == Type.CLIENT)
            {
                CLIENT.apply();
            } else if(event.getConfig().getType() == Type.COMMON)
            {
                COMMON.apply();
                loadJsonFiles();
            }
        }
    }
    
    private static void loadJsonFiles()
	{
		File fileOverrides = new File("config/oreexcavation_overrides.json");
		
		if(fileOverrides.exists())
		{
			ToolOverrideHandler.INSTANCE.loadOverrides(JsonHelper.ReadFromFile(fileOverrides));
		} else
		{
			JsonObject json = ToolOverrideHandler.INSTANCE.getDefaultOverrides();
			JsonHelper.WriteToFile(fileOverrides, json);
			ToolOverrideHandler.INSTANCE.loadOverrides(json);
		}
		
		ShapeRegistry.INSTANCE.loadShapes(new File("config/oreexcavation_shapes.json"));
		
		File fileGroups = new File("config/oreexcavation_groups.json");
		
		if(fileGroups.exists())
		{
			BlockGroups.INSTANCE.readFromJson(JsonHelper.ReadFromFile(fileGroups));
		} else
		{
			JsonObject json = BlockGroups.INSTANCE.getDefaultJson();
			JsonHelper.WriteToFile(fileGroups, json);
			BlockGroups.INSTANCE.readFromJson(json);
		}
	}
}
