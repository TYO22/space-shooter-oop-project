package com.mycompany.spaceshootertes; 

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineEvent;

public class SoundManager {

    // soundMap hanya akan menyimpan suara yang TIDAK memerlukan pemutaran simultan (seperti backsound)
    private Map<String, Clip> soundMap;
    // Kita akan menyimpan path untuk suara yang memerlukan pemutaran cepat
    private Map<String, String> sfxPathMap; 

    public SoundManager() {
        soundMap = new HashMap<>();
        sfxPathMap = new HashMap<>();

        // Muat semua suara loop & jingle (yang tidak perlu simultan)
        loadClip("intro", "/res/intro.wav");
        loadClip("backsound", "/res/backsound.wav");
        loadClip("shield_break", "/res/shield_break.wav");
        loadClip("boss_dash", "/res/boss_dash.wav");
        loadClip("boss_backsound", "/res/boss_backsound.wav");
        loadClip("laser_charge", "/res/laser_charge.wav");
        loadClip("laser_fire", "/res/laser_fire.wav");
        loadClip("player_die", "/res/player_die.wav");
        loadClip("game_over_jingle", "/res/game_over_jingle.wav");
        loadClip("game_win_jingle", "/res/game_win_jingle.wav");
        loadClip("explosion_boss", "/res/explosion_boss.wav");
        loadClip("level_clear", "/res/level_clear.wav");
        loadClip("player_hit", "/res/player_hit.wav"); // Biasanya tidak perlu simultan
        loadClip("menu_select", "/res/menu_select.wav");
        loadClip("menu_confirm", "/res/menu_confirm.wav");
        
        // --- SUARA YANG MEMBUTUHKAN PEMUTARAN SIMULTAN (SFX) ---
        // Simpan path-nya saja, jangan muat Clip-nya sekarang
        sfxPathMap.put("laserPlayer", "/res/laserPlayer.wav");
        sfxPathMap.put("laserEnemy", "/res/laserEnemy.wav"); // Mungkin juga perlu simultan
        sfxPathMap.put("explosion_enemy", "/res/explosion.wav");
        sfxPathMap.put("powerup_collect", "/res/powerup_collect.wav");
        sfxPathMap.put("shield_activate", "/res/shield_activate.wav");
    }

    // Metode lama diubah namanya menjadi loadClip, hanya untuk suara yang di-cache
    private void loadClip(String name, String filePath) {
        try {
            URL url = getClass().getResource(filePath);
            if (url == null) {
                System.err.println("File suara tidak ditemukan: " + filePath);
                return;
            }
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            soundMap.put(name, clip);
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Gagal memuat suara: " + filePath);
            e.printStackTrace();
        }
    }
    
    // Metode ini sekarang menangani pemutaran suara
    public void playSound(String name) {
        // 1. Cek apakah suara adalah SFX yang butuh pemutaran simultan
        if (sfxPathMap.containsKey(name)) {
            // Panggil metode non-blocking untuk SFX
            playSoundNonBlocking(name);
            return;
        }

        // 2. Jika bukan SFX, gunakan Clip yang di-cache (untuk loop/jingle)
        Clip clip = soundMap.get(name);
        if (clip != null) {
            if (clip.isRunning()) {
                clip.stop(); 
            }
            clip.setFramePosition(0); 
            clip.start();
        }
    }

    // --- METODE BARU: Memutar SFX secara Simultan ---
    private void playSoundNonBlocking(String name) {
        String filePath = sfxPathMap.get(name);
        if (filePath == null) {
            Clip clip = soundMap.get(name);
            if (clip != null) {
                // Mainkan suara yang di-cache sebagai non-blocking jika tidak terdaftar di sfxPathMap
                if (clip.isRunning()) clip.stop();
                clip.setFramePosition(0);
                clip.start();
            }
            return;
        }

        // Muat Clip baru setiap kali dipanggil (memungkinkan pemutaran simultan)
        try {
            URL url = getClass().getResource(filePath);
            if (url == null) return;
            
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            
            // Tambahkan LineListener untuk menutup Clip setelah selesai dimainkan
            clip.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    if (event.getType() == LineEvent.Type.STOP) {
                        event.getLine().close();
                    }
                }
            });
            
            clip.start();
            
        } catch (Exception e) {
            System.err.println("Gagal memutar SFX non-blocking: " + name);
            // e.printStackTrace(); // Opsional untuk debugging
        }
    }
    // --------------------------------------------------

    public void loopSound(String name) {
        Clip clip = soundMap.get(name);
        if (clip != null) {
            if (!clip.isRunning()) { 
                clip.setFramePosition(0); 
                clip.loop(Clip.LOOP_CONTINUOUSLY); 
            }
        }
    }

    public void stopSound(String name) {
        Clip clip = soundMap.get(name);
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }
}