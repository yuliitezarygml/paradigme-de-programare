package server;

import common.ChatRoom;
import common.ClientInfo;
import common.NetworkMessage;

/**
 * Интерфейс слушателя событий Сервера (ServerListener).
 * Позволяет полностью отделить сетевую логику сервера от графического интерфейса пользователя (GUI).
 */
public interface ServerListener {
    // Сервер успешно запущен на указанном порту и локальном IP
    void onServerStarted(int port, String localIp);
    
    // Сервер остановлен
    void onServerStopped();
    
    // Новый клиент подключился к серверу
    void onClientConnected(ClientInfo client);
    
    // Клиент отключился от сервера
    void onClientDisconnected(ClientInfo client);
    
    // Клиент сменил комнату чата
    void onClientRoomChanged(ClientInfo client, String oldRoom, String newRoom);
    
    // Получено новое текстовое сообщение (или Reply)
    void onMessageReceived(NetworkMessage message);
    
    // Получен переданный файл
    void onFileTransferred(NetworkMessage message);
    
    // Создана новая комната чата
    void onRoomCreated(ChatRoom room);
    
    // Комната чата удалена
    void onRoomDeleted(String roomName);
    
    // Событие системного журнала (лог)
    void onLogEvent(String level, String message);
}
