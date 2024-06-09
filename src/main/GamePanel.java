package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Font;
import javax.swing.JPanel;
import java.util.ArrayList;
import piece.Piece;
import piece.Pawn;
import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Queen;
import piece.Rook;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.text.SimpleDateFormat;

public class GamePanel extends JPanel implements Runnable {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    private static final int FPS = 144;
    Thread gameThread;
    Board board = new Board();
    Mouse mouse = new Mouse();
    private static final int MAX_NAME_LENGTH = 10;

    //  Match Details
    private int gameID;
    int moveNumber = 0;
    private int consecutiveMovesWithoutPawnOrCapture = 0;

    // PIECES
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    public ArrayList<Piece> promotionPieces = new ArrayList<>();
    Piece activePiece, checkingPiece;

    //  NAMES
    private String whitePlayerName;
    private String blackPlayerName;

    // Variables for storing moves
    private ArrayList<String> whiteMoves = new ArrayList<>();
    private ArrayList<String> blackMoves = new ArrayList<>();

    // COLOR
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    int currentColor = WHITE;

    // BOOLEANS
    boolean isValidMove;
    boolean isValidSquare;
    boolean promotion;
    boolean gameOver;
    boolean stalemate;

    // TIMER
    private long startTime;
    private long elapsedTime;

    public GamePanel(String player1, String player2) {
        this.whitePlayerName = trimName(player1);
        this.blackPlayerName = trimName(player2);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        addMouseMotionListener(mouse);
        addMouseListener(mouse);
        gameID = Database.getLastGameId() + 1;
        setPieces();
        copyPieces(pieces, simPieces);
    }

    private String trimName(String name) {
        if (name.length() > MAX_NAME_LENGTH) {
            return name.substring(0, MAX_NAME_LENGTH) + "...";
        }
        return name;
    }

