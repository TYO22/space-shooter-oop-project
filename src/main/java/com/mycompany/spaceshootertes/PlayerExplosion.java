package com.mycompany.spaceshootertes; 

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class PlayerExplosion {

    private double x, y; // Posisi (pojok kiri atas)
    private int width, height; // Ukuran
    
    private BufferedImage spriteSheet;
    private BufferedImage[] frames; // Array untuk menyimpan semua 12 frame
    private int currentFrame;
    
    // --- Konfigurasi Sprite Sheet (PENTING) ---
    private final int ROWS = 3;
    private final int COLS = 4;
    private final int TOTAL_FRAMES = 12; // 3 baris * 4 kolom
    // -----------------------------------------
    
    private long lastFrameTime;
    private long frameDuration = 70; // 70ms per frame (sedikit lebih cepat)
    
    private boolean isActive;

    public PlayerExplosion(double x, double y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        
        this.currentFrame = 0;
        this.isActive = true;
        this.lastFrameTime = System.currentTimeMillis();
        this.frames = new BufferedImage[TOTAL_FRAMES];

        try {
            // Pastikan Anda menamai file Anda "player_explosion.png" di /res/
            spriteSheet = ImageIO.read(getClass().getResource("/res/player_explosion.png"));
            
            int frameWidth = spriteSheet.getWidth() / COLS;
            int frameHeight = spriteSheet.getHeight() / ROWS;

            // Potong-potong sprite sheet dan simpan ke array
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    int index = row * COLS + col;
                    frames[index] = spriteSheet.getSubimage(
                        col * frameWidth,
                        row * frameHeight,
                        frameWidth,
                        frameHeight
                    );
                }
            }
            
        } catch (Exception e) {
            System.err.println("Gagal memuat /res/player_explosion.png");
            isActive = false;
        }
    }

    /**
     * Memajukan animasi ke frame berikutnya.
     * @return true jika animasi masih aktif, false jika sudah selesai.
     */
    public boolean update() {
        if (!isActive) return false;

        long now = System.currentTimeMillis();
        if (now - lastFrameTime > frameDuration) {
            currentFrame++;
            lastFrameTime = now;
            
            if (currentFrame >= TOTAL_FRAMES) {
                isActive = false; // Animasi selesai
            }
        }
        return isActive;
    }

    /**
     * Menggambar frame animasi saat ini.
     */
    public void draw(Graphics g) {
        if (!isActive || frames[currentFrame] == null) {
            return;
        }
        
        // Gambar frame saat ini
        g.drawImage(frames[currentFrame], (int)x, (int)y, width, height, null);
    }

    public boolean isActive() {
        return isActive;
    }
}