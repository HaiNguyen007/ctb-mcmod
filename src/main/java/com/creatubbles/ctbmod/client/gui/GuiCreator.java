package com.creatubbles.ctbmod.client.gui;

import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;

import lombok.SneakyThrows;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.creatubbles.ctbmod.CTBMod;
import com.creatubbles.ctbmod.common.creator.ContainerCreator;
import com.creatubbles.ctbmod.common.http.Creation;
import com.creatubbles.ctbmod.common.http.CreationsRequest;
import com.creatubbles.ctbmod.common.http.Creator;
import com.creatubbles.ctbmod.common.http.CreatorsRequest;
import com.creatubbles.ctbmod.common.http.Image.ImageType;
import com.creatubbles.ctbmod.common.http.Login;
import com.creatubbles.ctbmod.common.http.LoginRequest;
import com.creatubbles.ctbmod.common.http.User;
import com.creatubbles.ctbmod.common.http.UserRequest;
import com.creatubbles.repack.enderlib.client.gui.GuiContainerBase;
import com.creatubbles.repack.enderlib.client.gui.widget.TextFieldEnder;
import com.creatubbles.repack.enderlib.client.gui.widget.VScrollbar;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

public class GuiCreator extends GuiContainerBase {

	private class PasswordTextField extends TextFieldEnder {

		public PasswordTextField(FontRenderer fnt, int x, int y, int width, int height) {
			super(fnt, x, y, width, height);
		}
		
		@Override
		public void setFocused(boolean state) {
			super.setFocused(state);
			GuiCreator.this.tfActualPassword.setFocused(state);
		}

		private String transformText(String text) {
			if (!"_".equals(text)) {
				text = text.replaceAll(".", "\u2022");
			}
			return text;
		}

		@Override
		public void setText(String text) {
			GuiCreator.this.tfActualPassword.setText(text);
			super.setText(transformText(text));
		}

		@Override
		public void writeText(String text) {
			GuiCreator.this.tfActualPassword.writeText(text);
			super.writeText(transformText(text));
		}
		
		@Override
		public void deleteWords(int cursor) {
			GuiCreator.this.tfActualPassword.deleteWords(cursor);
			super.deleteWords(cursor);
		}
		
		@Override
		public void deleteFromCursor(int cursor) {
			GuiCreator.this.tfActualPassword.deleteFromCursor(cursor);
			super.deleteFromCursor(cursor);
		}
	}
	
	private class LoginRunnable implements Runnable {

		@Override
		public void run() {
			// thread.isInterrupted() checks throughout to quit execution if the action is canceled
			if (!thread.isInterrupted() && getUser() == null) {
				loginReq = new LoginRequest(new Login(tfEmail.getText(), tfActualPassword.getText()));
				loginReq.run();
			}
			if (!thread.isInterrupted() && loginReq != null && loginReq.failed()) {
				if (loginReq.getException() != null) {
					header = loginReq.getException().getLocalizedMessage();
				} else {
					header = loginReq.getFailedResult().getMessage();
				}
				header = EnumChatFormatting.YELLOW.toString().concat(header);
				loginReq = null;
			} else {
				if (!thread.isInterrupted() && getUser() == null) {
					userReq = new UserRequest(loginReq.getSuccessfulResult().getAccessToken());
					userReq.run();
					CTBMod.cache.activateUser(userReq.getSuccessfulResult());
					CTBMod.cache.getActiveUser().setAccessToken(loginReq.getSuccessfulResult().getAccessToken());
					CTBMod.cache.save();
				}

				if (!thread.isInterrupted() && getCreator() == null) {
					CreatorsRequest creatorsReq = new CreatorsRequest(Integer.toString(getUser().getId()));
					creatorsReq.run();
					CTBMod.cache.setCreators(creatorsReq.getSuccessfulResult().getCreators());
				}

				Creation[] creations = creationList.getCreations();
				if (!thread.isInterrupted() && creations == null) {
					for (Creator c : CTBMod.cache.getCreators()) {
						CreationsRequest creationsReq = new CreationsRequest(c.getId());
						creationsReq.run();
						creations = ArrayUtils.addAll(creations, creationsReq.getSuccessfulResult());
					}
					creationList.setCreations(creations);
					CTBMod.cache.setCreationCache(creations);
				}

				if (thread.isInterrupted()) {
					return;
				}

				for (Creation c : creations) {
					c.getImage().download(ImageType.LIST_VIEW);
				}
			}
		}
	}

	public enum State {
		LOGGED_OUT,
		USER_SELECT,
		LOGGING_IN,
		LOGGED_IN;
	}

