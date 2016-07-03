package org.hva.createit.sensei.ui.sweetblue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.idevicesinc.sweetblue.BleManager;
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
public class ViewController extends LinearLayout
{
	public static enum State
	{
		DEVICE_LIST,
		DEVICE_DETAIL;
	}
	
	private final BleManager m_bleMngr;
	private final FrameLayout m_inner;
	
	private State m_state = null; 
	
	public ViewController(Context context, BleManager bleMngr)
	{
		super(context);

		m_bleMngr = bleMngr;

		this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		this.setOrientation(VERTICAL);

		BleBar bleStateBar = new BleBar(context, bleMngr);
		bleStateBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		bleStateBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
		this.addView(bleStateBar);

		m_inner = new FrameLayout(context);
		m_inner.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		this.addView(m_inner);
		
		setState(State.DEVICE_LIST);
	}
	
	public void setState(State state)
	{
		if( m_state != null )
		{
			m_inner.removeAllViews();
		}
		
		m_state = state;
		
		View newView = null;
		
		if( m_state == State.DEVICE_LIST )
		{
			newView = new DeviceList(getContext(), m_bleMngr);
		}
		else if( m_state == State.DEVICE_DETAIL )
		{
		}
		
		if( newView != null )
		{
			newView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
			m_inner.addView(newView);
		}
	}
}
