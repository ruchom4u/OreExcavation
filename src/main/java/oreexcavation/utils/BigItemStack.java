package oreexcavation.utils;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

import java.util.ArrayList;
import java.util.List;

/**
 * Purpose built container class for holding ItemStacks larger than 127. <br>
 * <b>For storage purposes only!
 */
public class BigItemStack
{
	public int stackSize = 0;
	private final ItemStack baseStack; // Ensures that this base stack is never null
	
	public BigItemStack(ItemStack stack)
	{
		baseStack = stack.copy();
		this.stackSize = baseStack.getCount();
		baseStack.setCount(1);
	}
	
	public BigItemStack(Block block)
	{
		this(block, 1);
	}
	
	public BigItemStack(Block block, int amount)
	{
		this(block.asItem(), amount);
	}
	
	public BigItemStack(Item item)
	{
		this(item, 1);
	}
	
	public BigItemStack(Item item, int amount)
	{
		baseStack = new ItemStack(item, 1);
		this.stackSize = amount;
	}
	
	/**
	 * @return ItemStack this BigItemStack is based on. Changing the base stack size does NOT affect the BigItemStack's size
	 */
	public ItemStack getBaseStack()
	{
		return baseStack;
	}
	
	/**
	 * Shortcut method to the NBTTagCompound in the base ItemStack
	 */
	public CompoundNBT GetTagCompound()
	{
		return baseStack.getTag();
	}
	
	/**
	 * Shortcut method to the NBTTagCompound in the base ItemStack
	 */
	public void SetTagCompound(CompoundNBT tags)
	{
		baseStack.setTag(tags);
	}
	
	/**
	 * Shortcut method to the NBTTagCompound in the base ItemStack
	 */
	public boolean HasTagCompound()
	{
		return baseStack.hasTag();
	}
	
	/**
	 * Breaks down this big stack into smaller ItemStacks for Minecraft to use (Individual stack size is dependent on the item)
	 */
	public List<ItemStack> getCombinedStacks()
	{
		List<ItemStack> list = new ArrayList<>();
		int tmp1 = Math.max(1, stackSize); // Guarantees this method will return at least 1 item
		
		while(tmp1 > 0)
		{
			int size = Math.min(tmp1, baseStack.getMaxStackSize());
			ItemStack stack = baseStack.copy();
			stack.setCount(size);
			list.add(stack);
			tmp1 -= size;
		}
		
		return list;
	}
	
	public BigItemStack copy()
	{
		BigItemStack stack = new BigItemStack(baseStack.copy());
		stack.stackSize = this.stackSize;
		return stack;
	}
	
	@Override
	public boolean equals(Object stack)
	{
		if(stack instanceof ItemStack)
		{
			return baseStack.isItemEqual((ItemStack)stack) && ItemStack.areItemStackTagsEqual(baseStack, (ItemStack)stack);
		} else
		{
			return super.equals(stack);
		}
	}
	
	public BigItemStack(CompoundNBT tags)
	{
		CompoundNBT itemNBT = tags.copy();
		itemNBT.putInt("Count", 1);
		if(tags.contains("id", 99))
        {
            itemNBT.putString("id", "" + tags.getShort("id"));
        }
		this.stackSize = tags.getInt("Count");
        this.baseStack = ItemStack.read(itemNBT); // Minecraft does the ID conversions for me
	}
	
	public CompoundNBT writeToNBT(CompoundNBT tags)
	{
		baseStack.write(tags);
		tags.putInt("Count", stackSize);
		return tags;
	}
}
