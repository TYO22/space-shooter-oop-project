package com.mycompany.spaceshootertes; 

import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import javax.swing.event.AncestorListener;
import javax.swing.event.AncestorEvent;
import java.io.InputStream; 
import java.awt.Font; 
import java.awt.FontMetrics;
import com.mycompany.spaceshootertes.PowerUp.PowerUpType;

public class GamePanel extends JPanel implements Runnable, KeyListener {

    final int SCREEN_WIDTH = 800;
    final int SCREEN_HEIGHT = 600;
    final int FPS = 60;
    
    enum GameState { START_MENU, PLAYING, PAUSED, LEVEL_TRANSITION, GAME_OVER, GAME_WIN }
    GameState currentState;
    
    private SoundManager soundManager;
    private long infoMaxPowerUpMessageTimer = 0;
    private String infoMaxPowerUpMessage = "";
    private final long INFO_MAX_POWER_UP_MESSAGE_DURATION = 2000; 
    
    Thread gameThread;
    
    Player player;
    List<Bullet> bullets;
    List<Enemy> enemies;
    List<EnemyBullet> enemyBullets;
    List<PowerUp> powerUps;
    List<Enemy> bossMinions; 
    List<LaserBeam> laserBeams;
    List<Drone> drones;
    List<Explosion> explosions;
    List<Meteor> menuMeteors;
    List<BossExplosion> bossExplosions;
    List<PlayerExplosion> playerExplosions;
    
    final int MAX_DRONES = 2;
    
    BufferedImage bgImage;
    BufferedImage titleLogoImage;
    BufferedImage pauseFrameImage;
    BufferedImage meteorImage; // <-- TAMBAHKAN INI
     // <-- TAMBAHKAN INI
    
    int logoScaledWidth;
    int logoScaledHeight;
    
    Font mainFont;
    Font thinFont;
    
    private int startMenuSelection = 0; // DIUBAH DARI PERMINTAAN SEBELUMNYA
    private int pauseMenuSelection = 0; 
    private int endMenuSelection = 0;
    
    private boolean hasPlayedDeathSounds = false;
    private boolean hasPlayedWinJingle = false;
    private boolean hasPlayedLevelClearJingle = false;
    
    double logoY;
    double logoTargetY;
    boolean isLogoAnimating;
    
    long lastShotTime;
    long shotCooldown = 800; 
    final long MIN_SHOT_COOLDOWN = 370;
    final long DEFAULT_SHOT_COOLDOWN = 800; 
    
    double playerBulletSpeed = 6.0; 
    final double MAX_BULLET_SPEED = 14.0;
    final double DEFAULT_BULLET_SPEED = 6.0; 
    
    long lastEnemySpawnTime, enemySpawnCooldown = 1500;
    
    Random random = new Random();

    int score = 0;
    int currentLevel = 1;
    final int MAX_LEVEL = 5;
    
    int currentWave;
    int totalWavesInLevel;
    
    // +++ VARIABEL BARU (DARI PERMINTAAN SEBELUMNYA) +++
    int waveEnemiesTypeA; // Jumlah musuh Tipe A (misal: Fighter) untuk wave ini
    int waveEnemiesTypeB; // Jumlah musuh Tipe B (misal: Scout) untuk wave ini
    int spawnedEnemiesTypeA; // Counter berapa banyak Tipe A yang sudah spawn
    int spawnedEnemiesTypeB; // Counter berapa banyak Tipe B yang sudah spawn
    // ++++++++++++++++++++++++++++++++++++++++++++++++++
    
    boolean waitingForWave;
    long waveMessageTimer, waveMessageDuration = 3000;
    
    boolean bossSpawned = false;
    FinalBoss finalBoss = null; 
    
    long transitionStartTime, transitionDuration = 3000;

