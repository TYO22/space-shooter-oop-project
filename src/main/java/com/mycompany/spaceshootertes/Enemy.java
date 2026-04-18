package com.mycompany.spaceshootertes; 

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.imageio.ImageIO;

public abstract class Enemy {

    public double x, y;
    public double speed;
    public int width, height;
    
    // --- DIUBAH: Variabel ini HARUS ada di kelas induk ---
    public int health;
    public int maxHealth; 
    public int damage;
    
    // --------------------------------------------------
    
    public BufferedImage enemyImage;
    protected BufferedImage gunImage;
    
    protected long lastShotTime;
    protected long shotCooldown;
    protected Random random = new Random();

    public Enemy(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        // Inisialisasi default
        this.health = 1; 
        this.maxHealth = 1; 
        this.damage = 1; 
        this.speed = 1.0;
        this.width = 50;
        this.height = 50;
        this.shotCooldown = 9999999L; // Default tidak menembak
        this.lastShotTime = System.currentTimeMillis();
    }
    
    protected void loadGunImage(String path) {
        try {
            this.gunImage = ImageIO.read(getClass().getResource(path));
        } catch (Exception e) {
            System.err.println("Gagal memuat gambar senjata musuh: " + path);
        }
    }
    
    protected void drawGun(Graphics g) {
        if (gunImage != null) {
            double gunX = x + (width / 2.0) - (gunImage.getWidth() / 2.0);
            double gunY = y;
            g.drawImage(gunImage, (int)gunX, (int)gunY, null);
        }
    }
    
    public void takeDamage(int damage) {
        this.health -= damage;
    }

    public boolean isDead() {
        return this.health <= 0;
    }

    public boolean shouldShoot() {
        long now = System.currentTimeMillis();
        if (now - lastShotTime > shotCooldown) {
            lastShotTime = now;
            return true;
        }
        return false;
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }
    
    public int getDamage() { 
        return damage;
    }

    public abstract void move();
    public abstract void draw(Graphics g);
    public abstract int getBulletDamage();
    public abstract int getScoreValue();
}