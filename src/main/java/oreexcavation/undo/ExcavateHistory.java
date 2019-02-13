package oreexcavation.undo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.utils.BigItemStack;
import oreexcavation.utils.XPHelper;

public class ExcavateHistory
{
	private final List<BlockHistory> history = new ArrayList<>();
	private final NonNullList<BigItemStack> stacks = NonNullList.create();
	private long experience = 0;
	
	private final Stopwatch timer;
	private final int dimension;
	
	public ExcavateHistory(int dimension)
	{
		this.timer = Stopwatch.createUnstarted();
		this.dimension = dimension;
	}
	
	/**
	 * Records the block and tile data associated with this position in the world for later restoration
	 */
	public BlockHistory recordPosition(World world, BlockPos pos)
	{
		IBlockState state = world.getBlockState(pos);
		TileEntity tile = world.getTileEntity(pos);
		NBTTagCompound tileData = null;
		
		if(tile != null)
		{
			tileData = new NBTTagCompound();
			tile.writeToNBT(tileData);
		}
		
		return new BlockHistory(pos, state, tileData);
	}
	
	public void addRecordedBlock(BlockHistory block)
	{
		if(history.contains(block) || block.state.getBlock() == Blocks.AIR)
		{
			return;
		}
		
		history.add(0, block); // Inserts at 0 so restoring places blocks in reverse order they were broke
	}
	
	public void setRecievedStacks(List<BigItemStack> items)
	{
		this.stacks.clear();
		for(BigItemStack s : items)
		{
			this.stacks.add(s.copy());
		}
	}
	
	public void setRecievedXP(long xp)
	{
		this.experience = xp;
	}
	
	public RestoreResult canRestore(MinecraftServer server, EntityPlayer player)
	{
		World world = server.worldServerForDimension(dimension);
		
		if(!player.capabilities.isCreativeMode)
		{
			if(XPHelper.getPlayerXP(player) < this.experience)
			{
				return RestoreResult.INVALID_XP;
			}
			
			int[] req = new int[stacks.size()];
			
			for(int i = 0; i < player.inventory.getSizeInventory(); i++)
			{
				ItemStack invoStack = player.inventory.getStackInSlot(i);
				
				if(!invoStack.isEmpty())
				{
					for(int n = 0; n < req.length; n++)
					{
					    //noinspection EqualsBetweenInconvertibleTypes // BigItemStack deals with this
						if(stacks.get(n).equals(invoStack))
						{
							req[n] += invoStack.getCount();
						}
					}
				}
			}
			
			for(int n = 0; n < req.length; n++)
			{
				if(req[n] < stacks.get(n).stackSize)
				{
					return RestoreResult.INVALID_ITEMS;
				}
			}
		}
		
		for(BlockHistory hist : history)
		{
			if(world.getBlockState(hist.pos).getBlock() != Blocks.AIR)
			{
				return RestoreResult.OBSTRUCTED;
			} else if(world.getEntitiesWithinAABBExcludingEntity(null, new AxisAlignedBB(hist.pos.getX(), hist.pos.getY(), hist.pos.getZ(), hist.pos.getX() + 1F, hist.pos.getY() + 1F, hist.pos.getZ() + 1F)).size() > 0)
			{
				return RestoreResult.OBSTRUCTED;
			}
		}
		
		return RestoreResult.SUCCESS;
	}
	
	public boolean tickRestore(MinecraftServer server, EntityPlayer player)
	{
		timer.reset();
		timer.start();
		
		if(!player.capabilities.isCreativeMode && stacks.size() > 0)
		{
			XPHelper.addXP(player, -this.experience, true);
			
			Iterator<BigItemStack> iterStacks = stacks.iterator();
			
			while(iterStacks.hasNext())
			{
				BigItemStack stack = iterStacks.next();
				
				for(int i = 0; i < player.inventory.getSizeInventory() && stack.stackSize > 0; i++)
				{
					ItemStack invoStack = player.inventory.getStackInSlot(i);
					
					//noinspection EqualsBetweenInconvertibleTypes // BigItemStack deals with this
					if(!invoStack.isEmpty() && stack.equals(invoStack))
					{
						int num = Math.min(stack.stackSize, invoStack.getCount());
						player.inventory.decrStackSize(i, num);
						stack.stackSize -= num;
					}
				}
				
				// Even if we don't have the necessary items we're not going to keep looking for them
				iterStacks.remove();
			}
		}
		
		Iterator<BlockHistory> iterator = history.iterator();
		World world = server.worldServerForDimension(dimension);
		
		while(iterator.hasNext())
		{
			if(ExcavationSettings.tpsGuard && timer.elapsed(TimeUnit.MILLISECONDS) > 40)
			{
				break;
			}
			
			BlockHistory entry = iterator.next();
			entry.restoreBlock(world);
			iterator.remove();
		}
		
		timer.stop();
		
		return history.size() <= 0;
	}
}
