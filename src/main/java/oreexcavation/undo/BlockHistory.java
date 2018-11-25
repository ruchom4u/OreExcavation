package oreexcavation.undo;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import oreexcavation.core.OreExcavation;
import oreexcavation.utils.BlockPos;

public class BlockHistory
{
	public final BlockPos pos;
	public final Block block;
	public final int meta;
	public final NBTTagCompound tileData;
	
	public BlockHistory(BlockPos pos, Block block, int meta, NBTTagCompound tileData)
	{
		this.pos = pos;
		this.block = block;
		this.meta = meta;
		this.tileData = tileData;
	}
	
	public BlockHistory(BlockSnapshot snapshot)
	{
		this.pos = new BlockPos(snapshot.x, snapshot.y, snapshot.z);
		this.block = snapshot.getReplacedBlock();
		this.meta = snapshot.meta;
		
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
		this.pos = new BlockPos(snapshot.x, snapshot.y, snapshot.z);
		this.block = snapshot.getReplacedBlock();
		this.meta = snapshot.meta;
        this.tileData = tileNBT;
    }
	
	public void restoreBlock(World world)
	{
		world.setBlock(pos.getX(), pos.getY(), pos.getZ(), block, meta, 2);
		TileEntity tile = world.getTileEntity(pos.getX(), pos.getY(), pos.getZ());
		
		if(tile != null && tileData != null)
		{
			tile.readFromNBT(tileData);
		}
	}
}
