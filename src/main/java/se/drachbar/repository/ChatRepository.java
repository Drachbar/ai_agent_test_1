package se.drachbar.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.drachbar.config.SQLiteConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ChatRepository {
    private static final Logger log = LoggerFactory.getLogger(ChatRepository.class);

    public static String getChat() {
        String sql = "SELECT chat FROM conversations WHERE id = 1";
        try (Connection conn = SQLiteConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getString("chat");
            }
        } catch (Exception e) {
            log.error("Error fetching chat: ", e);
        }
        return ""; // Tom sträng om inget finns
    }

    public static void updateChat(String chatMessage) {
        String sql = "UPDATE conversations SET chat = ? WHERE id = 1";
        try (Connection conn = SQLiteConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, chatMessage);
            pstmt.executeUpdate();
            log.info("Chat message updated: {}", chatMessage);
        } catch (Exception e) {
            log.error("Error: ", e);
        }
    }

    public static void insertLabel(String label) {
        String sql = "UPDATE conversations SET label = ? WHERE id = 1";
        try (Connection conn = SQLiteConnection.connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, label);
            pstmt.executeUpdate();
            log.info("Label updated: {}", label);
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
                int generatedId = rs.getInt("id");
                log.info("New conversation created with id: {}", generatedId);
                return generatedId;
            }
        } catch (Exception e) {
            log.error("Error: ", e);
        }
        return -1; // Returnera -1 vid fel
    }

}
