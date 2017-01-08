package oreexcavation.core.proxies;

import net.minecraftforge.fml.relauncher.Side;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.core.OreExcavation;
import oreexcavation.network.PacketExcavation;


public class ClientProxy extends CommonProxy
{
	@Override
	public boolean isClient()
	{
		return true;
	}
	
	@Override
	public void registerHandlers()
	{
		super.registerHandlers();
		
		ExcavationKeys.registerKeys();
		
		OreExcavation.instance.network.registerMessage(PacketExcavation.ClientHandler.class, PacketExcavation.class, 0, Side.CLIENT);
	}
}
