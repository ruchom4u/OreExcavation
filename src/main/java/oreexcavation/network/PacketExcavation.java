package oreexcavation.network;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;

public class PacketExcavation
{
	private final CompoundNBT tags;
	
	public PacketExcavation(CompoundNBT tags)
	{
		this.tags = tags;
	}
	
	public PacketExcavation(PacketBuffer buf)
    {
        tags = buf.readCompoundTag();
    }
    
    public CompoundNBT getTags()
    {
        return this.tags;
    }
	
	public void toBytes(PacketBuffer buf)
	{
	    buf.writeCompoundTag(tags);
	}
}
