package vg.civcraft.mc.civchat2.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import vg.civcraft.mc.civchat2.BungeePlayers;
import vg.civcraft.mc.civchat2.CivChat2;
import vg.civcraft.mc.civchat2.CivChat2Manager;
import vg.civcraft.mc.civchat2.database.DatabaseManager;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.group.Group;

public class CivChatMessageListener implements PluginMessageListener {

	public static final String CHANNEL = "CIVCHAT";

	@Override
	public void onPluginMessageReceived(String channel, Player source, byte[] bytes) {
		ByteArrayDataInput in = ByteStreams.newDataInput(bytes);

		if (CHANNEL.equals(channel)) {
			String subchannel = in.readUTF();
			if ("LIST".equals(subchannel)) {
				int len = in.readInt();
				Set<UUID> uuids = new HashSet<>(len);

				for (int i = 0; i < len; i++) {
					uuids.add(readUUID(in));
				}

				BungeePlayers.getInstance().updatePlayers(uuids);
			}
		} else if ("BungeeCord".equals(channel)) {
			String subchannel = in.readUTF();

			if ("MESSAGE".equals(subchannel)) {
				forwardChannel(in);
			} else if ("REPLY".equals(subchannel)) {
				reply(in);
			} else if ("CHATCHANNEL".equals(subchannel)) {
				chatChannel(in);
			} else if ("CHATGROUP".equals(subchannel)) {
				chatGroup(in);
			} else if ("IGNOREPLAYER".equals(subchannel)) {
				ignorePlayer(in);
			} else if ("IGNOREGROUP".equals(subchannel)) {
				ignoreGroup(in);
			} else if ("MUTE".equalsIgnoreCase(subchannel)) {
				mutePlayer(in);
			} else if ("UNMUTE".equalsIgnoreCase(subchannel)) {
				unmutePlayer(in);
			}
		}
	}

	private void mutePlayer(ByteArrayDataInput in) {
		ByteArrayDataInput msgin = unwrapForward(in);
		UUID uuid = readUUID(msgin);

		CivChat2.getInstance().getDatabaseManager().mute(uuid);
	}

	private void unmutePlayer(ByteArrayDataInput in) {
		ByteArrayDataInput msgin = unwrapForward(in);
		UUID uuid = readUUID(msgin);

		CivChat2.getInstance().getDatabaseManager().unmute(uuid);
	}

	private void ignoreGroup(ByteArrayDataInput in) {
		ByteArrayDataInput msgin = unwrapForward(in);

		DatabaseManager datman = CivChat2.getInstance().getDatabaseManager();

		boolean add = msgin.readBoolean();

		UUID from = readUUID(msgin);
		String group = msgin.readUTF();

		if (add) {
			datman.addIgnoredGroup(from, group);
		} else {
			datman.removeIgnoredGroup(from, group);
		}
	}

	private void ignorePlayer(ByteArrayDataInput in) {
		ByteArrayDataInput msgin = unwrapForward(in);

		DatabaseManager datman = CivChat2.getInstance().getDatabaseManager();

		boolean add = msgin.readBoolean();

		UUID from = readUUID(msgin);
		UUID to = readUUID(msgin);

		if (add) {
			datman.addIgnoredPlayer(from, to);
		} else {
			datman.removeIgnoredPlayer(from, to);
		}
	}

	private void chatGroup(ByteArrayDataInput in) {
		ByteArrayDataInput msgin = unwrapForward(in);

		UUID from = readUUID(msgin);
		int groupId = msgin.readInt();

		CivChat2Manager chatman = CivChat2.getInstance().getCivChat2Manager();

		if (groupId == -999) {
			chatman.removeGroupChat(from);
		} else {
			Group group = GroupManager.getGroup(groupId);
			chatman.addGroupChat(from, group);
		}
	}

	private void chatChannel(ByteArrayDataInput in) {
		ByteArrayDataInput msgin = unwrapForward(in);

		UUID from = readUUID(msgin);

		String name = msgin.readUTF();

		CivChat2Manager chatman = CivChat2.getInstance().getCivChat2Manager();

		if (name.equals("CLEAR")) {
			chatman.removeChannel(from);
		} else {
			UUID to = UUID.fromString(name);
			chatman.addChatChannel(from, to);
		}
	}

	private void reply(ByteArrayDataInput in) {
		ByteArrayDataInput msgin = unwrapForward(in);

		CivChat2Manager chatman = CivChat2.getInstance().getCivChat2Manager();

		UUID uuid =readUUID(msgin);
		UUID uuid2 = readUUID(msgin);

		System.out.println("Got reply " + NameAPI.getCurrentName(uuid) + " and " + NameAPI
				.getCurrentName(uuid2));

		chatman.addPlayerReply(uuid, uuid2);
		chatman.addPlayerReply(uuid2, uuid);
	}

	public void forwardChannel(ByteArrayDataInput in) {
		ByteArrayDataInput msgin = unwrapForward(in);

		int playersCount = msgin.readInt();
		Set<UUID> players = new HashSet<>(playersCount);

		for (int i = 0; i < playersCount; i++) {
			players.add(readUUID(msgin));
		}

		String message = msgin.readUTF();

		for (UUID uuid : players) {
			Player player = Bukkit.getPlayer(uuid);
			if (player != null) {
				player.sendMessage(message);
			}
		}
	}

	private ByteArrayDataInput unwrapForward(ByteArrayDataInput in) {
		short len = in.readShort();
		byte[] msgbytes = new byte[len];
		in.readFully(msgbytes);

		return ByteStreams.newDataInput(msgbytes);
	}

	private UUID readUUID(ByteArrayDataInput in) {
		long mostsig = in.readLong();
		long leastsig = in.readLong();
		return new UUID(mostsig, leastsig);
	}
}
