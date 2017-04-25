package oreexcavation.undo;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;

public class BlockHistory
{
	public final BlockPos pos;
	public final IBlockState state;
	public final NBTTagCompound tileData;
	
	public BlockHistory(BlockPos pos, IBlockState state, NBTTagCompound tileData)
	{
		this.pos = pos;
		this.state = state;
		this.tileData = tileData;
	}
	
	public BlockHistory(BlockSnapshot snapshot)
	{
		this.pos = snapshot.getPos();
		this.state = snapshot.getReplacedBlock();
		
		TileEntity tile = snapshot.getTileEntity();
		
		if(tile != null)
		{
			this.tileData = new NBTTagCompound();
			tile.writeToNBT(tileData);
		} else
		{
			tileData = null;
		}
	}
	
	public void restoreBlock(World world)
	{
		world.setBlockState(pos, state, 2);
		TileEntity tile = world.getTileEntity(pos);
		
		if(tile != null && tileData != null)
		{
			tile.readFromNBT(tileData);
		}
	}
}
