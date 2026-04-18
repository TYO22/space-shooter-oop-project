package com.mycompany.spaceshootertes; 

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.AlphaComposite; 
import java.awt.Graphics2D;

public class BossMinion extends Enemy {
    private double targetY;
    private boolean isArriving;
    
    public BossMinion(double startX, double startY) {
        super(startX, startY);
        
        this.speed = 1.5;
        this.health = 10;
        this.maxHealth = 10; // Pastikan maxHealth juga diatur
        this.shotCooldown = 1500;
        this.lastShotTime = System.currentTimeMillis() + random.nextInt(1500);

        this.isArriving = true;
        this.targetY = 150 + random.nextInt(100); 

        loadEnemyImage();
        loadGunImage("/res/gun_lvl0.png"); 

        if (enemyImage != null) {
            this.width = enemyImage.getWidth();
            this.height = enemyImage.getHeight();
        } else {
            this.width = 50; 
            this.height = 50;
        }
    }

    public void loadEnemyImage() {
        try {
            enemyImage = ImageIO.read(getClass().getResource("/res/minion_boss.png")); 
        } catch (Exception e) {
            System.err.println("Gagal memuat minion_boss.png untuk minion. Pakai kotak abu-abu.");
        }
    }

    @Override
    public void move() {
        if (isArriving) {
            y += speed * 3.0; 
            if (y >= targetY) {
                y = targetY;
                isArriving = false; 
            }
        } else {
            y += speed;
        }
    }

    @Override
    public void draw(Graphics g) {
              
        if (enemyImage != null) {
            g.drawImage(enemyImage, (int)x, (int)y, width, height, null);
        } else {
            g.setColor(Color.DARK_GRAY);
            g.fillRect((int)x, (int)y, width, height);
        }
        
        // Gambar senjata (tidak transparan)
        super.drawGun(g);
    }
    
    @Override
    public int getBulletDamage() {
        return 2;
    }
    
    @Override
    public void takeDamage(int damage) {
        // Minion kebal saat baru masuk (isArriving == true)
        if (isArriving) return; 
        super.takeDamage(damage);
    }

    @Override
    public boolean shouldShoot() {
        // Minion tidak bisa menembak saat baru masuk
        if (isArriving) return false; 

        long now = System.currentTimeMillis();
        if (now - lastShotTime > shotCooldown) {
            lastShotTime = now;
            return true;
        }
        return false;
    }
    
    @Override
    public int getScoreValue() {
        return 75; // HP 10, tapi lebih sulit. Skor 75
    }
}