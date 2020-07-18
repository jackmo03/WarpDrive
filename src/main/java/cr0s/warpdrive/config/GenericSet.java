package cr0s.warpdrive.config;

import cr0s.warpdrive.WarpDrive;
import cr0s.warpdrive.api.IXmlRepresentable;
import cr0s.warpdrive.api.IXmlRepresentableUnit;

import org.w3c.dom.Element;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

import net.minecraftforge.common.util.Constants.NBT;

/**
 * Represents a set of 'units' that will be chosen randomly during world generation.
 **/
public class GenericSet<E extends IXmlRepresentableUnit> implements IXmlRepresentable, Comparable<GenericSet<E>> {
	
	protected String group;
	protected String name;
	private final E unitDefault;
	private final String nameElementUnit;
	private final XmlRandomCollection<E> units;
	private final ArrayList<String> importGroupNames;
	private final ArrayList<String> importGroups;
	
	public String getFullName() {
		return group + ":" + name;
	}
	
	@Nonnull
	@Override
	public String getName() {
		return name;
	}
	
	public GenericSet(final String group, final String name, final E unitDefault, final String nameElementUnit) {
		this.group = group;
		this.name = name;
		this.unitDefault = unitDefault;
		this.nameElementUnit = nameElementUnit;
		units = new XmlRandomCollection<>();
		importGroupNames = new ArrayList<>();
		importGroups = new ArrayList<>();
	}
	
	public GenericSet(final CompoundNBT tagCompound, final E unitDefault, final String nameElementUnit) {
		if (tagCompound.contains("group")) {
			group = tagCompound.getString("group");
		} else {
			group = null;
		}
		name = tagCompound.getString("name");
		this.unitDefault = unitDefault;
		this.nameElementUnit = nameElementUnit;
		units = new XmlRandomCollection<>();
		units.loadFromNBT(tagCompound.getCompound("units"), (String name) -> {
			if (unitDefault instanceof Filler) {
				final Filler filler = new Filler();
				if (filler.loadFromName(name)) {
					return (E) filler;
				}
			}
			return unitDefault; // TODO not implemented
		});
		
		final ListNBT listImportGroupNames = tagCompound.getList("importGroupNames", NBT.TAG_STRING);
		importGroupNames = new ArrayList<>();
		for (int indexImportGroupName = 0; indexImportGroupName < listImportGroupNames.size(); indexImportGroupName++) {
			final String importGroupName = listImportGroupNames.getString(indexImportGroupName);
			importGroupNames.add(importGroupName);
		}
		
		final ListNBT listImportGroups = tagCompound.getList("importGroups", NBT.TAG_STRING);
		importGroups = new ArrayList<>();
		for (int indexImportGroup = 0; indexImportGroup < listImportGroups.size(); indexImportGroup++) {
			final String importGroup = listImportGroups.getString(indexImportGroup);
			importGroups.add(importGroup);
		}
	}
	
	public CompoundNBT write(@Nonnull final CompoundNBT tagCompound) {
		if (group != null) {
			tagCompound.putString("group", group);
		}
		tagCompound.putString("name", name);
		tagCompound.put("units", units.write(new CompoundNBT()));
		
		if (!importGroupNames.isEmpty()) {
			final ListNBT listImportGroupNames = new ListNBT();
			for (final String importGroupName : importGroupNames) {
				listImportGroupNames.add(StringNBT.valueOf(importGroupName));
			}
			tagCompound.put("importGroupNames", listImportGroupNames);
		}
		
		if (!importGroups.isEmpty()) {
			final ListNBT listImportGroups = new ListNBT();
			for (final String importGroup : importGroups) {
				listImportGroups.add(StringNBT.valueOf(importGroup));
			}
			tagCompound.put("importGroups", listImportGroups);
		}
		
		return tagCompound;
	}
	
	public boolean isEmpty() {
		return units.isEmpty();
	}
	
	public E getRandomUnit(final Random random) {
		E unit = units.getRandomEntry(random);
		if (unit == null) {
			WarpDrive.logger.error(String.format("null %s encountered in set %s of %d elements, using default %s instead",
			                                     nameElementUnit, getFullName(), units.elements().size(), unitDefault));
			unit = unitDefault;
		}
		return unit;
	}
	
	@Override
	public boolean loadFromXmlElement(final Element element) throws InvalidXmlException {
		final List<Element> listChildren = XmlFileManager.getChildrenElementByTagName(element, nameElementUnit);
		for (final Element elementChild : listChildren) {
			@SuppressWarnings("unchecked")
			final E unit = (E) unitDefault.constructor();
			units.loadFromXML(unit, elementChild);
		}
		
		final List<Element> listImports = XmlFileManager.getChildrenElementByTagName(element, "import");
		if (!listImports.isEmpty()) { 
			for (final Element elementImport : listImports) {
				final String importGroup = elementImport.getAttribute("group");
				final String importName = elementImport.getAttribute("name");
				if (!importGroup.isEmpty()) {
					if (!importName.isEmpty()) {
						importGroupNames.add(importGroup + ":" + importName);
					} else {
						importGroups.add(importGroup);
					}
				} else {
					WarpDrive.logger.warn(String.format("Ignoring import with no group definition in import element from %s", getFullName()));
				}
			}
		}
		
		return true;
	}
	
	@Override
	public int compareTo(@Nonnull final GenericSet genericSet) {
		return name.compareTo(genericSet.name);
	}
	
	@Override
	public String toString() {
		return getFullName() + "(" + (units == null ? "-empty-" : units.elements().size()) + ")";
	}
	
	/**
	 * Adds the units from the given genericSet into this one. Must be pre-finishConstruction()
	 *
	 * @param genericSet
	 *            The genericSet to add from
	 */
	public void loadFrom(final GenericSet<E> genericSet) {
		units.loadFrom(genericSet.units);
	}
	
	/**
	 * Return static import dependencies
	 * 
	 * @return null or a list of group:names to be imported
	 **/
	public Collection<String> getImportGroupNames() {
		return importGroupNames; 
	}
	
	/**
	 * Return dynamic import dependencies
	 * 
	 * @return null or a list of groups to be imported
	 **/
	public Collection<String> getImportGroups() {
		return importGroups; 
	}
}
