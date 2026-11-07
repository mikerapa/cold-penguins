package dev.coldpenguins;

import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

final class BlackTippedArrowRenderer extends ArrowRenderer<BlackTippedArrow, net.minecraft.client.renderer.entity.state.ArrowRenderState> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            ColdPenguins.ID, "textures/entity/projectiles/black_tipped_arrow.png");

    BlackTippedArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public net.minecraft.client.renderer.entity.state.ArrowRenderState createRenderState() {
        return new net.minecraft.client.renderer.entity.state.ArrowRenderState();
    }

    @Override
    public Identifier getTextureLocation(net.minecraft.client.renderer.entity.state.ArrowRenderState arrow) {
        return TEXTURE;
    }
}