    public GamePanel() {
        
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(this);
        
        this.addAncestorListener(new AncestorListener() {
            @Override public void ancestorAdded(AncestorEvent event) { requestFocusInWindow(); }
            @Override public void ancestorRemoved(AncestorEvent event) {}
            @Override public void ancestorMoved(AncestorEvent event) {}
        });

        try {
            bgImage = ImageIO.read(getClass().getResource("/res/background.png"));
        } catch (Exception e) { 
            System.err.println("Gagal memuat /res/background.png");
            e.printStackTrace(); 
        }
        
        try {
            titleLogoImage = ImageIO.read(getClass().getResource("/res/judul.png")); 

            if (titleLogoImage != null) {
                int targetWidth = 600; 
                double aspectRatio = (double)titleLogoImage.getHeight() / titleLogoImage.getWidth();
                logoScaledWidth = targetWidth;
                logoScaledHeight = (int)(targetWidth * aspectRatio);
            } else {
                logoScaledWidth = 600;
                logoScaledHeight = 300;
                
            }

        } catch (Exception e) { 
            System.err.println("Gagal memuat /res/judul.png");
            e.printStackTrace(); 
        }

        loadFonts();
        loadPauseFrameImage();
        
        menuMeteors = new CopyOnWriteArrayList<>();
        try {
            // Ganti "meteor.png" jika nama file Anda berbeda
            meteorImage = ImageIO.read(getClass().getResource("/res/meteor.png")); 
            
            if (meteorImage != null) {
                
                for (int i = 0; i < 7; i++) {
                    menuMeteors.add(new Meteor(meteorImage, SCREEN_WIDTH, SCREEN_HEIGHT));
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat /res/meteor.png. Meteor tidak akan ditampilkan.");
        }
        
        soundManager = new SoundManager(); 
        soundManager.playSound("intro");
        
        currentState = GameState.START_MENU;
        player = new Player(SCREEN_WIDTH / 2.0 - 25, SCREEN_HEIGHT - 100, this); 
        
        logoY = SCREEN_HEIGHT; 
        logoTargetY = (SCREEN_HEIGHT / 2.0) - (logoScaledHeight / 2.0);
        isLogoAnimating = true; 
        
        bullets = new CopyOnWriteArrayList<>();
        enemies = new CopyOnWriteArrayList<>();
        enemyBullets = new CopyOnWriteArrayList<>();
        powerUps = new CopyOnWriteArrayList<>();
        bossMinions = new CopyOnWriteArrayList<>(); 
        laserBeams = new CopyOnWriteArrayList<>();
        drones = new CopyOnWriteArrayList<>();
        explosions = new CopyOnWriteArrayList<>();
        bossExplosions = new CopyOnWriteArrayList<>();
        playerExplosions = new CopyOnWriteArrayList<>();
        
        startGameThread();
    }
    
    // +++ METODE HELPER BARU (DARI PERMINTAAN SEBELUMNYA) +++
    /**
     * Mereset counter spawn setiap kali wave baru dimulai.
     */
    private void resetWaveSpawnCounters() {
        spawnedEnemiesTypeA = 0;
        spawnedEnemiesTypeB = 0;
    }
    // +++++++++++++++++++++++++++++++++++++++++++++++++++++
    
    private void loadFonts() {
        try {
            InputStream isMain = getClass().getResourceAsStream("/res/kenvector_future.ttf");
            mainFont = Font.createFont(Font.TRUETYPE_FONT, isMain).deriveFont(20f);
            InputStream isThin = getClass().getResourceAsStream("/res/kenvector_future_thin.ttf");
            thinFont = Font.createFont(Font.TRUETYPE_FONT, isThin).deriveFont(20f);
        } catch (Exception e) {
            System.err.println("Gagal memuat font kustom! Menggunakan font default.");
            e.printStackTrace();
            mainFont = new Font("Arial", Font.BOLD, 20);
            thinFont = new Font("Arial", Font.PLAIN, 20);
        }
    }
    
    private void loadPauseFrameImage() {
        try {
            pauseFrameImage = ImageIO.read(getClass().getResource("/res/pause_frame.png"));
        } catch (Exception e) {
            System.err.println("Gagal memuat /res/pause_frame.png");
            e.printStackTrace();
        }
    }

    private void resetMovementFlags() {
        player.setMovingLeft(false);
        player.setMovingRight(false);
        player.setMovingUp(false);
        player.setMovingDown(false);
    }
    
    // +++ METODE BARU (DARI PERMINTAAN SEBELUMNYA) +++
    private void selectStartMenuItem() {
        switch (startMenuSelection) {
            case 0: // PLAY
                restartGame(); 
                break;
            case 1: // QUIT
                System.exit(0); 
                break;
        }
    }
    // +++++++++++++++++++++++++++++++++++++++++++++++
    
    private void selectPauseMenuItem() {
        switch (pauseMenuSelection) {
            case 0: // RESUME
                currentState = GameState.PLAYING;
                soundManager.loopSound("backsound");
                break;
            case 1: // RESTART
                restartGame(); 
                break;
            case 2: // EXIT
                System.exit(0); 
                break;
        }
    }
    
    private void selectEndMenuItem() {
        switch (endMenuSelection) {
            case 0: // MAIN LAGI
                restartGame();
                break;
            case 1: // KELUAR
                System.exit(0);
                break;
        }
    }

    public void restartGame() {
        player = new Player(SCREEN_WIDTH / 2.0 - 25, SCREEN_HEIGHT - 100, this);
        resetMovementFlags();
        
        player.hasShownWeaponMax = false;
        player.hasShownDroneMax = false;
        player.hasShownBulletSpeedMax = false;
        player.hasShownFireRateMax = false;
        infoMaxPowerUpMessage = "";
        
        hasPlayedDeathSounds = false;
        hasPlayedWinJingle = false;
        hasPlayedLevelClearJingle = false;
        
        score = 0;
        
        shotCooldown = DEFAULT_SHOT_COOLDOWN;
        playerBulletSpeed = DEFAULT_BULLET_SPEED;
        
        bullets.clear();
        enemies.clear();
        enemyBullets.clear();
        powerUps.clear();
        bossMinions.clear(); 
        laserBeams.clear();
        drones.clear();
        explosions.clear(); 
        
        startNewLevel(1); 
        currentState = GameState.PLAYING;
        
        soundManager.stopSound("intro");
        soundManager.stopSound("boss_backsound"); // Tambahkan ini
        soundManager.loopSound("backsound");
    }
    
    // *** METODE INI DIMODIFIKASI DARI PERMINTAAN SEBELUMNYA ***
    private void startNewLevel(int level) {
        bullets.clear();
        enemies.clear(); 
        enemyBullets.clear();
        powerUps.clear();
        laserBeams.clear();
        bossMinions.clear(); 
        
        hasPlayedLevelClearJingle = false;
        
        currentLevel = level;
        
        int newMaxAmmo;
        
        // Kita tentukan manual jumlah ammo untuk tiap level
        switch (level) {
            case 1:
                newMaxAmmo = 200; // Cukup untuk musuh Lvl 1
                break;
            case 2:
                newMaxAmmo = 250; // Sedikit lebih banyak
                break;
            case 3:
                newMaxAmmo = 350; // Musuh Lvl 3 lebih sulit
                break;
            case 4:
                newMaxAmmo = 600; // LOMPATAN BESAR untuk Lvl 4 yang padat
                break;
            case 5:
                newMaxAmmo = 750; // Cukup untuk Boss 500 HP + Minion
                break;
            default:
                newMaxAmmo = 200;
        }
        
        // Atur max ammo baru di player
        player.setMaxAmmo(newMaxAmmo);
        // Isi ulang amunisi ke max baru
        player.refillAmmo();
        
        // --- LOGIKA SPAWN BARU (Dari Permintaan Sebelumnya) ---
        
        if (currentLevel == 1) {
            totalWavesInLevel = 3; 
            waveEnemiesTypeA = 5; // Tipe A: Scouts
            waveEnemiesTypeB = 0; // Tipe B: Tidak ada
            enemySpawnCooldown = 1500;
        } else if (currentLevel == 2) {
            totalWavesInLevel = 4; 
            waveEnemiesTypeA = 8; // Tipe A: Fighters
            waveEnemiesTypeB = 5; // Tipe B: Scouts
            enemySpawnCooldown = 1200;
        } else if (currentLevel == 3) {
            totalWavesInLevel = 4; 
            waveEnemiesTypeA = 6; // Tipe A: Weavers
            waveEnemiesTypeB = 6; // Tipe B: Fighters
            enemySpawnCooldown = 1200;
        } else if (currentLevel == 4) {
            totalWavesInLevel = 5; 
            waveEnemiesTypeA = 10; // Tipe A: Weavers
            waveEnemiesTypeB = 2; // Tipe B: Cruisers (Lebih sedikit tapi kuat)
            enemySpawnCooldown = 1200;
        } else if (currentLevel == 5) {
            totalWavesInLevel = 1; 
            waveEnemiesTypeA = 0;
            waveEnemiesTypeB = 0;
            bossSpawned = false; finalBoss = null; 
            bossMinions.clear();
        }
        
        currentWave = 1;
        waitingForWave = true;
        waveMessageTimer = System.currentTimeMillis();
        
        // Panggil helper baru untuk reset counter wave pertama
        resetWaveSpawnCounters();
    }
    
    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1000000000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while (gameThread != null) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {
                if (currentState == GameState.PLAYING || 
                    currentState == GameState.LEVEL_TRANSITION || 
                    currentState == GameState.START_MENU ||
                    currentState == GameState.GAME_OVER || // <-- TAMBAHKAN INI
                    currentState == GameState.GAME_WIN) {  // <-- TAMBAHKAN INI
                    update();
                }
                repaint();
                delta--;
            }
        }
    }

