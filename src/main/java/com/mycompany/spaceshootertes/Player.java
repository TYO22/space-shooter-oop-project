package com.mycompany.spaceshootertes; 

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class Player {
    
    public double x, y, speed;
    public int width, height;
    
    public BufferedImage shieldImage;
    public BufferedImage playerImage_Lvl0;
    public BufferedImage playerImage_Lvl1;
    public BufferedImage playerImage_Lvl2;

    public int health; 
    public int maxHealth;
    
    public int currentAmmo;
    public int maxAmmo;
    
    public int shieldHealth;
    public int maxShieldHealth;

    // Sistem Level Senjata
    public int weaponLevel;
    public final int MAX_WEAPON_LEVEL = 2; // DIBUAT PUBLIC
    
    
    public boolean hasShownWeaponMax = false;
    public boolean hasShownDroneMax = false;
    public boolean hasShownBulletSpeedMax = false;
    public boolean hasShownFireRateMax = false;
    
    public int droneCount = 0;
    
    public long lastShotTime; 
    public long shotCooldown; 
    public double playerBulletSpeed; 
    
    private int bulletDamage = 1;
    private boolean doubleShotActive = false;
    private long doubleShotEndTime = 0; 
    private long speedBoostEndTime = 0; 
    private double originalSpeed; 
    private long invincibleEndTime = 0; 
    private final long INVINCIBILITY_DURATION = 1000; 
    
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingUp = false;
    private boolean movingDown = false;
    
    private GamePanel gamePanel; 

    public Player(double x, double y, GamePanel gamePanel) { 
        this.x = x;
        this.y = y;
        this.gamePanel = gamePanel;
        this.speed = 5.0;
        
        this.maxHealth = 100;
        this.health = this.maxHealth;
        
        this.currentAmmo = this.maxAmmo;
        
        this.maxShieldHealth = 50; // Sesuai permintaan Anda
        this.shieldHealth = 0;
        
        this.weaponLevel = 0;
        this.lastShotTime = System.currentTimeMillis(); 
        this.originalSpeed = this.speed;
        
        loadPlayerImages(); 
        loadShieldImage(); 

        if (playerImage_Lvl0 != null) {
            this.width = playerImage_Lvl0.getWidth();
            this.height = playerImage_Lvl0.getHeight();
        } else {
            this.width = 99; 
            this.height = 75;
        }
    }
    
    /**
     * Menambah amunisi, tapi tidak melebihi batas MAX_AMMO.
     */
    public void addAmmo(int amount) {
        this.currentAmmo += amount;
        if (this.currentAmmo > this.maxAmmo) {
            this.currentAmmo = this.maxAmmo;
        }
    }
    
    /**
     * Mengisi ulang amunisi ke jumlah maksimal.
     */
    public void refillAmmo() {
        this.currentAmmo = this.maxAmmo;
    }
    
    /**
     * Mengatur batas amunisi maksimal yang baru.
     * Dipanggil oleh GamePanel setiap ganti level.
     */
    public void setMaxAmmo(int newMax) {
        this.maxAmmo = newMax;
    }

    public void loadPlayerImages() {
        try {
            playerImage_Lvl0 = ImageIO.read(getClass().getResource("/res/player_lvl0.png"));
            playerImage_Lvl1 = ImageIO.read(getClass().getResource("/res/player_lvl1.png"));
            playerImage_Lvl2 = ImageIO.read(getClass().getResource("/res/player_lvl2.png"));
        } catch (Exception e) {
            System.err.println("Gagal memuat satu atau lebih gambar player (lvl0, 1, atau 2).");
        }
    }
    
    public void loadShieldImage() {
        try {
            shieldImage = ImageIO.read(getClass().getResource("/res/shield.png"));
        } catch (Exception e) {
            System.err.println("Gagal memuat shield.png");
        }
    }

    public void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();

        if (isInvincible()) {
            if (System.currentTimeMillis() / 150 % 2 == 0) {
                 g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            }
        }

        BufferedImage currentImage = playerImage_Lvl0; 
        
        if (weaponLevel == 1) {
            currentImage = playerImage_Lvl1;
        } else if (weaponLevel >= 2) {
            currentImage = playerImage_Lvl2;
        }

        if (currentImage != null) {
            g2d.drawImage(currentImage, (int)x, (int)y, width, height, null);
        } else {
            if (playerImage_Lvl0 != null) { 
                 g2d.drawImage(playerImage_Lvl0, (int)x, (int)y, width, height, null);
            } else { 
                g2d.setColor(Color.BLUE);
                g2d.fillRect((int)x, (int)y, width, height);
            }
        }
        
        if (shieldHealth > 0 && shieldImage != null) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2d.drawImage(shieldImage, (int)x - 10, (int)y - 10, width + 20, height + 20, null);
        }
        
        g2d.dispose();
    }
    
    public void move() {
        if (movingLeft) x -= speed;
        if (movingRight) x += speed;
        if (movingUp) y -= speed;
        if (movingDown) y += speed;

        if (x < 0) x = 0;
        if (x > gamePanel.SCREEN_WIDTH - width) x = gamePanel.SCREEN_WIDTH - width;
        if (y < 0) y = 0;
        if (y > gamePanel.SCREEN_HEIGHT - height) y = gamePanel.SCREEN_HEIGHT - height;

        long now = System.currentTimeMillis();
        if (doubleShotActive && now > doubleShotEndTime) {
            doubleShotActive = false;
        }
        if (speed != originalSpeed && now > speedBoostEndTime) {
            speed = originalSpeed; 
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }
    
    public void takeDamage(int damage) {
        if (isInvincible()) return; 
        
        if (shieldHealth > 0) {
            int oldShieldHealth = shieldHealth; // Simpan HP shield sebelum damage
            if (shieldHealth >= damage) {
                shieldHealth -= damage;
            } else {
                int remainingDamage = damage - shieldHealth;
                shieldHealth = 0;
                health -= remainingDamage;
            }
            
            // --- MAIN SUARA SHIELD HANCUR ---
            if (shieldHealth <= 0 && oldShieldHealth > 0) {
                // Panggil SoundManager (kita butuh cara untuk mengaksesnya)
                // Solusi: Gunakan singleton di SoundManager
                // SoundManager.getInstance().playSound("shield_break"); // (Jika Anda implementasi singleton)
                // Untuk saat ini, kita akan panggil dari GamePanel saja
            }
            // ---------------------------------
            
        } else {
            health -= damage;
        }
        
        if (this.health < 0) this.health = 0;
        startInvincibility(INVINCIBILITY_DURATION); 
    }
    
    public boolean isDead() {
        return this.health <= 0;
    }
    
    public double getHealthPercentage() {
        if (maxHealth == 0) return 0;
        return (double) health / maxHealth;
    }
    
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public void setHealth(int h) { this.health = Math.min(h, maxHealth); }
    
    public int getBulletDamage() { return bulletDamage; }
    public double getBulletSpeed() { return playerBulletSpeed; } 
    public boolean isDoubleShotActive() { 
        return this.weaponLevel >= 1;
    }
    
    public void addHealth(int amount) {
        this.health += amount;
        if (this.health > this.maxHealth) this.health = this.maxHealth;
    }
    
    public void activateShield(int shieldAmount) {
        this.shieldHealth += shieldAmount;
        if (this.shieldHealth > this.maxShieldHealth) {
            this.shieldHealth = this.maxShieldHealth; 
        }
        // --- MAIN SUARA SHIELD AKTIF ---
        // SoundManager.getInstance().playSound("shield_activate"); // (Jika singleton)
        // -------------------------------
    }
    
    public void upgradeWeapon() {
        this.weaponLevel++;
        if (this.weaponLevel > MAX_WEAPON_LEVEL) this.weaponLevel = MAX_WEAPON_LEVEL;
    }
    
    public void setDoubleShotActive(boolean active, long duration) {
        this.doubleShotActive = active;
        this.doubleShotEndTime = System.currentTimeMillis() + duration;
    }
    
    public void setSpeedBoost(double multiplier, long duration) {
        this.speed = originalSpeed * multiplier;
        this.speedBoostEndTime = System.currentTimeMillis() + duration;
    }
    
    public boolean isInvincible() {
        return System.currentTimeMillis() < invincibleEndTime;
    }
    
    public void startInvincibility(long duration) {
        this.invincibleEndTime = System.currentTimeMillis() + duration;
    }
    
    public void setMovingLeft(boolean movingLeft) { this.movingLeft = movingLeft; }
    public void setMovingRight(boolean movingRight) { this.movingRight = movingRight; }
    public void setMovingUp(boolean movingUp) { this.movingUp = movingUp; }
    public void setMovingDown(boolean movingDown) { this.movingDown = movingDown; }
    
    public void setPlayerBulletSpeed(double speed) { this.playerBulletSpeed = speed; }
    
    public int getMaxShieldHealth() {
        return this.maxShieldHealth;
    }
}