package com.drmangotea.tfmg.ponder.scenes;

import com.drmangotea.tfmg.content.electricity.utilities.polarizer.PolarizerBlockEntity;
import com.drmangotea.tfmg.registry.TFMGItems;
import com.simibubi.create.Create;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class ElectricityScenes {

    public static void magnetBootstrap(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("magnet_bootstrap", "Magnets: from Lightning to the Polarizer");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.75f);
        scene.showBasePlate();

        Selection lightningRod = util.select().fromTo(5, 1, 1, 5, 3, 1);
        Selection polarizerSetup = util.select().fromTo(2, 1, 5, 6, 1, 5);
        scene.world().setKineticSpeed(polarizerSetup, 128);

        // Item entities are not part of the block scene, so the identify key cannot name them;
        // the item bubble below is what tells the viewer what is lying on the ground.
        ItemStack ingots = new ItemStack(TFMGItems.MAGNETIC_ALLOY_INGOT.get(), 6);
        ElementLink<EntityElement> drop = scene.world().createItemEntity(util.vector().centerOf(2, 2, 2), util.vector().of(0, 0, 0), ingots);
        scene.overlay().showControls(util.vector().centerOf(2, 1, 2), Pointing.DOWN, 60).withItem(ingots);
        scene.overlay().showText(70)
                .attachKeyFrame()
                .text("Magnets are the first thing the electric age needs, and the first one has to be made without electricity")
                .pointAt(util.vector().centerOf(2, 1, 2))
                .placeNearTarget();
        scene.idle(80);

        scene.overlay().showText(130)
                .attachKeyFrame()
                .text("Magnetic alloy ingots are not magnets yet. A strong enough magnetic pulse aligns their grains permanently, and nothing pulses harder than lightning")
                .pointAt(util.vector().centerOf(2, 1, 2))
                .placeNearTarget();
        scene.idle(140);

        scene.world().showIndependentSection(lightningRod, Direction.DOWN);
        scene.overlay().showText(120)
                .attachKeyFrame()
                .text("Drop a stack of ingots on the ground in a thunderstorm next to a lightning rod, or hit the stack with a Channeling trident")
                .pointAt(util.vector().topOf(util.grid().at(5, 3, 1)))
                .placeNearTarget();
        scene.idle(130);

        scene.effects().emitParticles(new Vec3(2.5, 1.2, 2.5), (world, x, y, z) -> {
            for (int i = 0; i < 6; i++)
                world.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, (Create.RANDOM.nextFloat() - .5f) * .6f, Create.RANDOM.nextFloat() * .6f, (Create.RANDOM.nextFloat() - .5f) * .6f);
            world.addParticle(ParticleTypes.FLASH, x, y + 1, z, 0, 0, 0);
        }, 1, 20);
        scene.idle(20);
        ItemStack magnets = new ItemStack(TFMGItems.MAGNET.get(), 2);
        scene.world().modifyEntity(drop, e -> ((ItemEntity) e).setItem(magnets));
        scene.overlay().showControls(util.vector().centerOf(2, 1, 2), Pointing.DOWN, 60).withItem(magnets);
        scene.overlay().showText(190)
                .attachKeyFrame()
                .text("The strike converts a random part of the stack - anywhere from none of it to all of it - and destroys whatever is left over, so split your alloy into several small drops instead of risking it all at once")
                .pointAt(util.vector().centerOf(2, 1, 2))
                .placeNearTarget();
        scene.idle(200);
        scene.world().modifyEntity(drop, Entity::discard);

        scene.world().showIndependentSection(polarizerSetup, Direction.DOWN);
        scene.overlay().showText(190)
                .attachKeyFrame()
                .text("That is only the bootstrap. With a few magnets you can build a generator, and with power the Polarizer does the same job with an electromagnet: every ten seconds, with no losses")
                .pointAt(util.vector().blockSurface(util.grid().at(2, 1, 5), Direction.WEST))
                .placeNearTarget();
        scene.idle(200);

        scene.overlay().showText(200)
                .attachKeyFrame()
                .text("The Polarizer draws V\u00b2 / 30 \u03a9 and only charges above " + PolarizerBlockEntity.MINIMUM_POWER + " W, so it wants at least 174 V: a small generator at 165 RPM or more, or a weaker one stepped up through a transformer. Each magnet takes 200 ticks")
                .pointAt(util.vector().blockSurface(util.grid().at(6, 1, 5), Direction.UP))
                .placeNearTarget();
        scene.idle(210);

        scene.overlay().showText(140)
                .attachKeyFrame()
                .text("Generators, motors, stators and the electric pump are all built from these magnets, and the first generator is what makes the Polarizer possible")
                .placeNearTarget();
        scene.idle(150);
    }
}