    public void update() {
        long now = System.currentTimeMillis();
        
        if (currentState == GameState.GAME_OVER || currentState == GameState.GAME_WIN) {
            
            // 7. Update Ledakan Biasa
            for (Explosion exp : explosions) {
                if (exp.update()) { 
                    explosions.remove(exp);
                }
            }
            // 12. Perbarui Ledakan Bos
            for (BossExplosion bossExplosion : bossExplosions) {
                bossExplosion.update();
            }
            bossExplosions.removeIf(bossExplosion -> !bossExplosion.isActive());
            
            // Perbarui Ledakan Pemain (dari implementasi sebelumnya)
            for (PlayerExplosion playerExp : playerExplosions) {
                playerExp.update();
            }
            playerExplosions.removeIf(playerExp -> !playerExp.isActive());
            
            return; // Lewati sisa logika update (pemain, musuh, dll)
        }
        
        if (currentState == GameState.START_MENU) {           
            for (Meteor meteor : menuMeteors) {
                    meteor.move();
                }
            if (isLogoAnimating) {
                logoY += (logoTargetY - logoY) * 0.05; 
                if (Math.abs(logoY - logoTargetY) < 1) {
                    logoY = logoTargetY;
                    isLogoAnimating = false;
                }
            } 
            return;
        }
        
        if (currentState == GameState.LEVEL_TRANSITION) {
            if (now - transitionStartTime > transitionDuration) {
                startNewLevel(currentLevel + 1); 
                currentState = GameState.PLAYING;
                soundManager.loopSound("backsound");
            }
            return;
        }
        
        // === Logika PLAYING ===

        player.move(); 
        
        // *** BAGIAN INI DIMODIFIKASI DARI PERMINTAAN SEBELUMNYA ***
        // Update posisi drone (drone.update dipanggil di sini)
        for (Drone drone : drones) {
            drone.update(player);
        }

        // Hapus drone dari list jika drone tersebut sudah "pergi" dan keluar layar
        drones.removeIf(drone -> drone.isOffScreen(SCREEN_HEIGHT));
        // ******************************************************

        if (!waitingForWave) {
            // *** BAGIAN INI DIMODIFIKASI DARI PERMINTAAN SEBELUMNYA ***
            if ((now - lastShotTime) > shotCooldown) { 
                
                // 1. Cek apakah pemain punya setidaknya SATU peluru untuk mencoba menembak
                if (player.currentAmmo > 0) { 
                    
                    soundManager.playSound("laserPlayer");
                    
                    int level = player.weaponLevel;
                    double shipCenterX = player.x + player.width / 2.0;
                    double bulletWidth = 9.0;

                    if (level == 0) { 
                        // Hanya butuh 1 peluru
                        if (player.currentAmmo > 0) { // Cek lagi (untuk keamanan)
                            double spawnX = shipCenterX - (bulletWidth / 2.0);
                            bullets.add(new Bullet(spawnX, player.y, 0.0, playerBulletSpeed));
                            player.currentAmmo--; // Kurangi 1
                        }
                    } 
                    else if (level == 1) { 
                        // Butuh 2 peluru
                        double spawnY = player.y + 10; 
                        double gunOffset = 20;
                        if (player.currentAmmo > 0) {
                            double leftGunX = shipCenterX - gunOffset - (bulletWidth / 2.0);
                            bullets.add(new Bullet(leftGunX, spawnY, -5.0, playerBulletSpeed));
                            player.currentAmmo--; // Kurangi 1
                        }
                        if (player.currentAmmo > 0) { // Cek lagi
                            double rightGunX = shipCenterX + gunOffset - (bulletWidth / 2.0);
                            bullets.add(new Bullet(rightGunX, spawnY, 5.0, playerBulletSpeed));
                            player.currentAmmo--; // Kurangi 1
                        }
                    } 
                    else if (level >= 2) { 
                        // Butuh 3 peluru
                        double centerSpawnX = shipCenterX - (bulletWidth / 2.0);
                        double centerSpawnY = player.y;
                        double wingSpawnY = player.y + 20; 
                        double wingGunOffset = 35;
                        
                        if (player.currentAmmo > 0) {
                            double leftWingX = shipCenterX - wingGunOffset - (bulletWidth / 2.0);
                            bullets.add(new Bullet(leftWingX, wingSpawnY, -15.0, playerBulletSpeed));
                            player.currentAmmo--; // Kurangi 1
                        }
                        if (player.currentAmmo > 0) {
                            bullets.add(new Bullet(centerSpawnX, centerSpawnY, 0.0, playerBulletSpeed));
                            player.currentAmmo--; // Kurangi 1
                        }
                        if (player.currentAmmo > 0) {
                            double rightWingX = shipCenterX + wingGunOffset - (bulletWidth / 2.0);
                            bullets.add(new Bullet(rightWingX, wingSpawnY, 15.0, playerBulletSpeed));
                            player.currentAmmo--; // Kurangi 1
                        }
                    }

                    // --- BLOK DRONE YANG SUDAH DIPERBAIKI ---
                    // (Dari Permintaan Sebelumnya)
                    for (Drone drone : drones) {
                        // Langsung panggil drone.shoot()
                        // Ini akan menggunakan amunisi drone sendiri.
                        // Kita tidak lagi mengecek atau mengurangi amunisi player.
                        drone.shoot(bullets, playerBulletSpeed);
                    }
                    // --- AKHIR BLOK PERBAIKAN ---
                    
                    // Reset cooldown *setelah* mencoba menembak
                    lastShotTime = now;
                    
                } else {
                    // Opsional: Jika Anda punya suara "ammo habis", mainkan di sini
                    // soundManager.playSound("ammo_empty_click"); 
                }
            }
            // ******************************************************
        }
            
        for (Bullet bullet : bullets) {
            bullet.move();
            if (bullet.y < 0 || bullet.x < 0 || bullet.x > SCREEN_WIDTH) {
                bullets.remove(bullet);
            }
        }

        for (Enemy enemy : enemies) {
            enemy.move();
            if (enemy.shouldShoot()) {
                soundManager.playSound("laserEnemy");
                if (!(enemy instanceof FinalBoss)) {
                    double spawnX = enemy.x + enemy.width / 2.0 - (5.0 / 2.0);
                    enemyBullets.add(new EnemyBullet(spawnX, enemy.y + enemy.height, enemy.getBulletDamage(), 180.0));
                } else {
                    FinalBoss boss = (FinalBoss) enemy;
                    double midX = boss.x + boss.width / 2.0;
                    double midY = boss.y + boss.height; // Bawah boss
                    double bulletWidth = 5.0;
                    
                    if (boss.getHealthPercentage() < 0.3) {
                        // Fase 3: Tembak 5 peluru dari tengah (Tidak diubah)
                        double spawnX = midX - (bulletWidth / 2.0);
                        enemyBullets.add(new EnemyBullet(spawnX, midY, boss.getBulletDamage() * 2, 160.0));
                        enemyBullets.add(new EnemyBullet(spawnX, midY, boss.getBulletDamage() * 2, 170.0));
                        enemyBullets.add(new EnemyBullet(spawnX, midY, boss.getBulletDamage() * 2, 180.0));
                        enemyBullets.add(new EnemyBullet(spawnX, midY, boss.getBulletDamage() * 2, 190.0));
                        enemyBullets.add(new EnemyBullet(spawnX, midY, boss.getBulletDamage() * 2, 200.0));
                    } else if (boss.getHealthPercentage() < 0.6) { // (Menggunakan logika kode Anda)
                        // Fase 2: Tembak 3 peluru dari tengah (Tidak diubah)
                        double spawnX = midX - (bulletWidth / 2.0);
                        enemyBullets.add(new EnemyBullet(spawnX, midY, boss.getBulletDamage() + 1, 170.0));
                        enemyBullets.add(new EnemyBullet(spawnX, midY, boss.getBulletDamage() + 1, 180.0));
                        enemyBullets.add(new EnemyBullet(spawnX, midY, boss.getBulletDamage() + 1, 190.0));
                    } else {
                        // --- INI PERBAIKANNYA (Fase 1) ---
                        // Fase 1: Tembak dari 2 senjata di samping (SESUAI GAMBAR)
                        
                        // Posisi Y senjata (sekitar 1/3 dari atas boss)
                        double spawnY = boss.y + 50; // PERKIRAAN: Sesuaikan angka 50 ini
                        
                        // Posisi X senjata (sekitar 50px dari tepi kiri/kanan)
                        double leftGunX = boss.x + 50 - (bulletWidth / 2.0); // PERKIRAAN: Sesuaikan angka 50 ini
                        double rightGunX = boss.x + boss.width - 50 - (bulletWidth / 2.0); // PERKIRAAN: Sesuaikan angka 50 ini
                        
                        enemyBullets.add(new EnemyBullet(leftGunX, spawnY, boss.getBulletDamage(), 180.0));
                        enemyBullets.add(new EnemyBullet(rightGunX, spawnY, boss.getBulletDamage(), 180.0));
                        // --- AKHIR PERBAIKAN ---
                    }
                }
            }
            if (enemy.y > SCREEN_HEIGHT && !(enemy instanceof FinalBoss)) { 
                enemies.remove(enemy);
                bossMinions.remove(enemy);
                player.takeDamage(10);
                soundManager.playSound("player_hit");
            }
        }
        
        // 12. Perbarui Ledakan Bos
        for (BossExplosion bossExplosion : bossExplosions) {
            bossExplosion.update();
        }
        bossExplosions.removeIf(bossExplosion -> !bossExplosion.isActive());
        
        for (PlayerExplosion playerExp : playerExplosions) {
            playerExp.update();
        }
        playerExplosions.removeIf(playerExp -> !playerExp.isActive());
        
        for (EnemyBullet eb : enemyBullets) {
            eb.move();
            if (eb.y > SCREEN_HEIGHT || eb.x < 0 || eb.x > SCREEN_WIDTH) {
                enemyBullets.remove(eb);
            }
        }

        for (PowerUp powerUp : powerUps) {
            powerUp.move();
            if (powerUp.y > SCREEN_HEIGHT) {
                powerUps.remove(powerUp);
            }
        }
        
        for (Explosion exp : explosions) {
            if (exp.update()) { 
                explosions.remove(exp);
            }
        }
        
        for (LaserBeam beam : laserBeams) {
            if (beam.update()) {
                laserBeams.remove(beam);
            }
        }

        if (waitingForWave) {
            // *** BAGIAN INI DIMODIFIKASI DARI PERMINTAAN SEBELUMNYA ***
            if (now - waveMessageTimer > waveMessageDuration) {
                waitingForWave = false;
                // Hapus reset counter dari sini
                
                // HAPUS player.refillAmmo(); DARI SINI
                
                soundManager.loopSound("backsound");
            }
        } else if (currentLevel == MAX_LEVEL) {
            // --- Logika Bos (Tidak Berubah) ---
            if (!bossSpawned) {
                enemies.clear(); 
                enemyBullets.clear(); 
                finalBoss = new FinalBoss(SCREEN_WIDTH / 2.0 - 100.0, -150.0, 500, SCREEN_WIDTH, this); 
                enemies.add(finalBoss);
                bossSpawned = true;
                soundManager.stopSound("backsound"); // Hentikan backsound level biasa
                soundManager.loopSound("boss_backsound");
            }
            // --- Akhir Logika Bos ---

        // *** BLOK INI DIMODIFIKASI TOTAL DARI PERMINTAAN SEBELUMNYA ***
        } else {
            // --- LOGIKA SPAWN BARU (DETERMINISTIK) ---
            
            // Cek apakah masih ada musuh yang perlu di-spawn di wave ini
            boolean stillHaveEnemiesToSpawn = (spawnedEnemiesTypeA < waveEnemiesTypeA) || (spawnedEnemiesTypeB < waveEnemiesTypeB);

            if (stillHaveEnemiesToSpawn && (now - lastEnemySpawnTime > enemySpawnCooldown)) {
                int randomX = random.nextInt(SCREEN_WIDTH - 100); 

                // Logika spawn berdasarkan level
                if (currentLevel == 1) {
                    // Tipe A: Scout
                    if (spawnedEnemiesTypeA < waveEnemiesTypeA) {
                        enemies.add(new EnemyScout(randomX, -50.0));
                        spawnedEnemiesTypeA++;
                    }
                } else if (currentLevel == 2) {
                    // Tipe A: Fighter (Spawn ini dulu)
                    if (spawnedEnemiesTypeA < waveEnemiesTypeA) {
                        enemies.add(new EnemyFighter(randomX, -50.0));
                        spawnedEnemiesTypeA++;
                    } 
                    // Tipe B: Scout (Spawn setelah Fighter selesai)
                    else if (spawnedEnemiesTypeB < waveEnemiesTypeB) {
                        enemies.add(new EnemyScout(randomX, -50.0));
                        spawnedEnemiesTypeB++;
                    }
                } else if (currentLevel == 3) {
                    // Tipe A: Weaver
                    if (spawnedEnemiesTypeA < waveEnemiesTypeA) {
                        enemies.add(new EnemyWeaver(randomX, -50.0));
                        spawnedEnemiesTypeA++;
                    } 
                    // Tipe B: Fighter
                    else if (spawnedEnemiesTypeB < waveEnemiesTypeB) {
                        enemies.add(new EnemyFighter(randomX, -50.0));
                        spawnedEnemiesTypeB++;
                    }
                } else if (currentLevel == 4) {
                    // Tipe B: Cruiser (Spawn musuh kuat ini dulu)
                    if (spawnedEnemiesTypeB < waveEnemiesTypeB) {
                        enemies.add(new EnemyCruiser(randomX, -80.0));
                        spawnedEnemiesTypeB++;
                    } 
                    // Tipe A: Weaver
                    else if (spawnedEnemiesTypeA < waveEnemiesTypeA) {
                        enemies.add(new EnemyWeaver(randomX, -50.0));
                        spawnedEnemiesTypeA++;
                    }
                }
                
                lastEnemySpawnTime = now;
            }
            // --- AKHIR LOGIKA SPAWN BARU ---
        }
        // **********************************************************
        
        
        // ========================================================================
        // --- BLOK TABRAKAN YANG DIPERBARUI ---
        // ========================================================================
        
        // 10. Cek Tabrakan (Peluru Player vs Musuh)
        for (Bullet bullet : bullets) {
            for (Enemy enemy : enemies) {
                if (enemy.isDead()) continue; 
                
                if (bullet.getBounds().intersects(enemy.getBounds())) {
                    bullet.hitEnemy(); 
                    if (bullet.isDead()) {
                        bullets.remove(bullet);
                    }
                    
                    enemy.takeDamage(bullet.damage);
                    if (enemy.isDead()) {
                        if (!(enemy instanceof FinalBoss)) { 
                            // Musuh biasa
                            explosions.add(new Explosion(enemy.x, enemy.y, enemy.width, enemy.height));
                            soundManager.playSound("explosion_enemy");
                        } else {
                            // Final Boss
                            soundManager.playSound("explosion_boss");
                            bossExplosions.add(new BossExplosion(
                                enemy.x + enemy.width / 2.0 - 150, // Sesuaikan offset X
                                enemy.y + enemy.height / 2.0 - 150, // Sesuaikan offset Y
                                300, 300 // Ukuran ledakan (misal 300x300, sesuaikan)
                            ));
                        }
                        
                        // --- PERBAIKAN LOGIKA POWER-UP (PENYEIMBANGAN BARU) ---
                        boolean canSpawnPowerUp = (currentLevel > 1);
                        
                        if (canSpawnPowerUp && !(enemy instanceof FinalBoss)) {
                            
                            double dropChance;
                            
                            // *** BAGIAN INI DIMODIFIKASI DARI PERMINTAAN SEBELUMNYA ***
                            // (Level 4 ATAU Level 5)
                            if (currentLevel == 4 || currentLevel == 5) {
                            // ******************************************************
                                dropChance = 0.30; // 30% PELUANG JATUH (Lebih tinggi)
                            } else {
                                dropChance = 0.20; // 20% PELUANG JATUH (Lebih rendah)
                            }

                            if (random.nextDouble() < dropChance) {
                                // Panggil metode helper baru untuk memilih TIPE
                                PowerUpType type = determinePowerUpType(currentLevel); 
                                powerUps.add(new PowerUp(enemy.x, enemy.y, type));
                            }
                        }
                        // ---------------------------------------------------------
                        
                        if (!(enemy instanceof FinalBoss)) {
                            enemies.remove(enemy);
                            bossMinions.remove(enemy); 
                        }
                        score += enemy.getScoreValue();
                    }
                    if (bullet.isDead()) break; 
                }
            }
        }

        // 11 Cek Tabrakan (Player vs Musuh)
        for (Enemy enemy : enemies) {
            if (player.isDead()) continue;
            
            if (player.getBounds().intersects(enemy.getBounds())) {
                if (enemy instanceof FinalBoss) {
                    FinalBoss fb = (FinalBoss) enemy;
                    if (!fb.isInvincibleAfterDash() && !player.isInvincible()) {
                        player.takeDamage(10);
                        soundManager.playSound("player_hit");
                    }
                } else {
                    if (!player.isInvincible()) {
                        player.takeDamage(5);
                        soundManager.playSound("player_hit");
                        explosions.add(new Explosion(enemy.x, enemy.y, enemy.width, enemy.height));
                        soundManager.playSound("explosion_enemy");
                        enemies.remove(enemy);
                        bossMinions.remove(enemy);
                    }
                }
            }
        }
        
        // 12. Cek Tabrakan (Player vs Peluru Musuh)
        for (EnemyBullet eb : enemyBullets) {
            if (player.isDead()) continue;
            if (player.getBounds().intersects(eb.getBounds())) {
                if (!player.isInvincible()) {
                    int oldShieldHealth = player.shieldHealth; 
                    player.takeDamage(eb.damage);
                    enemyBullets.remove(eb);
                    
                    if (player.shieldHealth <= 0 && oldShieldHealth > 0) {
                        soundManager.playSound("shield_break");
                    } else if (player.shieldHealth <= 0) {
                        soundManager.playSound("player_hit"); 
                    }
                }
            }
        }
        
        // 13. Cek Tabrakan (Player vs Power-up)
        for (PowerUp powerUp : powerUps) {
            if (player.isDead()) continue;
            if (player.getBounds().intersects(powerUp.getBounds())) {
                applyPowerUp(powerUp.getType()); 
                powerUps.remove(powerUp);
                soundManager.playSound("powerup_collect");
            }
        }
        
        // 14. Cek Tabrakan (Player vs Laser)
        for (LaserBeam beam : laserBeams) {
            if (player.isDead()) continue;
            if (player.getBounds().intersects(beam.getBounds())) {
                if (!player.isInvincible()) {
                    int oldShieldHealth = player.shieldHealth;
                    player.takeDamage(beam.damage); 
                    
                    if (player.shieldHealth <= 0 && oldShieldHealth > 0) {
                        soundManager.playSound("shield_break"); 
                    } else if (player.shieldHealth <= 0) {
                        soundManager.playSound("player_hit"); 
                    }
                }
            }
        }
        
        // 15. Cek Game Over
        if (player.isDead()) {
            if (!hasPlayedDeathSounds) {
                soundManager.stopSound("backsound");
                soundManager.playSound("player_die");       // 1. Putar suara kematian
                soundManager.playSound("game_over_jingle"); // 2. Putar jingle
                playerExplosions.add(new PlayerExplosion(player.x, player.y, player.width, player.height));
                
                hasPlayedDeathSounds = true; // Set bendera agar tidak diputar lagi
            }
            currentState = GameState.GAME_OVER;
            endMenuSelection = 0;
        }

        // 16. Cek Kemenangan / Level Up
        // *** BLOK INI DIMODIFIKASI TOTAL DARI PERMINTAAN SEBELUMNYA ***
        if (currentLevel == MAX_LEVEL) {
            if (bossSpawned && finalBoss != null && finalBoss.isDead()) {
                
                // Hanya jalankan ini SATU KALI saat pemain menang
                if (!hasPlayedWinJingle) {
                    soundManager.stopSound("backsound");
                    soundManager.stopSound("boss_backsound");
                    soundManager.playSound("game_win_jingle");
                    hasPlayedWinJingle = true; // Set bendera
                }
                
                currentState = GameState.GAME_WIN;
                endMenuSelection = 0;
            }
        } else if (!waitingForWave) {
            
            // --- LOGIKA AKHIR WAVE BARU ---
            
            // 1. Cek apakah semua musuh Tipe A DAN Tipe B sudah di-spawn
            boolean allEnemiesSpawned = (spawnedEnemiesTypeA >= waveEnemiesTypeA) && (spawnedEnemiesTypeB >= waveEnemiesTypeB);

            // 2. Cek apakah semua musuh sudah di-spawn DAN layar sudah bersih
            if (allEnemiesSpawned && enemies.isEmpty() && explosions.isEmpty()) {
                currentWave++;
                
                if (currentWave > totalWavesInLevel) {
                    // Level Selesai -> Transisi
                    if (!hasPlayedLevelClearJingle) {
                        soundManager.stopSound("backsound");
                        soundManager.playSound("level_clear");
                        hasPlayedLevelClearJingle = true;
                    }
                    currentState = GameState.LEVEL_TRANSITION;
                    transitionStartTime = System.currentTimeMillis();
                    resetMovementFlags();
                    
                } else {
                    // Wave Berikutnya Dimulai
                    waitingForWave = true;
                    waveMessageTimer = System.currentTimeMillis();
                    
                    // --- LOGIKA PENINGKATAN WAVE BARU ---
                    // Tambahkan jumlah musuh untuk wave berikutnya
                    if (currentLevel == 1) {
                        waveEnemiesTypeA += 3; // Tambah 3 Scouts
                    } else if (currentLevel == 2) {
                        waveEnemiesTypeA += 3; // Tambah 3 Fighters
                        waveEnemiesTypeB += 2; // Tambah 2 Scouts
                    } else if (currentLevel == 3) {
                        waveEnemiesTypeA += 2; // Tambah 2 Weavers
                        waveEnemiesTypeB += 2; // Tambah 2 Fighters
                    } else if (currentLevel == 4) {
                        waveEnemiesTypeA += 4; // Tambah 4 Weavers
                        // Tambah 1 Cruiser setiap 2 wave (di wave 2 & 4)
                        if (currentWave % 2 == 0) { 
                           waveEnemiesTypeB += 1;
                        }
                    }
                    
                    // Panggil helper untuk mereset counter
                    resetWaveSpawnCounters();
                }
            }
            // --- AKHIR LOGIKA AKHIR WAVE BARU ---
        }
        // **********************************************************
        
        checkPowerUpMaxLevels();
    }

