package common;

import java.io.Serializable;

/**
 * Информация о подключенном к серверу клиенте.
 * Используется для отображения в панели администратора сервера и аудита активных сетевых соединений.
 */
public class ClientInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    // Имя пользователя
    private final String username;
    // IP-адрес клиента
    private final String ipAddress;
    // Порт сокета клиента
    private final int port;
    // Текущая комната, в которой находится клиент
    private String currentRoom;
    // Время подключения (timestamp в миллисекундах)
    private final long connectedAt;

    public ClientInfo(String username, String ipAddress, int port, String currentRoom) {
        this.username = username;
        this.ipAddress = ipAddress;
        this.port = port;
        this.currentRoom = currentRoom;
        this.connectedAt = System.currentTimeMillis();
    }

    public String getUsername() {
        return username;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public int getPort() {
        return port;
    }

    public String getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(String currentRoom) {
        this.currentRoom = currentRoom;
    }

    public long getConnectedAt() {
        return connectedAt;
    }

    @Override
    public String toString() {
        return username + " (" + ipAddress + ":" + port + ") [" + currentRoom + "]";
    }
}
