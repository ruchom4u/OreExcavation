package oreexcavation.undo;

import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import oreexcavation.core.OreExcavation;

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
            tileData = new NBTTagCompound();
            
            try
            {
                snapshot.getTileEntity().writeToNBT(tileData);
            } catch(Exception e)
            {
                OreExcavation.logger.error("Unable to save undo state for tile entity: " + snapshot.getTileEntity().toString(), e);
            }
		} else
		{
			tileData = null;
		}
	}
    
    public BlockHistory(BlockSnapshot snapshot, NBTTagCompound tileNBT)
    {
        this.pos = snapshot.getPos();
        this.state = snapshot.getReplacedBlock();
        this.tileData = tileNBT;
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
