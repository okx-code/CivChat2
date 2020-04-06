package vg.civcraft.mc.civchat2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import vg.civcraft.mc.civchat2.database.DatabaseManager;
import vg.civcraft.mc.civchat2.event.GlobalChatEvent;
import vg.civcraft.mc.civchat2.event.GroupChatEvent;
import vg.civcraft.mc.civchat2.event.PrivateMessageEvent;
import vg.civcraft.mc.civchat2.utility.CivChat2Config;
import vg.civcraft.mc.civchat2.utility.CivChat2FileLogger;
import vg.civcraft.mc.civmodcore.util.Guard;
import vg.civcraft.mc.civmodcore.util.TextUtil;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class CivChat2Manager {

	private CivChat2Config config;

	private CivChat2FileLogger chatLog;

	private CivChat2 instance;

	private DatabaseManager DBM;

	// chatChannels in hashmap with (Player 1 name, player 2 name)
	private HashMap<UUID, UUID> chatChannels;

	// groupChatChannels have (Player, Group)
	private final HashMap<UUID, Group> groupChatChannels;

	// replyList has (playerName, whotoreplyto)
	private final HashMap<UUID, UUID> replyList;

	private final Set<UUID> afkPlayers;

	protected static final GroupManager GM = NameAPI.getGroupManager();

	private String defaultColor;

	public CivChat2Manager(CivChat2 pluginInstance) {

		instance = pluginInstance;
		config = instance.getPluginConfig();
		chatLog = instance.getCivChat2FileLogger();
		DBM = instance.getDatabaseManager();
		defaultColor = config.getDefaultColor();
		chatChannels = new HashMap<UUID, UUID>();
		groupChatChannels  = new HashMap<UUID, Group>();
		replyList = new HashMap<UUID, UUID>();
		afkPlayers = new HashSet<UUID>();
	}


	/**
	 * Gets the channel for player to player chat
	 * @param name    Player name of the channel
	 * @return        Returns a String of channel name, null if doesn't exist
	 */
	public UUID getChannel(UUID player) {

		Guard.ArgumentNotNull(player, "player");

		return chatChannels.get(player);
	}

	/**
	 * Removes the channel from the channel storage
	 * @param name    Player Name of the channel
	 *
	 */
	public void removeChannel(UUID player) {

		Guard.ArgumentNotNull(player, "player");

		chatChannels.remove(player);
	}

	/**
	 * Adds a channel for player to player chat, if player1 is
	 * currently in a chatChannel this will overwrite it
	 * @param player1   Sender's name
	 * @param player2   Receiver's name
	 */
	public void addChatChannel(UUID player1, UUID player2) {

		Guard.ArgumentNotNull(player1, "player1");
		Guard.ArgumentNotNull(player2, "player2");

		if (getChannel(player1) != null) {
			chatChannels.put(player1, player2);
			CivChat2.debugmessage("addChatChannel adding channel for P1: " + player1 + " P2: " + player2);
		} else {
			chatChannels.put(player1, player2);
			CivChat2.debugmessage("addChatChannel adding channel for P1: " + player1 + " P2: " + player2);
		}
	}

	/**
	 * Method to Send private message between to players
	 * @param sender Player sending the message
	 * @param receiver Player Receiving the message
	 * @param chatMessage Message to send from sender to receive
	 */
	public void sendPrivateMsg(Player sender, UUID receiver, String chatMessage) {

		PrivateMessageEvent event = new PrivateMessageEvent(sender, receiver, chatMessage);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			return;
		}

		StringBuilder sb = new StringBuilder();

		String senderName = sender.getName();
		String receiverName = NameAPI.getCurrentName(receiver);

		String senderMessage = sb.append(ChatColor.LIGHT_PURPLE)
								.append("To ")
								.append(receiverName)
								.append(": ")
								.append(chatMessage)
								.toString();
		sb.delete(0, sb.length());

		String receiverMessage = sb.append(ChatColor.LIGHT_PURPLE)
									.append("From ")
									.append(senderName)
									.append(": ")
									.append(chatMessage)
									.toString();
		sb.delete(0, sb.length());

		CivChat2.debugmessage(sb.append("ChatManager.sendPrivateMsg Sender: " )
								.append( senderName)
								.append(" receiver: ")
								.append( receiverName)
								.append( " Message: ")
								.append(chatMessage)
								.toString());
		sb.delete(0, sb.length());

		if (isPlayerAfk(receiver)) {
			BungeePlayers.sendMessage(receiver, receiverMessage);
			sender.sendMessage(parse(ChatStrings.chatPlayerAfk));
			return;
		// Player is ignoring the sender
		} else if (DBM.isIgnoringPlayer(receiver, sender.getUniqueId()))  {
			sender.sendMessage(parse(ChatStrings.chatPlayerIgnoringYou));
			return;
		} else if (DBM.isIgnoringPlayer(sender.getUniqueId(), receiver)) {
			sender.sendMessage(parse(ChatStrings.chatNeedToUnignore, receiverName));
			return;
		}
		CivChat2.debugmessage("Sending private chat message");
		chatLog.logPrivateMessage(sender, chatMessage, receiverName);
		replyList.put(receiver, sender.getUniqueId());
		replyList.put(sender.getUniqueId(), receiver);

		CivChatMessageDispatcher.dispatchReply(receiver, sender.getUniqueId());

		sender.sendMessage(senderMessage);

		BungeePlayers.sendMessage(receiver, receiverMessage);
	}

	/**
	 * Method to broadcast a message in global chat
	 * @param sender Player who sent the message
	 * @param chatMessage Message to send
	 * @param recipients Players in range to receive the message
	 */
	public void broadcastMessage(Player sender, String chatMessage, String messageFormat, Set<Player> recipients) {

		Guard.ArgumentNotNull(sender, "sender");
		Guard.ArgumentNotNull(chatMessage, "chatMessage");
		Guard.ArgumentNotNullOrEmpty(messageFormat, "messageFormat");
		Guard.ArgumentNotNull(recipients, "recipients");

		GlobalChatEvent event = new GlobalChatEvent(sender, chatMessage, messageFormat);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			return;
		}

		int range = config.getChatRange();
		int height = config.getYInc();
		Location location = sender.getLocation();
		int y = location.getBlockY();
		double scale = (config.getYScale()) / 1000;

		StringBuilder sb = new StringBuilder();

		// Do height check
		// Player is above chat increase range
		if (y > height) {
			CivChat2.debugmessage("Player is above Y chat increase range");
			int above = y - height;
			int newRange = (int) (range + (range * (scale * above)));
			range = newRange;
			CivChat2.debugmessage(sb.append("New chatrange = [" )
									.append(range)
									.append("]")
									.toString());
			sb.delete(0, sb.length());
		}

		ChatColor color = ChatColor.valueOf(defaultColor);

		Set<String> receivers = new HashSet<String>();
		// Loop through players and send to those that are close enough
		for (Player receiver : recipients) {
			if (!DBM.isIgnoringPlayer(receiver.getUniqueId(), sender.getUniqueId())) {
				if (receiver.getWorld().equals(sender.getWorld())) {
					double receiverDistance = location.distance(receiver.getLocation());
					if (receiverDistance <= range) {
						ChatColor newColor = ChatColor.valueOf(config.getColorAtDistance(receiverDistance));
						newColor = newColor != null ? newColor : color;
						receiver.sendMessage(String.format(messageFormat, newColor + NameAPI.getCurrentName(sender.getUniqueId()),
							newColor + chatMessage));
					}
				}
				receivers.add(receiver.getName());
			}
		}
		receivers.remove(sender.getName());
		chatLog.logGlobalMessage(sender, chatMessage, receivers);
	}

	/**
	 * Gets whether a player is AFK
	 * @param player The player to check
	 * @return true if the player is AFK
	 */
	public boolean isPlayerAfk(UUID player) {

		Guard.ArgumentNotNull(player, "player");

		return afkPlayers.contains(player);
	}

	/**
	 * Sets the AFK status of a player
	 * @param player The player to change
	 * @return The player AFK status
	 */
	public boolean setPlayerAfk(Player player, boolean afkStatus) {

		Guard.ArgumentNotNull(player, "player");

		if (afkStatus) {
			afkPlayers.add(player.getUniqueId());
		} else {
			afkPlayers.remove(player.getUniqueId());
		}
		return afkStatus;
	}

	/**
	 * Gets the player to send reply to
	 * @param sender the person sending reply command
	 * @return the UUID of the person to reply to, null if none
	 */
	public UUID getPlayerReply(Player sender) {

		Guard.ArgumentNotNull(sender, "sender");

		return replyList.get(sender.getUniqueId());
	}

	/**
	 * Add a player to the replyList
	 * @param player The player using the reply command.
	 * @param replyPlayer The the player that will receive the reply
	 */
	public void addPlayerReply(UUID player, UUID replyPlayer) {

		Guard.ArgumentNotNull(player, "player");
		Guard.ArgumentNotNull(replyPlayer, "replyPlayer");

		replyList.put(player, replyPlayer);
	}

	/**
	 * Method to add a group chat channel
	 * @param name Player sending the message
	 * @param group Group sending the message to
	 */
	public void addGroupChat(UUID player, Group group) {

		Guard.ArgumentNotNull(player, "player");
		Guard.ArgumentNotNull(group, "group");

		groupChatChannels.put(player, group);
	}

	/**
	 * Method to send a message to a group
	 * @param name sender sending the message
	 * @param group Group to send the message too
	 * @param groupMsg Message to send to the group
	 */
	public void sendGroupMsg(Player sender, Group group, String message) {

		Guard.ArgumentNotNull(sender, "sender");
		Guard.ArgumentNotNull(group, "group");
		Guard.ArgumentNotNullOrEmpty(message, "message");

		GroupChatEvent event = new GroupChatEvent(sender, group.getName(), message);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			return;
		}

		List<Player> onlineMembers = new ArrayList<>();
		Set<UUID> bungeeRecipients = new HashSet<>();

		List<UUID> membersUUID = group.getAllMembers();
		for (UUID uuid : membersUUID) {
			if (NameAPI.getGroupManager().hasAccess(group, uuid, PermissionType.getPermission("READ_CHAT"))
					&& !DBM.isIgnoringGroup(uuid, group.getName())
					&& !DBM.isIgnoringPlayer(uuid, sender.getUniqueId())) {
				// Only add online players to members
				Player toAdd = Bukkit.getPlayer(uuid);
				if (toAdd != null && toAdd.isOnline()) {
					onlineMembers.add(toAdd);
				} else if (BungeePlayers.isOnline(uuid)) {
					bungeeRecipients.add(uuid);
				}
			}

		}

		String formatted = parse(ChatStrings.chatGroupMessage, group.getName(), sender.getName(), message);
		String senderName = NameAPI.getCurrentName(sender.getUniqueId());

		for (Player receiver : onlineMembers) {
			receiver.sendMessage(formatted);
		}

		CivChatMessageDispatcher.dispatch(bungeeRecipients, formatted);

		Set<String> players = new HashSet<>();
		for (UUID uuid : membersUUID) {
			players.add(NameAPI.getCurrentName(uuid));
		}
		players.remove(senderName);
		chatLog.logGroupMessage(sender, message, group.getName(), players);
	}

	/**
	 * Method to remove player from a group chat
	 * @param player The player to remove from chat
	 */
	public void removeGroupChat(UUID player) {

		Guard.ArgumentNotNull(player, "player");

		groupChatChannels.remove(player);
	}

	/**
	 * Method to get the group player is currently chatting in
	 * @param name Players name
	 * @return Group they are currently chatting in
	 */
	public Group getGroupChatting(Player player) {

		Guard.ArgumentNotNull(player, "player");

		return groupChatChannels.get(player.getUniqueId());
	}

	public String parse(String text) {

		return TextUtil.parse(text);
	}

	public String parse(String text, Object... args) {

		return TextUtil.parse(text, args);
	}
}
