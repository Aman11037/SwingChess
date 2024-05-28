package piece;

import main.GamePanel;
import main.Type;

public class Pawn extends Piece {

    public Pawn(int color, int col, int row) {
        super(color, col, row);
        type = Type.PAWN;
        if (color == GamePanel.WHITE) {
            image = getImage("/piece/w_pawn");
        } else {
            image = getImage("/piece/b_pawn");
        }
    }

    public boolean isValidMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            int move = (color == GamePanel.WHITE) ? -1 : 1;
            // Checking the hitting piece
            pieceAtTarget = getPieceAtTarget(targetCol, targetRow);
            // 1 square movement
            if (targetCol == preCol && targetRow == preRow + move && pieceAtTarget == null) {
                return true;
                // 2 squares movement
            } else if (targetCol == preCol && targetRow == preRow + move * 2 && pieceAtTarget == null && !moved && !pieceIsOnAStraightLine(targetCol, targetRow)) {
                return true;
                // Diagonal movement and capture only if a piece is on a square diagonally in front of it
            } else if (Math.abs(targetCol - preCol) == 1 && targetRow == preRow + move && pieceAtTarget != null && pieceAtTarget.color != color) {
                return true;
            }
        }
        return false;
    }
    
    public char getSymbol() {
        return 'P'; 
    }
}
