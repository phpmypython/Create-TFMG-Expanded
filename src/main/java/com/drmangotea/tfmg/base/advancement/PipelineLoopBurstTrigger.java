package com.drmangotea.tfmg.base.advancement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Fires when a pipe bursts under pressure that came round to itself - a Booster Station whose inlet is fed,
 * however many stations later, by its own outlet.
 * <p>
 * It carries nothing but the player, because the interesting part is that it happened at all.
 */
public class PipelineLoopBurstTrigger extends SimpleCriterionTrigger<PipelineLoopBurstTrigger.Instance> {

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        trigger(player, instance -> true);
    }

    /** The criterion an advancement uses to listen for this. */
    public Criterion<Instance> criterion() {
        return new Criterion<>(this, new Instance(Optional.empty()));
    }

    public record Instance(Optional<ContextAwarePredicate> player) implements SimpleInstance {

        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(builder -> builder
                .group(EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(Instance::player))
                .apply(builder, Instance::new));
    }
}
