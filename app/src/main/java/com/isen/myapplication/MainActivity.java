package com.isen.myapplication;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.animation.DecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BLE";
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic txCharacteristic;

    private Handler handler = new Handler();
    private TextView textView;
    private TextView connectedDeviceName;
    private TextView tempTextView, humTextView, lTextView, rTextView, angleTextView;
    private Button scanButton, stopScanButton, disconnectButton;
    private ListView deviceListView;
    private ArrayAdapter<String> deviceListAdapter;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private ArrayList<String> deviceNames = new ArrayList<>();
    private ImageView solarPanelView;

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String displayName = device.getName() + "\n" + device.getAddress();
            if (device.getName() != null && !deviceNames.contains(displayName)) {
                deviceList.add(device);
                deviceNames.add(displayName);
                runOnUiThread(() -> deviceListAdapter.notifyDataSetChanged());
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connecté à l'appareil");
                runOnUiThread(() -> connectedDeviceName.setText("Connecté à : " + gatt.getDevice().getName()));
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Déconnecté de l'appareil");
                runOnUiThread(() -> {
                    connectedDeviceName.setText("Aucun appareil connecté");
                    textView.setText("");
                });
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattService uartService = gatt.getService(UART_SERVICE_UUID);
            if (uartService != null) {
                txCharacteristic = uartService.getCharacteristic(TX_CHAR_UUID);
                if (txCharacteristic != null) {
                    gatt.setCharacteristicNotification(txCharacteristic, true);
                    Log.d(TAG, "Notification activée");
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            String data = new String(characteristic.getValue());
            Log.d(TAG, "Reçu brut : " + data);

            runOnUiThread(() -> {
                textView.setText(data);

                if (data.contains("T:") && data.contains("H:")) {
                    try {
                        String[] parts = data.split(" ");
                        for (String part : parts) {
                            if (part.startsWith("T:")) {
                                String tempValue = part.substring(2).replace("C", "");
                                tempTextView.setText(tempValue + " °C");
                            } else if (part.startsWith("H:")) {
                                String humValue = part.substring(2).replace("%", "");
                                humTextView.setText(humValue + " %");
                            }

                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur parsing T/H: " + e.getMessage());
                    }
                }

                if (data.startsWith("L") && data.contains("R") && data.contains("A")) {
                    try {
                        int lStart = data.indexOf("L") + 1;
                        int rStart = data.indexOf("R") + 1;
                        int aStart = data.indexOf("A") + 1;

                        String lStr = data.substring(lStart, rStart - 1);
                        String rStr = data.substring(rStart, aStart - 1);
                        String angleStr = data.substring(aStart);

                        lTextView.setText("L: " + lStr);
                        rTextView.setText("R: " + rStr);
                        angleTextView.setText("Angle: " + angleStr + "°");

                        animatePanel(Integer.parseInt(angleStr));
                    } catch (Exception e) {
                        Log.e(TAG, "Erreur parsing format compact L/R/A: " + e.getMessage());
                    }
                }
            });
        }
    };

    private void animatePanel(int angle) {
        if (solarPanelView == null) return;

        solarPanelView.animate()
                .rotation(angle)
                .setInterpolator(new DecelerateInterpolator())
                .setDuration(700)
                .start();
    }

    private void startScan() {
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner != null) {
            scanner.startScan(scanCallback);
            Log.d(TAG, "Scan BLE lancé...");
            handler.postDelayed(this::stopScan, 10000);
        }
    }

    private void stopScan() {
        if (scanner != null) {
            scanner.stopScan(scanCallback);
            Log.d(TAG, "Scan BLE arrêté.");
        }
    }

    private void checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Les permissions Bluetooth sont requises", Toast.LENGTH_LONG).show();
            } else {
                startScan();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectedDeviceName = findViewById(R.id.connectedDeviceName);
        textView = findViewById(R.id.bleTextView);
        tempTextView = findViewById(R.id.tempTextView);
        humTextView = findViewById(R.id.humTextView);
        lTextView = findViewById(R.id.lTextView);
        rTextView = findViewById(R.id.rTextView);
        angleTextView = findViewById(R.id.angleTextView);

        scanButton = findViewById(R.id.scanButton);
        stopScanButton = findViewById(R.id.stopScanButton);
        disconnectButton = findViewById(R.id.disconnectButton);
        deviceListView = findViewById(R.id.deviceListView);
        solarPanelView = findViewById(R.id.panelImage);

        deviceListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceNames);
        deviceListView.setAdapter(deviceListAdapter);

        scanButton.setOnClickListener(v -> {
            deviceList.clear();
            deviceNames.clear();
            deviceListAdapter.notifyDataSetChanged();
            startScan();
        });

        stopScanButton.setOnClickListener(v -> stopScan());

        disconnectButton.setOnClickListener(v -> {
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
                bluetoothGatt = null;
                connectedDeviceName.setText("Aucun appareil connecté");
                textView.setText("");
                Log.d(TAG, "Déconnexion effectuée");
            }
        });

        deviceListView.setOnItemClickListener((parent, view, position, id) -> {
            BluetoothDevice selectedDevice = deviceList.get(position);
            stopScan();
            connectToDevice(selectedDevice);
        });

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Veuillez activer le Bluetooth", Toast.LENGTH_LONG).show();
        }

        checkAndRequestPermissions();
    }

    @Override
    protected void onDestroy() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
        super.onDestroy();
    }
}