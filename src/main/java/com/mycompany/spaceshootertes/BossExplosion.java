package com.mycompany.spaceshootertes; 

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;

public class BossExplosion {

    private BufferedImage spriteSheet;
    private BufferedImage[] frames; // Array untuk menyimpan setiap frame animasi
    private int currentFrame;
    private long lastFrameTime;
    private long frameDuration = 80; // Durasi setiap frame (ms) - atur sesuai keinginan
    private double x, y; // Posisi ledakan
    private int width, height; // Ukuran area ledakan
    
    // Jumlah baris dan kolom di sprite sheet Anda
    private final int ROWS = 3;
    private final int COLS = 4;
    private final int FRAME_COUNT = ROWS * COLS;

    private boolean active; // Apakah ledakan masih aktif

    public BossExplosion(double x, double y, int targetWidth, int targetHeight) {
        this.x = x;
        this.y = y;
        this.width = targetWidth;  // Ini akan menjadi lebar target
        this.height = targetHeight; // Ini akan menjadi tinggi target
        this.currentFrame = 0;
        this.lastFrameTime = System.currentTimeMillis();
        this.active = true;

        try {
            
            spriteSheet = ImageIO.read(getClass().getResource("/res/boss_explosion.png"));
            
            // Inisialisasi array frames
            frames = new BufferedImage[FRAME_COUNT];
            
            // Hitung lebar dan tinggi setiap sprite di sheet
            int spriteWidth = spriteSheet.getWidth() / COLS;
            int spriteHeight = spriteSheet.getHeight() / ROWS;

            // Potong setiap frame dari sprite sheet
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    int frameIndex = r * COLS + c;
                    frames[frameIndex] = spriteSheet.getSubimage(
                        c * spriteWidth,
                        r * spriteHeight,
                        spriteWidth,
                        spriteHeight
                    );
                }
            }
            
        } catch (IOException e) {
            System.err.println("Gagal memuat sprite sheet ledakan bos: boss_explosion.png");
            e.printStackTrace();
            this.active = false; // Jika gagal, pastikan tidak mencoba menggambar
        }
    }

    public void update() {
        if (!active) return;

        long now = System.currentTimeMillis();
        if (now - lastFrameTime > frameDuration) {
            currentFrame++;
            lastFrameTime = now;

            if (currentFrame >= FRAME_COUNT) {
                active = false; // Animasi selesai
            }
        }
    }

    public void draw(Graphics g) {
        if (!active || currentFrame >= FRAME_COUNT || frames[currentFrame] == null) {
            return;
        }
        
        // Gambar frame saat ini, skalakan ke ukuran target (width, height)
        g.drawImage(frames[currentFrame], (int)x, (int)y, width, height, null);
    }

    public boolean isActive() {
        return active;
    }
}