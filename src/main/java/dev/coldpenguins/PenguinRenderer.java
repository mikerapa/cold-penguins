package dev.coldpenguins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = ColdPenguins.ID, value = Dist.CLIENT)
public final class PenguinRenderer extends MobRenderer<Penguin, PenguinRenderer.State, PenguinRenderer.Model> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            ColdPenguins.ID, "textures/entity/penguin.png");

    private static final Identifier CHICK_TEXTURE = Identifier.fromNamespaceAndPath(
            ColdPenguins.ID, "textures/entity/penguin_chick.png");

    private static final Identifier GOLDEN_TEXTURE = Identifier.fromNamespaceAndPath(
            ColdPenguins.ID, "textures/entity/penguin_golden.png");

    public PenguinRenderer(EntityRendererProvider.Context context) {
        super(context, new Model(), 0.35F);
        addLayer(new SantaHatLayer(this));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ColdPenguins.PENGUIN.get(), PenguinRenderer::new);
        event.registerEntityRenderer(ColdPenguins.THROWN_EGG.get(), ThrownItemRenderer::new);
        event.registerEntityRenderer(ColdPenguins.BLACK_ARROW_PROJECTILE.get(), BlackTippedArrowRenderer::new);
    }

    @SubscribeEvent
    public static void registerHatModel(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private final SantaHatModel hat = new SantaHatModel();

            @Override
            public net.minecraft.client.model.Model getHumanoidArmorModel(ItemStack stack,
                    net.minecraft.client.resources.model.EquipmentClientInfo.LayerType layerType,
                    net.minecraft.client.model.Model original) {
                return hat;
            }
        }, ColdPenguins.SANTA_HAT.get());
    }

    public static final class State extends LivingEntityRenderState {
        boolean golden, santaHat, dancing, buildingNest;
        int entityId;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(Penguin penguin, State state, float partialTick) {
        super.extractRenderState(penguin, state, partialTick);
        state.golden = penguin.hasGoldenPlumage();
        state.santaHat = penguin.hasSantaHat();
        state.dancing = penguin.isDancing();
        state.buildingNest = penguin.isBuildingNest();
        state.entityId = penguin.getId();
    }

    private static final class SantaHatLayer extends RenderLayer<State, Model> {
        private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
                ColdPenguins.ID, "textures/models/armor/santa_hat_layer_1.png");
        private final SantaHatModel hat = new SantaHatModel();

        SantaHatLayer(RenderLayerParent<State, Model> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State penguin,
                           float headYaw, float headPitch) {
            if (!penguin.santaHat || penguin.isInvisible) {
                return;
            }
            pose.pushPose();
            getParentModel().root().translateAndRotate(pose);
            getParentModel().head.translateAndRotate(pose);
            // The head crown is at -4 model pixels; keep the brim bottom above it.
            pose.translate(0, 2.5F / 16.0F, 0);
            pose.scale(0.82F, 0.82F, 0.82F);
            collector.submitModelPart(hat.head, pose, RenderTypes.entityCutout(TEXTURE), light,
                    LivingEntityRenderer.getOverlayCoords(penguin, 0), null);
            pose.popPose();
        }
    }

    @Override
    public Identifier getTextureLocation(State penguin) {
        if (penguin.isBaby) {
            return CHICK_TEXTURE;
        }
        return penguin.golden ? GOLDEN_TEXTURE : TEXTURE;
    }

    @Override
    protected void setupRotations(State penguin, PoseStack pose, float bodyRot, float scale) {
        super.setupRotations(penguin, pose, bodyRot, scale);
        if (penguin.isInWater && penguin.deathTime == 0) {
            float pivot = 0.45F * penguin.ageScale;
            float pitch = Mth.clamp(penguin.xRot, -80.0F, 80.0F);
            pose.translate(0, pivot, 0);
            pose.rotate(Axis.XP.rotationDegrees(-75.0F - pitch));
            pose.translate(0, -pivot, 0);
        }
    }

    @Override
    protected void scale(State penguin, PoseStack pose) {
        pose.scale(penguin.ageScale, penguin.ageScale, penguin.ageScale);
    }

    static final class Model extends EntityModel<State> {
        private final ModelPart head;
        private final ModelPart leftFlipper;
        private final ModelPart rightFlipper;
        private final ModelPart leftFoot;
        private final ModelPart rightFoot;

        Model() {
            this(createRoot());
        }

        Model(ModelPart root) {
            super(root);
            head = root.getChild("head");
            leftFlipper = root.getChild("left_flipper");
            rightFlipper = root.getChild("right_flipper");
            leftFoot = root.getChild("left_foot");
            rightFoot = root.getChild("right_foot");
        }

        private static ModelPart createRoot() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition parts = mesh.getRoot();
            parts.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-4, -10, -3, 8, 10, 6), PartPose.offset(0, 22, 0));
            parts.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 17)
                    .addBox(-3.5F, -4, -3, 7, 5, 6).texOffs(30, 17)
                    .addBox(-1.5F, -1, -5, 3, 2, 2), PartPose.offset(0, 12, 0));
            parts.addOrReplaceChild("left_flipper", CubeListBuilder.create().texOffs(30, 0)
                    .addBox(0, 0, -2, 1, 7, 4), PartPose.offset(4, 13, 0));
            parts.addOrReplaceChild("right_flipper", CubeListBuilder.create().texOffs(30, 0)
                    .addBox(-1, 0, -2, 1, 7, 4), PartPose.offset(-4, 13, 0));
            parts.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(42, 0)
                    .addBox(-1.5F, 0, -4, 3, 2, 5), PartPose.offset(2, 22, 0));
            parts.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(42, 0)
                    .addBox(-1.5F, 0, -4, 3, 2, 5), PartPose.offset(-2, 22, 0));

            return LayerDefinition.create(mesh, 64, 32).bakeRoot();
        }

        @Override
        public void setupAnim(State penguin) {
            super.setupAnim(penguin);
            float limbSwing = penguin.walkAnimationPos;
            float limbSwingAmount = penguin.walkAnimationSpeed;
            float age = penguin.ageInTicks;
            float headYaw = penguin.yRot;
            float headPitch = penguin.xRot;
            root.x = 0;
            root.y = 0;
            root.z = 0;
            root.xRot = 0;
            root.yRot = 0;
            root.zRot = 0;
            head.zRot = 0;
            leftFoot.y = rightFoot.y = 22;
            leftFoot.z = rightFoot.z = 0;
            head.yRot = headYaw * Mth.DEG_TO_RAD;
            head.xRot = headPitch * Mth.DEG_TO_RAD;

            if (penguin.isInWater) {
                float stroke = Mth.sin(age * 0.55F);
                head.xRot = 0.0F;
                leftFlipper.zRot = -0.95F + 0.40F * stroke;
                leftFlipper.xRot = 0.55F * Mth.cos(age * 0.55F);
                rightFlipper.xRot = leftFlipper.xRot;
                leftFoot.xRot = -0.35F + 0.12F * stroke;
                rightFoot.xRot = -0.35F - 0.12F * stroke;
            } else if (penguin.dancing) {
                float beat = age * (float) (Math.PI * 128.0 / (60.0 * 20.0));
                float sway = Mth.sin(beat);
                root.zRot = 0.19F * sway;
                root.x = 18.0F * Mth.sin(root.zRot);
                root.y = -2.3F * Math.abs(Mth.sin(beat * 2));
                root.yRot = 0.28F * Mth.sin(beat * 0.5F);
                head.zRot = -root.zRot * 0.6F;
                head.xRot = 0.13F * Mth.sin(beat * 2);
                leftFlipper.zRot = -0.75F - 0.40F * sway;
                leftFlipper.xRot = 0.25F * Mth.sin(beat * 2);
                rightFlipper.xRot = -leftFlipper.xRot;
                leftFoot.y -= 1.5F * Math.max(0, sway);
                rightFoot.y -= 1.5F * Math.max(0, -sway);
                leftFoot.xRot = 0.2F * sway;
                rightFoot.xRot = -0.2F * sway;
            } else if (penguin.buildingNest) {
                float peck = (1.0F + Mth.sin((age + penguin.entityId * 1.7F) * 0.78F)) * 0.5F;
                root.xRot = 0.13F + 0.30F * peck;
                root.z = -16.0F * Mth.sin(root.xRot);
                head.xRot = 0.16F + 0.48F * peck;
                leftFlipper.zRot = -0.22F - 0.28F * peck;
                leftFlipper.xRot = rightFlipper.xRot = -0.38F * peck;
                leftFoot.xRot = rightFoot.xRot = -root.xRot;
            } else {
                float stride = limbSwing * 0.92F;
                float amplitude = Mth.clamp(limbSwingAmount * 1.6F, 0.0F, 1.0F);
                float step = Mth.sin(stride) * amplitude;
                root.zRot = 0.14F * step;
                root.x = 18.0F * Mth.sin(root.zRot);
                root.y = -0.65F * Math.abs(step);
                head.zRot = -0.65F * root.zRot;
                leftFoot.y -= 1.35F * Math.max(0, step);
                rightFoot.y -= 1.35F * Math.max(0, -step);
                leftFoot.z = -1.6F * step;
                rightFoot.z = 1.6F * step;
                leftFoot.xRot = 0.38F * step;
                rightFoot.xRot = -0.38F * step;
                float cycle = (age + (penguin.entityId & 63)) % 145.0F;
                float flap = cycle < 18.0F ? Mth.sin(cycle * 0.38F) * 0.42F : 0.0F;
                leftFlipper.zRot = -0.15F - 0.20F * amplitude - flap;
                leftFlipper.xRot = -0.28F * step;
                rightFlipper.xRot = 0.28F * step;
            }
            rightFlipper.zRot = -leftFlipper.zRot;
        }

    }
}