	private static final int XSIZE_DEFAULT = 176, XSIZE_SIDEBAR = 270;
	private static final int ID_LOGIN = 0, ID_USER = 1, ID_CANCEL = 2, ID_LOGOUT = 3;
	private static final ResourceLocation BG_TEX = new ResourceLocation(CTBMod.DOMAIN, "textures/gui/creator.png");
	
	static final ResourceLocation OVERLAY_TEX = new ResourceLocation(CTBMod.DOMAIN, "textures/gui/creator_overlays.png");
	static final User DUMMY_USER = new User(0, "No Users", "", "", "", false, false);

	private Multimap<IHideable, State> visibleMap = MultimapBuilder.hashKeys().enumSetValues(State.class).build();
	
	private TextFieldEnder tfEmail, tfActualPassword;
	private VScrollbar scrollbar;
	private PasswordTextField tfVisualPassword;
	private GuiButtonHideable loginButton, userButton, cancelButton, logoutButton;
	private OverlayCreationList creationList;
	// Only empty when the "select user" state is active
	private OverlayUserSelection userSelection;
	
	private Thread thread;
	private LoginRequest loginReq;
	private UserRequest userReq;
	

	private String header = "Plesae log in to Creatubbles:";

	public GuiCreator(InventoryPlayer inv) {
		super(new ContainerCreator(inv));
		this.mc = Minecraft.getMinecraft();

		// Must be done before getState() is called
		userSelection = new OverlayUserSelection(10, 10);
		visibleMap.put(userSelection, State.USER_SELECT);
		addOverlay(userSelection);

		ySize += 44;
		xSize = getState() == State.LOGGED_IN ? XSIZE_SIDEBAR : XSIZE_DEFAULT;
		tfEmail = new TextFieldEnder(getFontRenderer(), (xSize / 2) - 75, 35, 150, 12);
		visibleMap.put(tfEmail, State.LOGGED_OUT);
		tfEmail.setFocused(true);
		textFields.add(tfEmail);
		
		// This is a dummy to store the uncensored PW
		tfActualPassword = new TextFieldEnder(getFontRenderer(), 0, 0, 0, 0);

		tfVisualPassword = new PasswordTextField(getFontRenderer(), (xSize / 2) - 75, 65, 150, 12);
		visibleMap.put(tfVisualPassword, State.LOGGED_OUT);
		textFields.add(tfVisualPassword);

		scrollbar = new VScrollbar(this, XSIZE_SIDEBAR - 11, 0, ySize + 1) {

			@Override
			public int getScrollMax() {
				return creationList.getMaxScroll();
			}
			
			@Override
			public void mouseWheel(int x, int y, int delta) {
				if (!isDragActive()) {
					scrollBy(-Integer.signum(delta) * 4);
				}
			}
		};

		creationList = new OverlayCreationList(XSIZE_DEFAULT, 0);
		visibleMap.put(creationList, State.LOGGED_IN);
		addOverlay(creationList);
	}

	@Override
	@SneakyThrows
	public void initGui() {
//		 CTBMod.cache.activateUser(null);
//		 CTBMod.cache.setCreators(null);

		super.initGui();
		buttonList.clear();

		addScrollbar(scrollbar);
		visibleMap.put(scrollbar, State.LOGGED_IN);

		addButton(loginButton = new GuiButtonHideable(ID_LOGIN, guiLeft + xSize / 2 - 75, guiTop + 90, 75, 20, "Log in"));
		addButton(userButton = new GuiButtonHideable(ID_USER, guiLeft + xSize / 2, guiTop + 90, 75, 20, "Saved Users"));
		addButton(cancelButton = new GuiButtonHideable(ID_CANCEL, guiLeft + xSize / 2 - 50, guiTop + 90, 100, 20, "Cancel"));
		addButton(logoutButton = new GuiButtonHideable(ID_LOGOUT, guiLeft + 10, guiTop + 100, 50, 20, "Log out"));

		visibleMap.put(loginButton, State.LOGGED_OUT);
		visibleMap.put(userButton, State.LOGGED_OUT);
		visibleMap.putAll(cancelButton, Lists.newArrayList(State.USER_SELECT, State.LOGGING_IN));
		visibleMap.put(logoutButton, State.LOGGED_IN);

		// TODO this needs to go
		if (thread == null && getUser() != null) {
			actionPerformed(loginButton);
		}
	}
	
	@Override
	protected void keyTyped(char c, int key) throws IOException {
		if ((c == '\r' || c == '\n') && (tfEmail.isFocused() || tfVisualPassword.isFocused())) {
			actionPerformed(loginButton);
		}
		super.keyTyped(c, key);
	}

