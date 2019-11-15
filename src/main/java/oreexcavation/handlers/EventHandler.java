package oreexcavation.handlers;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent.KeyInputEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.world.BlockEvent.BreakEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
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
import org.lwjgl.glfw.GLFW;

@SuppressWarnings({"unused", "WeakerAccess"})
public class EventHandler
{
	public static MiningAgent captureAgent;
	public static boolean skipNext = false;
	
	@SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onKeyEvent(KeyInputEvent event)
    {
        Minecraft mc = Minecraft.getInstance();
        if(mc.currentScreen != null) return;
        
        boolean shapeKey = ExcavationKeys.shapeKey.isPressed();
        boolean editKey = ExcavationKeys.shapeEdit.isPressed();
        
        if(editKey || (shapeKey && (event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0))
        {
            mc.displayGuiScreen(new GuiEditShapes());
        } else if(shapeKey)
        {
            ShapeRegistry.INSTANCE.toggleShape();
            
            ExcavateShape shape = ShapeRegistry.INSTANCE.getActiveShape();
            
            if(shape == null)
            {
                mc.player.sendStatusMessage(new StringTextComponent("Excavate Shape: NONE"), false);
            } else
            {
                mc.player.sendStatusMessage(new StringTextComponent("Excavate Shape: " + shape.getName()), false);
            }
        }
    }
	
	@SubscribeEvent(priority=EventPriority.LOWEST)
	public static void onEntitySpawn(EntityJoinWorldEvent event)
	{
		if(event.getWorld().isRemote || !event.getEntity().isAlive() || event.isCanceled())
		{
			return;
		}
		
		if(captureAgent != null)
		{
			if(event.getEntity() instanceof ItemEntity)
			{
				ItemEntity eItem = (ItemEntity)event.getEntity();
				ItemStack stack = eItem.getItem();
				
				captureAgent.addItemDrop(stack);
				
				event.setCanceled(true);
			} else if(event.getEntity() instanceof ExperienceOrbEntity)
			{
				ExperienceOrbEntity orb = (ExperienceOrbEntity)event.getEntity();
				
				captureAgent.addExperience(orb.getXpValue());
				
				event.setCanceled(true);
			}
		}
	}
	
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onBlockBreak(BreakEvent event)
	{
		if(event.getWorld().getWorld().isRemote || event.isCanceled())
		{
			return;
		}
		
		if(captureAgent != null && !captureAgent.hasMinedPosition(event.getPos()))
		{
			return; // Prevent unnecessary checks if an agent was responsible
		}
		
		if(!(event.getPlayer() instanceof ServerPlayerEntity) || event.getPlayer() instanceof FakePlayer)
		{
			return;
		}
		
		ServerPlayerEntity player = (ServerPlayerEntity)event.getPlayer();
		
		if(player.getHeldItem(Hand.MAIN_HAND).isEmpty() && !ExcavationSettings.openHand)
		{
			return;
		} else if(isToolBlacklisted(player.getHeldItem(Hand.MAIN_HAND)))
		{
			return;
		} else if(isBlockBlacklisted(event.getState()))
		{
			return;
		} else if(event.getState().getBlock().isAir(event.getState(), event.getWorld(), event.getPos()))
		{
			return;
		}
		
		BlockPos p = event.getPos();
		BlockState s = event.getState();
		
		// === Game Stages ===
		/*if(ExcavationSettings.gamestagesInstalled)
		{
			IStageData stage = GameStageSaveHandler.getPlayerData(player.getUniqueID());
			
			if(stage != null)
			{
				ToolOverride tover = ToolOverrideHandler.INSTANCE.getOverride(player.getHeldItem(EnumHand.MAIN_HAND));
				tover = tover != null ? tover : ToolOverrideDefault.DEFAULT;
				
				if(!StringUtils.isNullOrEmpty(tover.getGameStage()) && !stage.hasStage(tover.getGameStage()))
				{
					return;
				}
				
				String blockStage = BlockGroups.INSTANCE.getStage(s);
				
				if(!StringUtils.isNullOrEmpty(blockStage) && !stage.hasStage(blockStage))
				{
					return;
				}
			}
		}*/
		
		if(ExcavationSettings.ignoreTools || ToolEffectiveCheck.canHarvestBlock(event.getWorld().getWorld(), s, p, player))
		{
			MiningAgent agent = MiningScheduler.INSTANCE.getActiveAgent(player.getUniqueID());
			
			if(agent == null)
			{
				CompoundNBT tag = new CompoundNBT();
				tag.putInt("x", p.getX());
				tag.putInt("y", p.getY());
				tag.putInt("z", p.getZ());
				tag.putInt("stateId", Block.getStateId(s));
				tag.putInt("side", ExcavateShape.getFacing(player, s, p).getIndex());
				
				OreExcavation.instance.network.sendTo(new PacketExcavation(tag), player.connection.getNetworkManager(), NetworkDirection.PLAY_TO_CLIENT);
			}
		}
	}
	
	@SubscribeEvent
	public static void onTick(TickEvent.ServerTickEvent event)
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
		
		MiningScheduler.INSTANCE.tickAgents(ServerLifecycleHooks.getCurrentServer());
		captureAgent = null;
	}

	public static boolean isExcavating = false;
	private static int cTick = 0;
	
	@SubscribeEvent
    @OnlyIn(Dist.CLIENT)
	public static void onClientTick(TickEvent.ClientTickEvent event)
	{
		cTick = (cTick + 1)%10;
		
		if(cTick != 0 || Minecraft.getInstance().player == null || !isExcavating || !ExcavationSettings.mustHold)
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
		} else if(ExcavationSettings.mineMode != 2 && !Minecraft.getInstance().player.isSneaking())
		{
			canContinue = false;
		}
		
		if(!canContinue)
		{
			isExcavating = false;
			CompoundNBT tags = new CompoundNBT();
			tags.putBoolean("cancel", true);
			OreExcavation.instance.network.sendToServer(new PacketExcavation(tags));
		}
	}
	
	@SubscribeEvent
	public static void onWorldUnload(WorldEvent.Unload event)
	{
		if(event.getWorld().getWorld().isRemote || event.getWorld().getWorld().getServer() == null || event.getWorld().getWorld().getServer().isServerRunning())
		{
			return;
		}
		
		MiningScheduler.INSTANCE.resetAll();
		captureAgent = null;
	}
	
	public static boolean isBlockBlacklisted(BlockState state)
	{
		if(state == null || state.getBlock() == Blocks.AIR)
		{
			return false;
		}
		
		return BlockBlacklist.INSTANCE.isBanned(state) != ExcavationSettings.invertBBlacklist;
	}
	
	public static boolean isToolBlacklisted(ItemStack stack)
	{
		if(stack == null || stack.isEmpty())
		{
			return false;
		}
		
		return ItemBlacklist.INSTANCE.isBanned(stack) != ExcavationSettings.invertTBlacklist;
	}
}
