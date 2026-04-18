package com.mycompany.spaceshootertes; 

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;

public class LaserBeam {
    public double x, y;
    public double width, height;
    public long activationTime;
    public long duration; // Durasi laser AKTIF (misal: 2000ms)
    public long warningDuration; // Durasi PERINGATAN (misal: 1500ms)
    public int damage;
    
    private BufferedImage laserImage;

    public LaserBeam(double x, double y, double width, double height, int damage, long duration, long warningDuration) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.damage = damage;
        this.activationTime = System.currentTimeMillis();
        this.duration = duration;
        this.warningDuration = warningDuration;
        
        loadLaserImage();
    }
    
    private void loadLaserImage() {
        try {
            
            laserImage = ImageIO.read(getClass().getResource("/res/laserRed16.png"));

            if (laserImage != null) {
                // HAPUS BARIS INI! INI BIANG MASALAHNYA
                // this.width = laserImage.getWidth(); 
            } else {
                 System.err.println("Gagal memuat /res/laserred16.png. Menggunakan fallback.");
                 this.width = 20;
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat /res/laserRed07.png. Menggunakan fallback warna.");
            this.width = 20;
        }
    }

    public boolean update() {
        long now = System.currentTimeMillis();
        // Laser akan dihapus setelah peringatan + durasi aktif selesai
        return (now - activationTime) > (duration + warningDuration);
    }

    // ========================================================================
    // --- METODE DRAW() YANG DIPERBARUI ---
    // ========================================================================
    
    public void draw(Graphics g) {
        long now = System.currentTimeMillis();
        long timeElapsed = now - activationTime;
        Graphics2D g2d = (Graphics2D) g.create();

        // Tentukan durasi berkedip (misalnya 500ms sebelum hilang)
        long fadeTime = 500; 
        
        // Kapan laser mulai berkedip (Waktu Peringatan + Waktu Durasi - Waktu Berkedip)
        long blinkStartTime = warningDuration + duration - fadeTime;

        if (timeElapsed < warningDuration) {
            // --- FASE 1: PERINGATAN (Kode Anda sudah benar) ---
            float alpha = (float) Math.abs(Math.sin(timeElapsed * 0.01)) * 0.7f;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            if (laserImage != null) {
                g2d.drawImage(laserImage, (int)x, (int)y, (int)width, (int)height, null);
            } else {
                g2d.setColor(Color.RED);
                g2d.fillRect((int)x, (int)y, (int)width, (int)height);
            }

        } else if (timeElapsed < blinkStartTime) {
            // --- FASE 2: AKTIF SOLID (Laser menembak penuh) ---
            if (laserImage != null) {
                g2d.drawImage(laserImage, (int)x, (int)y, (int)width, (int)height, null);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.fillRect((int)x, (int)y, (int)width, (int)height);
            }

        } else if (timeElapsed < (warningDuration + duration)) {
            // --- FASE 3: BERKEDIP (Sesaat sebelum hilang) ---
            // Berkedip setiap 100ms
            if (System.currentTimeMillis() / 100 % 2 == 0) {
                 if (laserImage != null) {
                    g2d.drawImage(laserImage, (int)x, (int)y, (int)width, (int)height, null);
                } else {
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect((int)x, (int)y, (int)width, (int)height);
                }
            }
            // Jika tidak, jangan gambar apa-apa (untuk efek berkedip)
            
        } 
        // Setelah (warningDuration + duration), laser tidak akan digambar
        // dan akan dihapus oleh GamePanel via update()

        g2d.dispose();
    }
    // ========================================================================
    // --- AKHIR METODE DRAW() ---
    // ========================================================================


    public Rectangle getBounds() {
        long now = System.currentTimeMillis();
        long timeElapsed = now - activationTime;
        
        // Laser hanya bisa mengenai player saat FASE 2 (Aktif Solid) dan FASE 3 (Berkedip)
        if (timeElapsed >= warningDuration && timeElapsed < (warningDuration + duration)) {
            return new Rectangle((int)x, (int)y, (int)width, (int)height);
        }
        return new Rectangle(0,0,0,0); // Tidak ada hitbox saat fase peringatan
    }
    
    public boolean isExpired() {
        return update();
    }
}