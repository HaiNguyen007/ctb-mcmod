package com.creatubbles.ctbmod.client.render;


import java.awt.geom.Rectangle2D;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.creatubbles.ctbmod.common.http.DownloadableImage;
import com.creatubbles.ctbmod.common.http.DownloadableImage.ImageType;
import com.creatubbles.ctbmod.common.painting.TilePainting;


public class RenderPainting extends TileEntitySpecialRenderer {
    
    private static final ResourceLocation BACKGROUND = new ResourceLocation("ctbmod", "textures/blocks/painting_bg.png");

    @Override
    public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partialTicks) {
        if (te instanceof TilePainting && ((TilePainting)te).getImage() != null) {
            TilePainting painting = (TilePainting) te;
            DownloadableImage image = painting.getImage();
            if (!image.hasSize(ImageType.FULL_VIEW)) {
                image.download(ImageType.FULL_VIEW);
            }
            
            ForgeDirection facing = ForgeDirection.getOrientation(te.getBlockMetadata() & 3 + 2);

            Minecraft.getMinecraft().renderEngine.bindTexture(image.getResource(ImageType.FULL_VIEW));
            
            Tessellator renderer = Tessellator.instance;

            int width = image.getWidth(ImageType.FULL_VIEW);
            int height = image.getHeight(ImageType.FULL_VIEW);

            // TODO this code is duped between here and OverlaySelectedCreation
            Rectangle2D.Double bounds = new Rectangle2D.Double(2 / 16f, 2 / 16f, painting.getWidth() - 4 / 16f, painting.getHeight() - 4 / 16f);
            if (width > height) {
                double h = bounds.height;
                bounds.height = bounds.getHeight() * ((double) height / width);
                bounds.y += (h - bounds.getHeight()) / 2;
            } else {
                double w = bounds.width;
                bounds.width = bounds.getWidth() * ((double) width / height);
                bounds.x += (w - bounds.getWidth()) / 2;
            }

            double scaledSize = image.getScaledSize(ImageType.FULL_VIEW);

            double maxU = width / scaledSize;
            double maxV = height / scaledSize;

            GL11.glPushMatrix();
            GL11.glTranslated(x, y, z);

            GL11.glPushMatrix();

            switch(facing) {
            case EAST:
                GL11.glTranslatef(0, 0, 1);
                GL11.glRotatef(90, 0, 1, 0);
                break;
            case NORTH:
                GL11.glTranslatef(1, 0, 1);
                GL11.glRotatef(180, 0, 1, 0);
                break;
            case WEST:
                GL11.glTranslatef(1, 0, 0);
                GL11.glRotatef(-90, 0, 1, 0);
                break;
            default:
                break;
            }
            
            
            GL11.glDisable(GL11.GL_LIGHTING);

            renderer.startDrawingQuads();
            
            double depth = 1/16d + 0.005;
            
            renderer.addVertexWithUV(bounds.getX(), bounds.getY() + bounds.getHeight(), depth, 0, 0);
            renderer.addVertexWithUV(bounds.getX(), bounds.getY(), depth, 0, maxV);
            renderer.addVertexWithUV(bounds.getX() + bounds.getWidth(), bounds.getY(), depth, maxU, maxV);
            renderer.addVertexWithUV(bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight(), depth, maxU, 0);
            
            renderer.draw();
//            
//            GL11.glDisable(GL11.GL_CULL_FACE);
//            Minecraft.getMinecraft().renderEngine.bindTexture(BACKGROUND);
//            
//            renderer.startDrawingQuads();
//            
//            renderer.addVertexWithUV(painting.getWidth(), painting.getHeight(), 0.005, 1, 0);
//            renderer.addVertexWithUV(0, painting.getHeight(), 0.005, 0, 0);
//            renderer.addVertexWithUV(0, 0, 0.005, 0, 1);
//            renderer.addVertexWithUV(painting.getWidth(), 0, 0.005, 1, 1);
//            
//            Tessellator.getInstance().draw();
//            GL11.glEnable(GL11.GL_LIGHTING);
//            GL11.glEnable(GL11.GL_CULL_FACE);
            renderer.setTranslation(0, 0, 0);
            
            GL11.glPopMatrix();
            GL11.glPopMatrix();
        }
    }
}
