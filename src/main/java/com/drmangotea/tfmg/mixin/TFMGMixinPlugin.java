package com.drmangotea.tfmg.mixin;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Decides which of this mod's mixins are worth applying.
 *
 * <p>Everything under {@link #PROPULSION_MIXIN_PACKAGE} is compat for Create Propulsion: Simulated
 * and names types from that mod, so it may only be applied when the mod is installed. Mixin asks
 * this plugin before it applies a mixin to a target, which is early enough that FML has not built
 * its {@code ModList} yet: {@code ModValidator} builds the {@link LoadingModList} first, then calls
 * {@code LoadingModList.addMixinConfigs()}, which only queues configs into
 * {@code DeferredMixinConfigRegistration}; Mixin is handed them later still, from
 * {@code FMLMixinPlatformAgent.prepare()}. So the loading list is always there and fully populated
 * by the time we are asked, while {@code ModList.get()} would be null.
 * (Checked against fancymodloader 4.0.43, the loader NeoForge 21.1.248 ships.)
 */
public class TFMGMixinPlugin implements IMixinConfigPlugin {

	private static final Logger LOGGER = LoggerFactory.getLogger("TFMG Mixin Plugin");

	/** Mixins under this package implement Create Propulsion: Simulated's heat contract. */
	private static final String PROPULSION_MIXIN_PACKAGE = "com.drmangotea.tfmg.mixin.compat.propulsion.";
	private static final String PROPULSION_MOD_ID = "createpropulsion";
	private static final String HEAT_CONSUMER_RESOURCE =
			"dev/propulsionteam/propulsionsimulated/content/heat/IHeatConsumer.class";

	/** {@code IHeatConsumer} as this build compiles against it: method name, then JVM descriptor. */
	private static final String[][] HEAT_CONSUMER_METHODS = {
			{ "isActive", "()Z" },
			{ "getOperatingThreshold", "()F" },
			{ "consumeHeat", "(FFZ)F" },
	};

	private Boolean propulsionHeatCompatUsable;

	@Override
	public void onLoad(String mixinPackage) {}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.startsWith(PROPULSION_MIXIN_PACKAGE))
			return isPropulsionHeatCompatUsable();
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

	/** Answered once and remembered, so the warning below is logged at most once. */
	private boolean isPropulsionHeatCompatUsable() {
		if (propulsionHeatCompatUsable == null)
			propulsionHeatCompatUsable = checkPropulsionHeatContract();
		return propulsionHeatCompatUsable;
	}

	private static boolean checkPropulsionHeatContract() {
		ModFileInfo modFile = LoadingModList.get().getModFileById(PROPULSION_MOD_ID);
		if (modFile == null)
			return false; // Not installed. The ordinary case, and not worth a log line.

		String problem = describeHeatContractProblem(modFile);
		if (problem == null)
			return true;

		LOGGER.warn("Create Propulsion: Simulated is installed but no longer declares the heat contract"
				+ " this build was compiled against ({}). Burner heating for the chemical vat is"
				+ " disabled; everything else is unaffected.", problem);
		return false;
	}

	/**
	 * Describes what is wrong with their heat contract, or null if it is the one we expect.
	 *
	 * <p>Read straight out of their jar with ASM rather than through {@code Class.forName}: at this
	 * point in start-up the mod files are open (Mixin reads its own configs out of them the same
	 * way) but the game class loader is not yet handing out mod classes, and loading one early would
	 * have it transformed and cached before every mixin is registered. Their mod file, not the whole
	 * search path, so a jar that happens to shade the same class cannot answer for them.
	 */
	private static String describeHeatContractProblem(ModFileInfo modFile) {
		ClassNode heatConsumer = new ClassNode();
		try {
			Path classFile = modFile.getFile().findResource(HEAT_CONSUMER_RESOURCE);
			if (classFile == null || !Files.exists(classFile))
				return HEAT_CONSUMER_RESOURCE + " is not in the mod file";
			new ClassReader(Files.readAllBytes(classFile)).accept(heatConsumer,
					ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		} catch (Throwable t) {
			return HEAT_CONSUMER_RESOURCE + " could not be read: " + t;
		}

		if ((heatConsumer.access & Opcodes.ACC_INTERFACE) == 0)
			return "IHeatConsumer is no longer an interface";

		for (String[] method : HEAT_CONSUMER_METHODS) {
			boolean found = heatConsumer.methods.stream()
					.anyMatch(m -> m.name.equals(method[0]) && m.desc.equals(method[1]));
			if (!found)
				return "IHeatConsumer no longer declares " + method[0] + method[1];
		}
		return null;
	}

}
