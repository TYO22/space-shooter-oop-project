package com.mycompany.spaceshootertes; 

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class Bullet {

    public double x, y;
    public int width, height;
    public int damage;
    public BufferedImage bulletImage;

    private double dx; 
    private double dy; 
    
    public int penetration; // Daya tembus

    // Konstruktor utama
    public Bullet(double startX, double startY, double angle, double speed, int penetration) {
        this.x = startX;
        this.y = startY;
        this.damage = 1;
        this.penetration = penetration;
        
        this.dx = Math.sin(Math.toRadians(angle)) * speed;
        this.dy = -Math.cos(Math.toRadians(angle)) * speed;
        
        loadBulletImage();
    }
    
    // Konstruktor lama (untuk peluru non-piercing)
    public Bullet(double startX, double startY, double angle, double speed) {
        this(startX, startY, angle, speed, 1); 
    }
    
    public void loadBulletImage() {
        try {
            if (this.penetration > 1) {
                bulletImage = ImageIO.read(getClass().getResource("/res/laserRed07.png"));
            } else {
                bulletImage = ImageIO.read(getClass().getResource("/res/playerBullet.png"));
            }
            this.width = bulletImage.getWidth(); 
            this.height = bulletImage.getHeight();
        } catch (Exception e) {
            System.err.println("Gagal memuat gambar peluru.");
            this.width = 5; 
            this.height = 10;
        }
    }

    public void move() {
        x += dx;
        y += dy;
    }

    public void draw(Graphics g) {
        if (bulletImage != null) {
            g.drawImage(bulletImage, (int)x, (int)y, width, height, null);
        } else {
            g.setColor(Color.YELLOW);
            g.fillRect((int)x, (int)y, width, height);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }
    
    public void hitEnemy() {
        this.penetration--;
    }
    
    public boolean isDead() {
        return this.penetration <= 0;
    }
}