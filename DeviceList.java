package org.hva.createit.sensei.ui.sweetblue;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.idevicesinc.sweetblue.BleManager;

import org.hva.createit.scl.ble.GattAttributes;

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
public class DeviceList extends ScrollView implements BleManager.DiscoveryListener
{
	private static final int BASE_COLOR = 0xFFFAFAFA;
	private static final int LIGHT_ALPHA = 0x33000000;
	private static final int DARK_ALPHA = 0x44000000;

	private final LinearLayout m_list;
	private final BleManager m_bleMngr;

	public DeviceList(Context context, BleManager bleMngr)
	{
		super(context);

		m_bleMngr = bleMngr;
		m_bleMngr.setListener_Discovery(this);

		m_list = new LinearLayout(context);
		m_list.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		m_list.setOrientation(LinearLayout.VERTICAL);

//		if(m_bleMngr.getDevice() != null) {
//			if(m_bleMngr.getDevice().getName_normalized() != null) {
//				DeviceListEntry entry = new DeviceListEntry(getContext(), m_bleMngr.getDevice());
//				entry.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
//				m_list.addView(entry);
//			}
//		}

		this.addView(m_list);
	}

	private void colorList()
	{
		for( int i = 0; i < m_list.getChildCount(); i++ )
		{
			View ithView = m_list.getChildAt(i);

			int alphaMask = i%2 == 0 ? LIGHT_ALPHA : DARK_ALPHA;
			int color = BASE_COLOR | alphaMask;

			ithView.setBackgroundColor(color);
		}
	}

	@Override public void onEvent(DiscoveryEvent event)
	{
		if( event.was(LifeCycle.DISCOVERED) || event.was(LifeCycle.REDISCOVERED ))
		{
			DeviceListEntry entry = new DeviceListEntry(getContext(), event.device());
			entry.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

			for(int i = 0; i < m_list.getChildCount(); i++){
				DeviceListEntry oldEntry = (DeviceListEntry) m_list.getChildAt(i);
				if(oldEntry.getDevice().equals(entry.getDevice())){
					return;
				}
			}

            UUID[] services = entry.getDevice().getAdvertisedServices();
            for(UUID service: services){
				Log.d(getClass().getSimpleName(), "Found service for device "+ entry.getDevice().getMacAddress() + " " + service.toString());
                if(service.compareTo(UUID.fromString(GattAttributes.HEART_RATE_SERVICE)) == 0){
                    m_list.addView(entry);
                }
            }

			colorList();
		}
		else if( event.was(LifeCycle.UNDISCOVERED) )
		{
			for( int i = 0; i < m_list.getChildCount(); i++ )
			{
				DeviceListEntry entry = (DeviceListEntry) m_list.getChildAt(i);

				if( entry.getDevice().equals(event.device()) )
				{
					m_list.removeViewAt(i);
					colorList();

					return;
				}
			}
		}
	}
}
