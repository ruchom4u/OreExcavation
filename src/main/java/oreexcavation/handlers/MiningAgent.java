package oreexcavation.handlers;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.events.EventExcavate.Break;
import oreexcavation.events.EventExcavate.Pass;
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
import oreexcavation.utils.ToolEffectiveCheck;
import oreexcavation.utils.XPHelper;
import org.apache.logging.log4j.Level;
import com.google.common.base.Stopwatch;

@SuppressWarnings("WeakerAccess")
public class MiningAgent
{
	private ItemStack blockStack = null;
	private Item origTool;
	
	public final List<BlockPos> minedBlocks = new ArrayList<>();
	private final List<BlockPos> checkedBlocks = new ArrayList<>();
	private final ArrayDeque<BlockPos> scheduled = new ArrayDeque<>();
	public final EntityPlayerMP player;
	public final BlockPos origin;
	public final UUID playerID;
	public EnumFacing facing = EnumFacing.SOUTH;
	public ExcavateShape shape = null;
	private final ExcavateHistory history;
	
	public final List<BlockEntry> blockGroup = new ArrayList<>();
	private final List<IExcavateFilter> filters = new ArrayList<>();
	public final IBlockState state;
	private final Block block;
	private final int meta;
	
	public ToolOverride toolProps = ToolOverrideDefault.DEFAULT;
	
	private boolean subtypes = true; // Ignore metadata
	private boolean strictSubs; // Disables subtypes and item block equality
	
	private final NonNullList<BigItemStack> drops = NonNullList.create();
	private int experience = 0;
    
    /** Purely for integrations who want to track some sort of data about the excavation.
     *  For example: tracking resource costs then subtracting that in bulk when done.
     */
	@SuppressWarnings("unused")
    public final NBTTagCompound auxNBT = new NBTTagCompound();
	
	public MiningAgent(EntityPlayerMP player, BlockPos origin, IBlockState state)
	{
		this.player = player;
		this.origin = origin;
		this.playerID = player.getUniqueID();
		
		this.state = state;
		this.block = state.getBlock();
		this.meta = block.getMetaFromState(state);
		
		this.history = new ExcavateHistory(player.world.provider.getDimension());
		this.blockGroup.addAll(BlockGroups.INSTANCE.getGroup(state));
		this.strictSubs = BlockGroups.INSTANCE.isStrict(state);
		
		ItemStack held = player.getHeldItem(EnumHand.MAIN_HAND);
		origTool = held.isEmpty()? null : held.getItem();
		
		if(!held.isEmpty())
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
				blockStack = (ItemStack)m_createStack.invoke(block, state);
				
				if(blockStack == null || blockStack.isEmpty())
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
					appendBlock(origin.add(i, j, k));
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
		if(origin == null || player == null || !player.isEntityAlive() || minedBlocks.size() >= toolProps.getLimit() || MinecraftForge.EVENT_BUS.post(new Pass(this, Phase.START))) return true;
		
		for(int n = 0; !scheduled.isEmpty(); n++)
		{
			if(n >= toolProps.getSpeed() || minedBlocks.size() >= toolProps.getLimit())
			{
				break;
			}
			
			if(ExcavationSettings.tpsGuard && timer.elapsed(TimeUnit.MILLISECONDS) > 40)
			{
				break;
			}
			
			ItemStack heldStack = player.getHeldItem(EnumHand.MAIN_HAND);
			Item heldItem = heldStack.isEmpty()? null : heldStack.getItem();
			
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
				checkedBlocks.add(pos);
				continue;
			}
			
			IBlockState s = player.world.getBlockState(pos);
			Block b = s.getBlock();
			int m = b.getMetaFromState(s);
			
			if(EventHandler.isBlockBlacklisted(s) || !b.canCollideCheck(s, false))
			{
				checkedBlocks.add(pos);
				continue;
			}
			
			boolean flag = b == block && (subtypes || m == meta);
			flag = flag || BlockGroups.INSTANCE.quickCheck(blockGroup, s);
			
			if(!flag && blockStack != null && !strictSubs)
			{
				ItemStack stack;
				
				try
				{
					stack = (ItemStack)m_createStack.invoke(b, s);
				} catch(Exception e)
				{
					stack = null;
				}
				
				if(stack != null && !stack.isEmpty() && stack.getItem() == blockStack.getItem() && stack.getItemDamage() == blockStack.getItemDamage())
				{
					flag = true;
				}
			}
			
			if(flag)
			{
                NBTTagCompound tileData = null;
                
				if(ExcavationSettings.maxUndos > 0)
				{
					player.world.captureBlockSnapshots = true;
					player.world.capturedBlockSnapshots.clear();
                    TileEntity tile = player.world.getTileEntity(pos);
                    if(tile != null) tileData = tile.writeToNBT(new NBTTagCompound());
				}
				
				if(!(ExcavationSettings.ignoreTools || ToolEffectiveCheck.canHarvestBlock(player.world, s, pos, player)))
				{
					checkedBlocks.add(pos);
					continue;
				} else if(player.interactionManager.tryHarvestBlock(pos) || player.world.getBlockState(pos).getBlock() == Blocks.AIR)
				{
					if(ExcavationSettings.maxUndos > 0)
					{
						player.world.captureBlockSnapshots = false;
						
						EventHandler.captureAgent = null;
						while(player.world.capturedBlockSnapshots.size() > 0)
						{
							BlockSnapshot snap = player.world.capturedBlockSnapshots.get(0);
							if(pos.equals(snap.getPos()))
							{
								history.addRecordedBlock(new BlockHistory(snap, tileData));
							} else
                            {
								history.addRecordedBlock(new BlockHistory(snap));
                            }
							player.world.capturedBlockSnapshots.remove(0);
							
							player.world.markAndNotifyBlock(snap.getPos(), player.world.getChunkFromChunkCoords(snap.getPos().getX() >> 4, snap.getPos().getZ() >> 4), snap.getReplacedBlock(), snap.getCurrentBlock(), snap.getFlag());
						}
						EventHandler.captureAgent = this;
					}
					
					if(!player.isCreative())
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
								appendBlock(pos.add(i, j, k));
							}
						}
					}
					
