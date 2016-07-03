package org.hva.createit.sensei.ui.sweetblue;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.idevicesinc.sweetblue.BleManager;
import com.idevicesinc.sweetblue.BleManager.NativeStateListener;
import com.idevicesinc.sweetblue.BleManager.StateListener;
import com.idevicesinc.sweetblue.BleManagerState;
import com.idevicesinc.sweetblue.utils.State;
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
public class BleStatusBar extends FrameLayout implements BleManager.StateListener, BleManager.NativeStateListener
{
	private final BleManager m_bleMngr;

	private final TextView m_status;

	public BleStatusBar(Context context, BleManager bleMngr)
	{
		super(context);

		m_bleMngr = bleMngr;
		m_bleMngr.setListener_State(this);
		m_bleMngr.setListener_NativeState(this);

		LayoutInflater li = LayoutInflater.from(context);
		View inner = li.inflate(R.layout.ble_status_bar, null);

		m_status = (TextView) inner.findViewById(R.id.ble_status);

//		updateStatus();

		this.addView(inner);
	}

	private void updateStatus(StateEvent event)
	{
//		SpannableString status = Utils_String.makeStateString(BleManagerState.values(), m_bleMngr.getStateMask());
		String status = "";
		State[] states = BleManagerState.values();
		for(State state: states){
			if(state.overlaps(m_bleMngr.getStateMask())){
				status = state.name();
			}
		}

		m_status.setText(status);
//
//		status = Utils_String.makeStateString(BleManagerState.values(), m_bleMngr.getNativeStateMask());
//		m_nativeStatus.setText(status);
	}

	@Override public void onEvent(StateListener.StateEvent event)
	{
		updateStatus(event);
	}

	@Override public void onEvent(NativeStateListener.NativeStateEvent event)
	{
//		updateStatus(event);
	}
}