	@Override
	@SneakyThrows
	public void updateScreen() {
		if (thread != null && !thread.isAlive()) {
			thread = null;
			loginReq = null;
			userReq = null;
		}
		if (CTBMod.cache.isDirty()) {
			actionPerformed(loginButton);
			CTBMod.cache.dirty(false);
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.disableLighting();

		State state = getState();

		for (Entry<IHideable, Collection<State>> e : visibleMap.asMap().entrySet()) {
			boolean visible = e.getValue().contains(state);
			e.getKey().setIsVisible(visible);
		}

		mc.getTextureManager().bindTexture(BG_TEX);
		int x = guiLeft;
		int y = guiTop;
		drawTexturedModalRect(x, y, 0, 0, XSIZE_DEFAULT, ySize);

		if (getState() == State.LOGGED_IN) {
			updateSize(XSIZE_SIDEBAR);
		} else {
			updateSize(XSIZE_DEFAULT);
		}

		// TODO localize all the things

		switch(state) {
		case LOGGED_IN:
			creationList.setScroll(scrollbar.getScrollPos());
			
			x += 40;
			y += 5;
			drawCenteredString(getFontRenderer(), EnumChatFormatting.UNDERLINE + getUser().getUsername(), x, y, 0xFFFFFF);
			Creator creator = getCreator();
			if (creator != null) {
				y += 15;
				drawCenteredString(getFontRenderer(), StringUtils.capitalize(getUser().getRole()), x, y, 0xFFFFFF);
				y += 15;
				drawCenteredString(getFontRenderer(), getUser().getCountry(), x, y, 0xFFFFFF);
				y += 15;
				drawCenteredString(getFontRenderer(), creator.getAge(), x, y, 0xFFFFFF);
			}
			
			x = guiLeft + 90;
			y = guiTop + 12;
			
			mc.getTextureManager().bindTexture(OVERLAY_TEX);
			drawTexturedModalRect(scrollbar.getX(), scrollbar.getY() + 8, creationList.getWidth() * 2, 8, 11, scrollbar.getWholeArea().height - 16);

			break;
		case LOGGED_OUT:
			x += xSize / 2;
			y += 5;
			drawCenteredString(getFontRenderer(), header, x, y, 0xFFFFFF);

			x = guiLeft + 13;
			y = guiTop + 20;
			drawString(getFontRenderer(), "Email/Username:", tfEmail.xPosition, tfEmail.yPosition - 10, 0xFFFFFF);

			y += 25;
			drawString(getFontRenderer(), "Password:", tfVisualPassword.xPosition, tfVisualPassword.yPosition - 10, 0xFFFFFF);
			break;
		case USER_SELECT:
			break;
		case LOGGING_IN:
			x += xSize / 2;
			y += 25;
			drawCenteredString(getFontRenderer(), "Logging in...", x, y, 0xFFFFFF);
			break;
		}
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
	}
	
	private void updateSize(int newSize) {
		if (xSize != newSize) {
			xSize = newSize;
			initGui();
		}
	}

	State getState() {
		if (!userSelection.isEmpty()) {
			return State.USER_SELECT;
		} else if (getUser() != null) {
			return State.LOGGED_IN;
		} else {
			return loginReq != null || (thread != null && (thread.isInterrupted() || !thread.isAlive())) ? State.LOGGING_IN : State.LOGGED_OUT;
		}
	}

	private User getUser() {
		return CTBMod.cache.getActiveUser();
	}
	
	private Creator getCreator() {
		return CTBMod.cache.getActiveCreator();
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
		case ID_LOGIN:
			thread = new Thread(new LoginRunnable());
			thread.start();
			break;
		case ID_USER:
			userSelection.addAll(CTBMod.cache.getSavedUsers());
			if (userSelection.isEmpty()) {
				userSelection.add(DUMMY_USER);
			}
			break;
		case ID_CANCEL:
			if (getState() == State.USER_SELECT) {
				userSelection.clear();
				break;
			} else {
				thread.interrupt();
				// I'm naughty, but let's see if it works
				// TODO PROBABLY NOT USE THIS
				// TODO SERIOUSLY THIS IS HORRIBLE
				thread.stop();
				// Intentional fall-through
			}
		case ID_LOGOUT:
			CTBMod.cache.activateUser(null);
			CTBMod.cache.setCreators(null);
			CTBMod.cache.setCreationCache(null);
			creationList.setCreations(null);
		}
	}
}
