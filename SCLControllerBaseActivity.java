
package org.hva.createit.sensei.ui;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;
import com.idevicesinc.sweetblue.utils.Uuids;

import org.hva.createit.scl.ble.GattAttributes;
import org.hva.createit.scl.data.HeartRateData;
import org.hva.createit.scl.data.MeasurementData;
import org.hva.createit.scl.dataaccess.HeartRateDAO;
import org.hva.createit.scl.dataaccess.MeasurementDAO;
import org.hva.createit.scl.sensor.SensorService;

import java.nio.charset.Charset;
import java.util.UUID;

import de.greenrobot.event.EventBus;

/** 2016 the Amsterdam University of Applied Sciences, Amsterdam, The Netherlands.
 *
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @co-author Joey van der Bie - j.h.f.van.der.bie@hva.nl
 **/


public class SCLControllerBaseActivity extends BaseActivity {
    // BluetoothHeartRateActivity{
    public static final String TAG = "SCL";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    public String mDeviceAddress;
    public String mDefaultDeviceAddress = "C3:4D:F2:BD:3B:63"; // mio
    private static final Interval SCAN_TIMEOUT = Interval.secs(30.0);
    SensorServiceReceiver sensorReceiver = null;
    Intent sensorIntent;
    Handler startupHandler = new Handler();
    /**
     * Messenger for communicating with the service.
     */
    Messenger mService = null;
    /**
     * Flag indicating whether we have called bind on the service.
     */
    boolean mBound;
    /**
     * Class for interacting with the main interface of the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the object we can use to
            // interact with the service. We are communicating with the
            // service using a Messenger, so here we get a client-side
            // representation of that from the raw IBinder object.
            mService = new Messenger(service);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            mService = null;
            mBound = false;
        }
    };
    // private TextView mConnectionState;
    // private TextView mDataField;
    private String mDeviceName;
    private String mDefaultDeviceName = "Mio Link";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        sensorIntent = new Intent(this, SensorService.class);
        startService(sensorIntent);

    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume/registering receiver");

        sensorReceiver = new SensorServiceReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SensorService.NEW_MEASUREMENT);
        registerReceiver(sensorReceiver, intentFilter);

        if(!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause/unregistering receiver");
        if (sensorReceiver != null) {
            unregisterReceiver(sensorReceiver);
        }
        sensorReceiver = null;


    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to the service
        bindService(new Intent(this, SensorService.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");

        if (sensorReceiver != null) {
            unregisterReceiver(sensorReceiver);
        }
        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        stopService(sensorIntent);

        m_bleManager.disconnectAll();
        EventBus.getDefault().unregister(this);

    }

    public void startSensors() {
        startService(sensorIntent);

        if (!mBound)
            return;
        // Create and send a message to the service, using a supported 'what'
        // value
        Message msg = Message.obtain(null, SensorService.MSG_START_SENSORS, 0,
                0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        startHeartRateComponents();

    }

    public void stopSensors() {
        if (!mBound) {
            bindService(new Intent(this, SensorService.class), mConnection,
                    Context.BIND_AUTO_CREATE);
        }

        Message msg = Message
                .obtain(null, SensorService.MSG_STOP_SENSORS, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        stopService(sensorIntent);
        m_bleManager.disconnectAll();
    }

    public void startLocationSensor() {
        startService(sensorIntent);

        if (!mBound)
            return;
        // Create and send a message to the service, using a supported 'what'
        // value
        Message msg = Message.obtain(null,
                SensorService.MSG_START_SENSOR_LOCATION, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void stopLocationSensor() {
        if (!mBound)
            return;
        // Create and send a message to the service, using a supported 'what'
        // value
        Message msg = Message.obtain(null,
                SensorService.MSG_STOP_SENSOR_LOCATION, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        stopService(sensorIntent);
    }

    public void startStepFrequencySensor() {
        startService(sensorIntent);

        if (!mBound)
            return;
        // Create and send a message to the service, using a supported 'what'
        // value
        Message msg = Message.obtain(null,
                SensorService.MSG_START_SENSOR_STEPFREQUENCY, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    public void stopStepFrequencySensor() {
        if (!mBound)
            return;
        // Create and send a message to the service, using a supported 'what'
        // value
        Message msg = Message.obtain(null,
                SensorService.MSG_STOP_SENSOR_STEPFREQUENCY, 0, 0);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected int getLayoutResource() {
        return 0;
    }


    private BleManager m_bleManager;
    private final Charset UTF8_CHARSET = Charset.forName("UTF-8");


    private void startHeartRateComponents() {

        final HeartRateDAO heartRateDAO = new HeartRateDAO(this);

        MeasurementDAO measurementDAO = new MeasurementDAO(this);
        MeasurementData heartRateSensorAddress = measurementDAO.getMeasurement("heartratesensor", 0);
        if(heartRateSensorAddress == null){
            Toast.makeText(this, "No Heart Rate sensor selected", Toast.LENGTH_LONG).show();
        }else{
            mDeviceAddress = new String(heartRateSensorAddress.getValue(), UTF8_CHARSET);
        }


        BluetoothEnabler.start(this);

        m_bleManager = BleManager.get(this);
//        if(m_bleManager.getDevice().getMacAddress().compareTo(mDeviceAddress)!=0) {
            m_bleManager.disconnectAll();
            m_bleManager.disconnectAll_remote();

            m_bleManager.startScan(SCAN_TIMEOUT, new BleManager.DiscoveryListener() {
                @Override
                public void onEvent(DiscoveryEvent event) {
                    Log.d(TAG, "Found: " + event.device().getMacAddress() + " Looking for: " + mDeviceAddress);


                    Log.d(TAG, "Found: " + event.device().getMacAddress() + " with lifecycle " + event.lifeCycle().toString());

                    if (event.was(LifeCycle.DISCOVERED) || event.was(LifeCycle.REDISCOVERED)) {
                        if (event.device().getMacAddress().compareTo(mDeviceAddress) != 0) {
                            return;
                        }
                        m_bleManager.stopScan();
                        event.device().connect(new BleDevice.StateListener() {
                            @Override
                            public void onEvent(com.idevicesinc.sweetblue.BleDevice.StateListener.StateEvent event) {
                                if (event.didEnter(BleDeviceState.INITIALIZED)) {
//                                    Log.i("SweetBlueExample", event.device().getName_debug() + " just initialized!");

                                    event.device().read(Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener() {
                                        @Override
                                        public void onEvent(com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent result) {
                                            if (result.wasSuccess()) {
//                                                Log.i("SweetBlueExample", "Battery level is " + result.data()[0] + "%");
                                            }
                                        }
                                    });
                                    BleDevice.ReadWriteListener listener = new BleDevice.ReadWriteListener() {
                                        @Override
                                        public void onEvent(com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.ReadWriteEvent e) {
//                                            Log.d("SweetBlueExample", e.toString());
                                            if (e.status() == com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Status.SUCCESS && e.type() == com.idevicesinc.sweetblue.BleDevice.ReadWriteListener.Type.NOTIFICATION) {
                                                // This is special handling for the Heart Rate Measurement profile. Data
                                                // parsing is
                                                // carried out as per profile specifications:
                                                // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml

                                                int flag = 0;//e.characteristic().getProperties();
                                                int format = -1;
                                                if ((flag & 0x01) != 0) {
                                                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
//                                                    Log.d(TAG, "Heart rate format UINT16.");
                                                } else {
                                                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
//                                                    Log.d(TAG, "Heart rate format UINT8.");
                                                }
                                                BluetoothGattCharacteristic c = new BluetoothGattCharacteristic(e.charUuid(), 0, 0);
                                                c.setValue(e.data());
                                                final int heartRate = c.getIntValue(format, 1);
                                                Log.d(TAG, String.format("Received heart rate: %d", heartRate));
                                                HeartRateData beat = new HeartRateData(0, heartRate, System.currentTimeMillis());

                                                heartRateDAO.store(beat);
                                                EventBus.getDefault().post(beat);
                                            }
                                        }
                                    };

                                    event.device().enableNotify(UUID.fromString(GattAttributes.HEART_RATE_MEASUREMENT), listener);
                                }
                            }
                        });
                    }
                }
            });

    }


    private class SensorServiceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context arg0, Intent arg1) {

        }
    }

}
