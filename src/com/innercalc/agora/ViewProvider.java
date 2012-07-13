/***
Copyright (c) 2008-2012 CommonsWare, LLC
Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
by applicable law or agreed to in writing, software distributed under the
License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS
OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.

From _The Busy Coder's Guide to Advanced Android Development_
  http://commonsware.com/AdvAndroid
*/

package com.innercalc.agora;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

public class ViewProvider implements RemoteViewsService.RemoteViewsFactory {
	final	public	static	int							MAXFEEDS_THIRTY	= 30;
	final	public	static	int							MAXFEEDS_FIFTEEN= 15;
	final	public	static	int							MAXFEEDS_TEN	= 10;
	final	public	static	int							THREE_MINUTES	= 3;
	final	public	static	int							FIVE_MINUTES	= 5;
	final	public	static	int							TEN_MINUTES		= 10;
	final	public	static	int							TWENTY_MINUTES	= 20;
	final	public	static	int							THIRTY_MINUTES	= 30;
			private 		LinkedList<RssChannel>		channelList		= new LinkedList<RssChannel>();
			private			RssChannel					currentChannel	= null;
			private			String						className		= "";
	
	private Context ctxt=null;
	private int appWidgetId;

	public void onCreate() {
	}
	
	public void onDestroy() {
		// TODO Auto-generated method stub
	}
	public void onDataSetChanged() {
		SharedPreferences prefs = ctxt.getSharedPreferences(MyWidget.PREFS_DB, 0);
		int page = prefs.getInt("changePage",0); 
		if(page != 0) {
			SharedPreferences.Editor prefset = prefs.edit();
			prefset.putInt("changePage",0);
			prefset.commit();
			switch(page) {
			case -1:
				currentChannel = (currentChannel != null && channelList.size() > 0) ? (currentChannel.getPosition()-1 < 0 || currentChannel.getPosition()-1 >= channelList.size()) ? channelList.getLast() : channelList.get(currentChannel.getPosition() - 1) : null;
				break;
			default:
				currentChannel = (currentChannel != null && channelList.size() > 0) ? (currentChannel.getPosition()+1 >= channelList.size()) ? channelList.getFirst() : channelList.get(currentChannel.getPosition()+1) : null;
				break;
			}
		}
		updateCurrentChannel();
		if(page == 0) {
			updateList();
		} else {
			RemoteViews views=new RemoteViews(ctxt.getPackageName(),R.layout.main);
			views.setScrollPosition(R.id.newsList,0);
			AppWidgetManager.getInstance(ctxt).notifyAppWidgetViewDataChanged(appWidgetId,R.id.newsList);
		}
	}

	/* update current channel */
	public void updateCurrentChannel() {
		RssChannel channel = currentChannel;
		RemoteViews views=new RemoteViews(ctxt.getPackageName(),R.layout.main);
		
		if(channel != null) {
			views.setTextViewText(R.id.channel,channel.getTitle());
			views.setTextViewText(R.id.update,ctxt.getString(R.string.lastUpdate) + ": " + channel.getUpdate());
		} else {
			try {
				views.setTextViewText(R.id.channel,ctxt.getString(R.string.app_name) + "-" + ctxt.getPackageManager().getPackageInfo(ctxt.getPackageName(), 0).versionName);
			} catch (Exception e) {
			}
			views.setTextViewText(R.id.update,ctxt.getString(R.string.updating));
		}
		try {
			Class<?> updtClass = Class.forName(className);
			Log.d("Concrete Class Name",updtClass.getCanonicalName());
			ComponentName cn = new ComponentName(ctxt,updtClass);
			AppWidgetManager.getInstance(ctxt).updateAppWidget(cn,views);
		} catch (Exception e) {
			AppWidgetManager.getInstance(ctxt).updateAppWidget(appWidgetId,views);
		}
	}
	
