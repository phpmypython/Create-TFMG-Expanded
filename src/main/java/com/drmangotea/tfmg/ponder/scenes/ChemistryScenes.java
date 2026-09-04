package com.drmangotea.tfmg.ponder.scenes;

import com.drmangotea.tfmg.registry.TFMGItems;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.EntityElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

public class ChemistryScenes {

    /**
     * Ponder's camera turn eases out over several seconds, and captions shown while it is still
     * creeping shimmer as they track their target. Once the turn is visually over (well under a
     * pixel of travel left) this snaps the camera onto its target so text renders pixel-aligned.
     * <p>
     * Shared with the other scenes in this package that walk their camera.
     */
    static void settleCamera(CreateSceneBuilder scene) {
        scene.addInstruction(ponderScene -> {
            LerpedFloat yaw = ponderScene.getTransform().yRotation;
            yaw.startWithValue(yaw.getChaseTarget());
        });
    }

    /**
     * Sulfur recovery, laid out like a real Claus unit: coke battery -> sour (H2S) gas line ->
     * reaction furnace (a heated vat) fed with combustion air -> condensed sulfur, furnace gas, water.
     */
    public static void clausPlant(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("claus_plant", "Sulfur Recovery: the Claus Unit");
        scene.configureBasePlate(0, 0, 9);
        scene.scaleSceneView(0.6f);
        scene.showBasePlate();

        Selection cokeOven = util.select().fromTo(1, 1, 1, 1, 2, 2);
        Selection sourGasLine = util.select().fromTo(1, 3, 1, 5, 4, 2);
        Selection vat = util.select().fromTo(5, 2, 3, 6, 3, 4);
        Selection fireboxes = util.select().fromTo(5, 1, 3, 6, 1, 4);
        Selection airLine = util.select().fromTo(7, 2, 3, 8, 2, 3);
        Selection furnaceGasLine = util.select().fromTo(5, 1, 5, 5, 2, 7);
        Selection waterLine = util.select().fromTo(2, 1, 4, 4, 2, 4);
        Selection sulfurOut = util.select().fromTo(3, 1, 3, 4, 2, 3);

        scene.world().setKineticSpeed(sourGasLine, 64);
        scene.world().setKineticSpeed(airLine, 64);
        scene.world().setKineticSpeed(furnaceGasLine, 64);
        scene.world().setKineticSpeed(waterLine, 64);

        scene.world().showIndependentSection(cokeOven, Direction.DOWN);
        scene.overlay().showText(100)
                .attachKeyFrame()
                .text("Coal contains sulfur. When a Coke Oven bakes coal, that sulfur leaves as hydrogen sulfide in the raw gas at the top, which is why it comes out as Sour Gas")
                .pointAt(util.vector().blockSurface(util.grid().at(1, 2, 1), Direction.WEST))
                .placeNearTarget();
        scene.idle(120);

        scene.world().showIndependentSection(sourGasLine, Direction.DOWN);
        scene.overlay().showText(90)
                .attachKeyFrame()
                .text("Sour Gas burns like Furnace Gas, so a Flarestack or a Blast Stove will happily take it. Every bucket burned is sulfur thrown away")
                .pointAt(util.vector().topOf(util.grid().at(3, 4, 1)))
                .placeNearTarget();
        scene.idle(110);

        scene.world().showIndependentSection(fireboxes, Direction.DOWN);
        scene.world().showIndependentSection(vat, Direction.DOWN);
        scene.overlay().showText(110)
                .attachKeyFrame()
                .text("A real Claus unit is a reaction furnace: a third of the hydrogen sulfide is burned with air, and the rest reacts with it over catalyst beds to condense elemental sulfur")
                .pointAt(util.vector().blockSurface(util.grid().at(5, 3, 3), Direction.WEST))
                .placeNearTarget();
        scene.idle(130);

        scene.overlay().showText(100)
                .attachKeyFrame()
                .text("Here the reaction furnace is a Steel or Firebrick Chemical Vat of at least four blocks at heat level 3: two Fireboxes, or three kindled Blaze Burners, under the footprint")
                .pointAt(util.vector().blockSurface(util.grid().at(5, 1, 3), Direction.WEST))
                .placeNearTarget();
        scene.idle(120);

        scene.world().showIndependentSection(airLine, Direction.DOWN);
        scene.overlay().showText(120)
                .attachKeyFrame()
                .text("Combustion air comes from an Air Intake, pumped into the vat like any other ingredient. The intake takes its rotation on the back face, so the pump goes on a side")
                .pointAt(util.vector().blockSurface(util.grid().at(8, 2, 3), Direction.UP))
                .placeNearTarget();
        scene.idle(140);

        scene.overlay().showText(100)
                .attachKeyFrame()
                .text("Every 2000 mB of Sour Gas and 500 mB of air becomes 2 Sulfur Dust, 1500 mB of Furnace Gas and 250 mB of Water")
                .pointAt(util.vector().blockSurface(util.grid().at(6, 3, 4), Direction.EAST))
                .placeNearTarget();
        scene.idle(120);

        // Outputs sit behind the vat from the opening angle: walk the camera around the plant
        scene.world().showIndependentSection(furnaceGasLine, Direction.DOWN);
        scene.rotateCameraY(180);
        scene.idle(70);
        settleCamera(scene);
        scene.overlay().showText(130)
                .attachKeyFrame()
                .text("The vat now holds two fluids, and a pump on its own takes whichever it finds first. A Smart Fluid Pipe against the vat, filtered to Furnace Gas, draws only the gas")
                .pointAt(util.vector().blockSurface(util.grid().at(5, 2, 5), Direction.EAST))
                .placeNearTarget();
        scene.idle(150);

        scene.overlay().showText(110)
                .attachKeyFrame()
                .text("Furnace Gas is the cleaned coke-oven gas a real steelworks fires its stoves with. Pipe it back to the Blast Stoves")
                .pointAt(util.vector().blockSurface(util.grid().at(5, 2, 7), Direction.SOUTH))
                .placeNearTarget();
        scene.idle(130);

        scene.world().showIndependentSection(waterLine, Direction.DOWN);
        scene.rotateCameraY(90);
        scene.idle(60);
        settleCamera(scene);
        scene.overlay().showText(120)
                .attachKeyFrame()
                .text("A second Smart Fluid Pipe, filtered to Water, pulls the water out of another face. Burning hydrogen sulfide makes water, and a real plant condenses it out the same way")
                .pointAt(util.vector().blockSurface(util.grid().at(4, 2, 4), Direction.UP))
                .placeNearTarget();
        scene.idle(140);

        // Stop due west: from the opening angle the sour gas run hides the depot
        scene.world().showIndependentSection(sulfurOut, Direction.DOWN);
        scene.rotateCameraY(45);
        scene.idle(60);
        settleCamera(scene);
        ItemStack sulfur = new ItemStack(TFMGItems.SULFUR_DUST.get(), 2);
        ElementLink<EntityElement> item = scene.world().createItemEntity(util.vector().centerOf(3, 3, 3), util.vector().of(0, 0, 0), sulfur);
        scene.overlay().showText(110)
                .attachKeyFrame()
                .text("Sulfur Dust leaves through any funnel or arm: enough for sulfuric acid, rubber and semiconductors")
                .pointAt(util.vector().topOf(util.grid().at(3, 2, 3)))
                .placeNearTarget();
        scene.idle(130);
        scene.world().modifyEntity(item, Entity::discard);
        scene.idle(10);
    }
}
