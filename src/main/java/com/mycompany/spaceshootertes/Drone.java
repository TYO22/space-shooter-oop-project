package com.mycompany.spaceshootertes; 

import java.awt.AlphaComposite;
import java.awt.Color; // Import Color
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.List;

public class Drone {

    public double x, y;
    public int width, height;
    
    // Offset dari player
    private double offsetX;
    private double offsetY;
    
    private BufferedImage droneImage;

    // --- LOGIKA AMUNISI & STATUS BARU ---
    public final int MAX_AMMO = 100; // Total amunisi drone
    public int currentAmmo;
    private boolean isLeaving = false; // Status drone (sedang mengikuti atau pergi)
    private double leavingSpeed = 8.0; // Kecepatan drone pergi ke atas
    // ------------------------------------

    public Drone(double offsetX, double offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        
        // Inisialisasi amunisi
        this.currentAmmo = MAX_AMMO;
        
        loadDroneImage();
    }
    
    private void loadDroneImage() {
        try {
            droneImage = ImageIO.read(getClass().getResource("/res/drone.png"));
            this.width = droneImage.getWidth();
            this.height = droneImage.getHeight();
        } catch (Exception e) {
            System.err.println("Gagal memuat /res/drone.png. Gunakan fallback.");
            this.width = 30;
            this.height = 30;
        }
    }
    
    // Update posisi drone
    public void update(Player player) {
        
        // --- TAMBAHAN BARU ---
        // Cek status amunisi *sebelum* memutuskan cara bergerak.
        // Ini memastikan drone "langsung pergi" saat amunisi 0.
        if (this.currentAmmo <= 0) {
            this.isLeaving = true;
        }
        // ---------------------
        
        if (isLeaving) {
            // Drone bergerak lurus ke BAWAH (mundur) untuk "pergi"
            this.y += leavingSpeed;
            
        } else {
            // Logika "following" yang mulus (interpolasi)
            double targetX = player.x + offsetX - (width / 2.0) + (player.width / 2.0);
            double targetY = player.y + offsetY;
            
            // Gerak 10% lebih dekat ke target setiap frame
            this.x += (targetX - this.x) * 0.1;
            this.y += (targetY - this.y) * 0.1;
        }
    }    
    /**
     * Metode untuk menembak.
     * Sekarang mengembalikan 'true' jika berhasil menembak, 'false' jika amunisi habis.
     */
    public boolean shoot(List<Bullet> bullets, double bulletSpeed) {
        // Jangan tembak jika sedang pergi atau amunisi habis
        if (isLeaving || currentAmmo <= 0) {
            return false;
        }
        
        // Tembak peluru lurus dari tengah drone
        double spawnX = this.x + this.width / 2.0 - (9.0 / 2.0); // Asumsi lebar peluru 9
        double spawnY = this.y;
        
        bullets.add(new Bullet(spawnX, spawnY, 0.0, bulletSpeed));
        
        // Kurangi amunisi drone
        this.currentAmmo--;
        
        // Cek jika amunisi baru saja habis
        if (this.currentAmmo <= 0) {
            isLeaving = true; // Set status untuk "pergi"
        }
        
        return true; // Berhasil menembak
    }
    
    /**
     * Pengecekan untuk GamePanel tahu kapan harus menghapus drone ini.
     * @param screenHeight Batas bawah layar (diberikan oleh GamePanel)
     */
    public boolean isOffScreen(int screenHeight) {
        // Hapus drone jika statusnya "pergi" DAN sudah keluar layar di BAWAH
        return isLeaving && (this.y > screenHeight);
    }
    
    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        // Buat drone sedikit transparan
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        
        if (droneImage != null) {
            g2d.drawImage(droneImage, (int)x, (int)y, width, height, null);
        }
        
        // --- GAMBAR AMMO BAR DI BAWAH DRONE ---
        if (!isLeaving) {
            double ammoRatio = (double) currentAmmo / MAX_AMMO;
            int barWidth = width; // Lebar bar = lebar drone
            int barHeight = 5;    // Tinggi bar 5 piksel
            int barX = (int)x;
            int barY = (int)y + height + 2; // 2 piksel di bawah drone

            // Gambar latar belakang bar (abu-abu gelap)
            g2d.setColor(new Color(50, 50, 50)); 
            g2d.fillRect(barX, barY, barWidth, barHeight);
            
            // Tentukan warna bar (merah jika kritis, cyan jika normal)
            if (ammoRatio < 0.2) { // 20% sisa
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.CYAN);
            }
            
            // Gambar sisa amunisi
            g2d.fillRect(barX, barY, (int)(barWidth * ammoRatio), barHeight);
        }
        // ------------------------------------
        
        g2d.dispose();
    }
    
    /**
     * Mengembalikan offset X drone (negatif untuk kiri, positif untuk kanan).
     */
    public double getOffsetX() {
        return this.offsetX;
    }
}