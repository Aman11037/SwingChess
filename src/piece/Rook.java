package piece;

import main.GamePanel;
import main.Type;

public class Rook extends Piece {

    public Rook(int color, int col, int row) {
        super(color, col, row);
        type = Type.ROOK;
        if (color == GamePanel.WHITE) {
            image = getImage("/piece/w_rook");
        } else {
            image = getImage("/piece/b_rook");
        }
    }

    public boolean isValidMove(int targetCol, int targetRow) {
        if (isWithinBoard(targetCol, targetRow) && !isSameSquare(targetCol, targetRow)) {
            // Rook can move as long as its the same column or row
            if (targetCol == preCol || targetRow == preRow) {
                if (isValidSquare(targetCol, targetRow) && !pieceIsOnAStraightLine(targetCol, targetRow)) {
                    return true;
                }
            }
        }
        return false;
    }

}
