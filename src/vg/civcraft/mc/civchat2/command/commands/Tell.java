package vg.civcraft.mc.civchat2.command.commands;

import java.util.List;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import vg.civcraft.mc.civchat2.ChatStrings;
import vg.civcraft.mc.civchat2.CivChatMessageDispatcher;
import vg.civcraft.mc.civchat2.command.ChatCommand;
import vg.civcraft.mc.namelayer.NameAPI;

public class Tell extends ChatCommand {

	public Tell(String name) {
		super(name);
		setIdentifier("tell");
		setDescription("Sends a private message to another player");
		setUsage("/tell <player> <message>");
		setSenderMustBePlayer(true);
		setErrorOnTooManyArgs(false);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {

		if (args.length == 0) {
			UUID chattingWith = chatMan.getChannel(player().getUniqueId());
			if (chattingWith != null) {
				chatMan.removeChannel(player().getUniqueId());
				CivChatMessageDispatcher.dispatchChatChannel(player().getUniqueId(), null);
				msg(ChatStrings.chatRemovedFromChat);
			} else {
				msg(ChatStrings.chatNotInPrivateChat);
			}
			return true;
		}

		UUID receiverUUID = argAsGlobalPlayer(0);
		if (receiverUUID == null) {
			msg(ChatStrings.chatPlayerNotFound);
			return true;
		}

		String receiver = NameAPI.getCurrentName(receiverUUID);

		// this shouldn't have sent in the first place
		/*if (! (receiver.isOnline())) {
			msg(ChatStrings.chatPlayerIsOffline);
			logger.debug(parse(ChatStrings.chatPlayerIsOffline));
			return true;
		}*/

		if (player().getName().equalsIgnoreCase(receiver)) {
			msg(ChatStrings.chatCantMessageSelf);
			return true;
		}

		if (args.length >= 2) {
			// Player and message
			StringBuilder builder = new StringBuilder();
			for (int x = 1; x < args.length; x++) {
				builder.append(args[x] + " ");
			}

			chatMan.sendPrivateMsg(player(), receiverUUID, builder.toString());
			return true;
		} else {
			if (DBM.isIgnoringPlayer(player().getName(), receiver)) {
				msg(ChatStrings.chatNeedToUnignore, getRealName(receiver));
				return true;
			}

			if (DBM.isIgnoringPlayer(receiver, player().getName())) {
				msg(ChatStrings.chatPlayerIgnoringYou);
				return true;
			}
			chatMan.addChatChannel(player().getUniqueId(), receiverUUID);
			CivChatMessageDispatcher.dispatchChatChannel(player().getUniqueId(), receiverUUID);
			msg(ChatStrings.chatNowChattingWith, getRealName(receiver));
			return true;
		}
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length != 1) {
			return null;
		}
		return findPlayers(args[0]);
	}
}
