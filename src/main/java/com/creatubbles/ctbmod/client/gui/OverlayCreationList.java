package com.creatubbles.ctbmod.client.gui;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import lombok.Getter;
import lombok.Synchronized;
import lombok.Value;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.Dimension;

import com.creatubbles.api.core.Creation;
import com.creatubbles.api.core.Creator;
import com.creatubbles.ctbmod.CTBMod;
import com.creatubbles.ctbmod.common.http.DownloadableImage;
import com.creatubbles.ctbmod.common.http.DownloadableImage.ImageType;
import com.creatubbles.repack.endercore.api.client.gui.IGuiScreen;
import com.creatubbles.repack.endercore.client.gui.widget.GuiToolTip;
import com.google.common.collect.Lists;

public class OverlayCreationList extends OverlayBase {

	@Value
	private class CreationAndLocation {

		private Creation creation;
		private Point location;
		private Rectangle bounds;
		private GuiToolTip tooltip;

		private CreationAndLocation(Creation c, Point p) {
			this.creation = c;
			this.location = p;
			this.bounds = new Rectangle(p.x, p.y, thumbnailSize, thumbnailSize);

			// TODO more localization here
			List<String> tt = Lists.newArrayList();
			tt.add(c.name);
			tt.add(c.creators.length == 1 ? "Creator:" : "Creators:");
			for (Creator creator : c.creators) {
				tt.add("    " + creator.name);
			}
			this.tooltip = new GuiToolTip(bounds, tt);
		}
	}

	public static final ResourceLocation LOADING_TEX = new ResourceLocation(CTBMod.DOMAIN, "textures/gui/bubble_outline.png");

	@Getter
	private Creation[] creations;

	@Getter
	private int paddingX = 4, paddingY = 4;

	@Getter
	private int minSpacing = 2;

	private int rows, cols;

	@Getter
	private int thumbnailSize = 64;
	
	@Getter
	private Creation selected;

	private List<CreationAndLocation> list = Lists.newArrayList();
	private List<CreationAndLocation> listAbsolute = Lists.newArrayList();
	private List<ISelectionCallback> callbacks = Lists.newArrayList();

	private int scroll = 0;

	public OverlayCreationList(int x, int y) {
		super(x, y, new Dimension(83, 210));
	}
	
	public void addCallback(ISelectionCallback callback) {
		this.callbacks.add(callback);
		callback.callback(getSelected());
	}

	public void setCreations(Creation[] creations) {
		this.creations = ArrayUtils.clone(creations);
		rebuildList();
	}

	@Override
	public void init(IGuiScreen screen) {
		super.init(screen);
		rebuildList();
	}

	@Synchronized("list")
	private void rebuildList() {
		clear();

		Creation[] creations = this.creations == null ? CTBMod.cache.getCreationCache() : this.creations;

        if (creations == null || creations.length == 0) {
            return;
        }

        Arrays.sort(creations, new Comparator<Creation>() {

            @Override
            public int compare(Creation o1, Creation o2) {
                return o1.name.compareTo(o2.name);
            }
        });

		int row = 0;
		int col = 0;

		// This is the dimensions we have to work with for thumbnails
		int usableWidth = getSize().getWidth() - (paddingX * 2);

		// The minimum size a thumbnail can take up
		int widthPerThumbnail = thumbnailSize + minSpacing;

		// This fancy math figures out the max creations that can fit in the available width
		// Simple division won't work due to it counting the spacing after the last item
		cols = 0;
		// So the initial width ignores the spacing
		int usedWidth = thumbnailSize;
		while (usedWidth <= usableWidth) {
			cols++;
			usedWidth += widthPerThumbnail;
		}

		// The amount of thumbnails on each row/column
		rows = creations.length / cols;

		// Use the max spacing possible, but no less than minSpacing
		int minWidth = thumbnailSize * cols;
		int spacing = usableWidth - minWidth;
		if (cols > 1) {
			spacing /= cols - 1;
		}

		spacing = Math.max(minSpacing, spacing);

		for (Creation c : creations) {
			int xMin = xRel + paddingX, yMin = yRel + paddingY;

			int x = xMin + (col * (thumbnailSize + spacing));
			int y = yMin + (row * (thumbnailSize + minSpacing)) - scroll;

			CreationAndLocation data = new CreationAndLocation(c, new Point(x, y));
			CreationAndLocation absoluteData = new CreationAndLocation(c, new Point(x, y + scroll));
			
			// Anything below the border is a waste to draw
			if (y > yRel + getHeight()) {
				listAbsolute.add(absoluteData);
				break;
			}

			// Anything completely above the border is a waste to draw
			if (y + thumbnailSize > yRel) {
				list.add(data);
				getGui().addToolTip(data.tooltip);
			}

			listAbsolute.add(absoluteData);

			col++;
			if (col >= cols) {
				row++;
				col = 0;
			}
		}
	}