    private void checkPowerUpMaxLevels() {
        // Hanya cek jika tidak ada pesan lain yang sedang tampil
        if (!infoMaxPowerUpMessage.isEmpty()) { 
            return;
        }
        
        if (drones.size() < MAX_DRONES) {
            player.hasShownDroneMax = false;
        }

        // Cek WEAPON
        if (player.weaponLevel >= player.MAX_WEAPON_LEVEL && !player.hasShownWeaponMax) {
            setInfoMaxPowerUpMessage("WEAPON MAX!");
            player.hasShownWeaponMax = true;
        }
        // Cek DRONE
        else if (drones.size() >= MAX_DRONES && !player.hasShownDroneMax) {
            setInfoMaxPowerUpMessage("DRONE MAX!");
            player.hasShownDroneMax = true;
        }
        // Cek BULLET SPEED
        else if (playerBulletSpeed >= MAX_BULLET_SPEED && !player.hasShownBulletSpeedMax) {
            setInfoMaxPowerUpMessage("BULLET SPEED MAX!");
            player.hasShownBulletSpeedMax = true;
        }
        // Cek FIRE RATE
        else if (shotCooldown <= MIN_SHOT_COOLDOWN && !player.hasShownFireRateMax) {
            setInfoMaxPowerUpMessage("FIRE RATE MAX!");
            player.hasShownFireRateMax = true;
        }
    }

