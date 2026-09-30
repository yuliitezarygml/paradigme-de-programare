package common;

import java.io.Serializable;

/**
 * Informații despre un client conectat la server.
 * Folosit pentru afișarea în dashboard-ul Serverului și monitorizarea conexiunilor.
 */
public class ClientInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private final String ipAddress;
    private final int port;
    private String currentRoom;
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
