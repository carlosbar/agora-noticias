package com.innercalc.agora;

import com.innercalc.agora.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

public abstract class MyWidget extends AppWidgetProvider {
	final	public	static	int						HTTP_PORT			= 80;
	final	public	static	String					PREFS_DB			= "com.innercalc.agora.dbconf";
	final	public	static	String					START_WIDGET		= "com.innercalc.agora.START_WIDGET";
	final	public	static	String					CHANGE_TEXT			= "com.innercalc.agora.CHANGE_TEXT";
	final	public	static	String					UPDATE_RSS			= "com.innercalc.agora.UPDATE_RSS";
	final	public	static	String					CHANGE_PAGE			= "com.innercalc.agora.CHANGE_PAGE";
	final	public	static	String					OPEN_LINK			= "com.innercalc.agora.OPEN_LINK";
	final	public	static	String					CONNECTION_ERROR	= "com.innercalc.agora.CONNECTION_ERROR";
	
	@Override
	public void onReceive(final Context context, final Intent intent) {
		super.onReceive(context, intent);
		final int	mainLayout = (this.getClass().getName().endsWith("Transp")) ?  R.layout.main_transp : R.layout.main;

		Log.d("onReceive",intent.getAction());
		
		if(MyWidget.UPDATE_RSS.equals(intent.getAction())) {
			if(!isMyServiceRunning(context)) {
				Log.d("onReceive",ViewService.class.getName() + " is not runnig");
				createService(context,mainLayout);
			}
			SharedPreferences prefs = context.getSharedPreferences(MyWidget.PREFS_DB, 0);
			SharedPreferences.Editor prefset = prefs.edit();
			prefset.putBoolean("updateRSS",true);
			prefset.commit();
			RemoteViews widget = new RemoteViews(context.getPackageName (), mainLayout);
			ComponentName cn = new ComponentName(context, this.getClass());
			int [] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(cn);
			AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(ids,R.id.newsList);
			AppWidgetManager.getInstance(context).updateAppWidget(cn,widget);
		} else if(MyWidget.CHANGE_PAGE.equals(intent.getAction())) {
			if(!isMyServiceRunning(context)) {
				Log.d("onReceive",ViewService.class.getName() + " is not runnig");
				createService(context,mainLayout);
			}
			SharedPreferences prefs = context.getSharedPreferences(MyWidget.PREFS_DB, 0);
			SharedPreferences.Editor prefset = prefs.edit();
			prefset.putInt("changePage",intent.getIntExtra("changePage",0));
			prefset.commit();
			RemoteViews widget = new RemoteViews(context.getPackageName (), mainLayout);
			ComponentName cn = new ComponentName(context, this.getClass());
			int [] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(cn);
			AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(ids,R.id.newsList);
			AppWidgetManager.getInstance(context).updateAppWidget(cn,widget);
		} else if(MyWidget.OPEN_LINK.equals(intent.getAction())) {
			Thread t = new Thread(new Runnable() {
				public void run() {
					/* start activity */
					if(intent.getStringExtra("itemLink") != null && intent.getStringExtra("itemLink").length() > 0) {
						if(intent.getStringExtra("itemLink").equals(MyWidget.CONNECTION_ERROR)) {
							SharedPreferences prefs = context.getSharedPreferences(MyWidget.PREFS_DB, 0);
							SharedPreferences.Editor prefset = prefs.edit();
							prefset.putBoolean("fullUpdate",true);
							prefset.commit();
							RemoteViews widget = new RemoteViews(context.getPackageName (), mainLayout);
							ComponentName cn = new ComponentName(context, this.getClass());
							int [] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(cn);
							AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(ids,R.id.newsList);
							AppWidgetManager.getInstance(context).updateAppWidget(cn,widget);
						} else {
							Intent linkIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(intent.getStringExtra("itemLink")));
							linkIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							context.startActivity(linkIntent);
						}
					}
				}
			});
			t.start();
		} else if(MyWidget.START_WIDGET.equals(intent.getAction()) || AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(intent.getAction())) {
			/* intent to start service that will control the listview */
			if(!isMyServiceRunning(context)) {
				Log.d("onReceive","creating " +ViewService.class.getName());
				createService(context,mainLayout);
			}
		}
	}

	/**
	 * verify if service is running
	 * @return
	 */
	private boolean isMyServiceRunning(Context context) {
	    ActivityManager manager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (ViewService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}	

	private void createService(Context context, int mainLayout) {
		Intent svcIntent=new Intent(context,ViewService.class);
		svcIntent.putExtra("className",this.getClass().getName());
		svcIntent.putExtra("layoutId",mainLayout);
		svcIntent.setData(Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME)));
		/* update listview */
		RemoteViews widget = new RemoteViews(context.getPackageName (), mainLayout);
		ComponentName cn = new ComponentName(context, this.getClass());
		int [] ids = AppWidgetManager.getInstance(context).getAppWidgetIds(cn);
		for(int i=0; i < ids.length; i++) {
			/* update channels */
			Intent upd = new Intent(context, this.getClass());
			upd.setAction(MyWidget.UPDATE_RSS);
			upd.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ids[i]);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, upd, PendingIntent.FLAG_UPDATE_CURRENT);
			widget.setOnClickPendingIntent(R.id.updateChannels, pendingIntent);
			/* open link */
			Intent click = new Intent(context,this.getClass());
			click.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ids[i]);
			click.setAction(MyWidget.OPEN_LINK);
			PendingIntent pending = PendingIntent.getBroadcast(context, 0, click, PendingIntent.FLAG_UPDATE_CURRENT);
			widget.setPendingIntentTemplate(R.id.newsList,pending);
			/* configure */
			Intent config = new Intent(context,Configure.class);
			config.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ids[i]);
			PendingIntent pconfig = PendingIntent.getActivity(context, 0, config, PendingIntent.FLAG_UPDATE_CURRENT);
			widget.setOnClickPendingIntent(R.id.config,pconfig);
			/* page left */
			Intent pageLeft = new Intent(context,this.getClass());
			pageLeft.setAction(MyWidget.CHANGE_PAGE);
			pageLeft.putExtra("changePage",-1);
			PendingIntent pChangeLeft = PendingIntent.getBroadcast(context, 0, pageLeft, PendingIntent.FLAG_UPDATE_CURRENT);
			widget.setOnClickPendingIntent(R.id.channelLeft, pChangeLeft);
			/* page right */
			Intent pageRight = new Intent(context,this.getClass());
			pageRight.setAction(MyWidget.CHANGE_PAGE);
			pageRight.putExtra("changePage",1);
			PendingIntent pChangeRight = PendingIntent.getBroadcast(context, 1, pageRight, PendingIntent.FLAG_UPDATE_CURRENT);
			widget.setOnClickPendingIntent(R.id.channelRight, pChangeRight);
			/* remote adapter */
			widget.setRemoteAdapter(ids[i],R.id.newsList,svcIntent);
		}
		AppWidgetManager.getInstance(context).notifyAppWidgetViewDataChanged(ids,R.id.newsList);
		AppWidgetManager.getInstance(context).updateAppWidget(cn,widget);
		
	}
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// TODO Auto-generated method stub
		int	mainLayout = (this.getClass().getName().endsWith("Transp")) ?  R.layout.main_transp : R.layout.main;

		SharedPreferences prefs = context.getSharedPreferences(MyWidget.PREFS_DB, 0);
		SharedPreferences.Editor prefset = prefs.edit();
		prefset.putBoolean("fullUpdate",true);
		prefset.commit();
		
		for(int i=0; i<appWidgetIds.length; i++) {
			RemoteViews widget=new RemoteViews(context.getPackageName(),mainLayout);
			appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds[i],R.id.newsList);
			appWidgetManager.updateAppWidget(appWidgetIds[i], widget);
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
}
