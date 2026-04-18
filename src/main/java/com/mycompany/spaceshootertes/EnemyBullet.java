package com.mycompany.spaceshootertes; 

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

public class EnemyBullet {

    public double x, y;
    public int width, height;
    public int damage;
    public BufferedImage bulletImage;

    private double dx;
    private double dy;

    public EnemyBullet(double startX, double startY, int damage, double angle) {
        this.x = startX;
        this.y = startY;
        this.damage = damage;
        
        double speed = 7.0;
        
        this.dx = Math.sin(Math.toRadians(angle)) * speed;
        this.dy = -Math.cos(Math.toRadians(angle)) * speed;
        
        loadBulletImage();
    }
    
    public void loadBulletImage() {
         try {
            bulletImage = ImageIO.read(getClass().getResource("/res/enemyBullet.png"));
            this.width = bulletImage.getWidth();
            this.height = bulletImage.getHeight();
        } catch (Exception e) {
            System.err.println("Gagal memuat enemyBullet.png");
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
            g.setColor(Color.PINK);
            g.fillRect((int)x, (int)y, width, height);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, width, height);
    }
}