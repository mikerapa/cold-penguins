package dev.coldpenguins;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

final class BlackTippedArrowRenderer extends ArrowRenderer<BlackTippedArrow> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ColdPenguins.ID, "textures/entity/projectiles/black_tipped_arrow.png");

    BlackTippedArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(BlackTippedArrow arrow) {
        return TEXTURE;
    }
}
