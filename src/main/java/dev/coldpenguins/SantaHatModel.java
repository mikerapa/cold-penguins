package dev.coldpenguins;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

final class SantaHatModel extends HumanoidModel<net.minecraft.client.renderer.entity.state.HumanoidRenderState> {
    SantaHatModel() {
        super(createParts().bakeRoot());
    }

    private static LayerDefinition createParts() {
        // Build only the accessory. Replacing a part in HumanoidModel.createMesh retains
        // its children, including the vanilla face-covering helmet shell.
        MeshDefinition mesh = new MeshDefinition();
        var head = mesh.getRoot().addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 16).addBox(-4.6F, -10.25F, -4.6F, 9.2F, 2.0F, 9.2F)
                .texOffs(0, 0).addBox(-4.1F, -14.25F, -4.1F, 8.2F, 4.0F, 8.2F)
                .texOffs(0, 0).addBox(-2.9F, -17.25F, -3.0F, 6.0F, 3.0F, 6.0F)
                .texOffs(0, 0).addBox(-0.7F, -18.75F, -2.0F, 4.0F, 2.0F, 4.0F)
                .texOffs(0, 0).addBox(2.4F, -18.25F, -1.4F, 2.0F, 2.3F, 2.8F)
                .texOffs(40, 16).addBox(3.5F, -16.75F, -1.7F, 3.2F, 3.2F, 3.2F),
                PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        for (String part : new String[]{"body", "right_arm", "left_arm", "right_leg", "left_leg"}) {
            mesh.getRoot().addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 32);
    }
}
