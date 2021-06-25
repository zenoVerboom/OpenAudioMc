package com.craftmend.openaudiomc.spigot.modules.voicechat.tasks;

import com.craftmend.openaudiomc.OpenAudioMc;
import com.craftmend.openaudiomc.api.impl.event.events.PlayerLeaveVoiceProximityEvent;
import com.craftmend.openaudiomc.api.impl.event.enums.VoiceEventCause;
import com.craftmend.openaudiomc.api.interfaces.AudioApi;
import com.craftmend.openaudiomc.generic.networking.client.objects.player.ClientConnection;
import com.craftmend.openaudiomc.generic.networking.packets.client.voice.PacketClientDropVoiceStream;
import com.craftmend.openaudiomc.generic.networking.payloads.client.voice.ClientVoiceDropPayload;
import com.craftmend.openaudiomc.generic.player.SpigotPlayerAdapter;
import com.craftmend.openaudiomc.generic.utils.data.Filter;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

 @AllArgsConstructor
public class PlayerProximityTicker implements Runnable {

    private int maxDistance;
    @Setter
    private Filter<ClientConnection, Player> filter;

    @Override
    public void run() {
        for (ClientConnection client : OpenAudioMc.getInstance().getNetworkingService().getClients()) {
            // am I valid? no? do nothing.
            if (!client.getClientRtcManager().isReady()) continue;

            Player player = ((SpigotPlayerAdapter) client.getPlayer()).getPlayer();

            // find clients in this world, in radius and that are connected with RTC
            Set<ClientConnection> applicableClients = filter.wrap(
                    OpenAudioMc.getInstance().getNetworkingService().getClients().stream(),
                    player
            ).collect(Collectors.toSet());

            // clear the applicable players if i'm disabled myself
            if (!client.getClientRtcManager().getBlockReasons().isEmpty()) applicableClients.clear();

            // find players that we don't have yet
            applicableClients
                    .stream()
                    .filter(peer -> !client.getClientRtcManager().getSubscriptions().contains(peer.getOwnerUUID()))
                    .forEach(peer -> {
                        // connect with these
                        client.getClientRtcManager().linkTo(peer);
                    });

            // check if we have any peers that are no longer applicable
            for (UUID uuid : client.getClientRtcManager().getSubscriptions()
                    .stream()
                    .filter(p -> p != client.getOwnerUUID())
                    .filter(uuid -> !applicableClients.stream().anyMatch(apc -> apc.getOwnerUUID() == uuid))
                    .collect(Collectors.toSet())) {

                // unsubscribe these
                ClientConnection peer = OpenAudioMc.getInstance().getNetworkingService().getClient(uuid);

                client.sendPacket(new PacketClientDropVoiceStream(new ClientVoiceDropPayload(peer.getStreamKey())));
                peer.sendPacket(new PacketClientDropVoiceStream(new ClientVoiceDropPayload(client.getStreamKey())));

                peer.getClientRtcManager().getSubscriptions().remove(client.getOwnerUUID());
                client.getClientRtcManager().getSubscriptions().remove(peer.getOwnerUUID());

                AudioApi.getInstance().getEventDriver().fire(new PlayerLeaveVoiceProximityEvent(client, peer, VoiceEventCause.NORMAL));
                AudioApi.getInstance().getEventDriver().fire(new PlayerLeaveVoiceProximityEvent(peer, client, VoiceEventCause.NORMAL));

                client.getClientRtcManager().updateLocationWatcher();
                peer.getClientRtcManager().updateLocationWatcher();
            }
        }
    }
}
