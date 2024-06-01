package main;

import java.awt.Color;
import java.awt.Graphics2D;

public class Board {

    public final int MAX_COL = 8;
    public final int MAX_ROW = 8;
    public static final int SQUARE_SIZE = 135;
    public static final int HALF_SQUARE_SIZE = SQUARE_SIZE / 2;

    public void draw(Graphics2D g2) {
        int c = 0;
        for (int row = 0; row < MAX_ROW; row++) {
            for (int col = 0; col < MAX_COL; col++) {
                if (c == 0) {
                    g2.setColor(new Color(204, 183, 174));
                    c = 1;
                } else {
                    g2.setColor(new Color(112, 102, 119));
                    c = 0;
                }
                g2.fillRect(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);
            }
            if (c == 0) {
                c = 1;
            } else {
                c = 0;
            }
        }
    }
}
