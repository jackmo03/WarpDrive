package cr0s.warpdrive.block.energy;

import cr0s.warpdrive.Commons;
import cr0s.warpdrive.api.WarpDriveText;
import cr0s.warpdrive.block.TileEntityAbstractLaser;
import cr0s.warpdrive.config.WarpDriveConfig;
import cr0s.warpdrive.data.BlockProperties;
import cr0s.warpdrive.data.EnergyWrapper;
import cr0s.warpdrive.data.Vector3;
import cr0s.warpdrive.item.ItemIC2reactorLaserFocus;
import cr0s.warpdrive.network.PacketHandler;
import ic2.api.reactor.IReactor;
import ic2.api.reactor.IReactorChamber;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import net.minecraftforge.fluids.IFluidBlock;

public class TileEntityIC2reactorLaserMonitor extends TileEntityAbstractLaser {
	
	public static TileEntityType<TileEntityIC2reactorLaserMonitor> TileEntityIC2reactorLaserMonitor;
	
	// persistent properties
	private int ticks = WarpDriveConfig.IC2_REACTOR_COOLING_INTERVAL_TICKS;
	
	// computed properties
	public Direction facing = null;
	private boolean isValid = false;
	
	public TileEntityIC2reactorLaserMonitor() {
		super(TileEntityIC2reactorLaserMonitor);
		
		laserMedium_maxCount = 1;
		peripheralName = "warpdriveIC2reactorLaserCooler";
		doRequireUpgradeToInterface();
	}
	
	// returns IReactor tile entities
	private IReactor findReactor() {
		assert world != null;
		for(final Direction facing : Direction.values()) {
			final TileEntity tileEntity = world.getTileEntity(pos.offset(facing, 2));
			if (tileEntity == null) {
				continue;
			}
			
			IReactor output = null;
			if (tileEntity instanceof IReactor) {
				output = (IReactor) tileEntity;
				
			} else if (tileEntity instanceof IReactorChamber) {
				final IReactor reactor = ((IReactorChamber) tileEntity).getReactorInstance();
				if (reactor == null) {
					continue;
				}
				
				// ignore if we're right next to the reactor
				// ignore if we're not aligned with the reactor
				final BlockPos blockPos = reactor.getCoreTe().getPos();
				if ( blockPos.getX() != pos.getX() + 3 * facing.getXOffset()
				  || blockPos.getY() != pos.getY() + 3 * facing.getYOffset()
				  || blockPos.getZ() != pos.getZ() + 3 * facing.getZOffset() ) {
					continue;
				}
				
				output = reactor;
			}
			
			// if reactor or chamber was found, check the space in between
			if (output != null) {
				final BlockPos blockPos = pos.offset(facing);
				final BlockState blockState = world.getBlockState(blockPos);
				final Block block = blockState.getBlock();
				final boolean isAir = block.isAir(blockState, world, blockPos);
				isValid = ( isAir
				         || block instanceof IFluidBlock
				         || block instanceof IReactorChamber
				         || !blockState.getMaterial().isOpaque() );
				this.facing = facing; 
				return output;
			}
		}
		isValid = false;
		this.facing = null;
		return null;
	}
	private boolean coolReactor(final IReactor reactor) {
		for (int x = 0; x < 9; x++) {
			for (int y = 0; y < 6; y++) {
				final ItemStack itemStack = reactor.getItemAt(x, y);
				if ( itemStack != null
				  && itemStack.getItem() instanceof ItemIC2reactorLaserFocus ) {
					final int heatInLaserFocus = itemStack.getDamage();
					final int heatEnergyCap = (int) Math.floor(Math.min(laserMedium_getEnergyStored(false) / WarpDriveConfig.IC2_REACTOR_ENERGY_PER_HEAT, heatInLaserFocus));
					final int heatToTransfer = Math.min(heatEnergyCap, WarpDriveConfig.IC2_REACTOR_COOLING_PER_INTERVAL); 
					if (heatToTransfer > 0) {
						if (laserMedium_consumeExactly((int) Math.ceil(heatToTransfer * WarpDriveConfig.IC2_REACTOR_ENERGY_PER_HEAT), false)) {
							ItemIC2reactorLaserFocus.addHeat(itemStack, -heatToTransfer);
							return true;
						}
					}
					return false;
				}
			}
		}
		return false;
	}
	
	@Override
	public void tick() {
		super.tick();
		
		assert world != null;
		if (world.isRemote()) {
			return;
		}
		
		ticks--;
		if (ticks <= 0)  {
			ticks = WarpDriveConfig.IC2_REACTOR_COOLING_INTERVAL_TICKS;
			final IReactor reactor = findReactor();
			updateBlockState();
			if (reactor == null) {
				return;
			}
			
			if (coolReactor(reactor)) {
				final Vector3 vMonitor = new Vector3(this).translate(0.5);
				PacketHandler.sendBeamPacket(world,
				                             vMonitor,
				                             new Vector3(reactor.getPosition()).translate(0.5D),
				                             0.0f, 0.8f, 1.0f, 20, 0, 20);
			}
		}
	}
	
	private void updateBlockState() {
		final BlockState blockStateNew = getBlockState()
				                                 .with(BlockProperties.ACTIVE, isValid)
				                                 .with(BlockProperties.FACING, facing != null ? facing : Direction.DOWN);
		updateBlockState(blockStateNew, null, null);
	}
	
	@Nonnull
	@Override
	public CompoundNBT write(@Nonnull CompoundNBT tagCompound) {
		tagCompound = super.write(tagCompound);
		tagCompound.putInt("ticks", ticks);
		return tagCompound;
	}
	
	@Override
	public void read(@Nonnull final CompoundNBT tagCompound) {
		super.read(tagCompound);
		ticks = tagCompound.getInt("ticks");
	}
	
	@Override
	public WarpDriveText getStatus() {
		if (world == null) {
			return super.getStatus();
		}
		
		if (facing != null) {
			return super.getStatus() 
					.append(Commons.getStyleCorrect(), "warpdrive.ic2_reactor_laser_cooler.reactor_found",
					        facing.name().toLowerCase());
		} else {
			return super.getStatus()
					.append(Commons.getStyleWarning(), "warpdrive.ic2_reactor_laser_cooler.no_reactor");
		}
	}
	
	@Override
	public Object[] getEnergyRequired() {
		final String units = energy_getDisplayUnits();
		final long energyPerTick = (long) Math.ceil( WarpDriveConfig.IC2_REACTOR_COOLING_PER_INTERVAL
		                                           * WarpDriveConfig.IC2_REACTOR_ENERGY_PER_HEAT
		                                           / WarpDriveConfig.IC2_REACTOR_COOLING_INTERVAL_TICKS );
		return new Object[] { true,
		                      EnergyWrapper.convert(energyPerTick, units) };
	}
}
