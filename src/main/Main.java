package main;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.sql.SQLException;
import piece.*;
import java.awt.BorderLayout;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
        JFrame window = new JFrame();
        window.setUndecorated(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        GamePanel gp = new GamePanel();
        window.add(gp);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        gp.launchGame();
    }
}
