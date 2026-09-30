package server;

import common.ChatRoom;
import common.ClientInfo;
import common.NetworkMessage;

/**
 * Interfață de ascultare a evenimentelor de pe Server.
 * Permite decuplarea logicii de rețea de interfața grafică (GUI).
 */
public interface ServerListener {
    void onServerStarted(int port, String localIp);
    void onServerStopped();
    void onClientConnected(ClientInfo client);
    void onClientDisconnected(ClientInfo client);
    void onClientRoomChanged(ClientInfo client, String oldRoom, String newRoom);
    void onMessageReceived(NetworkMessage message);
    void onFileTransferred(NetworkMessage message);
    void onRoomCreated(ChatRoom room);
    void onRoomDeleted(String roomName);
    void onLogEvent(String level, String message);
}