					minedBlocks.add(pos);
					
					MinecraftForge.EVENT_BUS.post(new Break(this, s, pos));
				} else
				{
					OreExcavation.logger.warn("Block harvest failed unexpectedly.\nBlock: " + s + "\nTool: " + heldStack + "\nPos: " + pos.toString());
				}
				
				player.world.capturedBlockSnapshots.clear();
				player.world.captureBlockSnapshots = false;
				
				checkedBlocks.add(pos);
			}
		}
		
		if(!player.isCreative()) XPHelper.syncXP(player);
		
		return scheduled.size() <= 0 || minedBlocks.size() >= toolProps.getLimit() || MinecraftForge.EVENT_BUS.post(new Pass(this, Phase.END));
	}
	
	/**
	 * Appends a block position to the miners current pass
	 */
	public void appendBlock(BlockPos pos)
	{
		if(pos == null || checkedBlocks.contains(pos) || scheduled.contains(pos))
		{
			return;
		} else if(player.getDistance(pos.getX(), pos.getY(), pos.getZ()) > toolProps.getRange() || !player.world.getWorldBorder().contains(pos) || !canDestroy(player, pos))
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
	
	private boolean canDestroy(EntityPlayer player, BlockPos pos)
	{
		if(player.capabilities.allowEdit)
		{
			return true;
		}
		
		IBlockState state = player.world.getBlockState(pos);
		ItemStack held = player.getHeldItemMainhand();
		
		return !held.isEmpty() && state.getBlock() != Blocks.AIR && held.canDestroy(state.getBlock());
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
					EntityItem eItem = new EntityItem(this.player.world, origin.getX() + 0.5D, origin.getY() + 0.5D, origin.getZ() + 0.5D, stack);
					this.player.world.spawnEntity(eItem);
				} else
				{
					EntityItem eItem = new EntityItem(this.player.world, player.posX, player.posY, player.posZ, stack);
					this.player.world.spawnEntity(eItem);
				}
			}
		}
		
		if(this.experience > 0)
		{
			EntityXPOrb orb;
			
			if(ExcavationSettings.autoPickup)
			{
				orb = new EntityXPOrb(this.player.world, player.posX, player.posY, player.posZ, experience);
			} else
			{
				orb = new EntityXPOrb(this.player.world, origin.getX(), origin.getY(), origin.getZ(), experience);
			}
			
			this.player.world.spawnEntity(orb);
		}
		
		drops.clear();
		this.experience = 0;
		
		EventHandler.captureAgent = ca;
	}
	
	public void addItemDrop(ItemStack stack)
	{
		for(BigItemStack bigStack : drops)
		{
		    //noinspection EqualsBetweenInconvertibleTypes // BigItemStack deals with this
			if(bigStack.equals(stack))
			{
				bigStack.stackSize += stack.getCount();
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
		return checkedBlocks.contains(pos);
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
			m_createStack = Block.class.getDeclaredMethod("func_180643_i", IBlockState.class);
			m_createStack.setAccessible(true);
		} catch(Exception e1)
		{
			try
			{
				m_createStack = Block.class.getDeclaredMethod("getSilkTouchDrop", IBlockState.class);
				m_createStack.setAccessible(true);
			} catch(Exception e2)
			{
				OreExcavation.logger.log(Level.INFO, "Unable to use block hooks for excavation", e2);
			}
		}
	}
}
