package com.example.xptable.client;

import com.example.xptable.XpTableBlockEntity;
import com.example.xptable.XpTableMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** The floating green book (like the enchanting table's) and the owner's name above a claimed table. */
public class XpTableRenderer implements BlockEntityRenderer<XpTableBlockEntity> {
    private static final ResourceLocation BOOK = new ResourceLocation(XpTableMod.MODID, "textures/entity/xp_table_book.png");
    private static final int NAME_COLOR = 0xFF80FF20;
    private static final int FULL_BRIGHT = 0xF000F0;

    private final BookModel book;
    private final Font font;

    public XpTableRenderer(BlockEntityRendererProvider.Context ctx) {
        book = new BookModel(ctx.bakeLayer(ModelLayers.BOOK));
        font = ctx.getFont();
    }

    @Override
    public void render(XpTableBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        ps.pushPose();
        ps.translate(0.5F, 0.75F, 0.5F);
        float t = be.time + pt;
        ps.translate(0.0F, 0.1F + Mth.sin(t * 0.1F) * 0.01F, 0.0F);
        float d = be.rot - be.oRot;
        while (d >= (float) Math.PI) d -= (float) Math.PI * 2F;
        while (d < -(float) Math.PI) d += (float) Math.PI * 2F;
        ps.mulPose(Axis.YP.rotation(-(be.oRot + d * pt)));
        ps.mulPose(Axis.ZP.rotationDegrees(80.0F));
        float flip = Mth.lerp(pt, be.oFlip, be.flip);
        float left = Mth.frac(flip + 0.25F) * 1.6F - 0.3F;
        float right = Mth.frac(flip + 0.75F) * 1.6F - 0.3F;
        book.setupAnim(t, Mth.clamp(left, 0.0F, 1.0F), Mth.clamp(right, 0.0F, 1.0F), Mth.lerp(pt, be.oOpen, be.open));
        book.renderToBuffer(ps, buf.getBuffer(RenderType.entitySolid(BOOK)), light, overlay, 1.0F, 1.0F, 1.0F, 1.0F);
        ps.popPose();

        if (be.claimed()) drawName(be.ownerName(), ps, buf);
    }

    private void drawName(String name, PoseStack ps, MultiBufferSource buf) {
        ps.pushPose();
        ps.translate(0.5, 1.5, 0.5);
        ps.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        ps.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f m = ps.last().pose();
        Component text = Component.literal(name);
        float x = -font.width(text) / 2f;
        int bg = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        font.drawInBatch(text, x, 0, 0x2080FF20, false, m, buf, Font.DisplayMode.SEE_THROUGH, bg, FULL_BRIGHT);
        font.drawInBatch(text, x, 0, NAME_COLOR, false, m, buf, Font.DisplayMode.NORMAL, 0, FULL_BRIGHT);
        ps.popPose();
    }
}
