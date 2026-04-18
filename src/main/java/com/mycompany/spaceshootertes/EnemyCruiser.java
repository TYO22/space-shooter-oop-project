package com.mycompany.spaceshootertes; 

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;

public class EnemyCruiser extends Enemy {

    public EnemyCruiser(double startX, double startY) {
        super(startX, startY);
        
        this.speed = 1.0;
        this.health = 10;
        this.shotCooldown = 2500;
        this.lastShotTime = System.currentTimeMillis() + random.nextInt(2500);

        loadEnemyImage();
        loadGunImage("/res/gun_lvl0.png");

        if (enemyImage != null) {
            this.width = enemyImage.getWidth();
            this.height = enemyImage.getHeight();
        } else {
            this.width = 80;
            this.height = 80;
        }
    }

    public void loadEnemyImage() {
        try {
            enemyImage = ImageIO.read(getClass().getResource("/res/enemyCruiser.png"));
        } catch (Exception e) {
            System.err.println("Gagal memuat /res/enemyCruiser.png. Pakai kotak putih.");
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
            g.setColor(Color.WHITE);
            g.fillRect((int)x, (int)y, width, height);
        }
        super.drawGun(g);
    }
    
    @Override
    public int getBulletDamage() {
        return 3;
    }
    
    @Override
    public int getScoreValue() {
        return 100; // HP 10, skor 100
    }
}