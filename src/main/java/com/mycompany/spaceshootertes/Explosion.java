package com.mycompany.spaceshootertes; 

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Explosion {

    private double x, y; // Posisi tengah ledakan
    private int width, height; // Ukuran ledakan di layar
    
    private BufferedImage spriteSheet;
    private int currentFrame;
    private int totalFrames;
    
    private int frameWidth; // Lebar SATU frame di sprite sheet
    private int frameHeight; // Tinggi SATU frame di sprite sheet
    private int framesPerRow; // Berapa frame per baris di sprite sheet
    
    private long lastFrameTime;
    private long frameDuration = 80; // 80ms per frame (bisa diatur)
    
    private boolean isFinished;

    /**
     * Membuat animasi ledakan baru.
     * @param x Posisi X (pojok kiri atas) dari objek yang hancur.
     * @param y Posisi Y (pojok kiri atas) dari objek yang hancur.
     * @param width Lebar objek yang hancur (untuk ukuran ledakan).
     * @param height Tinggi objek yang hancur (untuk ukuran ledakan).
     */
    public Explosion(double x, double y, int width, int height) {
        // Posisikan ledakan di tengah objek yang hancur
        this.x = x + (width / 2.0);
        this.y = y + (height / 2.0);
        
        // Atur ukuran ledakan agar pas dengan objeknya
        this.width = width; 
        this.height = height;
        
        this.currentFrame = 0;
        this.isFinished = false;
        this.lastFrameTime = System.currentTimeMillis();

        try {
            // Muat file .png transparan yang sudah Anda siapkan
            spriteSheet = ImageIO.read(getClass().getResource("/res/explosion.png"));
            
            // --- KONFIGURASI UNTUK SPRITE ANDA ---
            this.totalFrames = 6;  // Total 6 frame
            this.framesPerRow = 3;  // 3 frame per baris
            int numRows = 2; // 2 baris
            // ------------------------------------
            
            this.frameWidth = spriteSheet.getWidth() / framesPerRow;
            this.frameHeight = spriteSheet.getHeight() / numRows;
            
        } catch (Exception e) {
            System.err.println("Gagal memuat /res/explosion.png");
            isFinished = true; // Langsung tandai selesai jika gambar gagal
        }
    }

    /**
     * Memperbarui frame animasi.
     * @return true jika animasi telah selesai.
     */
    public boolean update() {
        if (isFinished) {
            return true;
        }

        long now = System.currentTimeMillis();
        if (now - lastFrameTime > frameDuration) {
            currentFrame++; // Pindah ke frame berikutnya
            lastFrameTime = now;
            
            if (currentFrame >= totalFrames) {
                isFinished = true; // Animasi selesai
            }
        }
        
        return isFinished;
    }

    /**
     * Menggambar frame animasi saat ini.
     */
    public void draw(Graphics g) {
        if (spriteSheet == null || isFinished) {
            return;
        }

        // Hitung posisi frame saat ini di dalam sprite sheet
        int col = currentFrame % framesPerRow;
        int row = currentFrame / framesPerRow;
        
        int sx1 = col * frameWidth;
        int sy1 = row * frameHeight;
        int sx2 = sx1 + frameWidth;
        int sy2 = sy1 + frameHeight;

        // Hitung posisi gambar di layar (dipusatkan di this.x, this.y)
        int drawX = (int)(this.x - this.width / 2.0);
        int drawY = (int)(this.y - this.height / 2.0);

        // Menggambar hanya bagian sprite sheet yang relevan ke layar
        g.drawImage(spriteSheet, 
                drawX, drawY, drawX + width, drawY + height, // Posisi di layar
                sx1, sy1, sx2, sy2, // Posisi di sprite sheet
                null);
    }
}