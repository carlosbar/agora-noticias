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
import java.util.concurrent.Semaphore;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.innercalc.agora.Channel.ChannelList;
import com.innercalc.agora.Channel.LocalChannel;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.Toast;

public class ViewProvider implements RemoteViewsService.RemoteViewsFactory {

	/* static */
	
	public final static int							MAXFEEDS_THIRTY	= 30;
	public final static	int							MAXFEEDS_FIFTEEN= 15;
	public final static	int							MAXFEEDS_TEN	= 10;
	public final static	int							THREE_MINUTES	= 3;
	public final static	int							FIVE_MINUTES	= 5;
	public final static	int							TEN_MINUTES		= 10;
	public final static	int							TWENTY_MINUTES	= 20;
	public final static	int							THIRTY_MINUTES	= 30;

	/* private */
	
	private	final		Semaphore					semaphore		= new Semaphore(1, true);
	private				String						className		= new String();
	private				int							layoutId		= R.layout.main;
	private				int							textColor;
	private				int							updateTimeout;
	private				LinkedList<RssChannel>		channelList;
	private				DatabaseHandler				db;
	private				Handler						handler;
	private				Context						ctxt;
	private				int							appWidgetId;
	private				int							idCurrentChannel;
	private				Thread						updateThread;

	/**
	 * message handler to receive messages from update thread
	 */
	Callback callback = new Callback() {
		public boolean handleMessage(Message msg) {
			if(msg.what == 0) {
				Log.d("callback","received update message");
				parseDB();
				/* send a broadcast so the widget will request a refresh in the list */
				Intent intent = new Intent(MyWidget.CHANGE_PAGE);
				intent.putExtra("changePage",0);
				ctxt.sendBroadcast(intent);
			}
			return false;
		}
	};

	/**
	 * get text color according to color code
	 * @param code color code defined in Configure.java
	 * @return the system color code
	 */
	public int getTextColor(int code) {
		int color = Color.LTGRAY;

		switch(code) {
		case 0:
			color = Color.LTGRAY;
			break;
		case 1:
			color = Color.DKGRAY;
			break;
		case 2:
			color = Color.WHITE;
			break;
		case 3:
			color = Color.BLACK;
			break;
		}
		return color;
	}
	
	/**
	 * retrieve database information to generate channel list
	 */
	public void parseDB() {
		SharedPreferences		prefs	= ctxt.getSharedPreferences(MyWidget.PREFS_DB, 0);
		LinkedList<RssChannel>	list	= new LinkedList<RssChannel>();
		ChannelList				clist	= ChannelList.fromJSONString(prefs.getString("rssfeed",""));

		Log.d("parseDB","updating items list");
		/* retrieve channels from database */
		for(LocalChannel lc : clist) {
			RssChannel dbchannel = db.getChannel(lc.url,list.size());
			if(dbchannel != null) {
	    		list.add(dbchannel);
			}
		}
		try {
			semaphore.acquire();
			channelList = list;
			semaphore.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
			channelList = list;
		}
	}
	
	/**
	 * retrieve the channel by its id
	 * @param id	channel id or -1 to retrieve last channel
	 * @return a channel instance
	 */
	public RssChannel getChannel(int id)
	{
		RssChannel					channel = null;
		LinkedList<RssChannel>		list;

		try {
			semaphore.acquire();
			list = channelList;
			semaphore.release();
		} catch(InterruptedException e) {
			e.printStackTrace();
			list = channelList;
		}
		/* did we get channels from database? */
		if(list.size() > 0) {
			/* if id == -1, get last, if id is invalid, get first */
			channel = (id == -1) ? list.get(list.size()-1) : (id >= list.size()) ? list.get(0) : list.get(id);
			if(channel.getPosition()+1 == list.size()) channel.setLastChannel(true);
		}
		return channel;
	}
	
