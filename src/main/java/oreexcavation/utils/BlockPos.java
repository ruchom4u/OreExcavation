package oreexcavation.utils;

public class BlockPos
{
	private final int x;
	private final int y;
	private final int z;
	
	public BlockPos(int x, int y, int z)
	{
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public int getX()
	{
		return x;
	}
	
	public int getY()
	{
		return y;
	}
	
	public int getZ()
	{
		return z;
	}
	
	public BlockPos offset(int i, int j, int k)
	{
		return new BlockPos(getX() + i, getY() + j, getZ() + k);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj instanceof BlockPos)
		{
			BlockPos p2 = (BlockPos)obj;
			
			return p2.x == this.x && p2.y == this.y && p2.z == this.z;
		}
		
		return false;
	}
}
