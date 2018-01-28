package oreexcavation.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.handlers.EventHandler;
import oreexcavation.handlers.MiningScheduler;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;
import oreexcavation.utils.BlockPos;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PacketExcavation implements IMessage
{
	private NBTTagCompound tags = new NBTTagCompound();
	
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
			final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
			
			if(player == null)
			{
				return null;
			}
			
			if(message.tags.getBoolean("cancel"))
			{
				MiningScheduler.INSTANCE.stopMining(player);
				return null;
			}
			
			final int x = message.tags.getInteger("x");
			final int y = message.tags.getInteger("y");
			final int z = message.tags.getInteger("z");
			
			final Block block = (Block)Block.blockRegistry.getObject(message.tags.getString("block"));
			final int meta = message.tags.getInteger("meta");
			
			if(block == null || block == Blocks.air)
			{
				return null;
			}
			
			final ExcavateShape shape;
			final EnumFacing facing;
			
			if(message.tags.hasKey("shape"))
			{
				if(!ExcavationSettings.allowShapes)
				{
					player.addChatComponentMessage(new ChatComponentText(EnumChatFormatting.RED + "Shape mining has been disabled"));
					return null;
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
				
				facing = EnumFacing.values()[message.tags.getInteger("side")];
			} else
			{
				shape = null;
				facing = EnumFacing.NORTH;
			}
			
			MiningScheduler.INSTANCE.startMining(player, new BlockPos(x, y, z), block, meta, shape, facing);
			
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
				if(ExcavationKeys.excavateKey.getKeyCode() == 0 || !ExcavationKeys.excavateKey.getIsKeyPressed())
				{
					return null;
				}
			} else if(ExcavationSettings.mineMode != 2 && !Minecraft.getMinecraft().thePlayer.isSneaking())
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
					message.tags.setInteger("side", ExcavateShape.getFacing(Minecraft.getMinecraft().thePlayer).ordinal());
				}
			}
			
			return new PacketExcavation(message.tags);
		}
	}
}
