package oreexcavation.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import oreexcavation.core.OreExcavation;

import java.awt.event.KeyEvent;

public class ExcavationKeys
{
	public static final KeyBinding excavateKey = new KeyBinding(OreExcavation.MODID + ".key.excavate", KeyEvent.VK_BACK_QUOTE, OreExcavation.NAME);
	public static final KeyBinding shapeKey = new KeyBinding(OreExcavation.MODID + ".key.shape_toggle", KeyEvent.VK_V, OreExcavation.NAME);
	public static final KeyBinding shapeEdit = new KeyBinding(OreExcavation.MODID + ".key.shape_edit", KeyEvent.VK_B, OreExcavation.NAME);
	
	public static void registerKeys()
	{
		ClientRegistry.registerKeyBinding(excavateKey);
		ClientRegistry.registerKeyBinding(shapeKey);
		ClientRegistry.registerKeyBinding(shapeEdit);
	}
}
