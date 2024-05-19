package main;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Mouse extends MouseAdapter {

    public int x, y;
    public boolean pressed;
    public int offsetX, offsetY;

    public void setOffsets(int offsetX, int offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
    }

    public void mousePressed(MouseEvent e) {
        pressed = true;
        updateMousePosition(e);
    }

    public void mouseReleased(MouseEvent e) {
        pressed = false;
        updateMousePosition(e);
    }

    public void mouseDragged(MouseEvent e) {
        updateMousePosition(e);
    }

    public void mouseMoved(MouseEvent e) {
        updateMousePosition(e);
    }

    private void updateMousePosition(MouseEvent e) {
        x = e.getX() - offsetX;
        y = e.getY() - offsetY;
    }
}
