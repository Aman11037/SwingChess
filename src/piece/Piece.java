package piece;

import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import main.Board;
import main.GamePanel;
import java.awt.Graphics2D;
import main.Type;

public class Piece {

    public Type type;
    public BufferedImage image;
    public int x, y;
    public int col, row, preCol, preRow;
    public int color;
    public Piece pieceAtTarget;
    public boolean moved;

    public Piece(int color, int col, int row) {
        this.color = color;
        this.col = col;
        this.row = row;
        x = getX(col);
        y = getY(row);
        preCol = col;
        preRow = row;
    }

    public BufferedImage getImage(String imagePath) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(getClass().getResourceAsStream(imagePath + ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    public int getY(int row) {
        return row * Board.SQUARE_SIZE;
    }

    public int getX(int col) {
        return col * Board.SQUARE_SIZE;
    }

    public int getCol(int x) {
        return ((x + Board.HALF_SQUARE_SIZE) / Board.SQUARE_SIZE);
    }

    public int getRow(int y) {
        return ((y + Board.HALF_SQUARE_SIZE) / Board.SQUARE_SIZE);
    }

    public int getIndex() {
        for (int index = 0; index < GamePanel.simPieces.size(); index++) {
            if (GamePanel.simPieces.get(index) == this) {
                return index;
            }
        }
        return 0;
    }

    public void updatePosition() {
        x = getX(col);
        y = getY(row);
        preCol = getCol(x);
        preRow = getRow(y);
        moved = true;
    }

    public void resetPosition() {
        col = preCol;
        row = preRow;
        x = getX(col);
        y = getY(row);
    }

    public boolean isValidMove(int targetCol, int targetRow) {
        return false;
    }

    public boolean isWithinBoard(int targetCol, int targetRow) {
        if (targetCol >= 0 && targetCol <= 7 && targetRow >= 0 && targetRow <= 7) {
            return true;
        }
        return false;
    }

    public Piece getPieceAtTarget(int targetCol, int targetRow) {
        for (Piece piece : GamePanel.simPieces) {
            if (piece.col == targetCol && piece.row == targetRow && piece != this) {
                return piece;
            }
        }
        return null;
    }

    public boolean pieceIsOnAStraightLine(int targetCol, int targetRow) {
        if (targetCol == preCol) {
            // Moving vertically
            int move = (targetRow > preRow) ? 1 : -1;
            for (int r = preRow + move; r != targetRow; r += move) {
                for (Piece piece : GamePanel.simPieces) {
                    if (piece.col == targetCol && piece.row == r) {
                        pieceAtTarget = piece;
                        return true;
                    }
                }
            }
        } else if (targetRow == preRow) {
            // Moving horizontally
            int move = (targetCol > preCol) ? 1 : -1;
            for (int c = preCol + move; c != targetCol; c += move) {
                for (Piece piece : GamePanel.simPieces) {
                    if (piece.col == c && piece.row == targetRow) {
                        pieceAtTarget = piece;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean pieceIsOnDiagonalLine(int targetCol, int targetRow) {
        // Check whether to move up left, up right, down left or down right
        int colMove = (targetCol > preCol) ? 1 : -1;
        int rowMove = (targetRow > preRow) ? 1 : -1;

        int c = preCol + colMove;
        int r = preRow + rowMove;

        while (c != targetCol && r != targetRow) {
            for (Piece piece : GamePanel.simPieces) {
                if (piece.col == c && piece.row == r) {
                    pieceAtTarget = piece;
                    return true;
                }
            }
            c += colMove;
            r += rowMove;
        }
        return false;
    }

    public boolean isSameSquare(int targetCol, int targetRow) {
        if (targetCol == preCol && targetRow == preRow) {
            return true;
        }
        return false;
    }

    public boolean isValidSquare(int targetCol, int targetRow) {
        pieceAtTarget = getPieceAtTarget(targetCol, targetRow);
        if (pieceAtTarget == null) {
            return true;
        } else {
            if (pieceAtTarget.color != this.color) {
                return true;
            } else {
                pieceAtTarget = null;
            }
        }
        return false;
    }

    public void draw(Graphics2D g2) {
        g2.drawImage(image, x, y, Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
    }
}
