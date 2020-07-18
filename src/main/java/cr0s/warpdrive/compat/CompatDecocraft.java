package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants.NBT;

public class CompatDecocraft implements IBlockTransformer {
	
	private static Class<?> classBlockFake;
	private static Class<?> classBlockProps;
	
	public static void register() {
		try {
			classBlockFake = Class.forName("com.mia.props.common.BlockFake");
			classBlockProps = Class.forName("com.mia.props.common.BlockProps");
			
			WarpDriveConfig.registerBlockTransformer("Decocraft", new CompatDecocraft());
		} catch(final ClassNotFoundException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final BlockState blockState, final TileEntity tileEntity) {
		return classBlockFake.isInstance(blockState.getBlock())
		    || classBlockProps.isInstance(blockState.getBlock());
	}
	
	@Override
	public boolean isJumpReady(final BlockState blockState, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public INBT saveExternals(final World world, final int x, final int y, final int z,
	                          final BlockState blockState, final TileEntity tileEntity) {
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final BlockState blockState, final TileEntity tileEntity) {
		// nothing to do
	}
	
	/*
	com.mia.props.common.BlockFake
		id: props:com.mia.props.common.tilefake
		metadata 2
		int[]   master  absolute xyz coordinates (BlockPos)

	com.mia.props.common.BlockProps
		id: props:com.mia.props.common.tileprops (and derivated)
		metadata 0 1 2 3
		int BlockRotation 0 4 8 12 / 1 5 9 13 / 2 6 10 14 / 3 7 11 15
		list<int[]> slaves  absolute xyz coordinates (BlockPos)
	*/
	
	//                                                   0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final int[]  mrotProps           = {  1,  2,  3,  0,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[]  rotBlockRotation    = {  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,  0,  1,  2,  3 };
	
	@Override
	public BlockState rotate(final BlockState blockState, final CompoundNBT nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		
		// Props rotation
		if (nbtTileEntity.contains("BlockRotation")) {
			final int blockRotation = nbtTileEntity.getInt("BlockRotation");
			switch (rotationSteps) {
			case 1:
				nbtTileEntity.putInt("BlockRotation", rotBlockRotation[blockRotation]);
				break;
			case 2:
				nbtTileEntity.putInt("BlockRotation", rotBlockRotation[rotBlockRotation[blockRotation]]);
				break;
			case 3:
				nbtTileEntity.putInt("BlockRotation", rotBlockRotation[rotBlockRotation[rotBlockRotation[blockRotation]]]);
				break;
			default:
				break;
			}
		}
		
		// Props reference to fake blocks (slaves)
		if (nbtTileEntity.contains("slaves", NBT.TAG_LIST)) {
			final ListNBT listOldSlaves = nbtTileEntity.getList("slaves", NBT.TAG_INT_ARRAY);
			for (int index = 0; index < listOldSlaves.size(); index++) {
				final int[] intSlavePos = listOldSlaves.getIntArrayAt(index);
				if (intSlavePos.length == 3) {// expecting a BlockPos
					final int x = intSlavePos[0];
					final int y = intSlavePos[1];
					final int z = intSlavePos[2];
					if (transformation.isInside(x, y, z)) {
						final BlockPos blockPosSlave = transformation.apply(x, y, z);
						intSlavePos[0] = blockPosSlave.getX();
						intSlavePos[1] = blockPosSlave.getY();
						intSlavePos[2] = blockPosSlave.getZ();
					} else {// (outside ship)
						// remove the link
						intSlavePos[0] = 0;
						intSlavePos[1] = 0;
						intSlavePos[2] = 0;
					}
				} else {
					WarpDrive.logger.error(String.format("Unexpected context for slaves[%d] int array in %s for %s",
					                                     index, nbtTileEntity, block));
				}
			}
		}
		
		// Slave block
		if (nbtTileEntity.contains("master", NBT.TAG_INT_ARRAY)) {
			final int[] intMasterPos = nbtTileEntity.getIntArray("master");
			if (intMasterPos.length == 3) {// expecting a BlockPos
				final int x = intMasterPos[0];
				final int y = intMasterPos[1];
				final int z = intMasterPos[2];
				if (transformation.isInside(x, y, z)) {
					final BlockPos blockPosMaster = transformation.apply(x, y, z);
					intMasterPos[0] = blockPosMaster.getX();
					intMasterPos[1] = blockPosMaster.getY();
					intMasterPos[2] = blockPosMaster.getZ();
				} else {// (outside ship)
					// remove the link
					intMasterPos[0] = 0;
					intMasterPos[1] = 0;
					intMasterPos[2] = 0;
				}
			} else {
				WarpDrive.logger.error(String.format("Unexpected context for master int array in %s for %s",
				                                     nbtTileEntity, block));
			}
		}
		
		if (classBlockProps.isInstance(blockState.getBlock())) {
			switch (rotationSteps) {
			case 1:
				return mrotProps[metadata];
			case 2:
				return mrotProps[mrotProps[metadata]];
			case 3:
				return mrotProps[mrotProps[mrotProps[metadata]]];
			default:
				return blockState;
			}
		}
		return blockState;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final BlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final INBT nbtBase) {
		// no operation
	}
}