    public void run() {
        startTime = System.currentTimeMillis();
        double drawInterval = 1000 / FPS;
        double nextDrawTime = System.currentTimeMillis() + drawInterval;

        while (gameThread != null) {
            updateTimer();
            update();
            repaint();
            try {
                double remainingTime = nextDrawTime - System.currentTimeMillis();
                if (remainingTime < 0) {
                    remainingTime = 0;
                }
                Thread.sleep((long) remainingTime);
                nextDrawTime = nextDrawTime + drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateTimer() {
        // Calculate elapsed time
        if (!gameOver && !stalemate) {
            long currentTime = System.currentTimeMillis();
            elapsedTime = currentTime - startTime;
        }
    }

    public void launchGame() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void setPieces() {
        pieces.add(new Pawn(WHITE, 0, 6));
        pieces.add(new Pawn(WHITE, 1, 6));
        pieces.add(new Pawn(WHITE, 2, 6));
        pieces.add(new Pawn(WHITE, 3, 6));
        pieces.add(new Pawn(WHITE, 4, 6));
        pieces.add(new Pawn(WHITE, 5, 6));
        pieces.add(new Pawn(WHITE, 6, 6));
        pieces.add(new Pawn(WHITE, 7, 6));
        pieces.add(new Rook(WHITE, 0, 7));
        pieces.add(new Rook(WHITE, 7, 7));
        pieces.add(new Knight(WHITE, 1, 7));
        pieces.add(new Knight(WHITE, 6, 7));
        pieces.add(new Bishop(WHITE, 2, 7));
        pieces.add(new Bishop(WHITE, 5, 7));
        pieces.add(new Queen(WHITE, 3, 7));
        pieces.add(new King(WHITE, 4, 7));

        pieces.add(new Pawn(BLACK, 0, 1));
        pieces.add(new Pawn(BLACK, 1, 1));
        pieces.add(new Pawn(BLACK, 2, 1));
        pieces.add(new Pawn(BLACK, 3, 1));
        pieces.add(new Pawn(BLACK, 4, 1));
        pieces.add(new Pawn(BLACK, 5, 1));
        pieces.add(new Pawn(BLACK, 6, 1));
        pieces.add(new Pawn(BLACK, 7, 1));
        pieces.add(new Rook(BLACK, 0, 0));
        pieces.add(new Rook(BLACK, 7, 0));
        pieces.add(new Knight(BLACK, 1, 0));
        pieces.add(new Knight(BLACK, 6, 0));
        pieces.add(new Bishop(BLACK, 2, 0));
        pieces.add(new Bishop(BLACK, 5, 0));
        pieces.add(new Queen(BLACK, 3, 0));
        pieces.add(new King(BLACK, 4, 0));
    }

    private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
        target.clear();
        for (int i = 0; i < source.size(); i++) {
            target.add(source.get(i));
        }
    }

    private void update() {
        if (promotion) {
            promoting();
        } else if (!gameOver && !stalemate) {
            ///// MOUSE BUTTON PRESSED /////
            if (mouse.pressed) {
                if (activePiece == null) {
                    // If the activePiece is null, check if you can pick up a piece
                    for (Piece piece : simPieces) {
                        // If the mouse is on an ally piece, pick it up as the activePiece
                        if (piece.color == currentColor && piece.col == mouse.x / Board.SQUARE_SIZE && piece.row == mouse.y / Board.SQUARE_SIZE) {
                            activePiece = piece;
                        }
                    }
                } else {
                    // If the player is holding a piece, simulate the move
                    simulate();
                }
            }
            if (!mouse.pressed) {
                if (activePiece != null) {
                    if (isValidSquare) {
                        // MOVE CONFIRMED
                        String move = getMoveString(activePiece, activePiece.preCol, activePiece.col, activePiece.row);
                        addMove(move);
                        moveNumber++;
                        Database.insertMove(gameID, moveNumber, (currentColor == WHITE) ? this.whitePlayerName : this.blackPlayerName, move, getCurrentTime());
                        //  CHECK FOR FIFTY-MOVE RULE
                        if (!activePiece.type.equals(Type.PAWN) && activePiece.pieceAtTarget == null) {
                            consecutiveMovesWithoutPawnOrCapture++;
                        } else {
                            // Reset the move count if the move involves pawn movement or capture
                            consecutiveMovesWithoutPawnOrCapture = 0;
                        }
                        // Update the list of pieces if a piece has been captured and removed during simulation
                        copyPieces(simPieces, pieces);
                        activePiece.updatePosition();
                        if (isKingInCheck() && isCheckmate()) {
                            gameOver = true;
                            if (currentColor == WHITE) {
                                insertGameRecord(whitePlayerName);
                            } else {
                                insertGameRecord(blackPlayerName);
                            }
                        } else if ((isStalemate() || consecutiveMovesWithoutPawnOrCapture >= 50) && !isKingInCheck()) {
                            stalemate = true;
                            insertGameRecord("Draw");
                        } else {    // The game is still going on
                            if (canPromote()) {
                                promotion = true;
                            } else {
                                changePlayer();
                            }
                        }
                    } else {
                        // The move is cancelled by the player so reset everything back to the last state
                        copyPieces(pieces, simPieces);
                        activePiece.resetPosition();
                        activePiece = null;
                    }

                }
            }
        }
    }

    private void simulate() {
        isValidMove = false;
        isValidSquare = false;

        // Reset the piece list in every loop
        // This is basically for restoring the removed piece during the simulation
        copyPieces(pieces, simPieces);

        // If a piece is being held, update its position
        activePiece.x = mouse.x - Board.HALF_SQUARE_SIZE;
        activePiece.y = mouse.y - Board.HALF_SQUARE_SIZE;
        activePiece.col = activePiece.getCol(activePiece.x);
        activePiece.row = activePiece.getCol(activePiece.y);

        if (activePiece.isValidMove(activePiece.col, activePiece.row)) {
            isValidMove = true;
            if (activePiece.pieceAtTarget != null) {
                simPieces.remove(activePiece.pieceAtTarget.getIndex());
            }
            if (!isIllegalMove(activePiece) && !oppCanCaptureKing()) {
                isValidSquare = true;
            }
        }

    }

    private void changePlayer() {
        currentColor = (currentColor == WHITE) ? BLACK : WHITE;
        activePiece = null;
    }

    private boolean canPromote() {
        if (activePiece.type == Type.PAWN) {
            if (currentColor == WHITE && activePiece.row == 0) {
                promotionPieces.clear();
                promotionPieces.add(new Bishop(currentColor, -1, 3));
                promotionPieces.add(new Rook(currentColor, -1, 4));
                promotionPieces.add(new Knight(currentColor, -1, 5));
                promotionPieces.add(new Queen(currentColor, -1, 6));
                return true;
            } else if (currentColor == BLACK && activePiece.row == 7) {
                promotionPieces.clear();
                promotionPieces.add(new Bishop(currentColor, 9, 3));
                promotionPieces.add(new Rook(currentColor, 9, 4));
                promotionPieces.add(new Knight(currentColor, 9, 5));
                promotionPieces.add(new Queen(currentColor, 9, 6));
                return true;
            }
        }
        return false;
    }

    private void promoting() {
        if (mouse.pressed) {
            for (Piece piece : promotionPieces) {
                if (piece.col == mouse.x / Board.SQUARE_SIZE && piece.row == mouse.y / Board.SQUARE_SIZE) {
                    switch ((piece.type)) {
                        case BISHOP:
                            simPieces.add(new Bishop(currentColor, activePiece.col, activePiece.row));
                            break;
                        case ROOK:
                            simPieces.add(new Rook(currentColor, activePiece.col, activePiece.row));
                            break;
                        case KNIGHT:
                            simPieces.add(new Knight(currentColor, activePiece.col, activePiece.row));
                            break;
                        case QUEEN:
                            simPieces.add(new Queen(currentColor, activePiece.col, activePiece.row));
                            break;
                        default:
                            break;
                    }
                    simPieces.remove(activePiece.getIndex());
                    copyPieces(simPieces, pieces);
                    activePiece = null;
                    promotion = false;
                    changePlayer();
                }
            }
        }
    }

    private boolean isIllegalMove(Piece king) {
        if (king.type == Type.KING) {
            for (Piece piece : simPieces) {
                if (piece.color != king.color && piece.isValidMove(king.col, king.row)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean oppCanCaptureKing() {
        Piece king = getKing(false);
        for (Piece piece : simPieces) {
            if (piece.color != king.color && piece.isValidMove(king.col, king.row)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCheckmate() {
        Piece king = getKing(true);
        if (kingCanMove(king)) {
            return false;
        } else {
            int colDiff = Math.abs(checkingPiece.col - king.col);
            int rowDiff = Math.abs(checkingPiece.row - king.row);
            if (colDiff == 0) {
                // The checking piece is attacking vertically
                if (checkingPiece.row < king.row) {
                    //  The checking piece above the king
                    for (int row = checkingPiece.row; row < king.row; row++) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.isValidMove(checkingPiece.col, row)) {
                                return false;
                            }
                        }
                    }
                } else if (checkingPiece.row > king.row) {
                    //  The checking piece is below the king
                    for (int row = checkingPiece.row; row > king.row; row--) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.isValidMove(checkingPiece.col, row)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (rowDiff == 0) {
                //  The checking piece is attacking horizontally
                if (checkingPiece.col < king.col) {
                    //  The checking piece is on the left
                    for (int col = checkingPiece.col; col < king.row; col++) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.isValidMove(col, checkingPiece.row)) {
                                return false;
                            }
                        }
                    }
                } else if (checkingPiece.col > king.col) {
                    //  The checking piece is on the right
                    for (int col = checkingPiece.col; col > king.row; col--) {
                        for (Piece piece : simPieces) {
                            if (piece != king && piece.color != currentColor && piece.isValidMove(col, checkingPiece.row)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (colDiff == rowDiff) {
                //  The checking piece is attacking diagonally
                if (checkingPiece.row < king.row) {
                    //  The checking piece is above the king
                    if (checkingPiece.col < king.col) {
                        //  The checking piece is on the upper left
                        for (int col = checkingPiece.col, row = checkingPiece.row; col < king.col; col++, row++) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.isValidMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    } else if (checkingPiece.col > king.col) {
                        //  The checking piece is on the upper right
                        for (int col = checkingPiece.col, row = checkingPiece.row; col > king.col; col--, row++) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.isValidMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                } else if (checkingPiece.row > king.row) {
                    //  The checking piece is below the king
                    if (checkingPiece.col < king.col) {
                        //  The checking piece is on the lower left
                        for (int col = checkingPiece.col, row = checkingPiece.row; col < king.col; col++, row--) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.isValidMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    } else if (checkingPiece.col > king.col) {
                        //  The checking piece is on the lower right
                        for (int col = checkingPiece.col, row = checkingPiece.row; col > king.col; col--, row--) {
                            for (Piece piece : simPieces) {
                                if (piece != king && piece.color != currentColor && piece.isValidMove(col, row)) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean kingCanMove(Piece king) {
        if (isValidMove(king, -1, -1)) {
            return true;
        }
        if (isValidMove(king, 0, -1)) {
            return true;
        }
        if (isValidMove(king, 1, -1)) {
            return true;
        }
        if (isValidMove(king, -1, 0)) {
            return true;
        }
        if (isValidMove(king, 1, 0)) {
            return true;
        }
        if (isValidMove(king, -1, 1)) {
            return true;
        }
        if (isValidMove(king, 0, 1)) {
            return true;
        }
        if (isValidMove(king, 1, 1)) {
            return true;
        }
        return false;
    }

    private boolean isValidMove(Piece king, int colPlus, int rowPlus) {
        boolean isValidMove = false;
        king.col += colPlus;
        king.row += rowPlus;
        if (king.isValidMove(king.col, king.row)) {
            if (king.pieceAtTarget != null) {
                simPieces.remove(king.pieceAtTarget.getIndex());
            }
            if (!isIllegalMove(king)) {
                isValidMove = true;
            }
        }
        king.resetPosition();
        copyPieces(pieces, simPieces);
        return isValidMove;
    }

    private boolean isKingInCheck() {
        Piece king = getKing(true);
        if (activePiece.isValidMove(king.col, king.row)) {
            checkingPiece = activePiece;
            return true;
        } else {
            checkingPiece = null;
            return false;
        }
    }

    private boolean isStalemate() {
        int count = 0;
        //  Count the number of pieces opponent has
        for (Piece piece : simPieces) {
            if (piece.color != currentColor) {
                count++;
            }
        }
        //  If opponent only has king left
        if (count == 1) {
            if (!kingCanMove(getKing(true))) {
                return true;
            }
        }
        return false;
    }

    private Piece getKing(boolean opponent) {
        Piece king = null;
        for (Piece piece : simPieces) {
            if (opponent) {
                if (piece.type == Type.KING && piece.color != currentColor) {
                    king = piece;
                }
            } else {
                if (piece.type == Type.KING && piece.color == currentColor) {
                    king = piece;
                }
            }
        }
        return king;
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawTimer(g2);
        drawBoardAndPieces(g2);
        drawStatusMessages(g2);
    }

    private void drawBoardAndPieces(Graphics2D g2) {
        int offsetX = (WIDTH - (Board.SQUARE_SIZE * board.MAX_COL)) / 2;
        int offsetY = (HEIGHT - (Board.SQUARE_SIZE * board.MAX_ROW)) / 2;

        mouse.setOffsets(offsetX, offsetY);
        g2.translate(offsetX, offsetY);

        board.draw(g2);
        for (Piece p : simPieces) {
            p.draw(g2);
        }

        if (activePiece != null) {
            drawActivePiece(g2);
        }

        g2.translate(-offsetX, -offsetY);
    }

    private void drawActivePiece(Graphics2D g2) {
        if (isValidMove) {
            drawMoveHighlight(g2);
        }
        activePiece.draw(g2);
    }

    private void drawMoveHighlight(Graphics2D g2) {
        g2.setColor(isIllegalMove(activePiece) || oppCanCaptureKing() ? Color.red : Color.white);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g2.fillRect(activePiece.col * Board.SQUARE_SIZE, activePiece.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    private void drawTimer(Graphics2D g2) {
        g2.setFont(new Font("Gabriola", Font.PLAIN, 50));
        g2.setColor(Color.WHITE);
        String timerText = String.format("Game Time: %02d:%02d", (elapsedTime / 1000) / 60, (elapsedTime / 1000) % 60);
        g2.drawString(timerText, 0, 50);
    }

    private void addMove(String move) {
        if (currentColor == WHITE) {
            whiteMoves.add(move);
        } else {
            blackMoves.add(move);
        }
    }

    private void drawMoves(Graphics2D g2) {
        drawMovesForColor(g2, whiteMoves, 0, 300);
        drawMovesForColor(g2, blackMoves, getWidth() - 415, 300);
    }

    private void drawMovesForColor(Graphics2D g2, ArrayList<String> moves, int x, int y) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Times New Roman", Font.PLAIN, 20));
        int increment = 30;

        for (int i = 0; i < moves.size(); i++) {
            if (y >= 1080) {
                x += 100;
                y = 300;
            }
            g2.drawString((i + 1) + ". " + moves.get(i), x, y);
            y += increment;
        }
    }

    private void drawStatusMessages(Graphics2D g2) {
        g2.setFont(new Font("Gabriola", Font.PLAIN, 75));
        g2.setColor(Color.WHITE);

        if (promotion) {
            drawPromotionMessage(g2);
        } else {
            drawMoves(g2);
            drawTurnMessage(g2);
        }

        if (gameOver) {
            drawGameOverMessage(g2);
        }

        if (stalemate) {
            drawStalemateMessage(g2);
        }
    }

    private void drawPromotionMessage(Graphics2D g2) {
        String message = "Promote to: ";
        int x = (currentColor == WHITE) ? 75 : 1550;
        g2.drawString(message, x, 350);
        drawPromotionPieces(g2, x + 60, 400);
    }

    private void drawPromotionPieces(Graphics2D g2, int x, int y) {
        for (Piece piece : promotionPieces) {
            g2.drawImage(piece.image, x, y, Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
            y += Board.SQUARE_SIZE + 10;
        }
    }

    private void drawTurnMessage(Graphics2D g2) {
        g2.setFont(new Font("Gabriola", Font.PLAIN, 50));
        String message = (currentColor == WHITE) ? this.whitePlayerName + " 's turn" : this.blackPlayerName + " 's turn";
        int x = (currentColor == WHITE) ? 0 : 1505;
        g2.drawString(message, x, 200);

        if (checkingPiece != null && checkingPiece.color != currentColor) {
            drawCheckMessage(g2, x);
        }
    }

    private void drawCheckMessage(Graphics2D g2, int x) {
        g2.setFont(new Font("Gabriola", Font.PLAIN, 50));
        g2.setColor(Color.RED);
        g2.drawString("The King is in check!", x, 100);
    }

    private void drawGameOverMessage(Graphics2D g2) {
        String message = (currentColor == WHITE) ? whitePlayerName + " Wins" : blackPlayerName + " Wins";
        g2.setFont(new Font("Gabriola", Font.PLAIN, 100));
        g2.setColor(Color.YELLOW);
        g2.drawString(message, 780, 600);
    }

    private void drawStalemateMessage(Graphics2D g2) {
        g2.setFont(new Font("Gabriola", Font.PLAIN, 100));
        g2.setColor(Color.YELLOW);
        g2.drawString("Game Draw(Stalemate)", 580, 600);
    }

    private String getCurrentTime() {
        long minutes = (elapsedTime / 1000) / 60;
        long seconds = (elapsedTime / 1000) % 60;
        String currentTime = String.format("%02d:%02d", minutes, seconds);
        return currentTime;
    }

    private String getMoveString(Piece piece, int initialCol, int targetCol, int targetRow) {
        char pieceSymbol = piece.getSymbol();
        char file = getFileSymbol(initialCol, targetCol);
        int rank = getRankSymbol(targetRow);
        char specialFile = getFileSymbol(piece, targetCol);
        String special = "";
        if (piece.pieceAtTarget != null) {
            special = "x";
        }
        if (piece.type == Type.PAWN && special == "") {
            return "" + file + rank;
        } else if (piece.type == Type.PAWN && special == "x") {
            return specialFile + special + file + rank;
        }

        return pieceSymbol + special + file + rank;
    }

    private void insertGameRecord(String winner) {
        String date = getCurrentDate();
        Database.insertGame(gameID, date, whitePlayerName, blackPlayerName, winner, getCurrentTime());
    }

    private char getFileSymbol(int initialCol, int targetCol) {
        char file = ' ';
        file = (char) ('a' + targetCol);
        return file;
    }

    private char getFileSymbol(Piece piece, int col) {
        char file = ' ';
        if (piece.preCol > col) {
            file = (char) ('b' + col);
        } else if (piece.preCol < col) {
            file = (char) ('`' + col);
        } else {
            file = (char) ('b' + col);
        }

        return file;
    }

    private int getRankSymbol(int row) {
        int rank = 8 - row;
        return rank;
    }

    private String getCurrentDate() {
        Date currentDate = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        return sdf.format(currentDate);
    }

    public void cancelGame() {
        if (!gameOver && !stalemate) {
            insertGameRecord("Match Cancelled");
        }
    }

}
