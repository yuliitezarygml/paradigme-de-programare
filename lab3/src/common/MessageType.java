package common;

import java.io.Serializable;

/**
 * Tipurile de mesaje transmise prin protocolul de rețea.
 * Tipuri de evenimente suportate între Server și Client.
 */
public enum MessageType implements Serializable {
    CONNECT,              // Clientul solicită conectarea cu un username
    CONNECT_ACK,          // Serverul confirmă conectarea și trimite starea inițială
    DISCONNECT,           // Clientul se deconectează voluntar
    
    CHAT_MESSAGE,         // Mesaj text normal de chat
    FILE_TRANSFER,        // Transfer de fișier binar
    
    CREATE_ROOM,          // Cerere de creare a unei noi camere de chat
    DELETE_ROOM,          // Cerere de ștergere a unei camere de chat
    JOIN_ROOM,            // Clientul trece într-o altă cameră
    ROOM_LIST,            // Actualizare a listei camerelor disponibile
    USER_LIST,            // Lista utilizatorilor conectați în camera curentă
    
    HISTORY_REQUEST,      // Solicitare de istoric pentru o cameră
    HISTORY_RESPONSE,     // Răspuns cu istoricul mesajelor din cameră
    
    SERVER_NOTIFICATION,  // Notificare de sistem (ex: utilizator conectat/deconectat)
    ERROR,                // Mesaj de eroare
    KICK                  // Serverul a deconectat forțat clientul
}
