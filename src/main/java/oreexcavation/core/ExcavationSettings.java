package oreexcavation.core;

/**
 * A container for all the configurable settings in the mod
 */
public class ExcavationSettings
{
	public static boolean hideUpdates = false;
	public static int mineSpeed = 128;
	public static int mineLimit = 64;
	public static int mineRange = 16;
	public static float exaustion = 0.1F;
	public static int experience = 0;
	public static int maxUndos = 3;
	
	/**
	 * -1 Disabled, 0 = Keybind, 1 = Sneak, 2 = Always
	 */
	public static int mineMode = 1;
	public static boolean mustHold = true;
	public static boolean invertTBlacklist = false;
	public static boolean invertBBlacklist = false;
	
	public static boolean openHand = false;
	public static boolean ignoreTools = false;
	public static boolean toolClass = false;
	public static boolean altTools = false;
	public static boolean tpsGuard = true;
	public static boolean autoPickup = false;
	public static boolean allowShapes = true;
}
