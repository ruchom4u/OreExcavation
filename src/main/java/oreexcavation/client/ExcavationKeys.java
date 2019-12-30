package oreexcavation.client;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import oreexcavation.core.OreExcavation;
import org.lwjgl.glfw.GLFW;

public class ExcavationKeys
{
	public static final KeyBinding excavateKey = new KeyBinding(OreExcavation.MODID + ".key.excavate", GLFW.GLFW_KEY_GRAVE_ACCENT, OreExcavation.NAME);
	public static final KeyBinding shapeKey = new KeyBinding(OreExcavation.MODID + ".key.shape_toggle", GLFW.GLFW_KEY_V, OreExcavation.NAME);
	public static final KeyBinding shapeEdit = new KeyBinding(OreExcavation.MODID + ".key.shape_edit", GLFW.GLFW_KEY_B, OreExcavation.NAME);
	
	public static void registerKeys()
	{
		ClientRegistry.registerKeyBinding(excavateKey);
		ClientRegistry.registerKeyBinding(shapeKey);
		ClientRegistry.registerKeyBinding(shapeEdit);
	}
}
