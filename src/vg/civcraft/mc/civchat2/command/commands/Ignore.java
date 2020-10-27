package vg.civcraft.mc.civchat2.command.commands;

import java.util.List;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import vg.civcraft.mc.civchat2.ChatStrings;
import vg.civcraft.mc.civchat2.CivChatMessageDispatcher;
import vg.civcraft.mc.civchat2.command.ChatCommand;
import vg.civcraft.mc.namelayer.NameAPI;

public class Ignore extends ChatCommand {

	public Ignore(String name) {

		super(name);
		setIdentifier("ignore");
		setDescription("Toggles ignoring a player");
		setUsage("/ignore <player>");
		setArguments(1, 1);
		setSenderMustBePlayer(true);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		UUID player = argAsGlobalPlayer(0);
		ignore(player);
		return true;
	}

	private void ignore(UUID ignore) {
		if (ignore == null) {
			msg(ChatStrings.chatPlayerNotFound);
			return;
		}

		String ignoreName = NameAPI.getCurrentName(ignore);
		String name = getRealName(player());
		if (ignoreName.equals(name)) {
			msg(ChatStrings.chatCantIgnoreSelf);
			return;
		}
		// Player added to the list
		if (!DBM.isIgnoringPlayer(name, ignoreName)) {
			CivChatMessageDispatcher.dispatchIgnorePlayer(player().getUniqueId(), ignore, true);
			DBM.addIgnoredPlayer(name, ignoreName);
			String debugMessage = "Player ignored another Player, Player: " + name + " IgnoredPlayer: " + ignoreName;
			logger.debug(debugMessage);
			msg(ChatStrings.chatNowIgnoring, ignoreName);
			// Player removed from the list
		} else {
			CivChatMessageDispatcher.dispatchIgnorePlayer(player().getUniqueId(), ignore, false);
			DBM.removeIgnoredPlayer(name, ignoreName);
			String debugMessage = "Player un-ignored another Player, Player: " + name + " IgnoredPlayer: " + ignoreName;
			logger.debug(debugMessage);
			msg(ChatStrings.chatStoppedIgnoring, ignoreName);
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
