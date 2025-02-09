package se.drachbar.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.drachbar.config.SQLiteConnection;
import se.drachbar.model.ConversationDto;
import se.drachbar.model.MessageDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class ChatRepository {
    private static final Logger log = LoggerFactory.getLogger(ChatRepository.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ConversationDto getChat(final int id) {
        String jsonData = null;

        String sql = "SELECT chat FROM conversations WHERE id = ?";
        try (Connection conn = SQLiteConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                jsonData = rs.getString("chat");
            }
        } catch (Exception e) {
            log.error("Error fetching chat: ", e);
        }
        if (jsonData != null) {
            try {
                return objectMapper.readValue(jsonData, ConversationDto.class);
            } catch (JsonProcessingException e) {
                log.error("Error processing json: ", e);
                throw new RuntimeException(e);
            }
        }

        return new ConversationDto(List.of()); // Tom sträng om inget finns
    }

    public static void updateChat(final List<MessageDto> messages) {
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(new ConversationDto(messages));
        } catch (JsonProcessingException e) {
            log.error("Error: ", e);
            throw new RuntimeException(e);
        }

        String sql = "UPDATE conversations SET chat = ? WHERE id = 1";
        try (Connection conn = SQLiteConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, jsonString);
            pstmt.executeUpdate();
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }

    public static String getLabel() {
        String sql = "SELECT label FROM conversations WHERE id = 1";
        try (Connection conn = SQLiteConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString("label");
            }
        } catch (Exception e) {
            log.error("Error fetching label: ", e);
        }
        return null; // Returnera null om ingen label finns
    }

    public static void insertLabel(String label) {
        String sql = "UPDATE conversations SET label = ? WHERE id = 1";
        try (Connection conn = SQLiteConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, label);
            pstmt.executeUpdate();
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }

    public static int createConversation() {
        String sql = "INSERT INTO conversations DEFAULT VALUES RETURNING id";
        try (Connection conn = SQLiteConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            log.error("Error: ", e);
        }
        return -1; // Returnera -1 vid fel
    }

}
