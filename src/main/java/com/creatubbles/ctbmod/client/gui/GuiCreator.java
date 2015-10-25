package com.creatubbles.ctbmod.client.gui;

import java.io.IOException;
import java.util.Collection;
import java.util.Map.Entry;

import lombok.SneakyThrows;
import lombok.Synchronized;
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
import com.creatubbles.ctbmod.common.creator.TileCreator;
import com.creatubbles.ctbmod.common.http.Creation;
import com.creatubbles.ctbmod.common.http.CreationsRequest;
import com.creatubbles.ctbmod.common.http.CreationsRequest.CreationsResponse;
import com.creatubbles.ctbmod.common.http.Creator;
import com.creatubbles.ctbmod.common.http.CreatorsRequest;
import com.creatubbles.ctbmod.common.http.Image.ImageType;
import com.creatubbles.ctbmod.common.http.Login;
import com.creatubbles.ctbmod.common.http.LoginRequest;
import com.creatubbles.ctbmod.common.http.User;
import com.creatubbles.ctbmod.common.http.UserRequest;
import com.creatubbles.repack.endercore.client.gui.GuiContainerBase;
import com.creatubbles.repack.endercore.client.gui.button.IconButton;
import com.creatubbles.repack.endercore.client.gui.button.MultiIconButton;
import com.creatubbles.repack.endercore.client.gui.widget.GuiToolTip;
import com.creatubbles.repack.endercore.client.gui.widget.TextFieldEnder;
import com.creatubbles.repack.endercore.client.gui.widget.VScrollbar;
import com.creatubbles.repack.endercore.client.render.EnderWidget;
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

			try {
				checkCancel();
				
				if (getUser() == null) {
					setState(State.LOGGING_IN, true);
					loginReq = new LoginRequest(new Login(tfEmail.getText(), tfActualPassword.getText()));
					loginReq.run();
				}

				checkCancel();

				if (loginReq != null && loginReq.failed()) {
					if (loginReq.getException() != null) {
						header = loginReq.getException().getLocalizedMessage();
					} else {
						header = loginReq.getFailedResult().getMessage();
					}
					header = EnumChatFormatting.YELLOW.toString().concat(header);
					loginReq = null;
					logout();
				} else {
					if (getUser() == null) {
						userReq = new UserRequest(loginReq.getSuccessfulResult().getAccessToken());
						userReq.run();
						CTBMod.cache.activateUser(userReq.getSuccessfulResult());
						CTBMod.cache.getActiveUser().setAccessToken(loginReq.getSuccessfulResult().getAccessToken());
						CTBMod.cache.save();
					}

					checkCancel();

					if (getCreator() == null) {
						setState(State.LOGGING_IN, true);
						CreatorsRequest creatorsReq = new CreatorsRequest(Integer.toString(getUser().getId()));
						creatorsReq.run();
						CTBMod.cache.setCreators(creatorsReq.getSuccessfulResult().getCreators());
					}

					checkCancel();

					Creation[] creations = creationList.getCreations();
					if (creations == null) {
						setState(State.LOGGING_IN, true);
						for (Creator c : CTBMod.cache.getCreators()) {
							int page = 1;
							CreationsRequest creationsReq = new CreationsRequest(c.getId());
							creationsReq.run();
							CreationsResponse resp = creationsReq.getSuccessfulResult();
							creations = ArrayUtils.addAll(creations, resp.getCreations());
							while (resp.getPage() < resp.getTotalPages()) {
								creationsReq = new CreationsRequest(c.getId(), ++page);
								creationsReq.run();
								resp = creationsReq.getSuccessfulResult();
								creations = ArrayUtils.addAll(creations, resp.getCreations());
							}
						}
						creationList.setCreations(creations);
						CTBMod.cache.setCreationCache(creations);
					}

					checkCancel();
					// Once we have all creation data, we can say we are "logged in" while the images download
					setState(State.LOGGED_IN, true);
					
					for (Creation c : creations) {
						c.getImage().download(ImageType.LIST_VIEW);
						checkCancel();
					}
				}
			} catch (InterruptedException e) {
				CTBMod.logger.info("Logging in canceled!");
				// Clear the cache
				logout();
			} catch (Exception e) {
				CTBMod.logger.error("Logging in uncountered an unknown error.", e);
				logout();
			} finally {
				// Thread cleanup, erase all evidence we were here
				// This assures a fresh start if a new login is attempted
				loginReq = null;
				userReq = null;
				thread = null;
				cancelButton.enabled = true;
			}
		}

		private void checkCancel() throws InterruptedException {
			if (thread.isInterrupted()) {
				throw new InterruptedException();
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
	private static final int ID_LOGIN = 0, ID_USER = 1, ID_CANCEL = 2, ID_LOGOUT = 3, ID_CREATE = 4;
	private static final int ID_H_PLUS = 5, ID_H_MINUS = 6, ID_W_PLUS = 7, ID_W_MINUS = 8;
	private static final ResourceLocation BG_TEX = new ResourceLocation(CTBMod.DOMAIN, "textures/gui/creator.png");
	private static final String DEFAULT_HEADER = "Please log in to Creatubbles:";
	
	static final ResourceLocation OVERLAY_TEX = new ResourceLocation(CTBMod.DOMAIN, "textures/gui/creator_overlays.png");
	static final User DUMMY_USER = new User(0, "No Users", "", "", "", false, false);

	private Multimap<IHideable, State> visibleMap = MultimapBuilder.hashKeys().enumSetValues(State.class).build();
	
	private TextFieldEnder tfEmail, tfActualPassword;
	private VScrollbar scrollbar;
	private PasswordTextField tfVisualPassword;
	private GuiButtonHideable loginButton, userButton, cancelButton;
	private IconButton logoutButton, createButton;
	private MultiIconButton heightUpButton, heightDownButton, widthUpButton, widthDownButton;
	private GuiToolTip userInfo;
	private OverlayCreationList creationList;
	private OverlayUserSelection userSelection;
	private OverlaySelectedCreation selectedCreation;
	
	private State state;
	
	private Thread thread;
	
	private LoginRequest loginReq;
	private UserRequest userReq;

	private String header = DEFAULT_HEADER;
	
	private TileCreator te;

	public GuiCreator(InventoryPlayer inv, TileCreator creator) {
		super(new ContainerCreator(inv, creator));
		this.te = creator;
		this.mc = Minecraft.getMinecraft();
		setState(getUser() == null ? State.LOGGED_OUT : State.LOGGED_IN, false);

		// Must be done before getState() is called
		userSelection = new OverlayUserSelection(10, 10);
		addOverlay(userSelection);

		ySize += 44;
		xSize = getState() == State.LOGGED_IN ? XSIZE_SIDEBAR : XSIZE_DEFAULT;
		tfEmail = new TextFieldEnder(getFontRenderer(), (XSIZE_DEFAULT / 2) - 75, 35, 150, 12);
		tfEmail.setFocused(true);
		textFields.add(tfEmail);
		
		// This is a dummy to store the uncensored PW
		tfActualPassword = new TextFieldEnder(getFontRenderer(), 0, 0, 0, 0);

		tfVisualPassword = new PasswordTextField(getFontRenderer(), (XSIZE_DEFAULT / 2) - 75, 65, 150, 12);
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
		addOverlay(creationList);

		logoutButton = new IconButton(this, ID_LOGOUT, 7, 106, EnderWidget.CROSS);
		logoutButton.setToolTip("Log Out");

		createButton = new IconButton(this, ID_CREATE, 110, 29, EnderWidget.TICK);
		createButton.setToolTip("Create!");

		selectedCreation = new OverlaySelectedCreation(10, 10, creationList, createButton);
		creationList.addCallback(selectedCreation);
		addOverlay(selectedCreation);

		userInfo = new GuiToolTip(new java.awt.Rectangle(), Lists.<String> newArrayList()) {

			@Override
			public java.awt.Rectangle getBounds() {
				if (getUser() == null) {
					System.out.println("!");
				}
				FontRenderer fr = getFontRenderer();
				return new java.awt.Rectangle(25, 110, fr.getStringWidth(getUser().getUsername()), 8);
			}

			@Override
			protected void updateText() {
				User u = getUser();
				setToolTipText(StringUtils.capitalize(u.getRole()), getCreator().getAge(), u.getCountry());
			}
		};
		addToolTip(userInfo);
		
		int y = 61;
		int x = 97;
	    heightUpButton = MultiIconButton.createAddButton(this, ID_H_PLUS, x, y);
	    heightDownButton = MultiIconButton.createMinusButton(this, ID_H_MINUS, x, y + 8);
	    x += 50;
	    widthUpButton = MultiIconButton.createAddButton(this, ID_W_PLUS, x, y);
	    widthDownButton = MultiIconButton.createMinusButton(this, ID_W_MINUS, x, y + 8);
	}

	@Override
	@SneakyThrows
	public void initGui() {
//		logout();

		super.initGui();
		buttonList.clear();

		addButton(loginButton = new GuiButtonHideable(ID_LOGIN, guiLeft + xSize / 2 - 75, guiTop + 90, 75, 20, "Log in"));
		addButton(userButton = new GuiButtonHideable(ID_USER, guiLeft + xSize / 2, guiTop + 90, 75, 20, "Saved Users"));
		addButton(cancelButton = new GuiButtonHideable(ID_CANCEL, guiLeft + xSize / 2 - 50, guiTop + 90, 100, 20, "Cancel"));
		logoutButton.onGuiInit();
		createButton.onGuiInit();
		heightUpButton.onGuiInit();
		heightDownButton.onGuiInit();
		widthUpButton.onGuiInit();
		widthDownButton.onGuiInit();

		addScrollbar(scrollbar);

		// Avoids CME when state is set during this method
		synchronized (visibleMap) {
			visibleMap.put(tfEmail, State.LOGGED_OUT);
			visibleMap.put(tfVisualPassword, State.LOGGED_OUT);
			visibleMap.put(userSelection, State.USER_SELECT);
			visibleMap.put(creationList, State.LOGGED_IN);
			visibleMap.put(selectedCreation, State.LOGGED_IN);
			visibleMap.put(userInfo, State.LOGGED_IN);
			visibleMap.put(loginButton, State.LOGGED_OUT);
			visibleMap.put(userButton, State.LOGGED_OUT);
			visibleMap.putAll(cancelButton, Lists.newArrayList(State.USER_SELECT, State.LOGGING_IN));
			visibleMap.put(logoutButton, State.LOGGED_IN);
			visibleMap.put(createButton, State.LOGGED_IN);
			visibleMap.put(heightUpButton, State.LOGGED_IN);
			visibleMap.put(heightDownButton, State.LOGGED_IN);
			visibleMap.put(widthUpButton, State.LOGGED_IN);
			visibleMap.put(widthDownButton, State.LOGGED_IN);
			visibleMap.put(scrollbar, State.LOGGED_IN);
		}

		creationList.setCreations(CTBMod.cache.getCreationCache());

		// TODO this needs to go
		if (thread == null && getUser() != null) {
			actionPerformed(loginButton);
		}
		
		updateVisibility();
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
		if (CTBMod.cache.isDirty()) {
			if (thread != null) {
				System.out.println("!");
			}
		}
		if (CTBMod.cache.isDirty() && thread == null) {
			actionPerformed(loginButton);
			CTBMod.cache.dirty(false);
		}
	}
	
	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		if (thread != null) {
			thread.interrupt();
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color(1, 1, 1, 1);
		GlStateManager.disableLighting();

		State state = getState();
		
		mc.getTextureManager().bindTexture(BG_TEX);
		int x = guiLeft;
		int y = guiTop;
		drawTexturedModalRect(x, y, 0, 0, XSIZE_DEFAULT, ySize);

		// TODO localize all the things

		switch(state) {
		case LOGGED_IN:
			if (getUser() == null) {
				System.out.println("!");
			}
			creationList.setScroll(scrollbar.getScrollPos());
			
			x += 25;
			y += 110;
			drawString(getFontRenderer(), getUser().getUsername(), x, y, 0xFFFFFF);
			
			x = guiLeft + 90;
			y = guiTop + 12;
			
			mc.getTextureManager().bindTexture(OVERLAY_TEX);
			drawTexturedModalRect(scrollbar.getX(), scrollbar.getY() + 8, creationList.getWidth() * 2, 8, 11, scrollbar.getWholeArea().height - 16);
			
			x = guiLeft + 70;
			y = guiTop + 19;
			drawTexturedModalRect(x, y, 94, 54, 36, 36);

			x += 74;
			y += 9;
			drawTexturedModalRect(x, y, 94, 90, 18, 18);

			x -= 28;
			y -= 6;
			if (createButton.enabled) {
				// Mojang didn't add an integer color method...
				GlStateManager.color(88f / 255f, 196f / 255f, 61f / 255f);
			} else {
				GlStateManager.color(0.5f, 0.5f, 0.5f);
			}
			EnderWidget.map.render(EnderWidget.ARROW_RIGHT, x, y, 32, 32, 1, true);

			x = guiLeft + 70;
			y = guiTop + 65;
			drawString(getFontRenderer(), "H: " + te.getHeight(), x, y, 0xFFFFFF);
			drawString(getFontRenderer(), "W: " + te.getWidth(), x + 50, y, 0xFFFFFF);
			
			y += 15;
			drawString(getFontRenderer(), "Paper: 100", x, y, 0xFFFFFF);
			y += 10;
			drawString(getFontRenderer(), "Dye: 100", x, y, 0xFFFFFF);
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
			String s = thread.isInterrupted() ? "Canceling..." : "Logging in...";
			drawCenteredString(getFontRenderer(), s, x, y, 0xFFFFFF);
			break;
		}
		super.drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
	}
	
	private void updateSize() {
		int newSize = getState() == State.LOGGED_IN ? XSIZE_SIDEBAR : XSIZE_DEFAULT;
		if (xSize != newSize) {
			xSize = newSize;
			initGui();
		}
	}

	State getState() {
		return state;
	}
	
	private void setState(State state, boolean update) {
		this.state = state;
		if (update) {
			updateSize();
			updateVisibility();
		}
	}

	@Synchronized("visibleMap")
	private void updateVisibility() {
		for (Entry<IHideable, Collection<State>> e : visibleMap.asMap().entrySet()) {
			boolean visible = e.getValue().contains(getState());
			e.getKey().setIsVisible(visible);
		}
	}

	private User getUser() {
		return CTBMod.cache.getActiveUser();
	}
	
	private Creator getCreator() {
		return CTBMod.cache.getActiveCreator();
	}
	
	@Override
	protected void mouseClicked(int x, int y, int button) throws IOException {
		super.mouseClicked(x, y, button);
		// This works around the fact that changing the visibility in actionPerformed
		// will cause buttons that didn't exist when the user clicked to be valid targets
		updateVisibility();
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		switch (button.id) {
		case ID_LOGIN:
			header = DEFAULT_HEADER;
			thread = new Thread(new LoginRunnable());
			thread.start();
			break;
		case ID_USER:
			userSelection.clear();
			userSelection.addAll(CTBMod.cache.getSavedUsers());
			if (userSelection.isEmpty()) {
				userSelection.add(DUMMY_USER);
			}
			setState(State.USER_SELECT, false);
			break;
		case ID_CANCEL:
			if (getState() == State.LOGGING_IN) {
				thread.interrupt();
				cancelButton.enabled = false;
			} else {
				logout();
			}
			break;
		case ID_LOGOUT:
			if (thread != null) {
				// Creation/Image data may still be processing, this will potentially save bandwidth
				thread.interrupt();
			}
			logout();
			break;
		case ID_W_MINUS:
			te.setWidth(te.getWidth() - 1);
			break;
		case ID_W_PLUS:
			te.setWidth(te.getWidth() + 1);
			break;
		case ID_H_MINUS:
			te.setHeight(te.getHeight() - 1);
			break;
		case ID_H_PLUS:
			te.setHeight(te.getHeight() + 1);
			break;
		}
		widthDownButton.enabled = te.getWidth() > te.getMinSize();
		widthUpButton.enabled = te.getWidth() < te.getMaxSize();
		heightDownButton.enabled = te.getHeight() > te.getMinSize();
		heightUpButton.enabled = te.getHeight() < te.getMaxSize();
	}

	private void logout() {
		setState(State.LOGGED_OUT, true);
		CTBMod.cache.activateUser(null);
		CTBMod.cache.setCreators(null);
		CTBMod.cache.setCreationCache(null);
		creationList.setCreations(null);
	}
}
