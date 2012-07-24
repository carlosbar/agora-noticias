package com.innercalc.agora;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class ViewService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		// TODO Auto-generated method stub
	    return(new ViewProvider(this.getApplicationContext(),intent));
	}
}
