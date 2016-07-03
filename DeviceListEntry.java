package org.hva.createit.sensei.ui.sweetblue;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.idevicesinc.sweetblue.BleDevice;
import com.idevicesinc.sweetblue.BleDeviceState;
import com.idevicesinc.sweetblue.utils.State;
import com.idevicesinc.sweetblue.utils.State.ChangeIntent;
import com.idevicesinc.sweetblue.utils.Uuids;
import com.infy.intelirun.ui.R;

import org.hva.createit.scl.ble.HeartrateActivity;
import org.hva.createit.scl.data.MeasurementData;
import org.hva.createit.scl.dataaccess.MeasurementDAO;

import java.nio.charset.Charset;
import java.util.UUID;

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

/**
 * original by
 * @author dougkoellmer
 */
public class DeviceListEntry extends FrameLayout implements BleDevice.StateListener
{
	private final BleDevice m_device;
	private final Button m_connect;
	private final Button m_disconnect;
	private final TextView m_status;
	private final TextView m_name;
	private final TextView m_signal;
	private final String TAG = "DeviceListEntry";
	private Context context;
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");


	public DeviceListEntry(Context context, BleDevice device)
	{
		super(context);
		this.context = context;

		m_device = device;
		m_device.setListener_State(this);
		m_device.setListener_ConnectionFail(new BleDevice.DefaultConnectionFailListener()
		{
			@Override public Please onEvent(ConnectionFailEvent event)
			{
				Please please = super.onEvent(event);

				if( !please.isRetry() )
				{
					final String toast =  event.device().getName_debug() + " connection failed with " + event.failureCountSoFar() + " retries - " + event.status() +
							"\nPlease restart your heart rate monitor";
					Toast.makeText(getContext(), toast, Toast.LENGTH_LONG).show();
				}

				return please;
			}
		});

		m_device.setListener_Bond(new BleDevice.BondListener()
		{
			@Override public void onEvent(BondEvent event)
			{
				final String toast =  event.device().getName_debug() + " bond attempt finished with status " + event.status();
				Toast.makeText(getContext(), toast, Toast.LENGTH_LONG).show();
			}
		});

		LayoutInflater li = LayoutInflater.from(context);
		View inner = li.inflate(R.layout.device_entry, null);

		m_connect = (Button) inner.findViewById(R.id.connect_button);
		m_disconnect = (Button) inner.findViewById(R.id.disconnect_button);
		m_status = (TextView) inner.findViewById(R.id.device_status);
		m_name = (TextView) inner.findViewById(R.id.device_name);
		m_signal = (TextView) inner.findViewById(R.id.signal_strength);

		m_connect.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				m_device.getManager().disconnectAll();
				m_device.connect();
				m_connect.setVisibility(View.GONE);
				m_disconnect.setVisibility(View.VISIBLE);
			}
		});

		m_disconnect.setOnClickListener(new OnClickListener()
		{
			@Override public void onClick(View v)
			{
				m_device.disconnect();
				m_connect.setVisibility(View.VISIBLE);
				m_disconnect.setVisibility(View.GONE);
			}
		});

		updateStatus(m_device.getStateMask());

		String name = m_device.getName_normalized();
		if( name.length() == 0 )
		{
			name = m_device.getMacAddress();
		}
		else
		{
			name += "\n(" + m_device.getMacAddress() + ")";
		}
		m_name.setText(name);

		m_signal.setText(""+m_device.getRssi());

		this.addView(inner);

		if( device.getLastDisconnectIntent() == ChangeIntent.UNINTENTIONAL )
		{
			device.connect();
		}
	}

	public BleDevice getDevice()
	{
		return m_device;
	}

	private void updateStatus(int deviceStateMask)
	{
//		SpannableString status = Utils_String.makeStateString(BleDeviceState.values(), deviceStateMask);
		String status = "";
		State[] states = BleDeviceState.values();
		for(State state: states){
			if(state.overlaps(deviceStateMask)){
				if(state == BleDeviceState.CONNECTED) {
					m_connect.setVisibility(View.GONE);
					m_disconnect.setVisibility(View.VISIBLE);
					status = state.name();
				}else if(state == BleDeviceState.DISCONNECTED) {
					m_connect.setVisibility(View.VISIBLE);
					m_disconnect.setVisibility(View.GONE);
					status = state.name();
				}
			}
		}
		m_status.setText(status);
	}

	@Override public void onEvent(StateEvent event) {
		updateStatus(event.newStateBits());
		if (event.didEnter(BleDeviceState.INITIALIZED)) {
			Log.i("SweetBlueExample", event.device().getName_debug() + " just initialized!");

			event.device().read(Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener() {
				@Override
				public void onEvent(ReadWriteEvent result) {
					if (result.wasSuccess()) {
						Log.i("SweetBlueExample", "Battery level is " + result.data()[0] + "%");
					}
				}
			});
			BleDevice.ReadWriteListener listener = new BleDevice.ReadWriteListener() {
				@Override
				public void onEvent(ReadWriteEvent e) {
					Log.d("SweetBlueExample", e.toString());
					if (e.status() == Status.SUCCESS && e.type() == Type.NOTIFICATION) {
						// This is special handling for the Heart Rate Measurement profile. Data
						// parsing is
						// carried out as per profile specifications:
						// http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml

						int flag = 0;//e.characteristic().getProperties();
						int format = -1;
						if ((flag & 0x01) != 0) {
							format = BluetoothGattCharacteristic.FORMAT_UINT16;
							Log.d(TAG, "Heart rate format UINT16.");
						} else {
							format = BluetoothGattCharacteristic.FORMAT_UINT8;
							Log.d(TAG, "Heart rate format UINT8.");
						}
						BluetoothGattCharacteristic c = new BluetoothGattCharacteristic(e.charUuid(), 0, 0);
						c.setValue(e.data());
						final int heartRate = c.getIntValue(format, 1);
						Log.d(TAG, String.format("Received heart rate: %d", heartRate));

						//storing value
						 SharedPreferences sharedPref = context.getSharedPreferences(HeartrateActivity.EXTRAS_DEVICE_ADDRESS, Context.MODE_PRIVATE);
						SharedPreferences.Editor editor = sharedPref.edit();
						editor.putString(HeartrateActivity.EXTRAS_DEVICE_ADDRESS, m_device.getMacAddress());

						MeasurementDAO measurementDAO = new MeasurementDAO(context);
						MeasurementData heartRateSensorAddress = measurementDAO.getMeasurement("heartratesensor", 0);
						if(heartRateSensorAddress == null){
							heartRateSensorAddress = new MeasurementData(0, "heartratesensor", 0,0);
							measurementDAO.store(heartRateSensorAddress);
							heartRateSensorAddress = measurementDAO.getMeasurement("heartratesensor", 0);
						}
						String storedAddress = new String(heartRateSensorAddress.getValue(), UTF8_CHARSET);
						if(storedAddress.compareTo(m_device.getMacAddress()) != 0) {
							heartRateSensorAddress.setValue(m_device.getMacAddress().getBytes(UTF8_CHARSET));
							measurementDAO.update(heartRateSensorAddress);
						}
					}
				}
			};

			event.device().enableNotify(UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb"), listener);
		}else if (event.didEnter(BleDeviceState.DISCONNECTED)) {
			event.device().disconnect_remote();
		}
	}
}
