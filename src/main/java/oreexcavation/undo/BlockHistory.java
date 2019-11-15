package oreexcavation.undo;

import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import oreexcavation.core.OreExcavation;

public class BlockHistory
{
    public final BlockPos pos;
    public final BlockState state;
    public final CompoundNBT tileData;
    
    public BlockHistory(BlockPos pos, BlockState state, CompoundNBT tileData)
    {
        this.pos = pos;
        this.state = state;
        this.tileData = tileData;
    }
    
    public BlockHistory(BlockSnapshot snapshot)
    {
        this.pos = snapshot.getPos();
        this.state = snapshot.getReplacedBlock();
        
        if(snapshot.getTileEntity() != null)
        {
            tileData = new CompoundNBT();
            
            try
            {
                snapshot.getTileEntity().write(tileData);
            } catch(Exception e)
            {
                OreExcavation.logger.error("Unable to save undo state for tile entity: " + snapshot.getTileEntity().toString(), e);
            }
        } else
        {
            this.tileData = null;
        }
    }
    
    public BlockHistory(BlockSnapshot snapshot, CompoundNBT tileNBT)
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
            tile.read(tileData);
        }
    }
}
