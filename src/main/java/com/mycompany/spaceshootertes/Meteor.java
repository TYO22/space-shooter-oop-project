package com.mycompany.spaceshootertes; 

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.imageio.ImageIO;

// --- TAMBAHKAN IMPORT INI ---
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
// -----------------------------

public class Meteor {

    public double x, y;
    private double speed;
    private int width, height;
    private BufferedImage originalImage; 
    private BufferedImage currentImage;  
    private Random rand = new Random();
    
    // --- VARIABEL BARU UNTUK ROTASI ---
    private double rotationAngle; // Sudut rotasi saat ini
    private double rotationSpeed; // Kecepatan rotasi (agar tiap meteor beda)
    // ----------------------------------
    
    private final int SCREEN_WIDTH;
    private final int SCREEN_HEIGHT;

    public Meteor(BufferedImage image, int screenWidth, int screenHeight) {
        this.originalImage = image;
        this.SCREEN_WIDTH = screenWidth;
        this.SCREEN_HEIGHT = screenHeight;
        
        // --- INISIALISASI ROTASI ---
        this.rotationAngle = rand.nextDouble() * Math.PI * 2; // Mulai dari sudut acak
        // Kecepatan acak (bisa positif/negatif agar arah putar acak)
        this.rotationSpeed = (rand.nextDouble() - 0.5) * 0.02; // Putaran pelan
        // -----------------------------
        
        double scale = 0.5 + (rand.nextDouble() * 0.5); 
        this.width = (int)(originalImage.getWidth() * scale);
        this.height = (int)(originalImage.getHeight() * scale);
        
        this.currentImage = new BufferedImage(width, height, originalImage.getType());
        Graphics g = this.currentImage.getGraphics();
        g.drawImage(originalImage, 0, 0, width, height, null);
        g.dispose();

        this.speed = 0.5 + (rand.nextDouble() * 1.0); 
        
        resetPosition(true); 
    }

    private void resetPosition(boolean isInitialSpawn) {
        if (isInitialSpawn) {
            this.x = rand.nextInt(SCREEN_WIDTH + width) - width; 
            this.y = rand.nextInt(SCREEN_HEIGHT + height) - height;
        } else {
            if (rand.nextBoolean()) {
                this.x = rand.nextInt(SCREEN_WIDTH);
                this.y = -height; 
            } else {
                this.x = -width; 
                this.y = rand.nextInt(SCREEN_HEIGHT);
            }
        }
    }

    public void move() {
        // Bergerak serong 45 derajat
        this.x += speed;
        this.y += speed;

        // --- UPDATE SUDUT ROTASI ---
        this.rotationAngle += this.rotationSpeed;
        // Jaga agar angka tidak terlalu besar (opsional)
        if (this.rotationAngle > Math.PI * 2) {
            this.rotationAngle -= Math.PI * 2;
        }
        // ---------------------------

        if (this.x > SCREEN_WIDTH + width || this.y > SCREEN_HEIGHT + height) { 
            resetPosition(false); 
            
            // Atur ulang rotasi, ukuran, dan kecepatan agar variatif
            this.rotationSpeed = (rand.nextDouble() - 0.5) * 0.02;
            double scale = 0.5 + (rand.nextDouble() * 0.5);
            this.width = (int)(originalImage.getWidth() * scale);
            this.height = (int)(originalImage.getHeight() * scale);
            this.currentImage = new BufferedImage(width, height, originalImage.getType());
            Graphics g = this.currentImage.getGraphics();
            g.drawImage(originalImage, 0, 0, width, height, null);
            g.dispose();
            this.speed = 0.5 + (rand.nextDouble() * 1.0);
        }
    }

    // ========================================================================
    // --- METODE DRAW() YANG DIPERBARUI UNTUK ROTASI ---
    // ========================================================================
    
    public void draw(Graphics g) {
        if (currentImage == null) {
            return;
        }

        // 1. Buat salinan Graphics2D
        Graphics2D g2d = (Graphics2D) g.create();

        // 2. Buat transformasi
        AffineTransform at = new AffineTransform();
        
        // 3. Pindahkan titik pivot ke TENGAH gambar
        // (x + width/2) dan (y + height/2)
        at.translate(x + (width / 2.0), y + (height / 2.0));
        
        // 4. Lakukan rotasi
        at.rotate(rotationAngle);
        
        // 5. Pindahkan titik pivot kembali ke POJOK KIRI ATAS
        at.translate(-width / 2.0, -height / 2.0);

        // 6. Gambar gambar menggunakan transformasi yang sudah diatur
        g2d.drawImage(currentImage, at, null);
        
        // 7. Hapus salinan g2d untuk mengembalikan Graphics ke keadaan normal
        g2d.dispose();
    }
    // ========================================================================
    // --- AKHIR METODE DRAW() ---
    // ========================================================================
}