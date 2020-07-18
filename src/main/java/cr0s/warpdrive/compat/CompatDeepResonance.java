package cr0s.warpdrive.compat;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import net.minecraftforge.common.util.Constants.NBT;

public class CompatDeepResonance implements IBlockTransformer {
	
	// mcjtylib rotation type
	private static Method methodBaseBlock_getRotationType; // all blocks extends a base class with parametrized rotation type => it's more efficient to read the property to differentiate them
	private static Class<Enum> classEnumRotationType;
	private static Enum RotationType_ROTATION;
	private static Enum RotationType_HORIZROTATION;
	private static Enum RotationType_NONE;
	
	// DeepResonance common block
	private static Class<?> classGenericDRBlock;
	
	@SuppressWarnings("unchecked")
	public static void register() {
		try {
			// mcjtylib rotation type
			final Class<?> classBaseBlock = Class.forName("mcjty.lib.blocks.BaseBlock");
			methodBaseBlock_getRotationType = classBaseBlock.getMethod("getRotationType");
			
			final Class<?> classRotationType = Class.forName("mcjty.lib.blocks.BaseBlock$RotationType");
			if (!classRotationType.isEnum()) {
				throw new RuntimeException("Invalid non-enum class, please report to mod author!");
			}
			classEnumRotationType = (Class<Enum>) classRotationType;
			RotationType_ROTATION = Enum.valueOf(classEnumRotationType, "ROTATION");
			RotationType_HORIZROTATION = Enum.valueOf(classEnumRotationType, "HORIZROTATION");
			RotationType_NONE = Enum.valueOf(classEnumRotationType, "NONE");
			
			// DeepResonance common block
			classGenericDRBlock = Class.forName("mcjty.deepresonance.blocks.GenericDRBlock");
			
			WarpDriveConfig.registerBlockTransformer("DeepResonance", new CompatDeepResonance());
		} catch(final ClassNotFoundException | RuntimeException | NoSuchMethodException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final BlockState blockState, final TileEntity tileEntity) {
		return classGenericDRBlock.isInstance(blockState.getBlock());
	}
	
	@Override
	public boolean isJumpReady(final BlockState blockState, final TileEntity tileEntity, final WarpDriveText reason) {
		return true;
	}
	
	@Override
	public INBT saveExternals(final World world, final int x, final int y, final int z,
	                          final BlockState blockState, final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void removeExternals(final World world, final int x, final int y, final int z,
	                            final BlockState blockState, final TileEntity tileEntity) {
		// nothing to do
	}
	
	//                                                  0   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15
	private static final int[] rotFacing           = {  0,  1,  5,  4,  2,  3,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	private static final int[] rotFacingHorizontal = {  3,  2,  0,  1,  7,  6,  4,  5, 11, 10,  8,  9, 15, 14, 12, 13 };
	
	@Override
	public BlockState rotate(final BlockState blockState, final CompoundNBT nbtTileEntity, final ITransformation transformation) {
		final byte rotationSteps = transformation.getRotationSteps();
		if (rotationSteps == 0) {
			return blockState;
		}
		
		// tank faces' input/output mode
		if (nbtTileEntity != null && nbtTileEntity.contains("settings")) {
			final ListNBT list = nbtTileEntity.getList("settings", NBT.TAG_COMPOUND);
			final Map<Integer, Integer> map = new HashMap<>(6);
			for (int index = 0; index < list.size(); index++) {
				final CompoundNBT tagCompound = list.getCompound(index);
				final int dir = tagCompound.getInt("dir");
				final int mode = tagCompound.getInt("n");
				map.put(rotFacing[dir], mode);
				switch (rotationSteps) {
				case 1:
					map.put(rotFacing[dir], mode);
					break;
				case 2:
					map.put(rotFacing[rotFacing[dir]], mode);
					break;
				case 3:
					map.put(rotFacing[rotFacing[rotFacing[dir]]], mode);
					break;
				default:
					break;
				}
			}
			for (int index = 0; index < list.size(); index++) {
				final CompoundNBT tagCompound = list.getCompound(index);
				final int dir = tagCompound.getInt("dir");
				final int mode = map.get(dir);
				tagCompound.putInt("n", mode);
			}
		}
		
		// mcjtylib rotation type
		final Enum enumRotationType;
		try {
			final Object object = methodBaseBlock_getRotationType.invoke(block);
			if (classEnumRotationType.isInstance(object)) {
				enumRotationType = (Enum) object;
			} else {
				WarpDrive.logger.error(String.format("Block %s has invalid non-Enum return value to getRotationType: %s",
				                                     block.getRegistryName(), object));
				return blockState;
			}
		} catch (final IllegalAccessException | InvocationTargetException exception) {
			exception.printStackTrace();
			return blockState;
		}
		
		// horizontal facing: 0 3 1 2 / 4 7 5 6 / 8 11 9 10 / 12 15 13 14
		if (enumRotationType == RotationType_HORIZROTATION) {
			switch (rotationSteps) {
			case 1:
				return rotFacingHorizontal[metadata];
			case 2:
				return rotFacingHorizontal[rotFacingHorizontal[metadata]];
			case 3:
				return rotFacingHorizontal[rotFacingHorizontal[rotFacingHorizontal[metadata]]];
			default:
				return blockState;
			}
		}
		
		// vanilla facing
		if (enumRotationType == RotationType_ROTATION) {
			switch (rotationSteps) {
			case 1:
				return rotFacing[metadata];
			case 2:
				return rotFacing[rotFacing[metadata]];
			case 3:
				return rotFacing[rotFacing[rotFacing[metadata]]];
			default:
				return blockState;
			}
		}
		
		if (enumRotationType != RotationType_NONE) {
			WarpDrive.logger.error(String.format("Invalid rotation type %s for block %s",
			                                     enumRotationType, block.getRegistryName()));
			
		}
		return blockState;
	}
	
	@Override
	public void restoreExternals(final World world, final BlockPos blockPos,
	                             final BlockState blockState, final TileEntity tileEntity,
	                             final ITransformation transformation, final INBT nbtBase) {
		// nothing to do
	}
}
