package oreexcavation.shapes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.VoxelShape;
import oreexcavation.core.OreExcavation;
import oreexcavation.utils.JsonHelper;
import org.apache.logging.log4j.Level;

@SuppressWarnings("WeakerAccess")
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
		this.reticle = (byte)MathHelper.clamp((y * 5) + x, 0, 24);
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
		this.reticle = (byte)MathHelper.clamp(JsonHelper.GetNumber(json, "reticle", 12).byteValue(), 0, 24);
		
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
			StringBuilder sb = new StringBuilder();
			
			for(int x = 0; x < 5; x++)
			{
				int mask = posToMask(x, y);
				
				boolean valid = (this.shape & mask) == mask;
				sb.append(valid? "X" : "O");
			}
			
			jmsk.add(new JsonPrimitive(sb.toString()));
		}
		
		json.add("mask", jmsk);
		
		return json;
	}
	
	/**
	 * Origin calculated from center
	 */
	public boolean isValid(BlockPos origin, BlockPos offset, Direction facing)
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
	
	private BlockPos counterRotate(BlockPos pos, Direction facing)
	{
		switch(facing)
		{
			case DOWN:
				return rotate(pos, Direction.DOWN);
			case EAST:
				return rotate(pos, Direction.WEST);
			case NORTH:
				return rotate(pos, Direction.NORTH);
			case SOUTH:
				return pos;
			case UP:
				return rotate(pos, Direction.UP);
			case WEST:
				return rotate(pos, Direction.EAST);
			default:
				return pos;
		}
	}
	
	/*
	 *      Z-
	 *      |
	 * X- ----- X+
	 *      |
	 *      Z+
	 */
	
	/* NORTH (Z-) & EAST (X+)
	 * SOUTH (Z+) & WEST (X-)*/
	private BlockPos rotate(BlockPos pos, Direction facing)
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
		return (int)Math.pow(2, idx);
	}
	
	public static Direction getFacing(PlayerEntity player, BlockState state, BlockPos pos)
	{
		BlockRayTraceResult rtr;
		
		try
		{
			double d = Math.sqrt(player.getDistanceSq(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D));
			VoxelShape aabb = state.getCollisionShape(player.world, pos);
			Vec3d v = player.getEyePosition(1F);
			Vec3d v2 = v.add(player.getLookVec().scale(d + 1D));
			aabb = aabb.withOffset(pos.getX(), pos.getY(), pos.getZ());
			rtr = aabb.rayTrace(v, v2, pos);
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.INFO, "Unable to get excavation direction for player " + player.getName(), e);
			rtr = null;
		}
		
		if(rtr != null)
		{
			return rtr.getFace().getOpposite();
		} else
		{
			return getFacing(player); // Fallback to player facing direction
		}
	}
	
	public static Direction getFacing(PlayerEntity player)
	{
		Vec3d dir = player.getLookVec();
		dir = dir.normalize();
		
		double ax = Math.abs(dir.x);
		double ay = Math.abs(dir.y);
		double az = Math.abs(dir.z);
		
		if(ax > ay && ax > az)
		{
			if(dir.x > 0)
			{
				return Direction.EAST;
			} else
			{
				return Direction.WEST;
			}
		} else if(az > ay && az > ax)
		{
			if(dir.z > 0)
			{
				return Direction.SOUTH;
			} else
			{
				return Direction.NORTH;
			}
		} else if(ay > ax && ay > az)
		{
			if(dir.y > 0)
			{
				return Direction.UP;
			} else
			{
				return Direction.DOWN;
			}
		}
		
		return Direction.SOUTH;
	}
}