    private void setInfoMaxPowerUpMessage(String message) {
        infoMaxPowerUpMessage = message;
        infoMaxPowerUpMessageTimer = System.currentTimeMillis();
    }
    
    public void spawnMinionEnemy(double x, double y) { 
        if (finalBoss != null && bossMinions.size() < finalBoss.MAX_MINIONS) { 
            BossMinion minion = new BossMinion(x, y); 
            minion.health = 10;
            minion.maxHealth = 10;
            minion.speed = 1.5;
            minion.damage = 2; 
            minion.shotCooldown = 1500; 
            bossMinions.add(minion);
            enemies.add(minion); 
        }
    }
    
    public void spawnLaserBeam(double x, double y, double width, double height) {
        laserBeams.add(new LaserBeam(x, y, width, height, finalBoss.damage * 2, 2000, 1500)); 
    }
    
    public void clearLaserBeams() {
        laserBeams.clear();
    }
    
    private void clearExpiredLaserBeams() {
        for (LaserBeam beam : laserBeams) {
            if (beam != null && beam.isExpired()) {
                laserBeams.remove(beam);
            }
        }
    }
    
    // *** METODE INI DIMODIFIKASI DARI PERMINTAAN SEBELUMNYA ***
    private void applyPowerUp(PowerUpType type) {
        switch (type) {
            case HEALTH: 
                player.addHealth(20); 
                break;
            case SHIELD: 
                player.activateShield(player.getMaxShieldHealth()); 
                soundManager.playSound("shield_activate"); 
                break;
            case WEAPON: 
                if (player.weaponLevel < player.MAX_WEAPON_LEVEL) { // Cek sebelum upgrade
                    player.upgradeWeapon(); 
                }
                break;
            case FIRE_RATE:
                if (shotCooldown > MIN_SHOT_COOLDOWN) { // Cek sebelum upgrade
                    shotCooldown -= 50;
                    if (shotCooldown < MIN_SHOT_COOLDOWN) shotCooldown = MIN_SHOT_COOLDOWN;
                }
                player.shotCooldown = this.shotCooldown;
                break;
            case BULLET_SPEED:
                if (playerBulletSpeed < MAX_BULLET_SPEED) { // Cek sebelum upgrade
                    playerBulletSpeed += 2.0;
                    if (playerBulletSpeed > MAX_BULLET_SPEED) playerBulletSpeed = MAX_BULLET_SPEED;
                }
                player.setPlayerBulletSpeed(this.playerBulletSpeed);
                break;
            case DRONE:
                if (drones.size() >= MAX_DRONES) { 
                    // Sudah penuh, jangan lakukan apa-apa
                } else if (drones.isEmpty()) {
                    // Jika benar-benar kosong, tambahkan drone kiri
                    drones.add(new Drone(-60, 20));
                } else if (drones.size() == 1) {
                    // Jika ada 1 drone, cek posisinya
                    Drone existingDrone = drones.get(0);
                    
                    if (existingDrone.getOffsetX() > 0) {
                        // Drone yang ada ada di KANAN (+60)
                        // Maka tambahkan drone KIRI (-60)
                        drones.add(new Drone(-60, 20));
                    } else {
                        // Drone yang ada ada di KIRI (-60)
                        // Maka tambahkan drone KANAN (+60)
                        drones.add(new Drone(60, 20));
                    }
                }
                player.droneCount = drones.size(); 
                break;
            // +++ TAMBAHAN DARI PERMINTAAN SEBELUMNYA +++
            case AMMO:
                player.addAmmo(50); // Tambah 50 peluru (bisa diubah)
                break;
            // ++++++++++++++++++++++++++++++++++++++++
        }
    }
    
    /**
     * Metode helper agar kelas lain (seperti FinalBoss) bisa memutar suara.
     */
    public void playSound(String name) {
        soundManager.playSound(name);
    }
    
    /**
     * Metode helper agar kelas lain (seperti FinalBoss) bisa menghentikan suara.
     */
    public void stopSound(String name) {
        soundManager.stopSound(name);
    }
    
