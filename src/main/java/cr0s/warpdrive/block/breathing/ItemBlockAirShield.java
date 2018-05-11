package cr0s.warpdrive.block.breathing;

import cr0s.warpdrive.block.ItemBlockAbstractBase;

import javax.annotation.Nonnull;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemBlockAirShield extends ItemBlockAbstractBase {
	
	public ItemBlockAirShield(final Block block) {
		super(block);
		setHasSubtypes(true);
		setUnlocalizedName("warpdrive.breathing.air_shield");
	}
	
	@Override
	public int getMetadata(final int damage) {
		return damage;
	}
	
	@Override
	public void getSubItems(@Nonnull final Item item, @Nonnull final CreativeTabs creativeTabs, @Nonnull final List<ItemStack> list) {
		for (int metadata = 0; metadata < 16; metadata++) {
			list.add(new ItemStack(item, 1, metadata));
		}
	}
	
	@Nonnull
	@Override
	public String getUnlocalizedName(final ItemStack itemstack) {
		return getUnlocalizedName();
	}
}
