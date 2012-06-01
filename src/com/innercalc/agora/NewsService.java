package com.innercalc.agora;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class NewsService extends RemoteViewsService {

	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		// TODO Auto-generated method stub
	    return(new NewsView(this.getApplicationContext(),intent));
	}

}
