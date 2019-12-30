package oreexcavation.core;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import oreexcavation.client.ExcavationKeys;
import oreexcavation.handlers.ConfigHandler;
import oreexcavation.handlers.EventHandler;
import oreexcavation.network.ExcPacketHandler;
import oreexcavation.network.PacketExcavation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(OreExcavation.MODID)
@SuppressWarnings({"WeakerAccess", "unused"})
public class OreExcavation
{
    public static final String MODID = "oreexcavation";
    public static final String NAME = "OreExcavation";
    public static final String CHANNEL = "oe_chan";
    public static final String NET_PROTOCOL = "1.0.0";
    public static final String GUI_FACTORY = "oreexcavation.handlers.ConfigGuiFactory";
    
    public static OreExcavation instance;
    
    public SimpleChannel network;
    public static Logger logger = LogManager.getLogger(MODID);
    
    public OreExcavation()
    {
        instance = this;
        
        ModLoadingContext.get().registerConfig(Type.COMMON, ConfigHandler.commonSpec);
        ModLoadingContext.get().registerConfig(Type.CLIENT, ConfigHandler.clientSpec);
        
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().register(ConfigHandler.class);
    }
    
    public void setup(final FMLCommonSetupEvent event)
    {
        network = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, CHANNEL), () -> NET_PROTOCOL, NET_PROTOCOL::equalsIgnoreCase, NET_PROTOCOL::equalsIgnoreCase);
		network.registerMessage(0, PacketExcavation.class, PacketExcavation::toBytes, PacketExcavation::new, ExcPacketHandler.INSTANCE);
		
		MinecraftForge.EVENT_BUS.register(EventHandler.class);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStart);
        
        ExcavationSettings.gamestagesInstalled = ModList.get().isLoaded("gamestages");
    }
    
    public void clientSetup(final FMLClientSetupEvent event)
    {
		ExcavationKeys.registerKeys();
    }
    
    public void onServerStart(FMLServerStartingEvent event)
    {
        CommandUndo.register(event.getCommandDispatcher());
    }
}
