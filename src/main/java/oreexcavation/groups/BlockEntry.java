package oreexcavation.groups;

import com.mojang.brigadier.StringReader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.arguments.BlockStateParser;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import oreexcavation.core.OreExcavation;
import oreexcavation.utils.TagIngredient;
import org.apache.logging.log4j.Level;

import javax.annotation.Nonnull;

@SuppressWarnings("WeakerAccess")
public class BlockEntry
{
    public final boolean isWildcard;
	public final ResourceLocation idName;
	public final TagIngredient tagIng;
	public final BlockState state;
	
	public BlockEntry()
    {
		this.idName = null;
		this.tagIng = null;
		this.state = null;
		this.isWildcard = true;
    }
	
	public BlockEntry(@Nonnull ResourceLocation idName)
	{
		this.idName = idName;
		this.tagIng = null;
		this.state = null;
		this.isWildcard = false;
	}
	
	public BlockEntry(@Nonnull BlockState blockState)
    {
        this.idName = blockState.getBlock().getRegistryName();
        this.tagIng = null;
        this.state = blockState;
		this.isWildcard = false;
    }
	
	public BlockEntry(@Nonnull String tagName)
	{
		this.tagIng = StringUtils.isNullOrEmpty(tagName) ? null : new TagIngredient(tagName);
		this.idName = null;
		this.state = null;
		this.isWildcard = false;
	}
	
	public boolean checkMatch(BlockState blockState)
	{
	    if(blockState == null || blockState.getBlock() == Blocks.AIR) return false;
	    if(isWildcard) return true;
	    if(state != null && state.equals(blockState)) return true;
	    if(tagIng != null && tagIng.apply(blockState)) return true;
		return idName != null && idName.equals(blockState.getBlock().getRegistryName());
	}
	
	public static BlockEntry readFromString(String s)
	{
		if(s == null || s.length() <= 0) return null;
		if(s.equalsIgnoreCase("*")) return new BlockEntry();
		
		String[] split = s.split(":");
		
		if(split.length <= 1 || split.length > 3) // Invalid
		{
            OreExcavation.logger.log(Level.WARN, "Invalid Block Entry format: " + s);
			return null;
		} else if(split.length == 2) // Simple ID
		{
			return new BlockEntry(new ResourceLocation(split[0], split[1]));
		} else if(s.startsWith("tag:"))
		{
		    return new BlockEntry(s.replaceFirst("tag:", ""));
        } else if(s.startsWith("state:"))// ID and state properties // TODO: Use BlockStateParser to read state properties (need to figure out how to (distinguish between a resource location and a state string)
		{
            BlockStateParser parser = new BlockStateParser(new StringReader(s.replaceFirst("state:", "")), false);
            try
            {
                parser.parse(false);
                if(parser.getState() != null) return new BlockEntry(parser.getState());
            } catch(Exception e)
            {
                OreExcavation.logger.log(Level.ERROR, "Unable to parse block state for Block Entry: " + s, e);
            }
		}
        
        OreExcavation.logger.log(Level.WARN, "Invalid Block Entry format: " + s);
        return null;
	}
}
