package oreexcavation.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;

// Cut down version from 1.12
public class OreIngredient
{
    private final NonNullList<ItemStack> ores;
    
    public OreIngredient(String ore)
    {
        ores = OreDictionary.getOres(ore);
    }
    
    public boolean apply(@Nullable ItemStack input)
    {
        if (input == null)
            return false;

        for (ItemStack target : this.ores)
            if (OreDictionary.itemMatches(target, input, false))
                return true;

        return false;
    }
}
