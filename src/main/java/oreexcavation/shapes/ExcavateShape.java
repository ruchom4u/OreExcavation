package oreexcavation.shapes;

import org.apache.logging.log4j.Level;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import oreexcavation.core.OreExcavation;
import oreexcavation.utils.BlockPos;
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
		json.addProperty("depth", Math.max(-1, maxDepth));
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
		rotOff = rotOff.offset(reticle%5, reticle/5, 0);
		
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
	
	public static EnumFacing getFacing(EntityPlayer player, Block block, int meta, BlockPos pos)
	{
		MovingObjectPosition mop = null;
		
		try
		{
			double d = player.getDistance(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
			block.setBlockBoundsBasedOnState(player.worldObj, pos.getX(), pos.getY(), pos.getZ());
			AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(block.getBlockBoundsMinX(), block.getBlockBoundsMinY(), block.getBlockBoundsMinZ(), block.getBlockBoundsMaxX(), block.getBlockBoundsMaxY(), block.getBlockBoundsMaxZ());
			aabb.offset(pos.getX(), pos.getY(), pos.getZ());
			Vec3 v = Vec3.createVectorHelper(player.posX, player.posY + player.eyeHeight, player.posZ);
			Vec3 vl = player.getLookVec();
			vl = Vec3.createVectorHelper(vl.xCoord * (d + 1D), vl.yCoord * (d + 1D), vl.zCoord * (d + 1D));
			Vec3 v2 = v.addVector(vl.xCoord, vl.yCoord, vl.zCoord);
			mop = aabb.calculateIntercept(v, v2);
		} catch(Exception e)
		{
			OreExcavation.logger.log(Level.INFO, "Unable to excavation direction for player " + player.getDisplayName(), e);
			mop = null;
		}
		
		if(mop != null)
		{
			// Manually flip facing direction
			int s = mop.sideHit - (mop.sideHit % 2);
			s += (mop.sideHit + 1) % 2;
			return EnumFacing.values()[s];
		} else
		{
			return getFacing(player); // Fallback to player facing direction
		}
	}
	
	public static EnumFacing getFacing(EntityPlayer player)
	{
		Vec3 dir = player.getLookVec();
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
