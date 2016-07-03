package org.hva.createit.sensei.ui.sweetblue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.idevicesinc.sweetblue.BleManager;
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
public class BleBar extends LinearLayout
{
	private static final Interval SCAN_TIMEOUT = Interval.secs(30.0);

	private final Button m_nuke;
	private final Button m_infiniteScan;
	private final Button m_stop;
	
	private final BleManager m_bleMngr;
	
	public BleBar(Context context, BleManager bleMngr)
	{
		super(context);
		
		m_bleMngr = bleMngr;
		
		setOrientation(VERTICAL);
		int padding = context.getResources().getDimensionPixelSize(R.dimen.default_padding);
		this.setPadding(padding, padding, padding, padding);
		
		LayoutInflater li = LayoutInflater.from(context);
		View inner = li.inflate(R.layout.ble_button_bar, null);
		
			m_nuke = (Button) inner.findViewById(R.id.ble_nuke_button);
		m_nuke.setOnClickListener(new OnClickListener()
		{	
			@Override public void onClick(View v)
			{
				m_bleMngr.reset();
			}
		});
		
		m_infiniteScan = (Button) inner.findViewById(R.id.scan_infinite_button);
		m_stop = (Button) inner.findViewById(R.id.scan_stop_button);
		m_stop.setVisibility(View.GONE);
		
		m_infiniteScan.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				m_bleMngr.startScan(SCAN_TIMEOUT);
				((ViewController) BleBar.this.getParent()).setState(ViewController.State.DEVICE_LIST);
				m_infiniteScan.setVisibility(View.GONE);
				m_stop.setVisibility(View.VISIBLE);
			}
		});
		
		m_stop.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				//--- DRK > Catch-all, stop both periodic and manual scanning.
				m_bleMngr.stopPeriodicScan();
				m_bleMngr.stopScan();
				m_infiniteScan.setVisibility(View.VISIBLE);
				m_stop.setVisibility(View.GONE);
			}
		});

		BleStatusBar scanBar = new BleStatusBar(context, bleMngr);
		scanBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		this.addView(scanBar);

		this.addView(inner);
		

	}
}
