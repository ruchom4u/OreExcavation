package oreexcavation.shapes;

import org.apache.logging.log4j.Level;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import oreexcavation.core.OreExcavation;
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
	private byte reticle = 12; // Middle index
	
	public void setName(String value)
	{
		this.name = value;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setReticle(int x, int y)
	{
		this.reticle = (byte)MathHelper.clamp_int((y * 5) + x, 0, 24);
	}
	
	public int getReticle()
	{
		return this.reticle;
	}
	
	public void setMaxDepth(int value)
	{
		this.maxDepth = value;
	}
	
	public int getMaxDepth()
	{
		return this.maxDepth;
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
		this.reticle = (byte)MathHelper.clamp_int(JsonHelper.GetNumber(json, "reticle", 12).byteValue(), 0, 24);
		
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
		json.addProperty("reticle", reticle);
		
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
		rotOff = rotOff.add(reticle%5, reticle/5, 0);
		
		if(rotOff.getX() < 0 || rotOff.getX() >= 5 || rotOff.getY() < 0 || rotOff.getY() >= 5 || rotOff.getZ() < 0 || (maxDepth >= 0 && rotOff.getZ() >= maxDepth))
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
	
	/* NORTH (Z-) & EAST (X+)
	 * SOUTH (Z+) & WEST (X-)*/
	private BlockPos rotate(BlockPos pos, EnumFacing facing)
	{
		switch(facing)
		{
			case DOWN:
				return new BlockPos(pos.getX(), pos.getZ() * -1, pos.getY() * -1);
			case EAST:
				return new BlockPos(pos.getZ(), pos.getY(), pos.getX() * -1);
			case NORTH:
				return new BlockPos(pos.getX() * -1, pos.getY(), pos.getZ() * -1);
			case SOUTH:
				return pos;
			case UP:
				return new BlockPos(pos.getX(), pos.getZ(), pos.getY());
			case WEST:
				return new BlockPos(pos.getZ() * -1, pos.getY(), pos.getX());
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
	
	public static EnumFacing getFacing(EntityPlayer player, IBlockState state, BlockPos pos)
	{
		RayTraceResult rtr = null;
		
		try
		{
			double d = player.getDistance(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
			AxisAlignedBB aabb = state.getBoundingBox(player.worldObj, pos);
			Vec3d v = player.getPositionEyes(1F);
			Vec3d v2 = v.add(player.getLookVec().scale(d + 1D));
			aabb = aabb.offset(pos);
			rtr = aabb.calculateIntercept(v, v2);
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.INFO, "Unable to excavation direction for player " + player.getName(), e);
			rtr = null;
		}
		
		if(rtr != null)
		{
			return rtr.sideHit.getOpposite();
		} else
		{
			return getFacing(player); // Fallback to player facing direction
		}
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
				return EnumFacing.EAST;
			} else
			{
				return EnumFacing.WEST;
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
