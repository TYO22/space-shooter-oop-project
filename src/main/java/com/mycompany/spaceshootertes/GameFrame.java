package com.mycompany.spaceshootertes; 

import javax.swing.JFrame;

public class GameFrame extends JFrame {

    public GameFrame() {
        GamePanel gamePanel = new GamePanel();
        
        this.add(gamePanel);
        
        this.setTitle("Space Shooter");
        
        // --- PERUBAHAN UNTUK FULLSCREEN ---
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setUndecorated(true); // Menghilangkan border jendela (title bar, close button, dll.)
        this.setExtendedState(JFrame.MAXIMIZED_BOTH); // Membuat jendela memenuhi layar
        this.setVisible(true); 
        // ------------------------------------
        
        // Penting: Panggil requestFocus SETELAH setVisible
        gamePanel.requestFocusInWindow();
    }
}