	/**
	 * called whenever the list have received an update
	 */
	public void onDataSetChanged() {
		SharedPreferences 			prefs	= ctxt.getSharedPreferences(MyWidget.PREFS_DB, 0);
		SharedPreferences.Editor	prefset = prefs.edit();
		RssChannel					channel = getChannel(idCurrentChannel);

		/* is this a background update? */
		if(prefs.getBoolean("updateRSS",false)) {
			prefset.putBoolean("updateRSS",false);
			prefset.commit();
			bgUpdate();
			return;
		} else if(prefs.getInt("updateTimeout",30) != updateTimeout) {
			if(prefs.getInt("updateTimeout",30) < updateTimeout) {
				bgUpdate();
			}
			updateTimeout = prefs.getInt("updateTimeout",30);
		}
		/* are we changing pages? */
		int page = prefs.getInt("changePage",0); 
		Log.d("onDataSetChanged","Current Channel:" + ((channel != null) ? channel.toString() : "null"));
		if(page != 0) {
			if(channel != null) {
				if(page < 0) {
					channel=(channel.getPosition() == 0) ? getChannel(-1) : getChannel(channel.getPosition()-1); 
				} else {
					channel=(channel.isLastChannel()) ? getChannel(0) : getChannel(channel.getPosition()+1); 
				}
			}
			idCurrentChannel = (channel == null) ? 0 : channel.getPosition();
			Log.d("onDataSetChanged",(channel != null) ? "Channel set to:"+channel.toString() : "No channel available");
			prefset.putInt("changePage",0);
			prefset.putInt("currentPageId",idCurrentChannel);
			prefset.commit();
		}
		textColor = prefs.getInt("textColor",textColor);
		/* update current channel */
		updateCurrentChannel();
		if(page != 0) {
			RemoteViews views=new RemoteViews(ctxt.getPackageName(),layoutId);
			views.setScrollPosition(R.id.newsList,0);
			AppWidgetManager.getInstance(ctxt).notifyAppWidgetViewDataChanged(appWidgetId,R.id.newsList);
		}
	}

	/**
	 *  update current channel
	 */
	public void updateCurrentChannel() {
		RssChannel channel;
		RemoteViews views=new RemoteViews(ctxt.getPackageName(),layoutId);

		channel = getChannel(idCurrentChannel);
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
		views.setTextColor(R.id.channel,getTextColor(textColor));
		views.setTextColor(R.id.update,getTextColor(textColor));
		try {
			Class<?> updtClass = Class.forName(className);
			Log.d("Concrete Class Name",updtClass.getCanonicalName());
			ComponentName cn = new ComponentName(ctxt,updtClass);
			AppWidgetManager.getInstance(ctxt).updateAppWidget(cn,views);
		} catch (Exception e) {
			e.printStackTrace();
			AppWidgetManager.getInstance(ctxt).updateAppWidget(appWidgetId,views);
		}
	}

