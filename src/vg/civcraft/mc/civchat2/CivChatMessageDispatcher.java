package vg.civcraft.mc.civchat2;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CivChatMessageDispatcher {
	public static void dispatch(UUID recipient, String message) {
		dispatch(Collections.singleton(recipient), message);
	}

	public static void dispatch(Collection<UUID> recipients, String message) {
		ByteArrayDataOutput msgout = ByteStreams.newDataOutput();

		msgout.writeInt(recipients.size());
		recipients.forEach(uuid -> msgout.writeUTF(uuid.toString()));
		msgout.writeUTF(message);

		ByteArrayDataOutput out = wrapForward("MESSAGE", msgout);

		sendPluginMessage("BungeeCord", out.toByteArray());
	}

	public static void dispatchReply(UUID from, UUID to) {
		ByteArrayDataOutput msgout = ByteStreams.newDataOutput();

		msgout.writeUTF(from.toString());
		msgout.writeUTF(to.toString());

		sendPluginMessage("BungeeCord", wrapForward("REPLY", msgout).toByteArray());
	}

	public static void dispatchChatChannel(UUID from, UUID to) {
		ByteArrayDataOutput msgout = ByteStreams.newDataOutput();

		msgout.writeUTF(from.toString());
		msgout.writeUTF(to == null ? "CLEAR" : to.toString());

		sendPluginMessage("BungeeCord", wrapForward("CHATCHANNEL", msgout).toByteArray());
	}

	public static void dispatchChatGroup(UUID from, int group) {
		ByteArrayDataOutput msgout = ByteStreams.newDataOutput();

		msgout.writeUTF(from.toString());
		msgout.writeInt(group);

		sendPluginMessage("BungeeCord", wrapForward("CHATGROUP", msgout).toByteArray());
	}

	public static void dispatchIgnorePlayer(UUID from, UUID to, boolean add) {
		ByteArrayDataOutput msgout = ByteStreams.newDataOutput();

		msgout.writeBoolean(add);
		msgout.writeUTF(from.toString());
		msgout.writeUTF(to.toString());

		sendPluginMessage("BungeeCord", wrapForward("IGNOREPLAYER", msgout).toByteArray());
	}

	public static void dispatchIgnoreGroup(UUID from, String group, boolean add) {
		ByteArrayDataOutput msgout = ByteStreams.newDataOutput();

		msgout.writeBoolean(add);
		msgout.writeUTF(from.toString());
		msgout.writeUTF(group);

		sendPluginMessage("BungeeCord", wrapForward("IGNOREGROUP", msgout).toByteArray());
	}

	public static ByteArrayDataOutput wrapForward(String subchannel, ByteArrayDataOutput data) {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("Forward");
		out.writeUTF("ONLINE");
		out.writeUTF(subchannel);

		byte[] bytes = data.toByteArray();
		out.writeShort(bytes.length);
		out.write(bytes);

		return out;
	}

	public static void dispatchPlayerList() {
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("LIST");
		out.writeUTF("ALL");

		sendPluginMessage("CIVCHAT", out.toByteArray());
	}

	private static void sendPluginMessage(String channel, byte[] bytes) {
		Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
		if (player == null) {
			return;
		}

		player.sendPluginMessage(CivChat2.getInstance(), channel, bytes);
	}
}
