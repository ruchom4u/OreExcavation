package oreexcavation.shapes;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import oreexcavation.utils.JsonHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class ExcavateShape
{
	private String name = "New Shape";
	
	private int shape = 0;
	private int maxDepth = -1;
	
	public void setName(String value)
	{
		this.name = value;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setMaxDepth(int value)
	{
		this.maxDepth = value;
	}
	
	public int getMaxDepth()
	{
		return this.maxDepth < 0? Integer.MAX_VALUE : this.maxDepth;
	}
	
	public int getShapeMask()
	{
		return this.shape;
	}
	
	public void setMask(int mask)
	{
		this.shape = mask;
	}
	
	public void setMask(int x, int y, boolean allow)
	{
		if(x < 0 || x >= 5 || y < 0 || y >= 5)
		{
			return;
		}
		
		int mask = posToMask(x, y);
		
		if(allow)
		{
			this.shape |= mask;
		} else
		{
			this.shape = this.shape & ~mask;
		}
	}
	
	public void readFromJson(JsonObject json)
	{
		this.name = JsonHelper.GetString(json, "name", "New Shape");
		this.maxDepth = JsonHelper.GetNumber(json, "depth", -1).intValue();
		
		JsonArray jmsk = JsonHelper.GetArray(json, "mask");
		
		this.shape = 0;
		
		int y = 4;
		for(JsonElement je : jmsk)
		{
			if(y < 0)
			{
				break;
			}
			
			if(je == null || !je.isJsonPrimitive())
			{
				continue;
			}
			
			String row = je.getAsString();
			
			for(int x = 0; x < 5 && x < row.length(); x++)
			{
				this.setMask(x, y, row.toUpperCase().charAt(x) == 'X');
			}
			
			y--;
		}
	}
	
	public JsonObject writeToJson(JsonObject json)
	{
		json.addProperty("name", name);
		json.addProperty("depth", maxDepth);
		
		JsonArray jmsk = new JsonArray();
		
		for(int y = 4; y >= 0; y--)
		{
			String row = "";
			
			for(int x = 0; x < 5; x++)
			{
				int mask = posToMask(x, y);
				
				boolean valid = (this.shape & mask) == mask;
				row = row + (valid? "X" : "O");
			}
			
			jmsk.add(new JsonPrimitive(row));
		}
		
		json.add("mask", jmsk);
		
		return json;
	}
	
	/**
	 * Origin calculated from center
	 */
	public boolean isValid(BlockPos origin, BlockPos offset, EnumFacing facing)
	{
		int x = offset.getX() - origin.getX();
		int y = offset.getY() - origin.getY();
		int z = offset.getZ() - origin.getZ();
		
		BlockPos rotOff = counterRotate(new BlockPos(x, y, z), facing);
		rotOff = rotOff.add(2, 2, 0);
		
		if(rotOff.getX() < 0 || rotOff.getX() >= 5 || rotOff.getY() < 0 || rotOff.getY() >= 5 || rotOff.getZ() < 0 || rotOff.getZ() > maxDepth)
		{
			return false;
		}
		
		int mask = posToMask(rotOff.getX(), rotOff.getY());
		
		return (this.shape & mask) == mask;
	}
	
	private BlockPos counterRotate(BlockPos pos, EnumFacing facing)
	{
		switch(facing)
		{
			case DOWN:
				return rotate(pos, EnumFacing.DOWN);
			case EAST:
				return rotate(pos, EnumFacing.WEST);
			case NORTH:
				return rotate(pos, EnumFacing.NORTH);
			case SOUTH:
				return pos;
			case UP:
				return rotate(pos, EnumFacing.UP);
			case WEST:
				return rotate(pos, EnumFacing.EAST);
			default:
				return pos;
		}
	}
	
	/*
	 *      Z-
	 *      |
	 * X+ ----- X-
	 *      |
	 *      Z+
	 */
	
	/* NORTH (Z-) & EAST (X-)
	 * SOUTH (Z+) & WEST (X+)*/
	private BlockPos rotate(BlockPos pos, EnumFacing facing)
	{
		switch(facing)
		{
			case DOWN:
				return new BlockPos(pos.getX(), pos.getZ() * -1, pos.getY() * -1);
			case EAST:
				return new BlockPos(pos.getZ() * -1, pos.getY(), pos.getX());
			case NORTH:
				return new BlockPos(pos.getX() * -1, pos.getY(), pos.getZ() * -1);
			case SOUTH:
				return pos;
			case UP:
				return new BlockPos(pos.getX(), pos.getZ(), pos.getY());
			case WEST:
				return new BlockPos(pos.getZ(), pos.getY(), pos.getX() * -1);
			default:
				return pos;
		}
	}
	
	public static int posToMask(int x, int y)
	{
		int idx = (y * 5) + x;
		int msk = (int)Math.pow(2, idx);
		return msk;
	}
	
	public static EnumFacing getFacing(EntityPlayer player)
	{
		Vec3d dir = player.getLookVec();
		dir = dir.normalize();
		
		double ax = Math.abs(dir.xCoord);
		double ay = Math.abs(dir.yCoord);
		double az = Math.abs(dir.zCoord);
		
		if(ax > ay && ax > az)
		{
			if(dir.xCoord > 0)
			{
				return EnumFacing.WEST;
			} else
			{
				return EnumFacing.EAST;
			}
		} else if(az > ay && az > ax)
		{
			if(dir.zCoord > 0)
			{
				return EnumFacing.SOUTH;
			} else
			{
				return EnumFacing.NORTH;
			}
		} else if(ay > ax && ay > az)
		{
			if(dir.yCoord > 0)
			{
				return EnumFacing.UP;
			} else
			{
				return EnumFacing.DOWN;
			}
		}
		
		return EnumFacing.SOUTH;
	}
}
