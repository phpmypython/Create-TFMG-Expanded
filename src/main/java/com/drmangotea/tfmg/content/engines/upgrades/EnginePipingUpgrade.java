package com.drmangotea.tfmg.content.engines.upgrades;


import com.drmangotea.tfmg.content.engines.types.AbstractSmallEngineBlockEntity;
import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.Optional;

public class EnginePipingUpgrade extends EngineUpgrade {

    public Optional<FluidTankBlockEntity> tank = Optional.empty();


    public void findTank(AbstractSmallEngineBlockEntity be) {
        Level level = be.getLevel();

        for (Direction direction : Direction.values()) {
            BlockPos pos = be.getBlockPos().relative(direction);
            if (level.getBlockEntity(pos) instanceof FluidTankBlockEntity foundTank) {

                tank = Optional.of(foundTank);
                return;
            }
        }
        tank = Optional.empty();
    }

    @Override
    public void updateUpgrade(AbstractSmallEngineBlockEntity be) {
        findTank(be);
    }

    @Override
    public void lazyTickUpgrade(AbstractSmallEngineBlockEntity engine) {

        // Re-resolved every lazy tick rather than only when the reference is missing: six
        // getBlockEntity calls at this rate cost nothing and a removed tank can never go stale.
        findTank(engine);
        if (tank.isEmpty())
            return;

        AbstractSmallEngineBlockEntity controller = engine.getControllerBE();
        if (controller == null || controller.fuelTank == null)
            return;

        // Ask the source what it would give up, ask the engine what it would take of that, then move
        // exactly that stack. Sizing the transfer by the source's own remaining headroom moved nothing
        // from a full tank, and re-reading the source's fluid after draining it dry filled the engine
        // with an empty stack, which destroyed the fuel.
        FluidTankBlockEntity tankBE = tank.get();
        FluidStack available = tankBE.getTankInventory().drain(500, IFluidHandler.FluidAction.SIMULATE);
        if (available.isEmpty())
            return;

        int amount = controller.fuelTank.fill(available, IFluidHandler.FluidAction.SIMULATE);
        if (amount == 0)
            return;

        FluidStack drained = tankBE.getTankInventory().drain(amount, IFluidHandler.FluidAction.EXECUTE);
        int filled = controller.fuelTank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled < drained.getAmount())
            tankBE.getTankInventory().fill(drained.copyWithAmount(drained.getAmount() - filled),
                    IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public Optional<? extends EngineUpgrade> createUpgrade() {
        return Optional.of(new EnginePipingUpgrade());
    }

    @Override
    public Item getItem() {
        return TFMGBlocks.INDUSTRIAL_PIPE.asItem();
    }
}
