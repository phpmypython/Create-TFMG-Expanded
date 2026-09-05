package com.drmangotea.tfmg.ponder.scenes;

import com.drmangotea.tfmg.content.decoration.pipes.TFMGPipes;
import com.drmangotea.tfmg.registry.TFMGBlocks;
import com.drmangotea.tfmg.registry.TFMGFluids;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.ParticleEmitter;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The pump station: building one, and what its pressure does to the line beyond it.
 * <p>
 * Both scenes share a layout - a creative fluid tank of Crude Oil feeding a west-to-east row of station
 * blocks, with the drives standing off to the north so they never sit in front of the row. The schematics
 * hold loose Pump Casings, which is what a player places; each scene swaps them for Pipeline Pumps at the
 * point its story assembles the station. Nothing drives a casing: rotation only ever reaches the row once
 * the Block of Steel has closed it into Pipeline Pumps.
 * <p>
 * The ponder world does not run the pressure walk, so the strain and the burst are scripted, not simulated.
 */
public class PipelineScenes {

    private static final float DRIVE_SPEED = 128;

    /**
     * How long both Plastic Pipes smoke. It runs out on the tick the near one bursts, which is also the tick
     * the far one stops being over-rated: the burst takes away the only pipe joining it to the station.
     */
    private static final int STRAIN_TICKS = 105;

    /**
     * Building a station: two stubs, a row of casings, a Block of Steel, and only then the drives.
     */
    public static void pipelinePump(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("pipeline_pump", "Pipeline Pump Station");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);

        Selection stubs = util.select().position(2, 1, 4)
                .add(util.select().position(6, 1, 4));
        Selection pumpRow = util.select().fromTo(3, 1, 4, 5, 1, 4);
        Selection line = util.select().position(0, 1, 4)
                .add(util.select().position(1, 1, 4))
                .add(util.select().position(7, 1, 4))
                .add(util.select().position(8, 1, 4));
        Selection drives = util.select().fromTo(3, 1, 2, 5, 1, 3);

        BlockPos westStub = util.grid().at(2, 1, 4);
        BlockPos eastStub = util.grid().at(6, 1, 4);
        BlockPos middleCasing = util.grid().at(4, 1, 4);
        Vec3 middleTop = util.vector().topOf(middleCasing);

        // The schematic stores the row as assembly leaves it. Turn the two stubs to face each other again,
        // which is how a loose row sits before a Block of Steel closes it
        scene.world().setBlock(westStub, stub(Direction.EAST), false);
        scene.world().setBlock(eastStub, stub(Direction.WEST), false);

        scene.world().showIndependentSection(stubs, Direction.DOWN);
        scene.idle(10);
        scene.world().showIndependentSection(pumpRow, Direction.DOWN);
        scene.overlay().showText(95)
                .attachKeyFrame()
                .text("A pump station is a Pipeline Stub at each end, with Pump Casings between them")
                .pointAt(middleTop)
                .placeNearTarget();
        scene.idle(110);

        scene.overlay().showControls(middleTop, Pointing.DOWN, 30)
                .rightClick()
                .withItem(TFMGBlocks.STEEL_BLOCK.asStack());
        scene.idle(35);
        scene.world().replaceBlocks(pumpRow, pump(Direction.EAST), true);
        scene.world().setBlock(westStub, stub(Direction.WEST), false);
        scene.world().setBlock(eastStub, stub(Direction.EAST), false);
        scene.overlay().showText(95)
                .attachKeyFrame()
                .text("Right-click a casing with a Block of Steel to close the row into Pipeline Pumps")
                .pointAt(middleTop)
                .placeNearTarget();
        scene.idle(110);

        // Rotation arrives only now: a loose casing is never driven. Drives turning on the finished row say
        // that on their own, so the beat carries no caption
        scene.world().showIndependentSection(drives, Direction.DOWN);
        scene.world().setKineticSpeed(drives, DRIVE_SPEED);
        scene.world().setKineticSpeed(pumpRow, DRIVE_SPEED);
        scene.idle(40);

        scene.world().showIndependentSection(line, Direction.DOWN);
        scene.overlay().showText(85)
                .attachKeyFrame()
                .text("Run pipe onto each stub. Fluid leaves the way the arrow points")
                .pointAt(util.vector().topOf(5, 1, 4))
                .placeNearTarget();
        scene.idle(100);

