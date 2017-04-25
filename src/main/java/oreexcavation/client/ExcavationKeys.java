package oreexcavation.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import oreexcavation.core.OreExcavation;
import org.lwjgl.input.Keyboard;

public class ExcavationKeys
{
	public static final KeyBinding excavateKey = new KeyBinding(OreExcavation.MODID + ".key.excavate", Keyboard.KEY_GRAVE, OreExcavation.NAME);
	public static final KeyBinding shapeKey = new KeyBinding(OreExcavation.MODID + ".key.shape", Keyboard.KEY_V, OreExcavation.NAME);
	
	public static void registerKeys()
	{
		ClientRegistry.registerKeyBinding(excavateKey);
		ClientRegistry.registerKeyBinding(shapeKey);
	}
}
