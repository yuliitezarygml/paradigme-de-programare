package client;

import common.ChatRoom;
import common.NetworkMessage;

import java.util.List;

/**
 * Интерфейс обратного вызова (слушатель событий) со стороны Клиента.
 * Позволяет GUI-интерфейсу реагировать на входящие сетевые события без прямой привязки к сокетам.
 */
public interface ClientListener {
    // Успешное подключение к серверу, передача начального списка комнат, пользователей и истории
    void onConnected(String username, List<ChatRoom> rooms, List<String> roomUsers, List<NetworkMessage> initialHistory);
    
    // Ошибка подключения к серверу (например: сервер оффлайн или неверный IP/порт)
    void onConnectionFailed(String error);
    
    // Разрыв соединения или отключение от сервера
    void onDisconnected(String reason);
    
    // Получено новое текстовое сообщение (обычное или ответ)
    void onMessageReceived(NetworkMessage message);
    
    // Получен переданный файл
    void onFileReceived(NetworkMessage message);
    
    // Обновлен список доступных комнат чата
    void onRoomListUpdated(List<ChatRoom> rooms);
    
    // Обновлен список участников в текущей комнате
    void onUserListUpdated(List<String> users);
    
    // Загружена история сообщений для комнаты
    void onRoomHistoryLoaded(String room, List<NetworkMessage> history);
    
    // Получено системное уведомление от сервера
    void onNotification(String text);
    
    // Получено сообщение об ошибке
    void onError(String error);
}
