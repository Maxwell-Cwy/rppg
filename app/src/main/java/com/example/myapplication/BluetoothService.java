package com.example.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.example.myapplication.model.OximeterData;
import com.example.myapplication.utils.HexUtils;
import com.example.myapplication.utils.BluetoothUtils;
import com.example.myapplication.utils.TimeUtils;

import java.util.UUID;

public class BluetoothService {
    private static final String TAG = "BluetoothService";

    // ########### 核心改造1：静态单例相关 ###########
    // 1. 静态单例实例（volatile保证多线程可见性）
    private static volatile BluetoothService INSTANCE;
    // 新增：全局监听器（替换原有构造方法传入的listener）
    private BluetoothListener mGlobalListener;

    // 血氧仪协议UUID
    private static final UUID OXIMETER_SERVICE_UUID =
            UUID.fromString("0000FFB0-0000-1000-8000-00805f9b34fb");
    private static final UUID OXIMETER_CHARACTERISTIC_UUID =
            UUID.fromString("0000FFB2-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CONFIG_DESCRIPTOR_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // 协议命令（全部加了强制转换，彻底解决 byte 报错）
    private static final byte[] DEVICE_READY_CMD = {
            (byte) 0xFF, (byte) 0xFE, 0x04, (byte) 0x87, 0x22, 0x61
    };
    private static final byte[] START_MEASURE_CMD = {
            (byte) 0xFF, (byte) 0xFE, 0x04, (byte) 0xB5, 0x01, (byte) 0xB0
    };

    // ########### 核心改造2：上下文改为全局Application上下文 ###########
    private final Context mAppContext; // 全局上下文，避免内存泄漏
    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic;
    private final OximeterData mOximeterData;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private boolean isReceivingData = false;
    private boolean isConnected = false;
    private String bluetoothDataStartTime;
    private String bluetoothDataEndTime;
    private static Integer index = 0;

    public interface BluetoothListener {
        void onBluetoothConnected(String deviceName, String deviceAddress);
        void onBluetoothConnectFailed(String errorMsg);
        void onBluetoothDisconnected();
        void onDataReceived(String hexData);
        void onDataStartReceiving(String startTime);  // 只保留这个有参数版本
        void onDataStopReceiving(String endTime);
    }

    // ########### 核心改造3：私有化构造方法 ###########
    // 禁止外部new，只能通过getInstance获取实例
    private BluetoothService(Context context) {
        // 关键：使用Application上下文，避免Activity销毁导致内存泄漏
        this.mAppContext = context.getApplicationContext();
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mOximeterData = new OximeterData();
    }

    // ########### 核心改造4：全局唯一获取实例的方法 ###########
    // 双重校验锁，保证线程安全
    public static BluetoothService getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BluetoothService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BluetoothService(context);
                }
            }
        }
        return INSTANCE;
    }

    // ########### 新增：设置全局监听器（供不同Activity切换） ###########
    public void setBluetoothListener(BluetoothListener listener) {
        this.mGlobalListener = listener;
    }

    // 原有方法：连接设备（仅修改监听器调用为mGlobalListener）
    public void connectToDevice(String deviceAddress) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            if (mGlobalListener != null) {
                mGlobalListener.onBluetoothConnectFailed("蓝牙未开启，请先开启蓝牙");
            }
            return;
        }

        if (!BluetoothUtils.isBluetoothAddressValid(deviceAddress)) {
            if (mGlobalListener != null) {
                mGlobalListener.onBluetoothConnectFailed("蓝牙地址不合法");
            }
            return;
        }

        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
        if (device == null) {
            if (mGlobalListener != null) {
                mGlobalListener.onBluetoothConnectFailed("无法获取蓝牙设备");
            }
            return;
        }

        mMainHandler.post(() -> {
            if (ActivityCompat.checkSelfPermission(mAppContext, android.Manifest.permission.BLUETOOTH_CONNECT)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (mGlobalListener != null) {
                    mGlobalListener.onBluetoothConnectFailed("缺少蓝牙连接权限");
                }
                return;
            }
            mBluetoothGatt = device.connectGatt(mAppContext, false, mGattCallback);
        });
    }

    // 原有方法：开始接收数据（仅修改监听器调用）
    public void startReceivingData() {
        if (!isConnected || mCharacteristic == null) {
            if (mGlobalListener != null) {
                mGlobalListener.onBluetoothConnectFailed("蓝牙未连接，无法开始接收数据");
            }
            return;
        }
        isReceivingData = true;
        bluetoothDataStartTime = TimeUtils.getPreciseTimeStamp();  // 记录开始
        Log.e("Time", "记录时间：" + bluetoothDataStartTime);
        mMainHandler.post(() -> {
            if (mGlobalListener != null) {
                mGlobalListener.onDataStartReceiving(bluetoothDataStartTime);
            }
        });
    }

    // 原有方法：停止接收数据（仅修改监听器调用）
    public void stopReceivingData() {
        bluetoothDataEndTime = TimeUtils.getPreciseTimeStamp();  // 记录结束
        isReceivingData = false;
        // 可发送停止命令给设备（如果协议支持）
        sendData(new byte[]{});
        mMainHandler.post(() -> {
            if (mGlobalListener != null) {
                mGlobalListener.onDataStopReceiving(bluetoothDataEndTime);
            }
        });
    }

    // 原有私有方法：发送数据（无修改）
    private void sendData(byte[] data) {
        if (mBluetoothGatt == null || mCharacteristic == null) {
            Log.e(TAG, "发送数据失败：GATT或特征值为空");
            return;
        }

        if (ActivityCompat.checkSelfPermission(mAppContext, android.Manifest.permission.BLUETOOTH_CONNECT)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mCharacteristic.setValue(data);
        mBluetoothGatt.writeCharacteristic(mCharacteristic);
    }

    // 原有方法：断开连接（无核心修改）
    public void disconnect() {
        if (mBluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(mAppContext, android.Manifest.permission.BLUETOOTH_CONNECT)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
        isConnected = false;
        isReceivingData = false;
        mCharacteristic = null;
    }

    // 原有方法：获取采集数据（无修改）
    public OximeterData getCollectedData() {
        return mOximeterData;
    }

    // 原有方法：获取连接状态（无修改）
    public boolean isConnected() {
        return isConnected;
    }

    // ########### 核心改造5：GATT回调中调用全局监听器 ###########
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if (ActivityCompat.checkSelfPermission(mAppContext, android.Manifest.permission.BLUETOOTH_CONNECT)
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                gatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                isConnected = false;
                mMainHandler.post(() -> {
                    if (mGlobalListener != null) {
                        mGlobalListener.onBluetoothDisconnected();
                    }
                });
                gatt.close();
                mBluetoothGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status != BluetoothGatt.GATT_SUCCESS) {
                mMainHandler.post(() -> {
                    if (mGlobalListener != null) {
                        mGlobalListener.onBluetoothConnectFailed("服务发现失败，错误码：" + status);
                    }
                });
                return;
            }

            BluetoothGattService service = gatt.getService(OXIMETER_SERVICE_UUID);
            if (service == null) {
                mMainHandler.post(() -> {
                    if (mGlobalListener != null) {
                        mGlobalListener.onBluetoothConnectFailed("未找到血氧仪服务");
                    }
                });
                return;
            }

            mCharacteristic = service.getCharacteristic(OXIMETER_CHARACTERISTIC_UUID);
            if (mCharacteristic == null) {
                mMainHandler.post(() -> {
                    if (mGlobalListener != null) {
                        mGlobalListener.onBluetoothConnectFailed("未找到血氧仪特征值");
                    }
                });
                return;
            }

            setCharacteristicNotification(true);
            isConnected = true;

            String deviceName = gatt.getDevice().getName();
            String deviceAddress = gatt.getDevice().getAddress();
            String displayName = (deviceName == null || deviceName.isEmpty())
                    ? "未知血氧仪设备" : deviceName;

            mMainHandler.post(() -> {
                if (mGlobalListener != null) {
                    mGlobalListener.onBluetoothConnected(displayName, deviceAddress);
                }
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            byte[] data = characteristic.getValue();
            if (data != null && data.length > 0) {
                String hexData = HexUtils.bytesToHex(data);

                // 只有在正式开始检测后才显示到界面
                if (isReceivingData) {
                    mMainHandler.post(() -> {
                        if (mGlobalListener != null) {
                            mGlobalListener.onDataReceived(hexData);
                        }
                    });
                    index++;
                    Log.w("数据总数：", "第" + index + ":" + hexData);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "特征值写入失败，错误码：" + status);
            }
        }
    };

    // 原有方法：设置特征值通知（无修改）
    private void setCharacteristicNotification(boolean enable) {
        if (mBluetoothGatt == null || mCharacteristic == null) {
            return;
        }

        if (ActivityCompat.checkSelfPermission(mAppContext, android.Manifest.permission.BLUETOOTH_CONNECT)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(mCharacteristic, enable);

        BluetoothGattDescriptor descriptor = mCharacteristic.getDescriptor(CLIENT_CONFIG_DESCRIPTOR_UUID);
        if (descriptor != null) {
            descriptor.setValue(enable
                    ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    // ########### 新增：获取已连接设备地址（可选，供Activity传递校验） ###########
    public String getConnectedDeviceAddress() {
        if (mBluetoothGatt != null && isConnected) {
            return mBluetoothGatt.getDevice().getAddress();
        }
        return null;
    }
}