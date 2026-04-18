package com.mycompany.spaceshootertes; 

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.util.Random;
import java.awt.AlphaComposite;
import java.awt.Font; // Import Font
import com.mycompany.spaceshootertes.PowerUp.PowerUpType; // Pastikan ini ada jika PowerUp digunakan

/**
 * Kelas FinalBoss merepresentasikan musuh terakhir dalam game.
 * Kelas ini memiliki beberapa fase serangan, termasuk dash, tembakan peluru,
 * serangan laser, dan kemampuan untuk memanggil minion (BossClone).
 */
public class FinalBoss extends Enemy {

    // --- Variabel Gambar dan Referensi ---
    private BufferedImage bossImage; // Gambar sprite bos
    private GamePanel gamePanel;     // Referensi ke panel game utama
    private BufferedImage warningLaserImage; // Gambar yang digunakan untuk indikator laser

    // --- Variabel Posisi dan Gerakan ---
    private double targetX; // Posisi X target (biasanya tengah)
    private double targetY; // Posisi Y target (biasanya di atas)
    private double moveSpeed = 0.8; // Kecepatan gerak horizontal dasar
    private double rotationAngle = 0; // Sudut rotasi (dulu digunakan untuk miring)

    // --- Variabel Logika "Move & Pause" ---
    private long lastMoveActionTime; // Timer untuk melacak kapan terakhir bergerak/berhenti
    private long moveDuration = 3000; // Durasi bergerak (3 detik)
    private long pauseDuration = 2000; // Durasi berhenti (2 detik)
    private boolean isPaused = true; // Flag untuk status berhenti

    // --- Variabel Serangan Peluru Normal ---
    public long lastShotTime;
    public long shotCooldown = 1300; // Jeda antar tembakan peluru

    // --- Variabel Serangan Laser ---
    public long lastLaserTime; // Timer untuk cooldown laser
    private long laserCooldown = 7000; // Jeda antar serangan laser (7 detik)
    private long laserChargeDuration = 2000; // Durasi pengisian (tidak terpakai)
    private long currentLaserChargeStartTime = 0; // Timer pengisian (tidak terpakai)
    private boolean isChargingLaser = false; // Flag pengisian (tidak terpakai)

    // Variabel Indikator Laser (Peringatan sebelum laser)
    private long laserIndicatorStartTime = 0;
    private final long LASER_INDICATOR_DURATION = 1500; // Durasi peringatan (1.5 detik)
    private boolean isLaserIndicatorActive = false; // Flag peringatan

    // --- Variabel Serangan Minion ---
    public long lastMinionSpawnTime;
    private long minionSpawnCooldown = 10000; // Jeda antar spawn minion (10 detik)
    public final int MAX_MINIONS = 3; // Jumlah minion maks di layar

    // --- Variabel Serangan Dash ---
    public long lastDashTime;
    private long dashCooldown = 8000; // Jeda antar dash (8 detik)
    private boolean isDashing = false; // Flag sedang dash
    private double dashStartX, dashStartY, dashEndX, dashEndY; // Koordinat awal & akhir dash
    private long dashStartTime; // Timer durasi dash
    private final long DASH_DURATION = 500; // Durasi dash (0.5 detik)
    private double originalMoveSpeed; // Menyimpan kecepatan gerak asli

    private Random random = new Random();
    
    // Melacak ambang batas HP berikutnya untuk menjatuhkan power-up. Mulai dari 80%.
    private int nextPowerUpThresholdPercent = 80;
    
    // --- Variabel Invincibility (Kebal) ---
    private boolean isInvincibleAfterDash = false; // Flag kebal setelah dash
    private long dashInvincibilityStartTime = 0; // Timer durasi kebal
    private final long DASH_INVINCIBILITY_DURATION = 1000; // Durasi kebal (1 detik)