    /**
     * Metode helper untuk menentukan TIPE power-up secara acak,
     * dengan pembobotan khusus untuk Level 4 dan 5.
     * (VERSI DIPERBAIKI DENGAN LOGIKA DRONE YANG BENAR)
     */
    private PowerUpType determinePowerUpType(int level) {
        
        // Logika Khusus untuk Level 4 ATAU 5
        if (level == 4 || level == 5) {
            
            double roll = random.nextDouble(); // Angka acak antara 0.0 dan 1.0

            // --- PEMBAGIAN PELUANG BARU (LOGIKA DRONE DIPERBAIKI) ---
            // 4 power-up utama (consumable) mendapat prioritas tertinggi.

            if (roll < 0.20) { // 20% peluang
                return PowerUpType.FIRE_RATE;
                
            } else if (roll < 0.40) { // 20% peluang
                return PowerUpType.BULLET_SPEED;
                
            } else if (roll < 0.60) { // 20% peluang
                return PowerUpType.AMMO;
                
            } else if (roll < 0.80) { // 20% peluang <-- DRONE SEKARANG DI SINI
                return PowerUpType.DRONE;
                
            } else if (roll < 0.88) { // 8% peluang
                return PowerUpType.HEALTH;
                
            } else if (roll < 0.94) { // 6% peluang
                return PowerUpType.SHIELD;
                
            } else { // 6% peluang sisa (hanya untuk WEAPON)
                return PowerUpType.WEAPON;
            }
            // --- AKHIR PEMBAGIAN BARU ---
        }
        
        
        // --- Logika untuk level lain (Level 2 & 3) ---
        // Peluangnya dibagi rata untuk SEMUA jenis power-up
        PowerUpType[] allTypes = PowerUpType.values();
        return allTypes[random.nextInt(allTypes.length)];
    }

