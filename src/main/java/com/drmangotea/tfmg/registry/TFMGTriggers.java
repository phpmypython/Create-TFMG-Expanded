package com.drmangotea.tfmg.registry;

import com.drmangotea.tfmg.TFMG;
import com.drmangotea.tfmg.base.advancement.PipelineLoopBurstTrigger;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.advancements.CriterionTrigger;

/**
 * TFMG's own advancement triggers. Create's are not reusable from here - its advancement builder and its
 * trigger list are package-private - so anything the mod wants to award is registered the vanilla way.
 */
public class TFMGTriggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS =
            DeferredRegister.create(Registries.TRIGGER_TYPE, TFMG.MOD_ID);

    public static final DeferredHolder<CriterionTrigger<?>, PipelineLoopBurstTrigger> PIPELINE_LOOP_BURST =
            TRIGGERS.register("pipeline_loop_burst", PipelineLoopBurstTrigger::new);

    public static void register(IEventBus modEventBus) {
        TRIGGERS.register(modEventBus);
    }
}
