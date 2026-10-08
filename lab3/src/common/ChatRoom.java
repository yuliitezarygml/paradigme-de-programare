package common;

import java.io.Serializable;
import java.util.Objects;

/**
 * Класс, представляющий комнату для общения (Chat Room).
 * Обеспечивает возможность отправки сообщений сразу на несколько компьютеров (широковещание в комнате).
 */
public class ChatRoom implements Serializable {
    private static final long serialVersionUID = 1L;

    // Название комнаты (всегда начинается с символа #)
    private final String name;
    // Описание / тема обсуждения в комнате
    private final String description;
    // Временная метка создания комнаты (в миллисекундах)
    private final long createdAt;
    // Имя создателя комнаты (пользователь или System/Admin)
    private final String createdBy;
    // Текущее количество активных пользователей в комнате
    private int userCount;

    public ChatRoom(String name, String description, String createdBy) {
        this.name = name.startsWith("#") ? name : "#" + name;
        this.description = description != null ? description : "";
        this.createdBy = createdBy != null ? createdBy : "System";
        this.createdAt = System.currentTimeMillis();
        this.userCount = 0;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public int getUserCount() {
        return userCount;
    }

    public void setUserCount(int userCount) {
        this.userCount = userCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatRoom)) return false;
        ChatRoom chatRoom = (ChatRoom) o;
        return name.equalsIgnoreCase(chatRoom.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    @Override
    public String toString() {
        return name + " (" + userCount + ")";
    }
}