        scene.overlay().showControls(middleTop, Pointing.DOWN, 40)
                .rightClick()
                .withItem(new ItemStack(TFMGFluids.LUBRICATION_OIL.getBucket().get()));
        scene.overlay().showText(85)
                .attachKeyFrame()
                .text("A bucket of Lubrication Oil lowers the stress a finished station demands")
                .pointAt(middleTop)
                .placeNearTarget();
        scene.idle(100);
    }

    /**
     * What the station's pressure does to the line beyond it: chaining, ratings, distance, strain and a burst.
     */
    public static void pipelinePressure(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("pipeline_pressure", "Pipeline Pressure");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();
        scene.idle(10);

        Selection station = util.select().fromTo(2, 1, 3, 5, 1, 3);
        Selection pumps = util.select().fromTo(3, 1, 3, 4, 1, 3);
        Selection feed = util.select().position(0, 1, 3)
                .add(util.select().position(1, 1, 3));
        Selection drives = util.select().fromTo(3, 1, 1, 4, 1, 2);
        Selection nearRun = util.select().fromTo(6, 1, 3, 8, 1, 3);
        Selection farRun = util.select().fromTo(8, 1, 4, 8, 1, 6);

        BlockPos firstPlastic = util.grid().at(8, 1, 4);
        BlockPos secondPlastic = util.grid().at(8, 1, 5);

        // This scene starts where the other one finished: the station is already built
        scene.world().replaceBlocks(pumps, pump(Direction.EAST), false);
        scene.world().showIndependentSection(station, Direction.DOWN);
        scene.world().showIndependentSection(feed, Direction.DOWN);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("A pump station puts pressure on everything downstream of its outlet")
                .pointAt(util.vector().topOf(5, 1, 3))
                .placeNearTarget();
        scene.idle(105);

        // Said here, at the inlet, rather than at the end: what a station passes on is the first thing the
        // rest of the scene measures against a rating
        scene.overlay().showText(95)
                .attachKeyFrame()
                .text("Stations can follow one another along a line; each adds its pressure to what reaches it")
                .pointAt(util.vector().topOf(2, 1, 3))
                .placeNearTarget();
        scene.idle(110);

        scene.world().showIndependentSection(drives, Direction.DOWN);
        scene.world().setKineticSpeed(drives, DRIVE_SPEED);
        scene.world().setKineticSpeed(pumps, DRIVE_SPEED);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("More pumps, or spinning pumps faster, means more pressure leaving the station")
                .pointAt(util.vector().topOf(4, 1, 2))
                .placeNearTarget();
        scene.idle(105);

        scene.world().showIndependentSection(nearRun, Direction.DOWN);
        scene.overlay().showText(95)
                .attachKeyFrame()
                .text("Every pipe has a pressure rating, and Goggles read what it carries against it")
                .pointAt(util.vector().topOf(6, 1, 3))
                .placeNearTarget();
        scene.idle(110);

        scene.overlay().showText(95)
                .attachKeyFrame()
                .text("Pressure fades with every block it travels, so the strongest pipe belongs nearest the station")
                .pointAt(util.vector().topOf(7, 1, 3))
                .placeNearTarget();
        scene.idle(110);

        scene.world().showIndependentSection(farRun, Direction.DOWN);
        scene.rotateCameraY(40);
        scene.idle(65);
        ChemistryScenes.settleCamera(scene);
        scene.effects().emitParticles(util.vector().centerOf(8, 1, 4), strain(), 0.5f, STRAIN_TICKS);
        scene.effects().emitParticles(util.vector().centerOf(8, 1, 5), strain(), 0.4f, STRAIN_TICKS);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Plastic Pipe cannot take what reaches it here, so it strains and smokes")
                .pointAt(util.vector().topOf(secondPlastic))
                .placeNearTarget();
        scene.idle(105);

        scene.world().destroyBlock(firstPlastic);
        scene.effects().emitParticles(util.vector().centerOf(8, 1, 4), (world, x, y, z) -> {
            for (int i = 0; i < 10; i++)
                world.addParticle(ParticleTypes.CLOUD, x, y, z, (Create.RANDOM.nextFloat() - .5f) * .5f,
                        Create.RANDOM.nextFloat() * .4f, (Create.RANDOM.nextFloat() - .5f) * .5f);
        }, 1, 6);
        scene.idle(10);
        scene.world().setBlock(firstPlastic,
                TFMGFluids.CRUDE_OIL.getSource().defaultFluidState().createLegacyBlock(), false);
        scene.overlay().showControls(util.vector().topOf(firstPlastic), Pointing.DOWN, 50)
                .withItem(TFMGPipes.PIPES.get(TFMGPipes.PipeMaterial.PLASTIC).getPipe().asStack());
        scene.overlay().showText(95)
                .attachKeyFrame()
                .text("Pushed past its rating for long enough a pipe bursts, drops as an item and spills its fluid")
                .pointAt(util.vector().topOf(firstPlastic))
                .placeNearTarget();
        scene.idle(110);
    }

    private static BlockState pump(Direction facing) {
        return TFMGBlocks.PIPELINE_PUMP.getDefaultState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    private static BlockState stub(Direction facing) {
        return TFMGBlocks.STATION_STUB.getDefaultState().setValue(HorizontalDirectionalBlock.FACING, facing);
    }

    /** The smoke a pipe over its rating gives off, matching what the real strain effect emits. */
    private static ParticleEmitter strain() {
        return (world, x, y, z) -> world.addParticle(ParticleTypes.SMOKE, x, y, z,
                (Create.RANDOM.nextFloat() - .5f) * .1f, Create.RANDOM.nextFloat() * .06f,
                (Create.RANDOM.nextFloat() - .5f) * .1f);
    }
}
