package oreexcavation.handlers;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.WorldEvent;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.client.GuiEditShapes;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.groups.BlockBlacklist;
import oreexcavation.groups.ItemBlacklist;
import oreexcavation.network.PacketExcavation;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;
import oreexcavation.utils.BlockPos;
import oreexcavation.utils.ToolEffectiveCheck;
import org.lwjgl.input.Keyboard;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EventHandler
{
	public static MiningAgent captureAgent;
	public static boolean skipNext = false;
	
	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
	{
		if(event.modID.equals(OreExcavation.MODID))
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
					Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText("Excavate Shape: NONE"));
				} else
				{
					Minecraft.getMinecraft().thePlayer.addChatComponentMessage(new ChatComponentText("Excavate Shape: " + shape.getName()));
				}
			}
		}
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onEntitySpawn(EntityJoinWorldEvent event)
	{
		if(event.world.isRemote || event.entity.isDead || event.isCanceled())
		{
			return;
		}
		
		if(captureAgent != null)
		{
			if(event.entity instanceof EntityItem)
			{
				EntityItem eItem = (EntityItem)event.entity;
				ItemStack stack = eItem.getEntityItem();
				
				captureAgent.addItemDrop(stack);
				
				event.setCanceled(true);
			} else if(event.entity instanceof EntityXPOrb)
			{
				EntityXPOrb orb = (EntityXPOrb)event.entity;
				
				captureAgent.addExperience(orb.getXpValue());
				
				event.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public void onBlockBreak(BlockEvent.BreakEvent event)
	{
		if(event.world.isRemote || event.isCanceled())
		{
			return;
		}
		
		if(captureAgent != null && !captureAgent.hasMinedPosition(new BlockPos(event.x, event.y, event.z)))
		{
			return; // Prevent unnecessary checks if an agent was responsible
		}
		
		if(!(event.getPlayer() instanceof EntityPlayerMP) || event.getPlayer() instanceof FakePlayer)
		{
			return;
		}
		
		EntityPlayerMP player = (EntityPlayerMP)event.getPlayer();
		
		if(player.getHeldItem() == null && !ExcavationSettings.openHand)
		{
			return;
		} else if(isToolBlacklisted(player.getHeldItem()) != ExcavationSettings.invertTBlacklist)
		{
			return;
		} else if(isBlockBlacklisted(event.block, event.blockMetadata) != ExcavationSettings.invertBBlacklist)
		{
			return;
		} else if(event.block.isAir(event.world, event.x, event.y, event.z))
		{
			return;
		}
		
		if(ExcavationSettings.ignoreTools || ToolEffectiveCheck.canHarvestBlock(event.world, event.block, event.blockMetadata, new BlockPos(event.x, event.y, event.z), player))
		{
			MiningAgent agent = MiningScheduler.INSTANCE.getActiveAgent(player.getUniqueID());
			
			if(agent == null)
			{
				NBTTagCompound tag = new NBTTagCompound();
				tag.setInteger("x", event.x);
				tag.setInteger("y", event.y);
				tag.setInteger("z", event.z);
				tag.setString("block", Block.blockRegistry.getNameForObject(event.block));
				tag.setInteger("meta", event.blockMetadata);
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
		
		MiningScheduler.INSTANCE.tickAgents(MinecraftServer.getServer());
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
			if(!ExcavationKeys.excavateKey.getIsKeyPressed())
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
		if(event.world.isRemote || MinecraftServer.getServer().isServerRunning())
		{
			return;
		}
		
		MiningScheduler.INSTANCE.resetAll();
		captureAgent = null;
	}
	
	public boolean isBlockBlacklisted(Block block, int metadata)
	{
		if(block == null || block == Blocks.air)
		{
			return false;
		}
		
		return BlockBlacklist.INSTANCE.isBanned(block, metadata);
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
