package oreexcavation.network;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkEvent.Context;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.core.ExcavationSettings;
import oreexcavation.core.OreExcavation;
import oreexcavation.handlers.EventHandler;
import oreexcavation.handlers.MiningScheduler;
import oreexcavation.shapes.ExcavateShape;
import oreexcavation.shapes.ShapeRegistry;
import org.apache.logging.log4j.Level;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ExcPacketHandler implements BiConsumer<PacketExcavation, Supplier<NetworkEvent.Context>>
{
    public static ExcPacketHandler INSTANCE = new ExcPacketHandler();
    
    public void accept(PacketExcavation packet, Supplier<NetworkEvent.Context> context)
    {
        switch(context.get().getDirection().getOriginationSide())
        {
            case CLIENT: // Sent from the client to the server
                acceptServer(packet, context);
                context.get().setPacketHandled(true);
                break;
            case SERVER: // Sent from the server to the client
                acceptClient(packet, context);
                context.get().setPacketHandled(true);
                break;
        }
    }
    
    private void acceptServer(PacketExcavation message, Supplier<NetworkEvent.Context> context)
    {
			final ServerPlayerEntity player = context.get().getSender();
			
			if(player == null || player.getServer() == null)
			{
				return;
			}
			
			if(message.getTags().getBoolean("cancel"))
			{
				player.getServer().deferTask(() -> MiningScheduler.INSTANCE.stopMining(player));
				return;
			}
			
			final int x = message.getTags().getInt("x");
			final int y = message.getTags().getInt("y");
			final int z = message.getTags().getInt("z");

			final BlockState state = Block.getStateById(message.getTags().getInt("stateId"));
			
			if(state.getBlock() == Blocks.AIR)
			{
				OreExcavation.logger.log(Level.INFO, "Recieved invalid block ID");
			}
			
			final ExcavateShape shape;
			final Direction facing;
			
			if(message.getTags().contains("shape"))
			{
				if(!ExcavationSettings.allowShapes)
				{
					player.sendStatusMessage(new StringTextComponent(TextFormatting.RED + "Shape mining has been disabled"), false);
					return;
				}
				
				shape = new ExcavateShape();
				shape.setMask(message.getTags().getInt("shape"));
				
				if(message.getTags().contains("depth"))
				{
					shape.setMaxDepth(message.getTags().getInt("depth"));
				}
				
				if(message.getTags().contains("origin"))
				{
					int origin = message.getTags().getInt("origin");
					shape.setReticle(origin%5, origin/5);
				}
				
				facing = Direction.byIndex(message.getTags().getInt("side"));
			} else
			{
				shape = null;
				facing = Direction.NORTH;
			}
			
			player.getServer().deferTask(() -> MiningScheduler.INSTANCE.startMining(player, new BlockPos(x, y, z), state, shape, facing));
    }
    
    @OnlyIn(Dist.CLIENT)
    private void acceptClient(PacketExcavation message, Supplier<Context> context)
    {
			if(ExcavationSettings.mineMode < 0)
			{
				return;
			} else if(ExcavationSettings.mineMode == 0)
			{
				if(ExcavationKeys.excavateKey.getKey().getKeyCode() == 0 || !ExcavationKeys.excavateKey.isKeyDown())
				{
					return;
				}
			} else if(ExcavationSettings.mineMode != 2 && !Minecraft.getInstance().player.func_225608_bj_())
			{
				return;
			}
			
			EventHandler.isExcavating = true;
			
			ExcavateShape shape = ShapeRegistry.INSTANCE.getActiveShape();
			
			if(shape != null)
			{
				message.getTags().putInt("shape", shape.getShapeMask());
				message.getTags().putInt("depth", shape.getMaxDepth());
				message.getTags().putInt("origin", shape.getReticle());
				
				if(!ExcavationSettings.useSideHit)
				{
					message.getTags().putInt("side", ExcavateShape.getFacing(Minecraft.getInstance().player).getIndex());
				}
			}
			
			OreExcavation.instance.network.sendToServer(new PacketExcavation(message.getTags()));
    }
}
