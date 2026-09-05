package com.drmangotea.tfmg.base;

import com.drmangotea.tfmg.TFMG;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.Map;


@EventBusSubscriber
public class TFMGRemapper {
    /**
     * @see String The old name of the block/item (String)
     * <p>
     * @see ResourceLocation The new name of the block/item (ResourceLocation)
     */
    private static final Map<String, ResourceLocation> reMapBlock = new HashMap<>();
    private static final Map<String, ResourceLocation> reMapItem = new HashMap<>();
    private static final Map<String, ResourceLocation> reMapFluid = new HashMap<>();

    static {
        //Blocks
        reMapBlock.put("copper_encased_brass_pipe", TFMG.asResource("encased_brass_pipe"));
        reMapBlock.put("copper_encased_steel_pipe", TFMG.asResource("encased_steel_pipe"));
        reMapBlock.put("copper_encased_aluminum_pipe", TFMG.asResource("encased_aluminum_pipe"));
        reMapBlock.put("copper_encased_cast_iron_pipe", TFMG.asResource("encased_cast_iron_pipe"));
        reMapBlock.put("copper_encased_plastic_pipe", TFMG.asResource("encased_plastic_pipe"));

        // Renamed in releases 1.3.0 to 1.5.1 of this project, which dropped the "casing" from the heavy
        // encased family. A world built on one of those releases holds the shorter names; without
        // these the blocks resolve to nothing and the chunk turns them to air on load. Aliases in
        // reMapBlock are applied to the item registry as well, so the block-items follow.
        reMapBlock.put("heavy_encased_shaft", TFMG.asResource("heavy_casing_encased_shaft"));
        reMapBlock.put("heavy_encased_steel_cogwheel", TFMG.asResource("heavy_casing_encased_steel_cogwheel"));
        reMapBlock.put("heavy_encased_large_steel_cogwheel", TFMG.asResource("heavy_casing_encased_large_steel_cogwheel"));
        reMapBlock.put("heavy_encased_aluminum_cogwheel", TFMG.asResource("heavy_casing_encased_aluminum_cogwheel"));
        reMapBlock.put("heavy_encased_large_aluminum_cogwheel", TFMG.asResource("heavy_casing_encased_large_aluminum_cogwheel"));

        //Items
        // Not a rename: the autogas cylinder is an item from releases 1.3.0 to 1.5.1 with no counterpart here.
        // Pointing it at the plain engine cylinder is a lossy substitution, chosen because the
        // alternative is the whole ItemStack failing to decode and the slot emptying silently.
        reMapItem.put("autogas_engine_cylinder", TFMG.asResource("engine_cylinder"));
    }

    @SubscribeEvent
    public static void remap(RegisterEvent event) {
        Registry<?> registry = event.getRegistry();

        if (registry.key() == Registries.BLOCK) {
            reMapBlock.forEach((string, resourceLocation) -> registry.addAlias(TFMG.asResource(string), resourceLocation));
            TFMG.LOGGER.info("[TFMG Remapper] Remapped {} blocks", reMapBlock.size());
        }
        if (registry.key() == Registries.ITEM) {
            reMapBlock.forEach((string, resourceLocation) -> registry.addAlias(TFMG.asResource(string), resourceLocation));
            reMapItem.forEach((string, resourceLocation) -> registry.addAlias(TFMG.asResource(string), resourceLocation));
            int reMapSize = reMapBlock.size() + reMapItem.size();
            TFMG.LOGGER.info("[TFMG Remapper] Remapped {} items", reMapSize);
        }
        if (registry.key() == Registries.FLUID) {
            reMapFluid.forEach((string, resourceLocation) -> registry.addAlias(TFMG.asResource(string), resourceLocation));
            TFMG.LOGGER.info("[TFMG Remapper] Remapped {} fluids", reMapFluid.size());
        }
    }
}
