package oreexcavation.handlers;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.util.BlockSnapshot;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.events.IExcavateFilter;
import oreexcavation.groups.BlockEntry;
import oreexcavation.groups.BlockGroups;
import oreexcavation.overrides.ToolOverride;
import oreexcavation.overrides.ToolOverrideDefault;
import oreexcavation.overrides.ToolOverrideHandler;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.undo.BlockHistory;
import oreexcavation.undo.ExcavateHistory;
import oreexcavation.utils.BigItemStack;
import oreexcavation.utils.BlockPos;
import oreexcavation.utils.ToolEffectiveCheck;
import oreexcavation.utils.XPHelper;
import org.apache.logging.log4j.Level;
import com.google.common.base.Stopwatch;

public class MiningAgent
{
	private ItemStack blockStack = null;
	private Item origTool; // Original tool the player was holding (must be the same to continue)
	private final List<BlockPos> mined = new ArrayList<>();
	private final ArrayDeque<BlockPos> scheduled = new ArrayDeque<>();
	public final EntityPlayerMP player;
	public final BlockPos origin;
	public final UUID playerID;
	public EnumFacing facing = EnumFacing.SOUTH;
	public ExcavateShape shape = null;
	private final ExcavateHistory history;
	
	public final List<BlockEntry> blockGroup = new ArrayList<>();
	private final List<IExcavateFilter> filters = new ArrayList<>();
	public final Block block;
	public final int meta;
	
	public ToolOverride toolProps = ToolOverrideDefault.DEFAULT;
	
	private boolean subtypes = true; // Ignore metadata
	private boolean strictSubs = false; // Disables subtypes and item block equality
	
	private List<BigItemStack> drops = new ArrayList<>();
	private int experience = 0;
	
	public MiningAgent(EntityPlayerMP player, BlockPos origin, Block block, int meta)
	{
		this.player = player;
		this.origin = origin;
		this.playerID = player.getUniqueID();
		
		this.block = block;
		this.meta = meta;
		
		this.history = new ExcavateHistory(player.worldObj.provider.dimensionId);
		this.blockGroup.addAll(BlockGroups.INSTANCE.getGroup(block, meta));
		this.strictSubs = BlockGroups.INSTANCE.isStrict(block, meta);
		
		ItemStack held = player.getHeldItem();
		origTool = held == null? null : held.getItem();
		
		if(held != null)
		{
			ToolOverride to = ToolOverrideHandler.INSTANCE.getOverride(held);
			toolProps = to != null? to : toolProps;
		}
	}
	
	public UUID getPlayerID()
	{
		return this.playerID;
	}
	
	public void setOverride(ToolOverride override)
	{
		this.toolProps = override;
	}
	
	public void addFilter(IExcavateFilter filter)
	{
		this.filters.add(filter);
	}
	
	public void init()
	{
		if(m_createStack != null)
		{
			try
			{
				blockStack = (ItemStack)m_createStack.invoke(block, meta);
				
				if(blockStack == null || blockStack.getItem() == null)
				{
					blockStack = null;
				}
			} catch(Exception e)
			{
				blockStack = null;
			}
		}
		
		if(!strictSubs)
		{
			this.subtypes = blockStack == null || !blockStack.getHasSubtypes();
		} else
		{
			this.subtypes = false;
		}
		
		for(int i = -1; i <= 1; i++)
		{
			for(int j = -1; j <= 1; j++)
			{
				for(int k = -1; k <= 1; k++)
				{
					appendBlock(origin.offset(i, j, k));
				}
			}
		}
	}
	
	public MiningAgent setShape(ExcavateShape shape, EnumFacing facing)
	{
		this.shape = shape;
		this.facing = facing;
		return this;
	}
	
