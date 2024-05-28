package main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

public class Database {

    private static final String url = "jdbc:sqlite:SwingChess.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(url);
            System.out.println("Connected to the database.");
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Error connecting to the database: " + e.getMessage());
        }
        return conn;
    }

    public static void insertGame(int gameId, String date, String whitePlayer, String blackPlayer, String winner) {
        String sql = "INSERT INTO Game (game_id, date, white_player, black_player, winner) VALUES(?,?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setString(2, date);
            pstmt.setString(3, whitePlayer);
            pstmt.setString(4, blackPlayer);
            pstmt.setString(5, winner);
            pstmt.executeUpdate();
            System.out.println("Game record inserted successfully.");
        } catch (SQLException e) {
            System.out.println("Error inserting game record: " + e.getMessage());
        }
    }

    public static void insertMove(int gameId, int moveNumber, String playerColor, String move) {
        String sql = "INSERT INTO Move (game_id, move_number, player_color, move) VALUES(?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, moveNumber);
            pstmt.setString(3, playerColor);
            pstmt.setString(4, move);
            pstmt.executeUpdate();
            System.out.println("Move record inserted successfully.");
        } catch (SQLException e) {
            System.out.println("Error inserting move record: " + e.getMessage());
        }
    }

    public static int getLastGameId() {
        String sql = "SELECT MAX(game_id) AS last_game_id FROM Move";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            int lastGameId = rs.getInt("last_game_id");
            System.out.println("Last Game ID retrieved successfully: " + lastGameId);
            return lastGameId;
        } catch (SQLException e) {
            System.out.println("Error retrieving last game ID: " + e.getMessage());
            return 0; // Return 0 if an error occurs
        }
    }
}
