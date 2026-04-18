package com.mycompany.spaceshootertes; 

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics;

public class EnemyFighter extends Enemy {

    // Konstruktor ini harus cocok dengan `Enemy.java`
    public EnemyFighter(double startX, double startY) {
        super(startX, startY);
        
        this.speed = 1.0; // Kecepatan lambat
        this.health = 2;
        this.maxHealth = 2; // Pastikan maxHealth di-set
        this.damage = 2; // Damage peluru
        this.shotCooldown = 2000;
        this.lastShotTime = System.currentTimeMillis() + random.nextInt(2000);

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
            enemyImage = ImageIO.read(getClass().getResource("/res/enemyFighter.png"));
        } catch (Exception e) {
            System.err.println("Gagal memuat enemyFighter.png. Pakai kotak orange.");
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
            g.setColor(Color.ORANGE);
            g.fillRect((int)x, (int)y, width, height);
        }
        super.drawGun(g);
    }
    
    @Override
    public int getBulletDamage() {
        return this.damage;
    }
    
    @Override
    public int getScoreValue() {
        return 20; // HP 2, skor 20
    }
}