	/**
	 * Returns true if the miner is no longer valid or has completed
	 */
	public boolean tickMiner(Stopwatch timer)
	{
		if(origin == null || player == null || !player.isEntityAlive() || mined.size() >= toolProps.getLimit())
		{
			return true;
		}
		
		for(int n = 0; !scheduled.isEmpty(); n++)
		{
			if(n >= toolProps.getSpeed() || mined.size() >= toolProps.getLimit())
			{
				break;
			}
			
			if(ExcavationSettings.tpsGuard && timer.elapsed(TimeUnit.MILLISECONDS) > 40)
			{
				break;
			}
			
			ItemStack heldStack = player.getHeldItem();
			Item heldItem = heldStack == null? null : heldStack.getItem();
			
			if(heldItem != origTool)
			{
				// Original tool has been swapped or broken
				return true;
			} else if(!hasEnergy(player))
			{
				return true;
			}
			
			BlockPos pos = scheduled.poll();
			
			if(pos == null)
			{
				continue;
			} else if(player.getDistance(pos.getX(), pos.getY(), pos.getZ()) > toolProps.getRange())
			{
				mined.add(pos);
				continue;
			}
			
			Block b = player.worldObj.getBlock(pos.getX(), pos.getY(), pos.getZ());
			int m = player.worldObj.getBlockMetadata(pos.getX(), pos.getY(), pos.getZ());
			
			if(EventHandler.isBlockBlacklisted(b, m) || !b.canCollideCheck(m, false))
			{
				mined.add(pos);
				continue;
			}
			
			boolean flag = b == block && (subtypes || m == meta);
			flag = flag || BlockGroups.INSTANCE.quickCheck(blockGroup, b, m);
			
			if(!flag && blockStack != null && !strictSubs)
			{
				ItemStack stack;
				
				try
				{
					stack = (ItemStack)m_createStack.invoke(b, m);
				} catch(Exception e)
				{
					stack = null;
				}
				
				if(stack != null && stack.getItem() == blockStack.getItem() && stack.getItemDamage() == blockStack.getItemDamage())
				{
					flag = true;
				}
			}
			
			if(flag)
			{
				if(ExcavationSettings.maxUndos > 0)
				{
					player.worldObj.captureBlockSnapshots = true;
					player.worldObj.capturedBlockSnapshots.clear();
				}
				
				if(!(ExcavationSettings.ignoreTools || ToolEffectiveCheck.canHarvestBlock(player.worldObj, b, m, pos, player)))
				{
					mined.add(pos);
					continue;
				} else if(player.theItemInWorldManager.tryHarvestBlock(pos.getX(), pos.getY(), pos.getZ()) || player.worldObj.getBlock(pos.getX(), pos.getY(), pos.getZ()) == Blocks.air)
				{
					if(ExcavationSettings.maxUndos > 0)
					{
						player.worldObj.captureBlockSnapshots = false;
						
						EventHandler.captureAgent = null;
						while(player.worldObj.capturedBlockSnapshots.size() > 0)
						{
							BlockSnapshot snap = player.worldObj.capturedBlockSnapshots.get(0);
							if(pos.equals(new BlockPos(snap.x, snap.y, snap.z)))
							{
								history.addRecordedBlock(new BlockHistory(snap));
							}
							player.worldObj.capturedBlockSnapshots.remove(0);
							
							player.worldObj.markAndNotifyBlock(snap.x, snap.y, snap.z, player.worldObj.getChunkFromChunkCoords(snap.x >> 4, snap.y >> 4), snap.getReplacedBlock(), snap.getCurrentBlock(), snap.flag);
						}
						EventHandler.captureAgent = this;
					}
					
					if(!player.capabilities.isCreativeMode)
					{
						player.getFoodStats().addExhaustion(toolProps.getExaustion());
						
						if(toolProps.getExperience() > 0)
						{
							XPHelper.addXP(player, -toolProps.getExperience(), false);
						}
					}
					
					for(int i = -1; i <= 1; i++)
					{
						for(int j = -1; j <= 1; j++)
						{
							for(int k = -1; k <= 1; k++)
							{
								appendBlock(pos.offset(i, j, k));
							}
						}
					}
				} else
				{
					OreExcavation.logger.warn("Block harvest failed unexpectedly.\nBlock: " + b + ":" + m + "\nTool: " + heldStack + "\nPos: " + pos.toString());
				}
				
				player.worldObj.capturedBlockSnapshots.clear();
				player.worldObj.captureBlockSnapshots = false;
				
				mined.add(pos);
			}
		}
		
		if(!player.capabilities.isCreativeMode)
		{
			XPHelper.syncXP(player);
		}
		
		return scheduled.size() <= 0 || mined.size() >= toolProps.getLimit();
	}
	
