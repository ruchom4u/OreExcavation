package oreexcavation.handlers;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.client.GuiEditShapes;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.groups.BlockBlacklist;
import oreexcavation.groups.ItemBlacklist;
import oreexcavation.network.PacketExcavation;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;
import oreexcavation.utils.ToolEffectiveCheck;
import org.lwjgl.input.Keyboard;

public class EventHandler
{
	public static MiningAgent captureAgent;
	public static boolean skipNext = false;
	
	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if(event.getModID().equals(OreExcavation.MODID))
		{
			ConfigHandler.config.save();
			ConfigHandler.initConfigs();
		}
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onKeyPressed(InputEvent event)
	{
		if(ExcavationKeys.shapeKey.isPressed())
		{
			Minecraft mc = Minecraft.getMinecraft();
			
			if(Keyboard.isKeyDown(Keyboard.KEY_LCONTROL))
			{
				mc.displayGuiScreen(new GuiEditShapes());
			} else
			{
				ShapeRegistry.INSTANCE.toggleShape();
				
				ExcavateShape shape = ShapeRegistry.INSTANCE.getActiveShape();
				
				if(shape == null)
				{
					Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new TextComponentString("Excavate Shape: NONE"));
				} else
				{
					Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new TextComponentString("Excavate Shape: " + shape.getName()));
				}
			}
		}
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onEntitySpawn(EntityJoinWorldEvent event)
	{
		if(event.getWorld().isRemote || event.getEntity().isDead || event.isCanceled())
		{
			return;
		}
		
		if(captureAgent != null)
		{
			if(event.getEntity() instanceof EntityItem)
			{
				EntityItem eItem = (EntityItem)event.getEntity();
				ItemStack stack = eItem.getEntityItem();
				
				captureAgent.addItemDrop(stack);
				
				event.setCanceled(true);
			} else if(event.getEntity() instanceof EntityXPOrb)
			{
				EntityXPOrb orb = (EntityXPOrb)event.getEntity();
				
				captureAgent.addExperience(orb.getXpValue());
				
				event.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onBlockBreak(BlockEvent.BreakEvent event)
	{
		if(event.getWorld().isRemote || event.isCanceled())
		{
			return;
		}
		
		if(captureAgent != null && !captureAgent.hasMinedPosition(event.getPos()))
		{
			return; // Prevent unnecessary checks if an agent was responsible
		}
		
		if(!(event.getPlayer() instanceof EntityPlayerMP) || event.getPlayer() instanceof FakePlayer)
		{
			return;
		}
		
		EntityPlayerMP player = (EntityPlayerMP)event.getPlayer();
		
		if(player.getHeldItem(EnumHand.MAIN_HAND) == null && !ExcavationSettings.openHand)
		{
			return;
		} else if(isToolBlacklisted(player.getHeldItem(EnumHand.MAIN_HAND)) != ExcavationSettings.invertTBlacklist)
		{
			return;
		} else if(isBlockBlacklisted(event.getState()) != ExcavationSettings.invertBBlacklist)
		{
			return;
		} else if(event.getState().getBlock().isAir(event.getState(), event.getWorld(), event.getPos()))
		{
			return;
		}
		
		BlockPos p = event.getPos();
		IBlockState s = event.getState();
		
		if(ExcavationSettings.ignoreTools || ToolEffectiveCheck.canHarvestBlock(event.getWorld(), s, p, player))
		{
			MiningAgent agent = MiningScheduler.INSTANCE.getActiveAgent(player.getUniqueID());
			
			if(agent == null)
			{
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("x", p.getX());
				tag.setInteger("y", p.getY());
				tag.setInteger("z", p.getZ());
				tag.setInteger("stateId", Block.getStateId(s));
				OreExcavation.instance.network.sendTo(new PacketExcavation(tag), player);
			}
		}
	}
	
	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event)
	{
		if(event.phase != TickEvent.Phase.END)
		{
			return;
		}
		
		if(skipNext)
		{
			skipNext = false;
			return;
		}
		
		MiningScheduler.INSTANCE.tickAgents(FMLCommonHandler.instance().getMinecraftServerInstance());
		captureAgent = null;
	}
	
	public static boolean isExcavating = false;
	private static int cTick = 0;
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onClientTick(TickEvent.ClientTickEvent event)
	{
		cTick = (cTick + 1)%10;
		
		if(cTick != 0 || Minecraft.getMinecraft().thePlayer == null || !isExcavating || !ExcavationSettings.mustHold)
		{
			return;
		}
		
		boolean canContinue = true;
		
		if(ExcavationSettings.mineMode < 0)
		{
			canContinue = false;
		} else if(ExcavationSettings.mineMode == 0)
		{
			if(!ExcavationKeys.excavateKey.isKeyDown())
			{
				canContinue = false;
			}
		} else if(ExcavationSettings.mineMode != 2 && !Minecraft.getMinecraft().thePlayer.isSneaking())
		{
			canContinue = false;
		}
		
		if(!canContinue)
		{
			isExcavating = false;
			NBTTagCompound tags = new NBTTagCompound();
			tags.setBoolean("cancel", true);
			OreExcavation.instance.network.sendToServer(new PacketExcavation(tags));
		}
	}
	
	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event)
	{
		if(event.getWorld().isRemote || event.getWorld().getMinecraftServer().isServerRunning())
		{
			return;
		}
		
		MiningScheduler.INSTANCE.resetAll();
		captureAgent = null;
	}
	
	public boolean isBlockBlacklisted(IBlockState state)
	{
		if(state == null || state.getBlock() == null || state.getBlock() == Blocks.AIR)
		{
			return false;
		}
		
		return BlockBlacklist.INSTANCE.isBanned(state);
	}
	
	public boolean isToolBlacklisted(ItemStack stack)
	{
		if(stack == null || stack.getItem() == null)
		{
			return false;
		}
		
		return ItemBlacklist.INSTANCE.isBanned(stack);
	}
}
