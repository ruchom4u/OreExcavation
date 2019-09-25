package oreexcavation.network;

import io.netty.buffer.ByteBuf;
import net.darkhax.gamestages.data.GameStageSaveHandler;
import net.darkhax.gamestages.data.IStageData;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.handlers.EventHandler;
import oreexcavation.handlers.MiningScheduler;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;
import org.apache.logging.log4j.Level;

public class PacketExcavation implements IMessage
{
	private NBTTagCompound tags = new NBTTagCompound();
	
	@SuppressWarnings("unused")
    public PacketExcavation()
	{
	}
	
	public PacketExcavation(NBTTagCompound tags)
	{
		this.tags = tags;
	}
	
	@Override
	public void fromBytes(ByteBuf buf)
	{
		tags = ByteBufUtils.readTag(buf);
	}
	
	@Override
	public void toBytes(ByteBuf buf)
	{
		ByteBufUtils.writeTag(buf, tags);
	}
	
	public static class ServerHandler implements IMessageHandler<PacketExcavation,PacketExcavation>
	{
		@Override
		public PacketExcavation onMessage(PacketExcavation message, MessageContext ctx)
		{
			final EntityPlayerMP player = ctx.getServerHandler().player;
			
			if(player == null || player.getServer() == null)
			{
				return null;
			}
			
			if(message.tags.getBoolean("cancel"))
			{
				player.getServer().addScheduledTask(() -> MiningScheduler.INSTANCE.stopMining(player));
				return null;
			}
			
			final int x = message.tags.getInteger("x");
			final int y = message.tags.getInteger("y");
			final int z = message.tags.getInteger("z");

			final IBlockState state = Block.getStateById(message.tags.getInteger("stateId"));
			
			if(state.getBlock() == Blocks.AIR)
			{
				OreExcavation.logger.log(Level.INFO, "Recieved invalid block ID");
			}
			
			final ExcavateShape shape;
			final EnumFacing facing;
			
			if(message.tags.hasKey("shape"))
			{
				if(!ExcavationSettings.allowShapes)
				{
					player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Shape mining has been disabled"), false);
					return null;
				} else if(ExcavationSettings.gamestagesInstalled && !StringUtils.isNullOrEmpty(ExcavationSettings.shapeStage))
                {
                    IStageData stage = GameStageSaveHandler.getPlayerData(player.getUniqueID());
                    if(stage != null && !stage.hasStage(ExcavationSettings.shapeStage)) return null;
                }
				
				shape = new ExcavateShape();
				shape.setMask(message.tags.getInteger("shape"));
				
				if(message.tags.hasKey("depth"))
				{
					shape.setMaxDepth(message.tags.getInteger("depth"));
				}
				
				if(message.tags.hasKey("origin"))
				{
					int origin = message.tags.getInteger("origin");
					shape.setReticle(origin%5, origin/5);
				}
				
				facing = EnumFacing.VALUES[message.tags.getInteger("side")];
			} else
			{
				shape = null;
				facing = EnumFacing.NORTH;
			}
			
			player.getServer().addScheduledTask(() -> MiningScheduler.INSTANCE.startMining(player, new BlockPos(x, y, z), state, shape, facing));
			
			return null;
		}
	}
	
	@SideOnly(Side.CLIENT)
	public static class ClientHandler implements IMessageHandler<PacketExcavation,PacketExcavation>
	{
		@Override
		public PacketExcavation onMessage(PacketExcavation message, MessageContext ctx)
		{
			if(ExcavationSettings.mineMode < 0)
			{
				return null;
			} else if(ExcavationSettings.mineMode == 0)
			{
				if(ExcavationKeys.excavateKey.getKeyCode() == 0 || !ExcavationKeys.excavateKey.isKeyDown())
				{
					return null;
				}
			} else if(ExcavationSettings.mineMode != 2 && !Minecraft.getMinecraft().player.isSneaking())
			{
				return null;
			}
			
			EventHandler.isExcavating = true;
			
			ExcavateShape shape = ShapeRegistry.INSTANCE.getActiveShape();
			
			if(shape != null)
			{
				message.tags.setInteger("shape", shape.getShapeMask());
				message.tags.setInteger("depth", shape.getMaxDepth());
				message.tags.setInteger("origin", shape.getReticle());
				
				if(!ExcavationSettings.useSideHit)
				{
					message.tags.setInteger("side", ExcavateShape.getFacing(Minecraft.getMinecraft().player).getIndex());
				}
			}
			
			return new PacketExcavation(message.tags);
		}
	}
}