	/**
	 * Appends a block position to the miners current pass
	 */
	public void appendBlock(BlockPos pos)
	{
		if(pos == null || mined.contains(pos) || scheduled.contains(pos))
		{
			return;
		} else if(player.getDistance(pos.getX(), pos.getY(), pos.getZ()) > toolProps.getRange() || !player.canPlayerEdit(pos.getX(), pos.getY(), pos.getZ(), facing.ordinal(), player.getHeldItem()))
		{
			return;
		} else if(shape != null && !shape.isValid(origin, pos, facing))
		{
			return;
		}
		
		for(IExcavateFilter filter : this.filters)
		{
			if(!filter.canHarvest(player, this, pos))
			{
				return;
			}
		}
		
		scheduled.add(pos);
	}
	
	private boolean hasEnergy(EntityPlayerMP player)
	{
		return (toolProps.getExaustion() <= 0 || player.getFoodStats().getFoodLevel() > 0) && (toolProps.getExperience() <= 0 || XPHelper.getPlayerXP(player) >= toolProps.getExperience());
	}
	
	public void dropEverything()
	{
		// Temporarily halt any ongoing captures
		MiningAgent ca = EventHandler.captureAgent;
		EventHandler.captureAgent = null;
		
		history.setRecievedStacks(drops);
		history.setRecievedXP(experience);
		
		for(BigItemStack bigStack : drops)
		{
			for(ItemStack stack : bigStack.getCombinedStacks())
			{
				if(!ExcavationSettings.autoPickup)
				{
					EntityItem eItem = new EntityItem(this.player.worldObj, origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D, stack);
					this.player.worldObj.spawnEntityInWorld(eItem);
				} else
				{
					EntityItem eItem = new EntityItem(this.player.worldObj, player.posX, player.posY, player.posZ, stack);
					this.player.worldObj.spawnEntityInWorld(eItem);
				}
			}
		}
		
		if(this.experience > 0)
		{
			EntityXPOrb orb = null;
			
			if(ExcavationSettings.autoPickup)
			{
				orb = new EntityXPOrb(this.player.worldObj, player.posX, player.posY, player.posZ, this.experience);
			} else
			{
				orb = new EntityXPOrb(this.player.worldObj, origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D, this.experience);
			}
			
			this.player.worldObj.spawnEntityInWorld(orb);
		}
		
		drops.clear();
		this.experience = 0;
		
		EventHandler.captureAgent = ca;
	}
	
	public void addItemDrop(ItemStack stack)
	{
		for(BigItemStack bigStack : drops)
		{
			if(bigStack.equals(stack))
			{
				bigStack.stackSize += stack.stackSize;
				return;
			}
		}
		
		this.drops.add(new BigItemStack(stack));
	}
	
	public void addExperience(int value)
	{
		this.experience += value;
	}
	
	public boolean hasMinedPosition(BlockPos pos)
	{
		return mined.contains(pos);
	}
	
	public ExcavateHistory getHistory()
	{
		return history;
	}
	
	private static Method m_createStack = null;
	
	static
	{
		try
		{
			m_createStack = Block.class.getDeclaredMethod("func_149644_j", int.class);
			m_createStack.setAccessible(true);
		} catch(Exception e1)
		{
			try
			{
				m_createStack = Block.class.getDeclaredMethod("createStackedBlock", int.class);
				m_createStack.setAccessible(true);
			} catch(Exception e2)
			{
				OreExcavation.logger.log(Level.INFO, "Unable to use block hooks for excavation", e2);
			}
		}
	}
}
