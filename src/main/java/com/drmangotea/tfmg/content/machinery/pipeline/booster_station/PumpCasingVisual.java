package com.drmangotea.tfmg.content.machinery.pipeline.booster_station;

import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import net.minecraft.core.Direction;

import java.util.function.Consumer;

/**
 * The Flywheel counterpart of {@link PumpCasingRenderer}: the same partial, turned to the same face, spinning
 * about the same axis.
 */
public class PumpCasingVisual extends KineticBlockEntityVisual<PumpCasingBlockEntity>
        implements SimpleTickableVisual {

    private final RotatingInstance shaft;

    public PumpCasingVisual(VisualizationContext context, PumpCasingBlockEntity blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        shaft = instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(PumpCasingRenderer.shaftModel(blockState)))
                .createInstance()
                .rotateToFace(Direction.SOUTH, PumpCasingRenderer.shaftFacing(blockState))
                .setup(blockEntity)
                .setPosition(getVisualPosition());
        shaft.setChanged();
    }

    @Override
    public void update(float partialTick) {
        shaft.setup(blockEntity).setChanged();
    }

    @Override
    public void tick(Context context) {
        applyOverstressEffect(blockEntity, shaft);
    }

    @Override
    public void updateLight(float partialTick) {
        relight(shaft);
    }

    @Override
    protected void _delete() {
        shaft.delete();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(shaft);
    }
}
