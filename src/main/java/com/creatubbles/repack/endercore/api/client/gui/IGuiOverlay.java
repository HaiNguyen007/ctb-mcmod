package com.creatubbles.repack.endercore.api.client.gui;

import java.awt.Rectangle;

import com.creatubbles.ctbmod.client.gui.IHideable;

public interface IGuiOverlay extends IHideable {

    void init(IGuiScreen screen);

    Rectangle getBounds();

    void draw(int mouseX, int mouseY, float partialTick);

    // //consume event?
    // boolean mouseClicked(int par1, int par2, int par3);
    //
    // boolean mouseClickMove(int par1, int par2, int par3, long p_146273_4_);
    //
    // boolean mouseMovedOrUp(int par1, int par2, int par3);

    boolean handleMouseInput(int x, int y, int b);

    boolean isMouseInBounds(int mouseX, int mouseY);
    
    void guiClosed();

}
