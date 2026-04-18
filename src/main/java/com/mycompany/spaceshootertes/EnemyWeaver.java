package com.mycompany.spaceshootertes; 

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;

public class EnemyWeaver extends Enemy {

    private double originalX;
    private double waveAngle;

    public EnemyWeaver(double startX, double startY) {
        super(startX, startY);
        
        this.originalX = startX;
        this.waveAngle = 0;
        
        this.speed = 1.0;
        this.health = 5;
        this.shotCooldown = 1800;
        this.lastShotTime = System.currentTimeMillis() + random.nextInt(1800);

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
            enemyImage = ImageIO.read(getClass().getResource("/res/enemyWeaver.png"));
        } catch (Exception e) {
            System.err.println("Gagal memuat /res/enemyWeaver.png. Pakai kotak cyan.");
        }
    }

    @Override
    public void move() {
        y += speed; 
        waveAngle += 0.05;
        x = originalX + Math.sin(waveAngle) * 100;
    }

    @Override
    public void draw(Graphics g) {
        if (enemyImage != null) {
            g.drawImage(enemyImage, (int)x, (int)y, width, height, null);
        } else {
            g.setColor(Color.CYAN);
            g.fillRect((int)x, (int)y, width, height);
        }
        super.drawGun(g);
    }
    
    @Override
    public int getBulletDamage() {
        return 2;
    }
    
    @Override
    public int getScoreValue() {
        return 50; // HP 5, skor 50
    }
}