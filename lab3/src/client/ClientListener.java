package client;

import common.ChatRoom;
import common.NetworkMessage;

import java.util.List;

/**
 * Interfață de ascultare a evenimentelor de rețea din perspectiva Clientului.
 */
public interface ClientListener {
    void onConnected(String username, List<ChatRoom> rooms, List<String> roomUsers, List<NetworkMessage> initialHistory);
    void onConnectionFailed(String error);
    void onDisconnected(String reason);
    void onMessageReceived(NetworkMessage message);
    void onFileReceived(NetworkMessage message);
    void onRoomListUpdated(List<ChatRoom> rooms);
    void onUserListUpdated(List<String> users);
    void onRoomHistoryLoaded(String room, List<NetworkMessage> history);
    void onNotification(String text);
    void onError(String error);
}
