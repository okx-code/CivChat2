package vg.civcraft.mc.civchat2.command.commands;

import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civchat2.BungeePlayers;
import vg.civcraft.mc.civchat2.ChatStrings;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civchat2.CivChatMessageDispatcher;
import vg.civcraft.mc.civchat2.command.ChatCommand;
import vg.civcraft.mc.namelayer.NameAPI;

public class Reply extends ChatCommand {

	public Reply(String name) {

		super(name);
		setIdentifier("reply");
		setDescription("Replies to a private message");
		setUsage("/reply <message>");
		setSenderMustBePlayer(true);
		setErrorOnTooManyArgs(false);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {

		Player player = (Player) sender;
		String senderName = player.getName();
		UUID receiverUUID = chatMan.getPlayerReply(player);

		if (!BungeePlayers.isOnline(receiverUUID)) {
			msg(ChatStrings.chatNoOneToReplyTo);
			return true;
		}

		String receiverName = NameAPI.getCurrentName(receiverUUID);

		/*if (!(receiver.isOnline())) {
			msg(ChatStrings.chatPlayerIsOffline);
			logger.debug(parse(ChatStrings.chatPlayerIsOffline));
			return true;
		}*/

		if (player.getName().equals(receiverName)) {
			CivChat2.warningMessage("Reply Command, Player Replying to themself??? Player: [" + senderName + "]");
			msg(ChatStrings.chatCantMessageSelf);
			return true;
		}

		if (args.length > 0) {
			StringBuilder sb = new StringBuilder();
			for (String s: args) {
				sb.append(s + " ");
			}
			chatMan.sendPrivateMsg(player, receiverUUID, sb.toString());
			return true;
		} else if (args.length == 0) {
			// Player to chat with reply user
			UUID playerUUID = player().getUniqueId();
			chatMan.removeChannel(playerUUID);
			chatMan.addChatChannel(playerUUID, receiverUUID);
			CivChatMessageDispatcher.dispatchChatChannel(playerUUID, receiverUUID);
			msg(ChatStrings.chatNowChattingWith, receiverName);
			return true;
		}

		return false;
	}
}
