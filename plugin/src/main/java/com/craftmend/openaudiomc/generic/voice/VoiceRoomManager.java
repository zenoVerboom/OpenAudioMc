package com.craftmend.openaudiomc.generic.voice;

import com.craftmend.openaudiomc.OpenAudioMcCore;
import com.craftmend.openaudiomc.generic.networking.client.objects.ClientConnection;
import com.craftmend.openaudiomc.generic.voice.exception.InvalidCallParameterException;
import com.craftmend.openaudiomc.generic.voice.exception.RequestPendingException;
import com.craftmend.openaudiomc.generic.voice.objects.Room;
import com.craftmend.openaudiomc.generic.voice.objects.RoomPrototype;
import com.craftmend.openaudiomc.generic.voice.packets.MemberLeftRoomPacket;
import com.craftmend.openaudiomc.generic.voice.packets.RoomClosedPacket;
import com.craftmend.openaudiomc.generic.voice.packets.RoomCreatedPacket;
import com.craftmend.openaudiomc.generic.voice.packets.subtypes.RoomMember;

import java.util.*;

public class VoiceRoomManager {

    private OpenAudioMcCore core;
    private Map<UUID, Room> voiceRooms = new HashMap<>();
    private Boolean isRequestPending = false;

    public VoiceRoomManager(OpenAudioMcCore core) {
        this.core = core;
    }

    public RoomPrototype createCall(List<ClientConnection> suggestedMembers) throws InvalidCallParameterException, RequestPendingException {
        if (isRequestPending) throw new RequestPendingException("There already is a room awaiting creation. Please wait.");

        List<ClientConnection> members = new ArrayList<>();
        List<ClientConnection> deniedMembers = new ArrayList<>();

        // check if the suggestion is not more than 16 members
        if (suggestedMembers.size() > 16) throw new InvalidCallParameterException("Call can't have more than 16 members.");

        // check for all the suggested members if they already have a call or not.
        // if they do, deny them
        for (ClientConnection suggestedMember : suggestedMembers) {
            if (findRoomOfClient(suggestedMember) == null) {
                members.add(suggestedMember);
            } else {
                deniedMembers.add(suggestedMember);
            }
        }

        // if there are no correct members, also die here
        if (members.size() <= 1) throw new InvalidCallParameterException("Call must have at least two members who are not already in another call.");

        // is the state valid?
        if (!this.core.getStateService().getCurrentState().isConnected()) throw new InvalidCallParameterException("There must at least be one connected client with OpenAudioMc before a call can be started");

        // setup array with members
        List<RoomMember> roomContent = new ArrayList<>();

        // fill it
        for (ClientConnection member : members) {
            roomContent.add(new RoomMember(member.getPlayer().getName(), member.getPlayer().getUniqueId(), false));
        }

        isRequestPending = true;
        this.core.getNetworkingService().requestRoomCreation(roomContent, (ok) -> {
            if (!ok) {
                isRequestPending = false;
                System.out.println(OpenAudioMcCore.getLOG_PREFIX() + "Failed to create call. Server denied or could not handle the request");
            }
        });

        return new RoomPrototype(members, deniedMembers);
    }

    public void clearCache() {
        voiceRooms = new HashMap<>();
        isRequestPending = false;
    }

    public void registerCall(RoomCreatedPacket packet) {
        this.isRequestPending = false;
        this.voiceRooms.put(packet.getRoomId(), new Room(packet.getRoomId(), packet.getMembers()));
        for (RoomMember member : packet.getMembers()) {
            ClientConnection clientConnection = OpenAudioMcCore.getInstance().getNetworkingService().getClient(member.getUuid());
            if (clientConnection != null) {
                clientConnection.getPlayer().sendMessage("ring ring");
            }
        }
    }

    public void leaveCall(MemberLeftRoomPacket packet) {
        Room room = voiceRooms.get(packet.getRoomId());
        if (room == null) return;
        room.getMembers().remove(packet.getMember());

        // send a message?
        // send a message! :-)
        // TODO: send a message
        ClientConnection clientConnection = OpenAudioMcCore.getInstance().getNetworkingService().getClient(packet.getMember());
        if (clientConnection != null) {
            clientConnection.getPlayer().sendMessage("left a call");
        }
    }

    public void closeCall(RoomClosedPacket packet) {
        voiceRooms.remove(packet.getRoomId());
    }

    public Room findRoomOfClient(ClientConnection clientConnection) {
        for (Map.Entry<UUID, Room> entry : voiceRooms.entrySet()) {
            Room room = entry.getValue();
            for (RoomMember roomMember : room.getMembers()) {
                if (roomMember.getUuid().equals(clientConnection.getPlayer().getUniqueId())) {
                    return room;
                }
            }
        }
        return null;
    }

    public void removePlayer(ClientConnection clientConnection) {
        Room room = findRoomOfClient(clientConnection);
        if (room != null) {
            RoomMember member = null;
            for (RoomMember itterationmember : room.getMembers()) {
                if (itterationmember.getUuid().equals(clientConnection.getPlayer().getUniqueId())) {
                    member = itterationmember;
                }
            }

            if (member != null) {
                room.getMembers().remove(member);
            }
        }
    }

}