	/**
	 * background update channels
	 */
	public void bgUpdate() {

		try {
			semaphore.acquire();
			if(updateThread != null) {
				semaphore.release();
				Log.d("bgUpdate","update already in process");
				return;
			}
			updateThread = new Thread(updateFunction);
			semaphore.release();
			updateThread.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * runnable to execute background update
	 */
	private Runnable updateFunction = new Thread(new Runnable() {
		public void run() {
			int						totalUpd	= 0;
			SharedPreferences		prefs		= ctxt.getSharedPreferences(MyWidget.PREFS_DB, 0);
			int						maxFeeds	= prefs.getInt("maxFeeds",MAXFEEDS_THIRTY);
			ChannelList				clist		= ChannelList.fromJSONString(prefs.getString("rssfeed",""));

			Log.d("updateFunction","Starting update for " + clist.size() + " channels");
			for(int c=0;c < clist.size();c++) {
				RssChannel				channel = new RssChannel(c);
				DocumentBuilderFactory	builderFactory;
				DocumentBuilder			builder;
				Element					root;
				Document				document;

				Log.d("updateFunction","channel: " + clist.get(c).name + "-" + clist.get(c).url);
				channel.setLink(clist.get(c).url);
				channel.setTitle(clist.get(c).name);
				try {
					builderFactory = DocumentBuilderFactory.newInstance();
					builder = builderFactory.newDocumentBuilder();
					document = builder.parse(clist.get(c).url);
					root = document.getDocumentElement();
				} catch (Exception e) {
					Log.d("updateFunction",e.getMessage());
					continue;
				}
				totalUpd++;
				SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
				channel.setUpdate(sdf.format(new Date(System.currentTimeMillis())));
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
								channel.setChannelTitle(n.getFirstChild().getNodeValue());
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
				db.addChannel(channel);
			}
			/* create interval update */
			Intent update = new Intent(MyWidget.UPDATE_RSS);
			update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
			PendingIntent alarmIntent = PendingIntent.getBroadcast(ctxt, 0, update,PendingIntent.FLAG_CANCEL_CURRENT);
			Calendar ct = Calendar.getInstance();
			ct.setTimeInMillis(System.currentTimeMillis());
			if(totalUpd > 0) {
				ct.add(Calendar.MINUTE,prefs.getInt("updateTimeout",30) > FIVE_MINUTES ? prefs.getInt("updateTimeout",30) : FIVE_MINUTES);
			} else {
				ct.add(Calendar.MINUTE,FIVE_MINUTES);
			}
			AlarmManager alarmManager = (AlarmManager) ctxt.getSystemService(Context.ALARM_SERVICE);
			alarmManager.set(AlarmManager.RTC, ct.getTimeInMillis(),alarmIntent);
			try {
				semaphore.acquire();
				updateThread = null;
				semaphore.release();
			} catch(InterruptedException e) {
				e.printStackTrace();
				updateThread = null;
			}
			Log.d("updateFunction","update done");
			handler.sendEmptyMessage(0);
		}
	});
	
	public ViewProvider(Context ctxt, Intent intent) {
		SharedPreferences 		prefs	= ctxt.getSharedPreferences(MyWidget.PREFS_DB, 0);

		this.ctxt=ctxt;
	    appWidgetId=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
	    className=intent.getStringExtra("className");
	    layoutId = intent.getIntExtra("layoutId",layoutId);
	    updateTimeout = prefs.getInt("updateTimeout",30);
		textColor = prefs.getInt("textColor",getTextColor(textColor));
		db = new DatabaseHandler(ctxt);
		handler = new Handler(callback);
		idCurrentChannel = prefs.getInt("currentPageId",idCurrentChannel);
		parseDB();
		bgUpdate();
	}
	
	public int getCount() {
		RssChannel channel;
		
		channel = getChannel(idCurrentChannel);
		Log.d("teste",(channel == null) ? "channel is null" : channel.getTitle()+":"+channel.getList().size());
		return((channel != null) ? channel.getList().size() : 0);
	}

	public RemoteViews getLoadingView() {
		RemoteViews row=new RemoteViews(ctxt.getPackageName(),R.layout.row);
		row.setTextViewText(android.R.id.text1,"");
		row.setTextColor(android.R.id.text1, getTextColor(textColor));
		return(row);
	}
	
	public RemoteViews getViewAt(int position) {
		RemoteViews row=new RemoteViews(ctxt.getPackageName(),R.layout.row);
		RssChannel channel;
		String text = "", link = "";
		
		channel = getChannel(idCurrentChannel);
		if(channel != null && position < channel.getList().size()) {
			text = channel.getList().get(position).getTitle();
			link = channel.getList().get(position).getLink();
		}
		row.setTextViewText(android.R.id.text1,text);
		row.setTextColor(android.R.id.text1, getTextColor(textColor));
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
		private	String				title="";
		private String				link="";
		private String				update="";
		private	int					position=0;
		private	String				channelTitle="";
		private	boolean				lastchannel;
		
		public String toString() {
			return this.position + ":" + this.title + ":" + this.link + ":" + this.update + ":" + this.channelTitle;
		}
		
		public RssChannel(int position) {
			this.position = position;
		}
		public RssChannel(int position, String title, String link, String update)
		{
			this.position = position;
			this.title = title;
			this.link = link;
			this.update = update;
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
		public void setChannelTitle(String title) {
			this.channelTitle = title;
		}
		public String getChannelTitle() {
			return this.channelTitle;
		}
		public void setLastChannel(boolean b) {
			this.lastchannel = b;
		}
		public boolean isLastChannel() {
			return this.lastchannel;
		}
	}

	/**
	 * retrieve hash from the given string
	 * @param str string to calculate hash
	 * @return calculated hash string
	 */
	public String getHash(String str) {
		byte [] plain = str.getBytes();

		try {
			plain = str.getBytes("UTF-8");
		} catch (Exception e) {
			plain = str.getBytes();
		}
		Checksum c = new CRC32();
		c.update(plain,0,plain.length);
		return Long.toHexString(c.getValue());
	}
	
	public class DatabaseHandler extends SQLiteOpenHelper {
	    private static final int		DATABASE_VERSION			= 4;
	    private static final String		DATABASE_NAME				= "newsdb";
	    private static final String		TABLE_RSSCHANNEL			= "channel";
	    private static final String		TABLE_RSSCHANNEL_ID			= "id";
	    private static final String		TABLE_RSSCHANNEL_MD5		= "md5";
	    private static final String		TABLE_RSSCHANNEL_TITLE		= "title";
	    private static final String		TABLE_RSSCHANNEL_LINK		= "link";
	    private static final String		TABLE_RSSCHANNEL_UPDATE		= "upd";
	    private static final String		TABLE_RSSITEM				= "item";
	    private static final String		TABLE_RSSITEM_ID			= "id";
	    private static final String		TABLE_RSSITEM_TITLE			= "title";
	    private static final String		TABLE_RSSITEM_LINK			= "link";
	    private static final String		TABLE_RSSITEM_PUBLISHED		= "published";
	    private static final String		TABLE_RSSITEM_CHANNELMD5	= "channelmd5";
	 
	    public DatabaseHandler(Context context) {
	        super(context, DATABASE_NAME, null, DATABASE_VERSION);
	    }

		@Override
		public void onCreate(SQLiteDatabase db) {
	    	String createChannel = "CREATE TABLE IF NOT EXISTS " + TABLE_RSSCHANNEL + " (" + TABLE_RSSCHANNEL_ID + " INT, " + TABLE_RSSCHANNEL_TITLE +" TEXT, " + TABLE_RSSCHANNEL_LINK + " TEXT, " + TABLE_RSSCHANNEL_UPDATE + " TEXT, " + TABLE_RSSCHANNEL_MD5 + " TEXT);";
	    	String createItems = "CREATE TABLE IF NOT EXISTS " + TABLE_RSSITEM + " (" + TABLE_RSSITEM_ID + " INT, " + TABLE_RSSITEM_TITLE + " TEXT, " + TABLE_RSSITEM_LINK + " TEXT, " + TABLE_RSSITEM_PUBLISHED + " TEXT, " + TABLE_RSSITEM_CHANNELMD5 + " TEXT, PRIMARY KEY("+TABLE_RSSITEM_CHANNELMD5+","+ TABLE_RSSITEM_ID +"));";
	    	Log.d("onCreate",createChannel);
	    	Log.d("onCreate",createItems);
	        db.execSQL(createChannel);
	        db.execSQL(createItems);
		}

		/**
		 * executed when the database if being upgraded
		 */
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.d("onUpgrade","Upgrading databases");
	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RSSCHANNEL);
	        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RSSITEM);
	        onCreate(db);
		}
		
		/**
		 * add a new channel into the database
		 * @param id		channel id
		 * @param title		channel title
		 * @param link		channel link
		 * @param update	channel last date&time update
		 */
		public void addChannel(RssChannel channel) {
		    SQLiteDatabase db = this.getWritableDatabase();

		    Log.d("addChannel","Adding channel "+channel.getPosition() + ":" + channel.getTitle()+":"+getHash(channel.getLink()));
		    /* delete old channel */
	    	db.delete(TABLE_RSSCHANNEL, TABLE_RSSCHANNEL_LINK + " = ?",new String[] {channel.getLink()});
	    	db.delete(TABLE_RSSITEM, TABLE_RSSITEM_CHANNELMD5 + " = ?",new String[] {getHash(channel.getLink())});
		    /* get database connection */
		    ContentValues values = new ContentValues();
		    /* prepare channel columns to insert into table */
		    Log.d("addChannel",channel.getPosition()+"-"+channel.getTitle()+"-"+channel.getLink()+"-"+channel.getUpdate()+"-"+channel.getList().size());
		    values.put(TABLE_RSSCHANNEL_ID,channel.getPosition());
		    values.put(TABLE_RSSCHANNEL_MD5,getHash(channel.getLink()));
		    values.put(TABLE_RSSCHANNEL_TITLE,channel.getTitle());
		    values.put(TABLE_RSSCHANNEL_LINK,channel.getLink());
		    values.put(TABLE_RSSCHANNEL_UPDATE,channel.getUpdate());
		    /* insert into table */
		    db.insert(TABLE_RSSCHANNEL, null, values);
		    /* now add items into channel */
		    for(int x=0;x < channel.getList().size();++x) {
			    values.clear();
			    Log.d("addChannel (add Item)",x+"-"+channel.getList().get(x).getTitle()+"-"+channel.getList().get(x).getLink()+"-"+channel.getList().get(x).getPublished()+"-"+channel.getPosition());
			    values.put(TABLE_RSSITEM_ID,x);
			    values.put(TABLE_RSSITEM_TITLE,channel.getList().get(x).getTitle());
			    values.put(TABLE_RSSITEM_LINK,channel.getList().get(x).getLink());
			    values.put(TABLE_RSSITEM_PUBLISHED,channel.getList().get(x).getPublished());
			    values.put(TABLE_RSSITEM_CHANNELMD5,getHash(channel.getLink()));
			    /* insert into table */
			    try {
			    	db.insertOrThrow(TABLE_RSSITEM, null, values);
			    } catch (SQLException e) {
			    	e.printStackTrace();
			    	db.update(TABLE_RSSITEM, values,TABLE_RSSITEM_ID + "=? AND " + TABLE_RSSITEM_CHANNELMD5 + " =?",new String[] {String.valueOf(x),getHash(channel.getLink())});
				}
		    }
		    /* ATTENTION! NEVER close database connection */
		    //db.close();			
		}

		/**
		 * retrieve the channel
		 * @param link	channel feed link
		 * @param position	new position or -1 to keep last position
		 * @return the given channel or null if not found
		 */
		public RssChannel getChannel(String link,int position) {
			SQLiteDatabase db = this.getReadableDatabase();
			RssChannel channel = null;

		    Log.d("getChannel","getting channel from database "+link);
		    Cursor cursor = db.query(TABLE_RSSCHANNEL, new String[] {TABLE_RSSCHANNEL_ID,TABLE_RSSCHANNEL_TITLE, TABLE_RSSCHANNEL_LINK,TABLE_RSSCHANNEL_UPDATE,TABLE_RSSCHANNEL_MD5},TABLE_RSSCHANNEL_LINK + " = ?",new String [] {link}, null, null, TABLE_RSSCHANNEL_ID, null);
		    if(cursor.getCount() > 0) {
		    	cursor.moveToFirst();
		    	Log.d("getChannel",cursor.getString(0)+":"+cursor.getString(1)+":"+cursor.getString(2)+":"+cursor.getString(3)+":"+cursor.getString(4));
	    		channel = new RssChannel((position == -1) ? cursor.getInt(0) : position,cursor.getString(1),cursor.getString(2),cursor.getString(3));
		    	Cursor icursor = db.query(TABLE_RSSITEM, new String[] {TABLE_RSSITEM_ID,TABLE_RSSITEM_TITLE, TABLE_RSSITEM_LINK,TABLE_RSSITEM_PUBLISHED,TABLE_RSSITEM_CHANNELMD5}, TABLE_RSSITEM_CHANNELMD5 + " = ?",new String[] {cursor.getString(4)}, null, null, TABLE_RSSITEM_ID, null);
			    if(icursor.getCount() > 0) {
			    	icursor.moveToFirst();
			    	do {
			    		channel.addItem(icursor.getString(1),icursor.getString(2),icursor.getString(3));
			    	} while(icursor.moveToNext());
			    }
			    icursor.close();
		    } else {
			    Log.d("getChannel",link + " not found");
		    }
	    	cursor.close();
		    /* ATTENTION! NEVER close database connection */
		    //db.close();
			return channel;
		}
	}

	public void onCreate() {
		// TODO Auto-generated method stub
		
	}

	public void onDestroy() {
		// TODO Auto-generated method stub
		
	}
}
