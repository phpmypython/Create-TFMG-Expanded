package com.drmangotea.tfmg.mixin.client;

import com.drmangotea.tfmg.content.decoration.pipes.PipeGoggleInfo;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Mechanical pumps and fluid valves are kinetic blocks, so they already write a stress readout of their
 * own. Appending to it rather than replacing it keeps both, and the check for a FluidTransportBehaviour
 * leaves every other kinetic block untouched.
 */
@Mixin(value = KineticBlockEntity.class, remap = false)
public abstract class KineticBlockEntityGoggleMixin extends BlockEntity {

    public KineticBlockEntityGoggleMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(method = "addToGoggleTooltip", at = @At("RETURN"), cancellable = true)
    private void tfmg$addPipeGoggleInfo(List<Component> tooltip, boolean isPlayerSneaking,
                                        CallbackInfoReturnable<Boolean> cir) {
        if (PipeGoggleInfo.addToTooltip(this, tooltip, isPlayerSneaking))
            cir.setReturnValue(true);
    }
}
