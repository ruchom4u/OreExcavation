package oreexcavation.network;

import net.minecraft.network.PacketBuffer;

import java.util.function.BiConsumer;

public class ExcavationEncoder implements BiConsumer<PacketExcavation, PacketBuffer>
{
    public static final ExcavationEncoder INSTANCE = new ExcavationEncoder();
    
    @Override
    public void accept(PacketExcavation packetExcavation, PacketBuffer packetBuffer)
    {
        packetExcavation.toBytes(packetBuffer);
    }
}
