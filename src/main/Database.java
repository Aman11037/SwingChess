package main;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

public class Database {

    private static final String url = "jdbc:sqlite:SwingChess.db";
    private static boolean connected = false;
    private static boolean tablesCreated = false;

    public static Connection connect() {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(url);
            if (!connected) {
                System.out.println("Connected to the Database.");
                connected = true;
            }
            if (!tablesCreated) {
                createTables(conn);
                tablesCreated = true;
            }
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Error connecting to the database: " + e.getMessage());
        }
        return conn;
    }

    private static void createTables(Connection conn) {
        String createGameTable = "CREATE TABLE IF NOT EXISTS Game ("
                + "game_id INTEGER PRIMARY KEY,"
                + "date DATE NOT NULL,"
                + "white_player VARCHAR(255) NOT NULL,"
                + "black_player VARCHAR(255) NOT NULL,"
                + "winner VARCHAR(15) CHECK(winner IN ('White', 'Black', 'Draw', 'Match Cancelled')),"
                + "game_time TIME"
                + ");";

        String createMoveTable = "CREATE TABLE IF NOT EXISTS Move ("
                + "game_id INTEGER,"
                + "move_number INTEGER,"
                + "player_name VARCHAR(255) NOT NULL,"
                + "move VARCHAR(50),"
                + "move_time TIME,"
                + "FOREIGN KEY(game_id) REFERENCES Game(game_id)"
                + ");";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createGameTable);
            stmt.execute(createMoveTable);
            System.out.println("Tables created or already exist.");
        } catch (SQLException e) {
            System.out.println("Error creating tables: " + e.getMessage());
        }
    }

    public static void insertGame(int gameId, String date, String whitePlayer, String blackPlayer, String winner, String gameTime) {
        String sql = "INSERT INTO Game (game_id, date, white_player, black_player, winner, game_time) VALUES(?,?,?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setString(2, date);
            pstmt.setString(3, whitePlayer);
            pstmt.setString(4, blackPlayer);
            pstmt.setString(5, winner);
            pstmt.setString(6, gameTime);
            pstmt.executeUpdate();
            System.out.println("Game record inserted successfully.");
        } catch (SQLException e) {
            System.out.println("Error inserting game record: " + e.getMessage());
        }
    }

    public static void insertMove(int gameId, int moveNumber, String playerName, String move, String moveTime) {
        String sql = "INSERT INTO Move (game_id, move_number, player_name, move, move_time) VALUES(?,?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, moveNumber);
            pstmt.setString(3, playerName);
            pstmt.setString(4, move);
            pstmt.setString(5, moveTime);
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
            return 0;
        }
    }
}
