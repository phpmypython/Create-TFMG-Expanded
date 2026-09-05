package com.drmangotea.tfmg.datagen;

import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.drmangotea.tfmg.registry.TFMGTriggers;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * TFMG's advancements.
 * <p>
 * They hang off {@code create:root} rather than a root of their own: the mod is a Create addon, one
 * advancement does not warrant its own tab, and this is where a player looks for the rest of the machinery.
 * An advancement whose parent is missing is dropped with a log line, so the worst a future Create rename can
 * do is make this unobtainable.
 */
public class TFMGAdvancements implements AdvancementProvider.AdvancementGenerator {

    private static final ResourceLocation CREATE_ROOT = ResourceLocation.fromNamespaceAndPath("create", "root");

    @Override
    public void generate(HolderLookup.Provider registries, Consumer<AdvancementHolder> saver,
                         ExistingFileHelper existingFileHelper) {
        Advancement.Builder.advancement()
                .parent(CREATE_ROOT)
                .display(new DisplayInfo(TFMGBlocks.PUMP_CASING.asStack(),
                        Component.translatable("advancement.tfmg.ouroboros"),
                        Component.translatable("advancement.tfmg.ouroboros.desc"),
                        Optional.empty(), AdvancementType.CHALLENGE, true, true, true))
                .addCriterion("pipeline_loop_burst", TFMGTriggers.PIPELINE_LOOP_BURST.get().criterion())
                .save(saver, "tfmg:ouroboros");
    }
}
