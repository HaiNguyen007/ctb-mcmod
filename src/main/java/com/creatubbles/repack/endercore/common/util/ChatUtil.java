package com.creatubbles.repack.endercore.common.util;

import io.netty.buffer.ByteBuf;
import lombok.NoArgsConstructor;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import com.creatubbles.ctbmod.common.network.PacketHandler;
import com.creatubbles.repack.endercore.common.Lang;

@UtilityClass
public class ChatUtil {

	@NoArgsConstructor
	public static class PacketNoSpamChat implements IMessage {

		private IChatComponent[] chatLines;

		private PacketNoSpamChat(IChatComponent... lines) {
			// this is guaranteed to be >1 length by accessing methods
			this.chatLines = lines;
		}

		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeInt(chatLines.length);
			for (IChatComponent c : chatLines) {
				ByteBufUtils.writeUTF8String(buf, IChatComponent.Serializer.componentToJson(c));
			}
		}

		@Override
		public void fromBytes(ByteBuf buf) {
			chatLines = new IChatComponent[buf.readInt()];
			for (int i = 0; i < chatLines.length; i++) {
				chatLines[i] = IChatComponent.Serializer.jsonToComponent(ByteBufUtils.readUTF8String(buf));
			}
		}

		public static class Handler implements IMessageHandler<PacketNoSpamChat, IMessage> {

			@Override
			public IMessage onMessage(PacketNoSpamChat message, MessageContext ctx) {
				sendNoSpamMessages(message.chatLines);
				return null;
			}
		}
	}

	private static final int DELETION_ID = 8675309;
	private static int lastAdded;

	private void sendNoSpamMessages(IChatComponent[] messages) {
		GuiNewChat chat = Minecraft.getMinecraft().ingameGUI.getChatGUI();
		for (int i = DELETION_ID + messages.length - 1; i <= lastAdded; i++) {
			chat.deleteChatLine(i);
		}
		for (int i = 0; i < messages.length; i++) {
			chat.printChatMessageWithOptionalDeletion(messages[i], DELETION_ID + i);
		}
		lastAdded = DELETION_ID + messages.length - 1;
	}

	/**
	 * Returns a standard {@link ChatComponentText} for the given {@link String}.
	 * 
	 * @param s
	 *            The string to wrap.
	 * @return An {@link IChatComponent} containing the string.
	 */
	public IChatComponent wrap(String s) {
		return new ChatComponentText(s);
	}

	/**
	 * @see #wrap(String)
	 */
	public IChatComponent[] wrap(String... s) {
		IChatComponent[] ret = new IChatComponent[s.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = wrap(s[i]);
		}
		return ret;
	}

	/**
	 * Returns a translatable chat component for the given string and format args.
	 * 
	 * @param s
	 *            The string to format
	 * @param args
	 *            The args to apply to the format
	 */
	public IChatComponent wrapFormatted(String s, Object... args) {
		return new ChatComponentTranslation(s, args);
	}

	/**
	 * Simply sends the passed lines to the player in a chat message.
	 * 
	 * @param player
	 *            The player to send the chat to
	 * @param lines
	 *            The lines to send
	 */
	public void sendChat(EntityPlayer player, String... lines) {
		sendChat(player, wrap(lines));
	}

	/**
	 * Localizes the lines before sending them.
	 * 
	 * @see #sendChat(EntityPlayer, String...)
	 */
	public void sendChatUnloc(EntityPlayer player, Lang lang, String... unlocLines) {
		sendChat(player, lang.localizeAll(lang, unlocLines));
	}

	/**
	 * Sends all passed chat components to the player.
	 * 
	 * @param player
	 *            The player to send the chat lines to.
	 * @param lines
	 *            The {@link IChatComponent chat components} to send.yes
	 */
	public void sendChat(EntityPlayer player, IChatComponent... lines) {
		for (IChatComponent c : lines) {
			player.addChatComponentMessage(c);
		}
	}

	/**
	 * Localizes the strings before sending them.
	 * 
	 * @see #sendNoSpamClient(String...)
	 */
	public void sendNoSpamClientUnloc(Lang lang, String... unlocLines) {
		sendNoSpamClient(lang.localizeAll(lang, unlocLines));
	}

	/**
	 * Same as {@link #sendNoSpamClient(IChatComponent...)}, but wraps the Strings automatically.
	 * 
	 * @param lines
	 *            The chat lines to send
	 * @see #wrap(String)
	 */
	public void sendNoSpamClient(String... lines) {
		sendNoSpamClient(wrap(lines));
	}

	/**
	 * Skips the packet sending, unsafe to call on servers.
	 * 
	 * @see #sendNoSpam(EntityPlayerMP, IChatComponent...)
	 */
	public void sendNoSpamClient(IChatComponent... lines) {
		sendNoSpamMessages(lines);
	}

	/**
	 * Localizes the strings before sending them.
	 * 
	 * @see #sendNoSpamSafe(String...)
	 */
	public void sendNoSpamUnloc(EntityPlayer player, Lang lang, String... unlocLines) {
		sendNoSpam(player, lang.localizeAll(lang, unlocLines));
	}

	/**
	 * @see #wrap(String)
	 * @see #sendNoSpamSafe(EntityPlayer, IChatComponent...)
	 */
	public void sendNoSpam(EntityPlayer player, String... lines) {
		sendNoSpam(player, wrap(lines));
	}

	/**
	 * First checks if the player is instanceof {@link EntityPlayerMP} before casting.
	 * 
	 * @see #sendNoSpam(EntityPlayerMP, IChatComponent...)
	 */
	public void sendNoSpam(EntityPlayer player, IChatComponent... lines) {
		if (player instanceof EntityPlayerMP) {
			sendNoSpam((EntityPlayerMP) player, lines);
		}
	}

	/**
	 * Localizes the strings before sending them.
	 * 
	 * @see #sendNoSpam(String...)
	 */
	public void sendNoSpamUnloc(EntityPlayerMP player, Lang lang, String... unlocLines) {
		sendNoSpam(player, lang.localizeAll(lang, unlocLines));
	}

	/**
	 * @see #wrap(String)
	 * @see #sendNoSpam(EntityPlayerMP, IChatComponent...)
	 */
	public void sendNoSpam(EntityPlayerMP player, String... lines) {
		sendNoSpam(player, wrap(lines));
	}

	/**
	 * Sends a chat message to the client, deleting past messages also sent via this method.
	 * <p>
	 * Credit to RWTema for the idea
	 * 
	 * @param player
	 *            The player to send the chat message to
	 * @param lines
	 *            The chat lines to send.
	 */
	public void sendNoSpam(EntityPlayerMP player, IChatComponent... lines) {
		if (lines.length > 0) {
			PacketHandler.INSTANCE.sendTo(new PacketNoSpamChat(lines), player);
		}
	}
}