    /**
     * Konstruktor untuk FinalBoss.
     * Menginisialisasi semua nilai default, memuat gambar, dan mengatur posisi awal.
     */
    public FinalBoss(double x, double y, int health, int screenWidth, GamePanel gamePanel) { 
        super(x, y); 

        // Inisialisasi status dasar dari kelas Enemy
        this.speed = 2; // Kecepatan masuk awal
        this.damage = 10; // Damage peluru normal
        this.health = health; 
        this.maxHealth = health; 

        this.gamePanel = gamePanel; // Menyimpan referensi ke GamePanel

        // Memuat gambar sprite bos
        try {
            bossImage = ImageIO.read(getClass().getResource("/res/boss.png"));
            if (bossImage != null) {
                this.width = bossImage.getWidth();  
                this.height = bossImage.getHeight(); 
            } else {
                System.err.println("Boss image is null after loading. Using default size.");
                this.width = 200; 
                this.height = 150;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load boss image. Using fallback color and default size.");
            this.width = 200; 
            this.height = 150;
        }

        // Memuat gambar untuk indikator laser
        loadWarningLaserImage();
        
        // Atur posisi awal di atas layar, di tengah
        this.y = -this.height; 
        this.x = screenWidth / 2.0 - this.width / 2.0; 

        // Atur posisi target (tempat bos akan berhenti)
        this.targetX = this.x; 
        this.targetY = 100; 

        // Inisialisasi semua timer cooldown
        this.lastShotTime = System.currentTimeMillis();
        this.lastLaserTime = System.currentTimeMillis();
        this.lastMinionSpawnTime = System.currentTimeMillis();
        this.lastDashTime = System.currentTimeMillis();

        this.originalMoveSpeed = this.moveSpeed; // Simpan kecepatan gerak horizontal
        
        this.lastMoveActionTime = System.currentTimeMillis(); // Mulai timer move/pause
    }
    
    /**
     * Memuat gambar yang digunakan untuk peringatan laser (laserRed07.png).
     */
    private void loadWarningLaserImage() {
        try {
            warningLaserImage = ImageIO.read(getClass().getResource("/res/laserRed07.png"));
        } catch (Exception e) {
            System.err.println("Gagal memuat warning laser image /res/laserRed07.png");
        }
    }
    
    /**
     * Metode utama yang mengontrol semua logika gerakan dan AI bos.
     * Dijalankan 60 kali per detik dari GamePanel.update().
     */
    @Override
    public void move() {
        long now = System.currentTimeMillis();

        // BLOK 1: Logika jika sedang kebal (setelah dash selesai)
        if (isInvincibleAfterDash && (now - dashInvincibilityStartTime > DASH_INVINCIBILITY_DURATION)) {
            isInvincibleAfterDash = false;
            // Set target kembali ke posisi default (atas-tengah)
            targetX = gamePanel.SCREEN_WIDTH / 2.0 - this.width / 2.0;
            targetY = 100;
        }

        // BLOK 2: Logika untuk kembali ke posisi Y target (setelah dash/kebal)
        // Ini HANYA menggerakkan sumbu Y untuk menghindari konflik/getaran
        if (!isInvincibleAfterDash && !isDashing && (Math.abs(y - targetY) > 1)) {
            // Bergerak 5% lebih dekat ke target Y setiap frame
            y += (targetY - y) * 0.05;
            if (Math.abs(y - targetY) < 1) y = targetY; // Kunci di posisi Y

        // BLOK 3: Logika saat pertama kali masuk ke layar
        } else if (y < targetY && !isDashing && !isChargingLaser && !isLaserIndicatorActive) {
            y += moveSpeed * 2; // Bergerak turun
            if (y >= targetY) {
                y = targetY; // Kunci di posisi Y
            }

        // BLOK 4: Logika utama AI (jika sudah di posisi dan tidak melakukan aksi lain)
        } else {
            
            // Cek untuk memulai DASH (jika tidak sedang laser/dash, cooldown selesai, dan HP < 70%)
            if (!isDashing && !isChargingLaser && !isLaserIndicatorActive && (now - lastDashTime > dashCooldown) && random.nextInt(10) < 3 && getHealthPercentage() < 0.7) {
                startDash();
            }

            // Jika sedang DASH, jalankan logika dash
            if (isDashing) {
                performDash(now);
                
            // Jika TIDAK DASH dan TIDAK PULIH DARI DASH...
            } else if (!isInvincibleAfterDash) { 
                
                // Bos HANYA bergerak ke kiri/kanan jika TIDAK sedang menyiapkan laser
                if (!isLaserIndicatorActive && !isChargingLaser) {
                
                    // --- LOGIKA "MOVE & PAUSE" ---
                    if (isPaused) {
                        // Bos sedang diam
                        if (now - lastMoveActionTime > pauseDuration) {
                            isPaused = false;
                            lastMoveActionTime = now;
                            // Mulai gerak ke arah acak
                            this.moveSpeed = (random.nextBoolean() ? originalMoveSpeed : -originalMoveSpeed);
                        } else {
                            // Selama diam, pelan-pelan kembali ke tengah
                            double centerTargetX = gamePanel.SCREEN_WIDTH / 2.0 - this.width / 2.0;
                            x += (centerTargetX - x) * 0.02; 
                            if (Math.abs(x - centerTargetX) < 1) x = centerTargetX;
                        }
                    } else {
                        // Bos sedang bergerak
                        if (now - lastMoveActionTime > moveDuration) {
                            isPaused = true;
                            lastMoveActionTime = now;
                            this.moveSpeed = 0; // Berhenti
                        } else {
                            // Lanjutkan gerakan
                            x += moveSpeed;
                            if (x < 0 || x > gamePanel.SCREEN_WIDTH - width) {
                                moveSpeed *= -1; // Balik arah di tepi layar
                            }
                        }
                    }
                    // --- AKHIR LOGIKA "MOVE & PAUSE" ---
                }

                // Cek untuk memulai LASER (jika cooldown selesai dan HP < 90%)
                if (!isChargingLaser && !isLaserIndicatorActive && (now - lastLaserTime > laserCooldown) && getHealthPercentage() < 0.9) {
                    startLaserCharge(); 
                }

                // Cek jika sedang LASER
                if (isLaserIndicatorActive) {
                    performLaserIndicator(now);
                } 
                
                // Cek untuk MINION (jika cooldown selesai dan HP < 80%)
                if ((now - lastMinionSpawnTime > minionSpawnCooldown) && gamePanel.bossMinions.size() < MAX_MINIONS && getHealthPercentage() < 0.8) {
                    spawnMinion();
                    lastMinionSpawnTime = now;
                }
            }
        }
    }

    /**
     * Memulai aksi Dash. Mengatur flag dan koordinat awal/akhir.
     */
    private void startDash() {
        gamePanel.playSound("boss_dash"); // Mainkan suara dash
        isDashing = true;
        dashStartTime = System.currentTimeMillis();
        dashStartX = x;
        dashStartY = y;
        dashEndX = random.nextDouble() * (gamePanel.SCREEN_WIDTH - width); // Target X acak
        dashEndY = gamePanel.SCREEN_HEIGHT - height - gamePanel.player.height - 70; // Target Y di area player
        this.moveSpeed = 0; // Berhenti gerak horizontal
    }

    /**
     * Melakukan gerakan Dash dari titik awal ke akhir selama DASH_DURATION.
     */
    private void performDash(long now) {
        long elapsedTime = now - dashStartTime;
        if (elapsedTime < DASH_DURATION) {
            // Interpolasi linear sederhana untuk gerakan
            double progress = (double) elapsedTime / DASH_DURATION;
            x = dashStartX + (dashEndX - dashStartX) * progress;
            y = dashStartY + (dashEndY - dashStartY) * progress;
        } else {
            // Dash selesai
            isDashing = false;
            lastDashTime = now;
            this.moveSpeed = originalMoveSpeed; 
            
            // Mulai kebal
            isInvincibleAfterDash = true;
            dashInvincibilityStartTime = now;
        }
    }

    /**
     * Memulai sekuens serangan laser (memulai indikator).
     */
    private void startLaserCharge() {
        gamePanel.playSound("laser_charge"); // Mainkan suara charge
        isLaserIndicatorActive = true;
        laserIndicatorStartTime = System.currentTimeMillis();
        gamePanel.clearLaserBeams(); // Hapus laser lama jika ada
    }

    /**
     * Mengelola durasi indikator laser. Saat selesai, tembakkan laser.
     */
    private void performLaserIndicator(long now) {
        if (now - laserIndicatorStartTime > LASER_INDICATOR_DURATION) {
            isLaserIndicatorActive = false;

            gamePanel.stopSound("laser_charge"); // Hentikan suara charge
            gamePanel.playSound("laser_fire");   // Mainkan suara tembakan

            // Tentukan posisi dan ukuran laser
            double laserX = x + width / 2.0 - 10; // Tengah boss
            double laserY = y + height;
            double laserWidth = 20; // Lebar laser
            double laserHeight = gamePanel.SCREEN_HEIGHT - laserY; // Panjang sampai bawah

            // Panggil GamePanel untuk membuat objek LaserBeam
            gamePanel.spawnLaserBeam(laserX, laserY, laserWidth, laserHeight);

            lastLaserTime = now; // Reset cooldown laser
        }
    }

    /**
     * Metode ini tidak terpakai (orphaned) karena logika laser diubah
     * untuk langsung menembak setelah indikator selesai.
     */
    private void performLaserCharge(long now) {
        if (now - currentLaserChargeStartTime > laserChargeDuration) {
            double laserX = x + width / 2.0 - 10; 
            double laserY = y + height;
            double laserWidth = 20;
            double laserHeight = gamePanel.SCREEN_HEIGHT - laserY;

            gamePanel.spawnLaserBeam(laserX, laserY, laserWidth, laserHeight);
            isChargingLaser = false; 
            lastLaserTime = now; 
            this.moveSpeed = originalMoveSpeed;
        }
    }

    /**
     * Memanggil GamePanel untuk membuat minion baru (BossClone) di atas layar.
     */
    private void spawnMinion() {
        double minionX = random.nextDouble() * (gamePanel.SCREEN_WIDTH - 50); 
        double minionY = -100.0; // 100 pixel di atas layar
        gamePanel.spawnMinionEnemy(minionX, minionY);
    }

    /**
     * Menentukan apakah bos harus menembakkan peluru normal.
     * Tidak akan menembak jika sedang melakukan aksi lain (dash, laser).
     */
    @Override
    public boolean shouldShoot() {
        if (isChargingLaser || isLaserIndicatorActive || isDashing || isInvincibleAfterDash) return false; 
        long now = System.currentTimeMillis();
        if (now - lastShotTime > shotCooldown) {
            lastShotTime = now;
            return true;
        }
        return false;
    }
    
    /**
     * Mengembalikan nilai damage untuk peluru normal.
     */
    @Override
    public int getBulletDamage() {
        return this.damage; 
    }

    /**
     * Mengambil damage.
     * DIUBAH: Bos sekarang menjatuhkan 1 power-up setiap kali HP-nya
     * berkurang melewati ambang 80%, 60%, 40%, dan 20%.
     */
    @Override
    public void takeDamage(int damage) {
        if (isInvincibleAfterDash) return; // Kebal saat pulih dari dash

        // 1. Simpan persentase HP saat ini SEBELUM mengambil damage
        double oldHealthPercentage = getHealthPercentage();

        // 2. Terapkan damage
        super.takeDamage(damage); 

        // 3. Dapatkan persentase HP baru SETELAH damage
        double newHealthPercentage = getHealthPercentage();

        // 4. Cek apakah HP baru telah melewati ambang batas
        //    (Kita cek 'oldHealthPercentage > ...' agar ini hanya terjadi sekali)
        if (oldHealthPercentage > (nextPowerUpThresholdPercent / 100.0) && 
            newHealthPercentage <= (nextPowerUpThresholdPercent / 100.0)) 
        {
            // Ya, ambang batas terlewati. Jatuhkan 1 power-up acak
            
            // Tentukan tipe acak
            PowerUpType type = PowerUpType.values()[random.nextInt(PowerUpType.values().length)];
            
            // Tentukan lokasi (tengah bos)
            double spawnX = this.x + (this.width / 2.0);
            double spawnY = this.y + (this.height / 2.0);
            
            // Tambahkan ke game
            gamePanel.powerUps.add(new PowerUp(spawnX, spawnY, type));
            
            // Siapkan ambang batas berikutnya (80 -> 60 -> 40 -> 20)
            nextPowerUpThresholdPercent -= 20;
        }
    }

    /**
     * Menggambar sprite bos, health bar, dan efek visual (kebal, indikator laser).
     */
    @Override
    public void draw(Graphics g) {
        if (bossImage != null) {
            Graphics2D g2d = (Graphics2D) g.create(); 

            // Memastikan bos tidak miring (perbaikan bug "teleng")
            rotationAngle = 0; 

            // Atur transformasi untuk menggambar (diabaikan karena rotasi 0)
            AffineTransform at = new AffineTransform();
            at.translate(x + width / 2, y + height / 2); 
            at.rotate(rotationAngle); 
            at.translate(-width / 2, -height / 2); 

            // Efek berkedip saat kebal
            if (isInvincibleAfterDash) {
                if (System.currentTimeMillis() / 150 % 2 == 0) {
                    g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.4f)); 
                } else {
                    g2d.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.8f)); 
                }
            }
            g2d.drawImage(bossImage, at, null);
            g2d.dispose(); 
        } else {
            // Fallback jika gambar gagal dimuat
            g.setColor(Color.RED);
            g.fillRect((int) x, (int) y, width, height);
        }

        // Gambar health bar di atas bos
        drawHealthBar(g);

        // --- Visualisasi Indikator Serangan Laser ---
        if (isLaserIndicatorActive) {
            Graphics2D g2d_laser = (Graphics2D) g.create();
            
            int laserX = (int) x + width / 2 - 10;
            int laserY = (int) y + height;
            int laserW = 20;
            int laserH = gamePanel.SCREEN_HEIGHT - laserY;

            // Efek berkedip transparan
            float alpha = (float) Math.abs(Math.sin(System.currentTimeMillis() * 0.01)) * 0.5f;
            g2d_laser.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            if (warningLaserImage != null) {
                // Gambar aset laser (merah)
                g2d_laser.drawImage(warningLaserImage, laserX, laserY, laserW, laserH, null);
                // Timpa dengan filter kuning untuk peringatan
                g2d_laser.setColor(new Color(255, 255, 0, 150)); 
                g2d_laser.fillRect(laserX, laserY, laserW, laserH);
            } else {
                g2d_laser.setColor(new Color(255, 255, 0, 100)); // Fallback
                g2d_laser.fillRect(laserX, laserY, laserW, laserH);
            }
            
            g2d_laser.dispose();
            
            // Animasi kotak target di bawah bos
            long now = System.currentTimeMillis();
            if (now / 200 % 2 == 0) { 
                g.setColor(Color.YELLOW);
                g.drawRect((int) x + width / 2 - 20, (int) y + height - 10, 40, 5);
            }
        }

        // Visualisasi teks "INVINCIBLE"
        if (isInvincibleAfterDash) {
            g.setColor(new Color(100, 100, 255, 120)); // Overlay biru
            g.fillRect((int)x, (int)y, width, height);
            
            long now = System.currentTimeMillis();
            if (now / 200 % 2 == 0) { // Berkedip teks
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 20)); // Gunakan font kustom jika mau
                g.drawString("INVINCIBLE", (int)x + width/2 - 50, (int)y + height/2);
            }
        }
    }

    /**
     * Menggambar health bar di atas kepala bos.
     */
    private void drawHealthBar(Graphics g) {
        int barWidth = width;
        int barHeight = 10;
        int barX = (int) x;
        int barY = (int) y - barHeight - 5; // 5 piksel di atas bos

        g.setColor(Color.RED);
        g.fillRect(barX, barY, barWidth, barHeight); 

        g.setColor(Color.GREEN);
        g.fillRect(barX, barY, (int) (barWidth * getHealthPercentage()), barHeight); 

        g.setColor(Color.WHITE);
        g.drawRect(barX, barY, barWidth, barHeight); 
    }

    /**
     * Menghitung persentase HP (0.0 - 1.0).
     */
    public double getHealthPercentage() {
        if (maxHealth == 0) return 0;
        return (double) health / maxHealth;
    }
    
    /**
     * Getter untuk status kebal (digunakan oleh GamePanel).
     */
    public boolean isInvincibleAfterDash() {
        return isInvincibleAfterDash;
    }
    
    @Override
    public int getScoreValue() {
        return 5000; // Bonus besar saat mengalahkan bos
    }
}