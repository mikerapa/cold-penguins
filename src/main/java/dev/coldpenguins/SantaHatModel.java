package dev.coldpenguins;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.world.entity.LivingEntity;

final class SantaHatModel extends HumanoidModel<LivingEntity> {
    SantaHatModel() {
        super(createParts().bakeRoot());
    }

    private static LayerDefinition createParts() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.6F, -8.5F, -4.6F, 9.2F, 2.0F, 9.2F)
                .texOffs(0, 0).addBox(-4.1F, -12.5F, -4.1F, 8.2F, 4.0F, 8.2F)
                .texOffs(0, 0).addBox(-2.9F, -15.5F, -3.0F, 6.0F, 3.0F, 6.0F)
                .texOffs(0, 0).addBox(-0.7F, -17.0F, -2.0F, 4.0F, 2.0F, 4.0F)
                .texOffs(0, 0).addBox(2.4F, -16.5F, -1.4F, 2.0F, 2.3F, 2.8F)
                .texOffs(40, 16).addBox(3.5F, -15.0F, -1.7F, 3.2F, 3.2F, 3.2F),
                PartPose.ZERO);
        mesh.getRoot().addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }
}