    // Di file GamePanel.java
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); 
        Graphics2D g2d = (Graphics2D) g.create(); 
        
        // ... (kode 'scale' Anda) ...
        int windowWidth = getWidth();
        int windowHeight = getHeight();
        double scaleX = (double) windowWidth / SCREEN_WIDTH;
        double scaleY = (double) windowHeight / SCREEN_HEIGHT;
        g2d.scale(scaleX, scaleY);

        if (bgImage != null) {
            g2d.drawImage(bgImage, 0, 0, SCREEN_WIDTH, SCREEN_HEIGHT, null);
        } else {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        }

        // --- PERBAIKAN ALUR RENDER ---
        
        if (currentState == GameState.START_MENU) {
            // 1. HANYA gambar menu utama
            drawStartMenu(g2d); 
            
        } else if (currentState == GameState.LEVEL_TRANSITION) {
            // 2. HANYA gambar layar transisi (Perbaikan Keluhan #1)
            drawLevelTransition(g2d); 
            
        } else {
            // 3. Untuk PLAYING, PAUSED, GAME_OVER, GAME_WIN:
            
            // Selalu gambar game (player, musuh, ledakan) di lapisan bawah
            drawGame(g2d);
            
            // Gambar UI/Menu di ATAS lapisan game
            if (currentState == GameState.PAUSED) {
                drawPausedScreen(g2d); 
            } else if (currentState == GameState.GAME_OVER) {
                drawGameOverScreen(g2d); // Teks "GAME OVER" di atas ledakan
            } else if (currentState == GameState.GAME_WIN) {
                drawGameWin(g2d); // Teks "YOU WIN" di atas ledakan
            }
            // (State PLAYING tidak menggambar apa-apa lagi di atasnya)
        }
        // --- AKHIR PERBAIKAN ---

        drawInfoMaxPowerUp(g2d);
        g2d.dispose(); 
    }
    
    private void drawInfoMaxPowerUp(Graphics2D g2d) {
        if (infoMaxPowerUpMessage.isEmpty()) {
            return; // Tidak ada pesan
        }

        long elapsedTime = System.currentTimeMillis() - infoMaxPowerUpMessageTimer;

        // Durasi total 2 detik (Req #2)
        if (elapsedTime > INFO_MAX_POWER_UP_MESSAGE_DURATION) {
            infoMaxPowerUpMessage = ""; // Hapus pesan setelah 2 detik
            return;
        }

        // --- Logika Animasi (Req #3) ---
        final long FADE_TIME = 300; // 0.3 detik untuk fade in/out
        float alpha = 1.0f; // Transparansi

        if (elapsedTime < FADE_TIME) {
            // 1. Fade In (0 - 300ms)
            alpha = (float) elapsedTime / FADE_TIME;
        } else if (elapsedTime > INFO_MAX_POWER_UP_MESSAGE_DURATION - FADE_TIME) {
            // 2. Fade Out (1700ms - 2000ms)
            long remainingTime = INFO_MAX_POWER_UP_MESSAGE_DURATION - elapsedTime;
            alpha = (float) remainingTime / FADE_TIME;
        }
        // 3. Hold (300ms - 1700ms) -> alpha tetap 1.0f

        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;

        Graphics2D g2d_info = (Graphics2D) g2d.create();
        g2d_info.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)); 

        g2d_info.setColor(Color.YELLOW);
        g2d_info.setFont(mainFont.deriveFont(24f)); // Gunakan font kustom
        int stringWidth = g2d_info.getFontMetrics().stringWidth(infoMaxPowerUpMessage);
        int x = (SCREEN_WIDTH - stringWidth) / 2;
        int y = SCREEN_HEIGHT / 2 - 100;
        g2d_info.drawString(infoMaxPowerUpMessage, x, y);

        g2d_info.dispose();
    }

    // *** METODE INI DIMODIFIKASI DARI PERMINTAAN SEBELUMNYA ***
    private void drawGame(Graphics g) { 
        if (!player.isDead()) { // <-- TAMBAHKAN KONDISI INI
            player.draw(g);
        }
        for (Bullet bullet : bullets) bullet.draw(g);
        for (Enemy enemy : enemies) {
            if (!enemy.isDead()) {
                enemy.draw(g);
            }
        }
        for (EnemyBullet eb : enemyBullets) eb.draw(g);
        for (PowerUp pu : powerUps) pu.draw(g);
        for (LaserBeam beam : laserBeams) beam.draw(g);
        for (Drone drone : drones) drone.draw(g);
        
        // ledakan enemy
        for (Explosion exp : explosions) {
            exp.draw(g);
        }
        
        // Gambar Ledakan Bos
        for (BossExplosion bossExplosion : bossExplosions) {
            bossExplosion.draw(g);
        }
        
        for (PlayerExplosion playerExp : playerExplosions) {
            playerExp.draw(g);
        }
        
        g.setColor(Color.WHITE);
        g.setFont(mainFont.deriveFont(20f));
        g.drawString("Level: " + currentLevel, 10, 40);
        
        if (currentLevel != MAX_LEVEL) {
             g.drawString("Wave: " + currentWave + " / " + totalWavesInLevel, 10, 60);
        }
        
        g.setColor(Color.WHITE); // Pastikan warna putih (atau CYAAN jika Anda mau)
        g.setFont(mainFont.deriveFont(20f));
        
        String scoreText = "SCORE: " + score;
        FontMetrics fm = g.getFontMetrics();
        
        // Hitung posisi X agar rata kanan
        // (Lebar Layar - Lebar Teks - 10 piksel padding)
        int scoreX = SCREEN_WIDTH - fm.stringWidth(scoreText) - 10;
        
        // Atur posisi Y (misal, 40, sejajar dengan "Level")
        int scoreY = 40; 
        
        g.drawString(scoreText, scoreX, scoreY);
        
        // --- BLOK AMMO PLAYER (DIMODIFIKASI) ---
        // Atur warna teks amunisi (merah jika hampir habis)
        if (player.currentAmmo <= 20) { // Sesuai permintaan (warning di 20)
            g.setColor(Color.RED);
        } else {
            g.setColor(Color.WHITE);
        }
        g.setFont(mainFont.deriveFont(20f));
        // Tampilkan "Ammo: [sisa] / [maksimal]"
        String ammoText = "Ammo: " + player.currentAmmo + " / " + player.maxAmmo;
        g.drawString(ammoText, 10, 80); 
        // --- AKHIR BLOK AMMO PLAYER ---
        
        
        // --- BLOK UI AMMO DRONE (BARU) ---
        int droneSlotY = 100; // Posisi Y awal untuk teks drone pertama
        int droneCounter = 1; // Teks "Drone 1", "Drone 2", dst.

        // Gunakan loop 'for-each'
        for (Drone drone : drones) {
            
            // --- INI PERBAIKANNYA ---
            // Cek amunisi > 0. Jika 0, drone sedang/akan pergi, jadi jangan digambar.
            if (drone.currentAmmo > 0) {
                
                // Tentukan rasio amunisi
                double ammoRatio = (double) drone.currentAmmo / drone.MAX_AMMO;
                
                // Set warna (merah jika kritis 20%, cyan jika normal)
                // Kita tidak perlu cek 'currentAmmo > 0' lagi karena sudah di atas
                if (ammoRatio <= 0.2) {
                     g.setColor(Color.RED);
                } else {
                     g.setColor(Color.CYAN);
                }
                
                g.setFont(mainFont.deriveFont(20f));
                
                // Buat teks (misal: "Drone 1 Ammo: 150 / 150")
                String droneAmmoText = "Drone " + droneCounter + " Ammo: " + drone.currentAmmo + " / " + drone.MAX_AMMO;
                
                // Gambar di posisi Y yang benar
                g.drawString(droneAmmoText, 10, droneSlotY); 
                
                // Siapkan slot berikutnya
                droneSlotY += 20; // Pindahkan Y ke bawah 20 piksel
                droneCounter++;   // Tambah counter drone
            }
        }    
        // --- AKHIR BLOK BARU ---
        
        // Kembalikan warna ke putih untuk sisa UI
        g.setColor(Color.WHITE);
        
        if (waitingForWave && currentLevel != MAX_LEVEL) {
            g.setFont(mainFont.deriveFont(50f));
            fm = g.getFontMetrics();
            String waveText = "WAVE " + currentWave + " START!";
            int x = (SCREEN_WIDTH - fm.stringWidth(waveText)) / 2;
            int y = SCREEN_HEIGHT / 2 - 50;
            g.drawString(waveText, x, y);
        }
        
        drawPlayerHealthBar(g);
        drawPlayerShieldBar(g);
    }
    
    private void drawPlayerHealthBar(Graphics g) {
        int barWidth = 200;
        int barHeight = 20;
        int x = (SCREEN_WIDTH - barWidth) / 2;
        int y = SCREEN_HEIGHT - 40;

        g.setColor(Color.RED);
        g.fillRect(x, y, barWidth, barHeight);
        g.setColor(Color.GREEN);
        g.fillRect(x, y, (int)(barWidth * player.getHealthPercentage()), barHeight);

        g.setColor(Color.WHITE);
        g.drawRect(x, y, barWidth, barHeight);

        g.setFont(mainFont.deriveFont(14f));
        String hpText = player.getHealth() + " / " + player.getMaxHealth();
        FontMetrics fm = g.getFontMetrics();
        int textX = (SCREEN_WIDTH - fm.stringWidth(hpText)) / 2;
        g.drawString(hpText, textX, y + 16);
    }
    
    private void drawPlayerShieldBar(Graphics g) {
        if (player.shieldHealth <= 0) {
            return;
        }

        int barWidth = 150; 
        int barHeight = 10; 
        int x = (SCREEN_WIDTH - barWidth) / 2; 
        int y = SCREEN_HEIGHT - 55; 

        double shieldRatio = (double)player.shieldHealth / player.getMaxShieldHealth();

        g.setColor(new Color(30, 30, 30)); 
        g.fillRect(x, y, barWidth, barHeight);

        g.setColor(Color.CYAN); 
        g.fillRect(x, y, (int)(barWidth * shieldRatio), barHeight);

        g.setColor(Color.WHITE);
        g.drawRect(x, y, barWidth, barHeight);
    }
    
    // *** METODE INI DIMODIFIKASI DARI PERMINTAAN SEBELUMNYA ***
    private void drawLevelTransition(Graphics g) {
        g.setColor(Color.CYAN);
        g.setFont(mainFont.deriveFont(40f));
        FontMetrics fm = g.getFontMetrics();
        String text1 = "LEVEL " + currentLevel + " COMPLETE!";
        int x1 = (SCREEN_WIDTH - fm.stringWidth(text1)) / 2;
        int y1 = SCREEN_HEIGHT / 2 - 50;
        g.drawString(text1, x1, y1);
        
        // --- Teks "Get ready" / "Warning" ---
        g.setFont(thinFont.deriveFont(25f));
        fm = g.getFontMetrics();
        
        String text2;
        int nextLevel = currentLevel + 1; // Simpan level berikutnya
        
        if (nextLevel == MAX_LEVEL) { 
            text2 = "WARNING: FINAL BOSS INCOMING!";
            g.setColor(Color.RED);
        } else {
            text2 = "Get ready for Level " + nextLevel + "...";
            g.setColor(Color.WHITE); // Pastikan warna kembali putih
        }
        int x2 = (SCREEN_WIDTH - fm.stringWidth(text2)) / 2;
        int y2 = SCREEN_HEIGHT / 2 + 30;
        g.drawString(text2, x2, y2);
        
        
        // --- BLOK BARU: Informasi Power-Up ---
        
        String powerUpText = "";
        
        // Logika ini didasarkan pada level BERIKUTNYA
        if (nextLevel == 2 || nextLevel == 3) {
            // Setelah Lvl 1 (masuk Lvl 2) & Setelah Lvl 2 (masuk Lvl 3)
            powerUpText = "Power-up drop chance: 20%";
        } else if (nextLevel == 4 || nextLevel == 5) {
            // Setelah Lvl 3 (masuk Lvl 4) & Setelah Lvl 4 (masuk Lvl 5)
            powerUpText = "Power-up drop chance: 30%";
        }

        // Gambar teks power-up jika sudah diatur
        if (!powerUpText.isEmpty()) {
            g.setColor(Color.YELLOW); // Beri warna kuning agar menonjol
            g.setFont(thinFont.deriveFont(20f)); // Sedikit lebih kecil
            fm = g.getFontMetrics();
            int x3 = (SCREEN_WIDTH - fm.stringWidth(powerUpText)) / 2;
            int y3 = y2 + 40; // 40 piksel di bawah teks "Get ready"
            g.drawString(powerUpText, x3, y3);
        }
        
        // --- AKHIR BLOK BARU ---
    }
    
    private void drawGameWin(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150)); 
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        g.setColor(Color.GREEN);
        g.setFont(mainFont.deriveFont(50f));
        FontMetrics fm = g.getFontMetrics();
        String text = "YOU WIN!";
        int x = (SCREEN_WIDTH - fm.stringWidth(text)) / 2;
        int y = SCREEN_HEIGHT / 2 - 100;
        g.drawString(text, x, y);
        
        g.setColor(Color.WHITE);
        g.setFont(mainFont.deriveFont(30f));
        fm = g.getFontMetrics();
        String scoreText = "Final Score: " + score;
        int scoreX = (SCREEN_WIDTH - fm.stringWidth(scoreText)) / 2;
        int scoreY = SCREEN_HEIGHT / 2;
        g.drawString(scoreText, scoreX, scoreY);
       
        drawEndGameMenu(g);
    }
    
    // *** METODE INI DIMODIFIKASI DARI PERMINTAAN SEBELUMNYA ***
    private void drawStartMenu(Graphics g) {
        for (Meteor meteor : menuMeteors) {
                meteor.draw(g);
        }
        
        if (titleLogoImage != null) {
            int logoX = (SCREEN_WIDTH / 2) - (logoScaledWidth / 2);
            g.drawImage(titleLogoImage, logoX, (int)logoY, logoScaledWidth, logoScaledHeight, null);
        } else {
            g.setColor(Color.WHITE);
            g.setFont(mainFont.deriveFont(50f)); 
            FontMetrics fm = g.getFontMetrics();
            String title = "SPACE SHOOTER";
            int titleX = (SCREEN_WIDTH - fm.stringWidth(title)) / 2;
            int titleY = SCREEN_HEIGHT / 2 - 100;
            g.drawString(title, titleX, titleY);
        }

        // --- BLOK YANG DIUBAH (MENGGANTI TEKS KEDIP) ---
        if (!isLogoAnimating) {
            Graphics2D g2d = (Graphics2D) g.create();
            Color selectColor = Color.CYAN;
            Color defaultColor = Color.WHITE;

            // Atur posisi Y untuk tombol-tombol
            int playY = (int)logoY + logoScaledHeight + 70; // Posisi teks lama
            int quitY = playY + 50; // 50 piksel di bawahnya

            // 1. Gambar Tombol PLAY
            if (startMenuSelection == 0) {
                g2d.setColor(selectColor);
                g2d.setFont(mainFont.deriveFont(30f)); 
            } else {
                g2d.setColor(defaultColor);
                g2d.setFont(thinFont.deriveFont(30f)); 
            }
            FontMetrics fmPlay = g2d.getFontMetrics();
            String playText = "PLAY";
            int playX = (SCREEN_WIDTH - fmPlay.stringWidth(playText)) / 2;
            g2d.drawString(playText, playX, playY);

            // 2. Gambar Tombol QUIT
            if (startMenuSelection == 1) {
                g2d.setColor(selectColor);
                g2d.setFont(mainFont.deriveFont(30f));
            } else {
                g2d.setColor(defaultColor);
                g2d.setFont(thinFont.deriveFont(30f));
            }
            FontMetrics fmQuit = g2d.getFontMetrics();
            String quitText = "QUIT"; // Anda bisa ganti "EXIT" di sini jika mau
            int quitX = (SCREEN_WIDTH - fmQuit.stringWidth(quitText)) / 2;
            g2d.drawString(quitText, quitX, quitY);

            g2d.dispose();
        }
        // --- AKHIR BLOK YANG DIUBAH ---
    }
    
    private void drawGameOverScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150)); 
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
        g.setColor(Color.RED);
        g.setFont(mainFont.deriveFont(50f));
        FontMetrics fm = g.getFontMetrics();
        String text = "GAME OVER";
        int x = (SCREEN_WIDTH - fm.stringWidth(text)) / 2;
        int y = SCREEN_HEIGHT / 2 - 100;
        g.drawString(text, x, y);
        g.setColor(Color.WHITE);
        g.setFont(mainFont.deriveFont(30f));
        fm = g.getFontMetrics();
        String scoreText = "Final Score: " + score;
        int scoreX = (SCREEN_WIDTH - fm.stringWidth(scoreText)) / 2;
        int scoreY = SCREEN_HEIGHT / 2;
        g.drawString(scoreText, scoreX, scoreY);
        drawEndGameMenu(g);
    }
    
    private void drawPausedScreen(Graphics g) {
        g.setColor(new Color(0, 0, 0, 150)); 
        g.fillRect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        Graphics2D g2d = (Graphics2D) g.create();
        Color selectColor = Color.CYAN;
        Color defaultColor = Color.WHITE;

        if (pauseFrameImage != null) {
            int frameX = (SCREEN_WIDTH / 2) - (600 / 2);
            int frameY = (SCREEN_HEIGHT / 2) - (360 / 2);
            g2d.drawImage(pauseFrameImage, frameX, frameY, 600, 360, null);
        } else {
            g2d.setColor(selectColor);
            g2d.setStroke(new BasicStroke(5));
            g2d.drawRect(100, 120, 600, 360);
        }

        int frameCenterY = 120 + (360 / 2); 
        int lineSpacing = 70; 
        int restartY = frameCenterY; 
        int resumeY = restartY - lineSpacing; 
        int exitY = restartY + lineSpacing; 

        if (pauseMenuSelection == 0) {
            g2d.setColor(selectColor);
            g2d.setFont(mainFont.deriveFont(35f)); 
        } else {
            g2d.setColor(defaultColor);
            g2d.setFont(thinFont.deriveFont(35f)); 
        }
        FontMetrics fmResume = g2d.getFontMetrics();
        String resumeText = "RESUME";
        int resumeX = (SCREEN_WIDTH - fmResume.stringWidth(resumeText)) / 2;
        g2d.drawString(resumeText, resumeX, resumeY);

        if (pauseMenuSelection == 1) {
            g2d.setColor(selectColor);
            g2d.setFont(mainFont.deriveFont(35f));
        } else {
            g2d.setColor(defaultColor);
            g2d.setFont(thinFont.deriveFont(35f));
        }
        FontMetrics fmRestart = g2d.getFontMetrics();
        String restartText = "RESTART";
        int restartX = (SCREEN_WIDTH - fmRestart.stringWidth(restartText)) / 2;
        g2d.drawString(restartText, restartX, restartY);

        if (pauseMenuSelection == 2) {
            g2d.setColor(selectColor);
            g2d.setFont(mainFont.deriveFont(35f));
        } else {
            g2d.setColor(defaultColor);
            g2d.setFont(thinFont.deriveFont(35f));
        }
        FontMetrics fmExit = g2d.getFontMetrics();
        String exitText = "EXIT";
        int exitX = (SCREEN_WIDTH - fmExit.stringWidth(exitText)) / 2;
        g2d.drawString(exitText, exitX, exitY);

        g2d.dispose();
    }
    
    private void drawEndGameMenu(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        Color selectColor = Color.CYAN;
        Color defaultColor = Color.WHITE;

        int restartY = SCREEN_HEIGHT / 2 + 50;
        int exitY = restartY + 50;
        
        if (endMenuSelection == 0) {
            g2d.setColor(selectColor);
            g2d.setFont(mainFont.deriveFont(30f)); 
        } else {
            g2d.setColor(defaultColor);
            g2d.setFont(thinFont.deriveFont(30f)); 
        }
        FontMetrics fmRestart = g2d.getFontMetrics();
        String restartText = "MAIN LAGI";
        int restartX = (SCREEN_WIDTH - fmRestart.stringWidth(restartText)) / 2;
        g2d.drawString(restartText, restartX, restartY);

        if (endMenuSelection == 1) {
            g2d.setColor(selectColor);
            g2d.setFont(mainFont.deriveFont(30f));
        } else {
            g2d.setColor(defaultColor);
            g2d.setFont(thinFont.deriveFont(30f));
        }
        FontMetrics fmExit = g2d.getFontMetrics();
        String exitText = "KELUAR";
        int exitX = (SCREEN_WIDTH - fmExit.stringWidth(exitText)) / 2;
        g2d.drawString(exitText, exitX, exitY);

        g2d.dispose();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    // *** METODE INI DIMODIFIKASI DARI PERMINTAAN SEBELUMNYA ***
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_P) {
            if (currentState == GameState.PLAYING) {
                currentState = GameState.PAUSED;
                soundManager.stopSound("backsound");
                soundManager.playSound("menu_confirm");
                pauseMenuSelection = 0; 
                resetMovementFlags();
            } 
        }

        if (code == KeyEvent.VK_ESCAPE) {
            if (currentState == GameState.PAUSED) {
                currentState = GameState.PLAYING;
                soundManager.loopSound("backsound");
            }
        }

        if (currentState == GameState.PLAYING) {
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) player.setMovingLeft(true);
            if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) player.setMovingRight(true);
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) player.setMovingUp(true);
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) player.setMovingDown(true);

        } else if (currentState == GameState.PAUSED) {
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
                pauseMenuSelection--;
                soundManager.playSound("menu_select"); 
                if (pauseMenuSelection < 0) {
                    pauseMenuSelection = 2; 
                }
            }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
                pauseMenuSelection++;
                soundManager.playSound("menu_select"); 
                if (pauseMenuSelection > 2) { 
                    pauseMenuSelection = 0; 
                }
            }
            if (code == KeyEvent.VK_ENTER) {
                soundManager.playSound("menu_confirm"); 
                selectPauseMenuItem(); 
            }

        } else if (currentState == GameState.START_MENU) {
            
            // Jangan lakukan apa-apa jika logo masih animasi
            if (isLogoAnimating) return; 

            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
                startMenuSelection--;
                soundManager.playSound("menu_select"); 
                if (startMenuSelection < 0) {
                    startMenuSelection = 1; // 1 adalah indeks maks (0=Play, 1=Quit)
                }
            }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
                startMenuSelection++;
                soundManager.playSound("menu_select"); 
                if (startMenuSelection > 1) { // 1 adalah indeks maks
                    startMenuSelection = 0; 
                }
            }
            if (code == KeyEvent.VK_ENTER) {
                soundManager.playSound("menu_confirm"); 
                selectStartMenuItem(); // Panggil metode baru
            }
        
        } else if (currentState == GameState.GAME_OVER || currentState == GameState.GAME_WIN) {
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
                endMenuSelection--;
                soundManager.playSound("menu_select"); 
                if (endMenuSelection < 0) {
                    endMenuSelection = 1; 
                }
            }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
                endMenuSelection++;
                soundManager.playSound("menu_select"); 
                if (endMenuSelection > 1) { 
                    endMenuSelection = 0; 
                }
            }
            if (code == KeyEvent.VK_ENTER) {
                soundManager.playSound("menu_confirm"); 
                selectEndMenuItem(); 
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (currentState == GameState.PLAYING) { 
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) player.setMovingLeft(false);
            if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) player.setMovingRight(false);
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) player.setMovingUp(false);
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) player.setMovingDown(false);
        }
    }
}