	/* update channel info */
	private void updateList() {
		final	LinkedList<RssChannel> list = new LinkedList<RssChannel>();
		final	SharedPreferences prefs = ctxt.getSharedPreferences(MyWidget.PREFS_DB, 0);
		int		updatedChannel = 0;

		try {
			String [] rss = prefs.getString("rssfeed","").split("[|]");
			int maxFeeds = prefs.getInt("maxFeeds",MAXFEEDS_THIRTY);
			for(int c=0;c < rss.length;c++) {
				RssChannel channel = new RssChannel(c);
	    		list.add(channel);
				/* current date & time */
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
				channel.setUpdate(sdf.format(new Date(System.currentTimeMillis())));
				channel.setLink(rss[c]);
	    		/* get document */
				DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = builderFactory.newDocumentBuilder();
				Document document;
				Element root;
				try {
					document = builder.parse(rss[c]);
					root = document.getDocumentElement();
					updatedChannel++;
				} catch (Exception e) {
					channel.setTitle(rss[c]);
					channel.addItem(ctxt.getString(R.string.updating),"","");
					continue;
				}
				NodeList items = root.getElementsByTagName("channel");
				for(int x=0;items != null && x < items.getLength();x++) {
					NodeList item = items.item(x).getChildNodes();
					for(int y=0;item != null && y < item.getLength();y++) {
						Node n = item.item(y);
						
						if(n == null || n.getNodeName() == null || n.getFirstChild() == null) {
							continue;
						}
						if(n.getNodeName().equals("title")) {
							if(n.getFirstChild().getNodeValue() != null) {
								channel.setTitle(n.getFirstChild().getNodeValue());
							}
							break;
						}
					}
				}
				items = root.getElementsByTagName("item");
				for(int x=0;items != null && x < items.getLength() && x < maxFeeds;x++) {
					String itemtitle=null,itemlink=null,itempublished=null;
					NodeList item = items.item(x).getChildNodes();
					
					for(int y = 0;item != null && y < item.getLength() ;y++) {
						Node n = item.item(y);
						if(n == null || n.getNodeName() == null || n.getFirstChild() == null) {
							continue;
						}
						if(n.getNodeName().equals("title")) {
							if(n.getFirstChild().getNodeValue() != null) itemtitle = n.getFirstChild().getNodeValue();
						} else if(n.getNodeName().equals("link")) {
							if(n.getFirstChild().getNodeValue() != null) itemlink = n.getFirstChild().getNodeValue();
						} else if(n.getNodeName().equals("pubDate")) {
							if(n.getFirstChild().getNodeValue() != null) itempublished = n.getFirstChild().getNodeValue();
						}
					}
					channel.addItem(itemtitle, itemlink, itempublished);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		/* create interval update */
		Intent update = new Intent(MyWidget.START_WIDGET);
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		PendingIntent alarmIntent = PendingIntent.getBroadcast(ctxt, 0, update,PendingIntent.FLAG_CANCEL_CURRENT);
		Calendar ct = Calendar.getInstance();
		ct.setTimeInMillis(System.currentTimeMillis());
		if(updatedChannel > 0) {
			ct.add(Calendar.MINUTE,prefs.getInt("updateTimeout",30) > FIVE_MINUTES ? prefs.getInt("updateTimeout",30) : FIVE_MINUTES);
			/* update channels */
			channelList=list;
			currentChannel = (list.isEmpty()) ? null : list.getFirst();
			updateCurrentChannel();
		} else {
			ct.add(Calendar.MINUTE,FIVE_MINUTES);
		}
		AlarmManager alarmManager = (AlarmManager) ctxt.getSystemService(Context.ALARM_SERVICE);
		alarmManager.set(AlarmManager.RTC, ct.getTimeInMillis(),alarmIntent);
	}
	
	public ViewProvider(Context ctxt, Intent intent) {
	    this.ctxt=ctxt;
	    appWidgetId=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
	    className=intent.getStringExtra("className");
		updateCurrentChannel();
	    updateList();
	}
	
	public int getCount() {
		RssChannel channel = currentChannel;
		
		Log.d("teste",(channel == null) ? "channel is null" : channel.getTitle()+":"+channel.getList().size());
		return((channel != null) ? channel.getList().size() : 0);
	}

	public RemoteViews getLoadingView() {
		RemoteViews row=new RemoteViews(ctxt.getPackageName(),R.layout.row);
		row.setTextViewText(android.R.id.text1,"");
		return(row);
	}
	
	public RemoteViews getViewAt(int position) {
		RemoteViews row=new RemoteViews(ctxt.getPackageName(),R.layout.row);
		RssChannel channel = currentChannel;
		String text = "", link = "";
		
		if(channel != null && position < channel.getList().size()) {
			text = channel.getList().get(position).getTitle();
			link = channel.getList().get(position).getLink();
		}
		row.setTextViewText(android.R.id.text1,text);
		Intent i=new Intent();
		Bundle extras=new Bundle();
		extras.putString("itemLink", link);
		i.putExtras(extras);
		row.setOnClickFillInIntent(android.R.id.text1, i);
		return(row);
	}
	
	public int getViewTypeCount() {
		return 1;
	}

	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	public boolean hasStableIds() {
		return true;
	}
	
	/** class to store rss items */
	
	public class RssItem {
		private String	title;
		private String	link;
		private boolean	clicked = false;
		private String	published;
		
		public RssItem(String title,String link,String published) {
			this.title = title;
			this.link = link;
			this.published = published;
		}
		public RssItem() {
			this.title = "";
			this.link = "";
		}
		public String getTitle() {
			return (this.title == null) ? "" : this.title;
		}
		public String getLink() {
			return (this.link == null) ? "" : this.link;
		}
		public String getPublished() {
			return (this.published == null) ? "" : this.published;
		}
		public void setClicked() {
			this.clicked = true;
		}
		public boolean getClicked() {
			return this.clicked;
		}
	}
	
	/** class to store rss channels */
	
	public class RssChannel {
		private	LinkedList<RssItem>	list = new LinkedList<RssItem>();
		private	String				title;
		private String				link;
		private String				update;
		private	int					position;
		
		public RssChannel(int position) {
			this.position = position;
		}
		public RssItem addItem(String title,String link,String published) {
			RssItem item = new RssItem(title,link,published);
			this.list.add(item);
			return item;
		}
		public void setRead(String title,String published) {
			for(int x=0;x < list.size();x++) {
				if(list.get(x).getTitle().equals(title) && list.get(x).getPublished().equals(published)) {
					list.get(x).setClicked();
					break;
				}
			}
		}
		public void setLink(String link) {
			this.link = link;
		}
		public String getLink() {
			return (this.link == null) ? "" : this.link;
		}
		public int getPosition() {
			return(this.position);
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public String getTitle() {
			return (this.title == null) ? "" : this.title;
		}
		public void setUpdate(String update) {
			this.update = update;
		}
		public String getUpdate() {
			return this.update;
		}
		public LinkedList<RssItem> getList() {
			return this.list;
		}
	}
}