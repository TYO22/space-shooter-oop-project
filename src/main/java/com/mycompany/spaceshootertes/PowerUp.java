package com.mycompany.spaceshootertes; 

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.awt.Color; 

public class PowerUp {

    
    // DIUBAH: Enum ini harus sinkron dengan GamePanel
    public enum PowerUpType {
        HEALTH,
        SHIELD,
        WEAPON,
        FIRE_RATE,
        BULLET_SPEED,
        DRONE,
        AMMO
    }

    public double x, y;
    private int width, height;
    private double speed = 2.0; 
    private PowerUpType type;
    private BufferedImage image;

    // PASTIKAN KONSTRUKTOR INI PUBLIC
    public PowerUp(double x, double y, PowerUpType type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.width = 30; // Ukuran default
        this.height = 30; // Ukuran default

        try {
            String imagePath = "";
            switch (type) {
                case HEALTH:
                    imagePath = "/res/powerup_health.png";
                    break;
                case SHIELD:
                    imagePath = "/res/powerup_shield.png";
                    break;
                case WEAPON:
                    imagePath = "/res/powerup_weapon.png";
                    break;
                case FIRE_RATE:
                    imagePath = "/res/powerup_firerate.png";
                    break;
                case BULLET_SPEED:
                    imagePath = "/res/powerup_bulletspeed.png";
                    break;
                case DRONE:
                    imagePath = "/res/powerup_drone.png";
                    break;
                case AMMO:
                    imagePath = "/res/powerup_ammo.png"; 
                    break;    
            }
            image = ImageIO.read(getClass().getResource(imagePath));
            if (image != null) {
                this.width = image.getWidth(); 
                this.height = image.getHeight();
            } else {
                 System.err.println("PowerUp image " + imagePath + " is null. Using default size.");
            }
        } catch (Exception e) {
            System.err.println("Failed to load PowerUp image for " + type + ". Using fallback color.");
        }
    }

    public void move() {
        y += speed;
    }

    public void draw(Graphics g) {
        if (image != null) {
            g.drawImage(image, (int) x, (int) y, width, height, null);
        } else {
            g.setColor(Color.MAGENTA); 
            g.fillRect((int) x, (int) y, width, height);
        }
    }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }
    
    // Getter untuk tipe
    public PowerUpType getType() {
        return type;
    }
}