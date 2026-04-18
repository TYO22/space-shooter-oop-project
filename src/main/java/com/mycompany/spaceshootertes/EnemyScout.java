package com.mycompany.spaceshootertes; 

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;

public class EnemyScout extends Enemy {

    public EnemyScout(double startX, double startY) {
        super(startX, startY);
        
        this.speed = 1.0; 
        this.health = 1;
        this.shotCooldown = 999999999L; // Tidak menembak
        this.lastShotTime = System.currentTimeMillis() + random.nextInt(3000);

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
            enemyImage = ImageIO.read(getClass().getResource("/res/enemyShip.png"));
        } catch (Exception e) {
            System.err.println("Gagal memuat enemyShip.png");
        }
    }

    @Override
    public void move() {
        y += speed;
    }

    @Override
    public void draw(Graphics g) {
        if (enemyImage != null) {
            g.drawImage(enemyImage, (int)x, (int)y, width, height, null);
        } else {
            g.setColor(Color.RED);
            g.fillRect((int)x, (int)y, width, height);
        }
        super.drawGun(g);
    }
    
    @Override
    public int getBulletDamage() {
        return 1;
    }
    
    @Override
    public int getScoreValue() {
        return 10; // HP 1, skor 10
    }
}