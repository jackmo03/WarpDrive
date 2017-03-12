package cr0s.warpdrive.compat;

import cr0s.warpdrive.api.IBlockTransformer;
import cr0s.warpdrive.api.ITransformation;
import cr0s.warpdrive.config.WarpDriveConfig;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

public class CompatSGCraft implements IBlockTransformer {
	
	private static Class<?> classBaseTileEntity;
	private static Class<?> classDHDBlock;
	private static Class<?> classSGBaseBlock;
	private static Class<?> classSGBaseTE;
	private static Method methodSGBaseTE_sgStateDescription;
	
	public static void register() {
		try {
			classBaseTileEntity = Class.forName("gcewing.sg.BaseTileEntity");
			classDHDBlock = Class.forName("gcewing.sg.DHDBlock");
			classSGBaseBlock = Class.forName("gcewing.sg.SGBaseBlock");
			classSGBaseTE = Class.forName("gcewing.sg.SGBaseTE");
			methodSGBaseTE_sgStateDescription = classSGBaseTE.getMethod("sgStateDescription");
			
			WarpDriveConfig.registerBlockTransformer("SGCraft", new CompatSGCraft());
		} catch(ClassNotFoundException | NoSuchMethodException | SecurityException exception) {
			exception.printStackTrace();
		}
	}
	
	@Override
	public boolean isApplicable(final Block block, final int metadata, final TileEntity tileEntity) {
		return classBaseTileEntity.isInstance(tileEntity);
	}
	
	@Override
	public boolean isJumpReady(final Block block, final int metadata, final TileEntity tileEntity, StringBuilder reason) {
		if (classSGBaseTE.isInstance(tileEntity)) {
			try {
				Object object = methodSGBaseTE_sgStateDescription.invoke(tileEntity);
				String state = (String)object;
				if (!state.equalsIgnoreCase("Idle")) {
					reason.append("Stargate is active (" + state + ")!");
					return false;
				}
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
				exception.printStackTrace();
			}
		}
		return true;
	}
	
	@Override
	public NBTBase saveExternals(final TileEntity tileEntity) {
		// nothing to do
		return null;
	}
	
	@Override
	public void remove(TileEntity tileEntity) {
		// nothing to do
	}
	
	private static final byte[] mrotDHD = {  3,  0,  1,  2,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15 };
	
	@Override
	public int rotate(final Block block, final int metadata, NBTTagCompound nbtTileEntity, final ITransformation transformation) {
		byte rotationSteps = transformation.getRotationSteps();
		
		// Link between stargate controller and DHD
		if (nbtTileEntity.hasKey("isLinkedToStargate")) {
			if ( nbtTileEntity.getBoolean("isLinkedToStargate")
			  && nbtTileEntity.hasKey("linkedX") && nbtTileEntity.hasKey("linkedY") && nbtTileEntity.hasKey("linkedZ")) {
				BlockPos targetLink = transformation.apply(nbtTileEntity.getInteger("linkedX"), nbtTileEntity.getInteger("linkedY"), nbtTileEntity.getInteger("linkedZ"));
				nbtTileEntity.setInteger("linkedX", targetLink.getX());
				nbtTileEntity.setInteger("linkedY", targetLink.getY());
				nbtTileEntity.setInteger("linkedZ", targetLink.getZ());
			}
		}
		
		// Reference of ring blocks to the controller block
		if (nbtTileEntity.hasKey("isMerged")) {
			if ( nbtTileEntity.getBoolean("isMerged")
			  && nbtTileEntity.hasKey("baseX") && nbtTileEntity.hasKey("baseY") && nbtTileEntity.hasKey("baseZ")) {
				BlockPos targetLink = transformation.apply(nbtTileEntity.getInteger("baseX"), nbtTileEntity.getInteger("baseY"), nbtTileEntity.getInteger("baseZ"));
				nbtTileEntity.setInteger("baseX", targetLink.getX());
				nbtTileEntity.setInteger("baseY", targetLink.getY());
				nbtTileEntity.setInteger("baseZ", targetLink.getZ());
			}
		}
		
		if (classDHDBlock.isInstance(block) || classSGBaseBlock.isInstance(block)) {
			switch (rotationSteps) {
			case 1:
				return mrotDHD[metadata];
			case 2:
				return mrotDHD[mrotDHD[metadata]];
			case 3:
				return mrotDHD[mrotDHD[mrotDHD[metadata]]];
			default:
				return metadata;
			}
		}
		
		return metadata;
	}
	
	@Override
	public void restoreExternals(TileEntity tileEntity, ITransformation transformation, NBTBase nbtBase) {
		// nothing to do
	}
}