	private void clear() {
		for (CreationAndLocation c : list) {
			getGui().removeToolTip(c.tooltip);
		}
		list.clear();
		listAbsolute.clear();
	}

    @Override
    @Synchronized("list")
    public void doDraw(int mouseX, int mouseY, float partialTick) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(GuiCreator.OVERLAY_TEX);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        drawTexturedModalRect(xRel, yRel, 0, 0, getWidth() + 11, getHeight());
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        Minecraft mc = Minecraft.getMinecraft();
		FontRenderer fr = mc.fontRenderer;
		if (list.size() == 0) {
			drawCenteredString(fr, "No Creations", xRel + (getWidth() / 2), yRel + 4, 0xFFFFFF);
		} else {
			for (CreationAndLocation c : list) {
				GL11.glPushMatrix();

				if (c.getCreation() == selected) {
					drawBoundingBox(c, 0xFF48C43D);
				}

				int x = c.getLocation().x;
				int y = c.getLocation().y;

				DownloadableImage img = getGui().images.get(c.creation);
				ImageType type = ImageType.LIST_VIEW;
				
				if (img == null) {
				    return;
				}

				ResourceLocation res = img.getResource(type);

				int w = 16, h = 16, scaledSize = 16;
				if (!img.hasSize(type)) {
					GL11.glEnable(GL11.GL_BLEND);
					res = LOADING_TEX;
				} else {
					w = img.getWidth(type);
					h = img.getHeight(type);
					scaledSize = img.getScaledSize(type);
				}

				if (res != null) {
					if (res != LOADING_TEX) {
						// Draw selection box
						if (isMouseInBounds(mouseX, mouseY) && c.getBounds().contains(mouseX - getGui().getGuiLeft(), mouseY - getGui().getGuiTop())) {
							drawBoundingBox(c, 0xFFFFFFFF);
						}
					}
					mc.getTextureManager().bindTexture(res);
				}

				int height = thumbnailSize;
				float v = 0;
				int pastBottom = (y + height) - (yRel + getHeight() - 1);
				int pastTop = (yRel + 1) - y;
				boolean clipV = false;
				if (pastBottom > 0) {
					height -= pastBottom;
				} else if (pastTop > 0) {
					clipV = true;
					height -= pastTop;
				}
				double heightRatio = (double) height / thumbnailSize;
				if (clipV) {
					v = h - ((float) heightRatio * h);
				}

				func_152125_a(x, y + Math.max(0, pastTop), 0, v, w, (int) (h * heightRatio), thumbnailSize, height, scaledSize, scaledSize);
				GL11.glPopMatrix();
			}
		}
	}

	private void drawBoundingBox(CreationAndLocation loc, int color) {
		Rectangle bounds = loc.getBounds();
		drawRect((int) bounds.getMinX() - 1, (int) Math.max(bounds.getMinY() - 1, yRel + 1), (int) bounds.getMaxX() + 1, (int) Math.min(bounds.getMaxY() + 1, yRel + getHeight()), color);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glColor4f(1, 1, 1, 1);
	}

	@Override
	public boolean handleMouseInput(int x, int y, int b) {
		if (b == 0) {
			if (!isMouseInBounds(x, y)) {
				return false;
			}
			for (CreationAndLocation c : list) {
				if (isMouseIn(x, y, c.getBounds())) {
					setSelected(c.getCreation());
					return true;
				}
			}
			setSelected(null);
			return true;
		}
		return super.handleMouseInput(x, y, b);
	}

	private void setSelected(Creation creation) {
		if (creation != getSelected()) {
			selected = creation;
			for (ISelectionCallback callback : callbacks) {
				callback.callback(getSelected());
			}
		}
	}

	public void setX(int x) {
		if (getX() != x) {
			rebuildList();
		}
		super.setX(x);
	}

	public void setY(int y) {
		if (getY() != y) {
			rebuildList();
		}
		super.setY(y);
	}

	public void setPaddingX(int paddingX) {
		if (this.paddingX != paddingX) {
			this.paddingX = paddingX;
			rebuildList();
		}
	}

	public void setPaddingY(int paddingY) {
		if (this.paddingY != paddingY) {
			this.paddingY = paddingY;
			rebuildList();
		}
	}

	public void setMinSpacing(int minSpacing) {
		if (this.minSpacing != minSpacing) {
			this.minSpacing = minSpacing;
			rebuildList();
		}
	}

	public void setThumbnailSize(int thumbnailSize) {
		if (this.thumbnailSize != thumbnailSize) {
			this.thumbnailSize = thumbnailSize;
			rebuildList();
		}
	}

	public int getMaxScroll() {
		return (rows * (thumbnailSize + minSpacing)) - getHeight() + minSpacing;
	}

	public void setScroll(int scroll) {
		if (this.scroll != scroll) {
			this.scroll = scroll;
			rebuildList();
		}
	}
}
