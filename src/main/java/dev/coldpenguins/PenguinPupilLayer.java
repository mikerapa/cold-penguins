package dev.coldpenguins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Small, non-emissive pupils attached to the animated head rather than painted onto it. */
final class PenguinPupilLayer extends RenderLayer<PenguinRenderer.State, PenguinRenderer.Model> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ColdPenguins.ID, "textures/entity/penguin_pupil.png");
    private final ModelPart pupil;

    PenguinPupilLayer(RenderLayerParent<PenguinRenderer.State, PenguinRenderer.Model> parent) {
        super(parent);
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("pupil", CubeListBuilder.create()
                .addBox(-0.5F, -0.5F, -0.04F, 1, 1, 0.02F), PartPose.ZERO);
        pupil = LayerDefinition.create(mesh, 4, 4).bakeRoot().getChild("pupil");
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, PenguinRenderer.State state,
                       float headYaw, float headPitch) {
        if (state.isInvisible) return;
        Eyes eyes = Eyes.forPenguin(state);
        float idle = state.deathTime == 0 ? 0.45F * Mth.sin(state.ageInTicks * 0.055F + state.entityId * 1.7F) : 0;
        float gazeX = state.deathTime == 0 ? Mth.clamp(-state.yRot / 50.0F + idle, -1, 1) : 0;
        float gazeY = state.deathTime == 0 ? Mth.clamp(state.xRot / 45.0F, -1, 1) : 0;
        pose.pushPose();
        getParentModel().root().translateAndRotate(pose);
        getParentModel().head().translateAndRotate(pose);
        for (int eye = 0; eye < 2; eye++) {
            float x = eye == 0 ? eyes.leftX : eyes.rightX;
            pose.pushPose();
            pose.translate((x + gazeX * eyes.travelX) / 16.0F,
                    (eyes.y + gazeY * eyes.travelY) / 16.0F, eyes.z / 16.0F);
            pose.scale(eyes.width, eyes.height, 1);
            collector.submitModelPart(pupil, pose, RenderTypes.entityCutout(TEXTURE), light,
                    LivingEntityRenderer.getOverlayCoords(state, 0), null, -1, state.outlineColor);
            pose.popPose();
        }
        pose.popPose();
    }

    private record Eyes(float leftX, float rightX, float y, float z,
                        float width, float height, float travelX, float travelY) {
        static Eyes forPenguin(PenguinRenderer.State state) {
            if (state.species == PenguinSpecies.ORIGINAL)
                return new Eyes(-2, 2, -1.5F, -3, 0.5F, 0.7F, 0.22F, 0.12F);
            if (state.isBaby)
                return new Eyes(-2.5F, 2.5F, -2, -3.5F, 0.45F, 0.95F, 0.22F, 0.35F);
            if (state.species == PenguinSpecies.EMPEROR)
                return new Eyes(-2.25F, 2.75F, -2.75F, -3.5F, 0.6F, 0.9F, 0.35F, 0.22F);
            if (state.species == PenguinSpecies.AFRICAN)
                return new Eyes(-1.25F, 1.75F, -2.75F, -3, 0.6F, 0.9F, 0.35F, 0.22F);
            return new Eyes(-2, 2, -3, -3, 0.45F, 0.95F, 0.22F, 0.35F);
        }
    }
}
