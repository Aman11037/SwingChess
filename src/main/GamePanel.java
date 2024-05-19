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

public class GamePanel extends JPanel implements Runnable {

    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;
    final int FPS = 144;
    Thread gameThread;
    Board board = new Board();
    Mouse mouse = new Mouse();

    // PIECES
    public static ArrayList<Piece> pieces = new ArrayList<>();
    public static ArrayList<Piece> simPieces = new ArrayList<>();
    public ArrayList<Piece> promotionPieces = new ArrayList<>();
    Piece activePiece, checkingPiece;

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

    // Timer variables
    private long startTime;
    private long elapsedTime;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.black);
        addMouseMotionListener(mouse);
        addMouseListener(mouse);
        setPieces();
        copyPieces(pieces, simPieces);

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
                    if ((isValidSquare)) {
                        // MOVE CONFIRMED

                        // Update the list of pieces if a piece has been captured and removed during simulation
                        copyPieces(simPieces, pieces);
                        activePiece.updatePosition();

                        if (isKingInCheck() && isCheckmate()) {
                            gameOver = true;
                        } else if (isStalemate() && !isKingInCheck()) {
                            stalemate = true;
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
        }
        return false;
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
        drawTimer(g);

        // Calculate the offset to center the board
        int offsetX = (WIDTH - (Board.SQUARE_SIZE * board.MAX_COL)) / 2;
        int offsetY = (HEIGHT - (Board.SQUARE_SIZE * board.MAX_ROW)) / 2;

        mouse.setOffsets(offsetX, offsetY);

        g2.translate(offsetX, offsetY);

        board.draw(g2);
        for (Piece p : simPieces) {
            p.draw(g2);
        }

        if (activePiece != null) {
            if (isValidMove) {
                if (isIllegalMove(activePiece) || oppCanCaptureKing()) {
                    g2.setColor(Color.red);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(activePiece.col * Board.SQUARE_SIZE, activePiece.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                } else {
                    g2.setColor(Color.white);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                    g2.fillRect(activePiece.col * Board.SQUARE_SIZE, activePiece.row * Board.SQUARE_SIZE, Board.SQUARE_SIZE, Board.SQUARE_SIZE);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                }
            }

            // Draw the active piece in the end so that it won't be hidden by the board or the colored square
            activePiece.draw(g2);
        }

        // Reset the translation
        g2.translate(-offsetX, -offsetY);

        // STATUS MESSAGES
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(new Font("Gabriola", Font.PLAIN, 75));
        g2.setColor(Color.WHITE);

        if (promotion) {
            if (currentColor == WHITE) {
                g2.drawString("Promote to: ", 80, 350);
                int xOffset = 150; // Initial x offset for drawing promotion pieces
                int yOffset = 400; // Initial y offset for drawing promotion pieces
                for (Piece piece : promotionPieces) {
                    g2.drawImage(piece.image, xOffset, yOffset, Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
                    // Use offsetX and offsetY to adjust the drawing position
                    yOffset += Board.SQUARE_SIZE + 10; // Increase the y offset for the next row
                }
            } else if (currentColor == BLACK) {
                g2.drawString("Promote to: ", 1550, 350);
                int xOffset = 1625; // Initial x offset for drawing promotion pieces
                int yOffset = 400; // Initial y offset for drawing promotion pieces
                for (Piece piece : promotionPieces) {
                    g2.drawImage(piece.image, xOffset, yOffset, Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
                    // Use offsetX and offsetY to adjust the drawing position
                    yOffset += Board.SQUARE_SIZE + 10; // Increase the y offset for the next row
                }
            }

        }
        if (currentColor == WHITE) {
            g2.drawString("White's turn", 75, 200);
            if (checkingPiece != null && checkingPiece.color == BLACK) {
                g2.setFont(new Font("Gabriola", Font.PLAIN, 50));
                g2.setColor(Color.RED);
                g2.drawString("The King is in check!", 75, 400);
            }
        } else {
            g2.drawString("Black's turn", 1550, 200);
            if (checkingPiece != null && checkingPiece.color == WHITE) {
                g2.setFont(new Font("Gabriola", Font.PLAIN, 50));
                g2.setColor(Color.RED);
                g2.drawString("The King is in check!", 1550, 400);
            }
        }

        if (gameOver) {
            String s = "";
            if (currentColor == WHITE) {
                s = "White Wins";
            } else if (currentColor == BLACK) {
                s = "Black Wins";
            }
            g2.setFont(new Font("Gabriola", Font.PLAIN, 100));
            g2.setColor(Color.YELLOW);
            g2.drawString(s, 780, 600);
        }

        if (stalemate) {
            g2.setFont(new Font("Gabriola", Font.PLAIN, 100));
            g2.setColor(Color.YELLOW);
            g2.drawString("Game Draw(Stalemate)", 580, 600);
        }
    }

    private void drawTimer(Graphics g) {
        g.setFont(new Font("Gabriola", Font.PLAIN, 50));
        g.setColor(Color.WHITE);
        long minutes = (elapsedTime / 1000) / 60;
        long seconds = (elapsedTime / 1000) % 60;
        String timerText = String.format("%02d:%02d", minutes, seconds);
        g.drawString("Game Time: " + timerText, 75, 50);
    }
}
