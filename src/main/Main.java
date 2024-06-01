package main;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {

    public static void main(String[] args) {
        String player1 = JOptionPane.showInputDialog(null, "Enter Player 1 name (White):");
        String player2 = JOptionPane.showInputDialog(null, "Enter Player 2 name (Black):");
        JFrame window = new JFrame();
        window.setUndecorated(true);
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.setResizable(false);
        GamePanel gp = new GamePanel(player1, player2);
        window.add(gp);
        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
        gp.launchGame();

        window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeGame(gp, window);
            }
        });

        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    closeGame(gp, window);
                }
            }
        });

        window.setFocusable(true);
        window.requestFocusInWindow();
    }

    private static void closeGame(GamePanel gp, JFrame window) {
        int option = JOptionPane.showConfirmDialog(window, "Are you sure you want to exit the game?", "Exit Game", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            gp.cancelGame();
            window.dispose();
            System.exit(0);
        }
    }
}
