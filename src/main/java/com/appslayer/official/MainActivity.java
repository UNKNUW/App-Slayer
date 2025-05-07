package com.appslayer.official;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import java.io.*;
import android.os.Vibrator;
import android.os.VibrationEffect;

public class MainActivity extends Activity {

    private Button btnToggleService, btnMode;
    private Button btnWhitelist, btnExtreme, btnGamelist, btnSaveFile;
    private EditText editText;
    private boolean isServiceRunning = false;
    private int currentMode = 1;
    private String currentFilePath;
    private Vibrator vibrator; // Tambahan: deklarasi vibrator

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        btnToggleService = findViewById(R.id.btnToggleService);
        btnMode = findViewById(R.id.btnMode);
        btnWhitelist = findViewById(R.id.btnWhitelist);
        btnExtreme = findViewById(R.id.btnExtreme);
        btnGamelist = findViewById(R.id.btnGamelist);
        btnSaveFile = findViewById(R.id.btnSaveFile);
        editText = findViewById(R.id.editText);

        // Tambahan: inisialisasi vibrator
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

        SharedPreferences prefs = getSharedPreferences("AppSlayerPrefs", MODE_PRIVATE);
        isServiceRunning = prefs.getBoolean("notifikasi", false);
        currentMode = prefs.getInt("mode", 1);

        if (isServiceRunning) {
            btnToggleService.setText("MATIKAN NOTIFIKASI");
            btnToggleService.setBackgroundColor(0xFF6A0DAD);
        } else {
            btnToggleService.setText("NYALAKAN NOTIFIKASI");
            btnToggleService.setBackgroundColor(0xFF012BFF);
        }

        updateModeButton();

        btnToggleService.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					vibrate(); // Tambahan: getar
					SharedPreferences.Editor editor = getSharedPreferences("AppSlayerPrefs", MODE_PRIVATE).edit();

					if (isServiceRunning) {
						stopService(new Intent(MainActivity.this, MyService.class));
						btnToggleService.setText("NYALAKAN NOTIFIKASI");
						btnToggleService.setBackgroundColor(0xFF012BFF);
						isServiceRunning = false;
						editor.putBoolean("notifikasi", false);
					} else {
						Intent intent = new Intent(MainActivity.this, MyService.class);
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
							startForegroundService(intent);
						} else {
							startService(intent);
						}
						btnToggleService.setText("MATIKAN NOTIFIKASI");
						btnToggleService.setBackgroundColor(0xFF6A0DAD);
						isServiceRunning = true;
						editor.putBoolean("notifikasi", true);
					}

					editor.apply();
				}
			});

        btnMode.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					vibrate(); // Tambahan: getar
					currentMode++;
					if (currentMode > 3) currentMode = 1;
					updateModeButton();
					writeModeToFile(currentMode);

					SharedPreferences.Editor editor = getSharedPreferences("AppSlayerPrefs", MODE_PRIVATE).edit();
					editor.putInt("mode", currentMode);
					editor.apply();
				}
			});

        btnWhitelist.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					vibrate(); // Tambahan: getar
					openFileForEditing("/data/adb/modules/appslayer/list/whitelist.txt");
				}
			});

        btnExtreme.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					vibrate(); // Tambahan: getar
					openFileForEditing("/data/adb/modules/appslayer/list/Extreme.txt");
				}
			});

        btnGamelist.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					vibrate(); // Tambahan: getar
					openFileForEditing("/data/adb/modules/appslayer/list/gamelist.txt");
				}
			});

        btnSaveFile.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					vibrate(); // Tambahan: getar
					saveFileChanges();
				}
			});
    }

    private void vibrate() {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }

    private void updateModeButton() {
        switch (currentMode) {
            case 1:
                btnMode.setText("MODE: NORMAL");
                btnMode.setBackgroundColor(0xFF4CAF50);
                btnMode.setTextColor(0xFFFFFFFF);
                Toast.makeText(this, "MODE NORMAL AKTIF", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                btnMode.setText("MODE: AGGRESSIVE");
                btnMode.setBackgroundColor(0xFFFFD700);
                btnMode.setTextColor(0xFF000000);
                Toast.makeText(this, "MODE AGGRESSIVE AKTIF", Toast.LENGTH_SHORT).show();
                break;
            case 3:
                btnMode.setText("MODE: EXTREME");
                btnMode.setBackgroundColor(0xFFFF3B30);
                btnMode.setTextColor(0xFFFFFFFF);
                Toast.makeText(this, "MODE EXTREME AKTIF", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void writeModeToFile(int mode) {
        try {
            String command = "echo " + mode + " > /data/adb/modules/appslayer/config_mode/mode.txt\n";
            java.lang.Process su = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes(command);
            os.writeBytes("exit\n");
            os.flush();
            su.waitFor();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal set mode:\n" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openFileForEditing(String path) {
        currentFilePath = path;
        editText.setVisibility(View.VISIBLE);
        btnSaveFile.setVisibility(View.VISIBLE);

        StringBuilder content = new StringBuilder();
        try {
            java.lang.Process su = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes("cat " + path + "\n");
            os.writeBytes("exit\n");
            os.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(su.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            su.waitFor();
            editText.setText(content.toString());
            Toast.makeText(this, "File dibuka.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            editText.setText("");
            Toast.makeText(this, "Gagal membuka file.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveFileChanges() {
        if (currentFilePath == null) return;

        try {
            java.lang.Process su = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());

            os.writeBytes("echo '' > " + currentFilePath + "\n");

            BufferedReader reader = new BufferedReader(new StringReader(editText.getText().toString()));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.replace("\"", "\\\""); // escape quote
                os.writeBytes("echo \"" + line + "\" >> " + currentFilePath + "\n");
            }

            os.writeBytes("exit\n");
            os.flush();
            su.waitFor();

            Toast.makeText(this, "Berhasil disimpan.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Gagal menyimpan file.", Toast.LENGTH_SHORT).show();
        }

        editText.setVisibility(View.GONE);
        btnSaveFile.setVisibility(View.GONE);
    }
}
