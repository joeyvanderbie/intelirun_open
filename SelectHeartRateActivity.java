package org.hva.createit.sensei.ui.sweetblue;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManagerConfig;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.utils.BluetoothEnabler;
import com.idevicesinc.sweetblue.utils.Interval;
import com.infy.intelirun.ui.R;

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
public class SelectHeartRateActivity extends Activity
{
	private static final int BLE_TURN_ON_REQUEST_CODE = 2;

	private BleManager m_bleMngr;
	private ViewController m_viewController;
	private AlertManager m_alertMngr;

	private final BleManagerConfig m_bleManagerConfig = new BleManagerConfig()
	{{
		// Mostly using default options for this demo, but provide overrides here if desired.

		// Disabling undiscovery so the list doesn't jump around...ultimately a UI problem so should be fixed there eventually.
		this.undiscoveryKeepAlive = Interval.DISABLED;

		this.loggingEnabled = true;
	}};

	public SelectHeartRateActivity()
	{
	}
	
	@Override protected void onCreate(Bundle savedInstanceState)
    {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_heartrate_settings);
		TextView titleBarText = (TextView) findViewById(R.id.title_bar_text);
		titleBarText.setText(R.string.connect_heart_rate_sensor);


		ImageButton backBtn = (ImageButton) findViewById(R.id.bluetooth_back_btn);
		backBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onBackPressed();
			}
		});

		LinearLayout ll = (LinearLayout) findViewById(R.id.main_layout);
		ll.removeAllViews();

		BluetoothEnabler.start(this);
		m_bleMngr = BleManager.get(getApplication(), m_bleManagerConfig);
		m_alertMngr = new AlertManager(this, m_bleMngr);
		m_viewController = new ViewController(this, m_bleMngr);
//		this.setContentView(m_viewController);
		ll.addView(m_viewController);

		if( !m_bleMngr.isBleSupported() )
		{
			m_alertMngr.showBleNotSupported();
		}
		else if( !m_bleMngr.is(BleManagerState.ON) )
		{
			m_bleMngr.turnOnWithIntent(this, BLE_TURN_ON_REQUEST_CODE);
		}
    }
	
    @Override protected void onResume()
    {
		super.onResume();
		m_bleMngr.onResume();
    }
    
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
		super.onActivityResult(requestCode, resultCode, data);
    }
    
    @Override protected void onPause()
    {
		super.onPause();
		m_bleMngr.onPause();
    }

	@Override protected void onDestroy(){
		m_bleMngr.disconnectAll();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		m_bleMngr.disconnectAll();
		m_bleMngr.disconnectAll_remote();
		m_bleMngr.stopScan();
//		m_bleMngr.reset();
		 super.onBackPressed();
	}


}
