package com.innercalc.agora;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.innercalc.agora.Channel.ChannelList;
import com.innercalc.agora.Channel.LocalChannel;
import com.innercalc.agora.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class Configure extends Activity {
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	private static String [] staticChannel[] = 
		{{"Estadão - Esportes","http://www.estadao.com.br/rss/esportes.xml"},
		{"Estadão - Cultura","http://www.estadao.com.br/rss/cultura.xml"},
		{"Estadão - Planeta","http://www.estadao.com.br/rss/planeta.xml"},
		{"Estadão - Educação","http://www.estadao.com.br/rss/educacao.xml"},
		{"Estadão - Ciência","http://www.estadao.com.br/rss/ciencia.xml"},
		{"Estadão - Saúde","http://www.estadao.com.br/rss/saude.xml"},
		{"Estadão - Internacional","http://www.estadao.com.br/rss/internacional.xml"},
		{"Estadão - Brasil","http://www.estadao.com.br/rss/brasil.xml"},
		{"Estadão - Últimas Notícias","http://www.estadao.com.br/rss/ultimas.xml"},
		{"Estadão - Manchetes","http://www.estadao.com.br/rss/manchetes.xml"},
		{"Folha.com - Turismo","http://feeds.folha.uol.com.br/folha/turismo/rss091.xml"},
		{"Folha.com - Pensata","http://feeds.folha.uol.com.br/folha/pensata/rss091.xml"},
		{"Folha.com - Painel do Leitor","http://feeds.folha.uol.com.br/folha/paineldoleitor/rss091.xml"},
		{"Folha.com - Equilíbrio","http://feeds.folha.uol.com.br/folha/equilibrio/rss091.xml"},
		{"Folha.com - Educação","http://feeds.folha.uol.com.br/folha/educacao/rss091.xml"},
		{"Folha.com - Em cima da hora","http://feeds.folha.uol.com.br/folha/emcimadahora/rss091.xml"},
		{"Folha.com - Ilustrada","http://feeds.folha.uol.com.br/folha/ilustrada/rss091.xml"},
		{"Folha.com - Mundo","http://feeds.folha.uol.com.br/folha/mundo/rss091.xml"},
		{"Folha.com - Informática","http://feeds.folha.uol.com.br/folha/informatica/rss091.xml"},
		{"Folha.com - Esporte","http://feeds.folha.uol.com.br/folha/esporte/rss091.xml"},
		{"Folha.com - Dinheiro","http://feeds.folha.uol.com.br/folha/dinheiro/rss091.xml"},
		{"Folha.com - Brasil","http://feeds.folha.uol.com.br/folha/brasil/rss091.xml"},
		{"Folha.com - Cotidiano","http://feeds.folha.uol.com.br/cotidiano/rss091.xml"},
		{"Folha.com - Ciência","http://feeds.folha.uol.com.br/ciencia/rss091.xml"}};

	private ChannelList channelList = Channel.addChannel(staticChannel);
	private ChannelList userFeeds;
	private int interval = ViewProvider.THIRTY_MINUTES;
	private int maxFeeds = ViewProvider.MAXFEEDS_FIFTEEN;

	public String join(String [] arr,String c)
	{
		String j = "";
		for(int x=0;x < arr.length;x++) {
			if(!j.isEmpty()) j+=c;
			j+=arr[x];
		}
		return j;
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.config);
		Intent intent = getIntent();
        
		/* Set the result to CANCELED. This will cause the widget host to cancel
		   out of the widget placement if they press the back button. */
		setResult(RESULT_CANCELED);
		String versionName;
		try {
			versionName = this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
			versionName="1";
		}
		setTitle(String.format("%s v. %s",getString(R.string.app_name),versionName));

		SharedPreferences prefs = getSharedPreferences(MyWidget.PREFS_DB, 0);
		userFeeds = ChannelList.fromJSONString(prefs.getString("rssfeed",channelList.toJSONString()));
		interval = prefs.getInt("updateTimeout",interval);
		maxFeeds = prefs.getInt("maxFeeds",maxFeeds);
		
		/* get widget id */
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		((SeekBar) findViewById(R.id.maxFeeds)).setProgress(maxFeeds-ViewProvider.MAXFEEDS_TEN);
		((SeekBar) findViewById(R.id.maxFeeds)).setMax(ViewProvider.MAXFEEDS_THIRTY-ViewProvider.MAXFEEDS_TEN);
		((SeekBar) findViewById(R.id.maxFeeds)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				maxFeeds = arg1 + ViewProvider.MAXFEEDS_TEN;
			}
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			public void onStopTrackingTouch(SeekBar seekBar) {
				Toast.makeText(Configure.this,maxFeeds + " " + getString(R.string.items),Toast.LENGTH_SHORT).show();
			}
		});
		
		((SeekBar) findViewById(R.id.interval)).setProgress(interval-ViewProvider.FIVE_MINUTES);
		((SeekBar) findViewById(R.id.interval)).setMax(ViewProvider.THIRTY_MINUTES-ViewProvider.FIVE_MINUTES);
		((SeekBar) findViewById(R.id.interval)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				interval = arg1 + ViewProvider.FIVE_MINUTES;
			}
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			public void onStopTrackingTouch(SeekBar seekBar) {
				Toast.makeText(Configure.this,interval + " " + getString(R.string.minutes),Toast.LENGTH_SHORT).show();
			}
		});
		
		((Button) findViewById(R.id.rssFeeds)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final AlertDialog.Builder dlg = new AlertDialog.Builder(Configure.this);
				final LayoutInflater layoutInflater = LayoutInflater.from(Configure.this);
				final View channel = layoutInflater.inflate(R.layout.channel, null);
				dlg.setView(channel);
				ChannelItem [] itemList = new ChannelItem[channelList.size()];

				/* verify which feeds are turned on */
				for(int c=0;c < channelList.size();c++) {
					boolean checked = false;
					for(int i=0;i < userFeeds.size();i++) {
						if(userFeeds.get(i).url.equals(channelList.get(c).url)) {
							checked = true;
							break;
						}
					}
	    			itemList[c] = new ChannelItem(channelList.get(c).url,channelList.get(c).name,checked);
				}
				final ChannelAdapter adapter = new ChannelAdapter(Configure.this,itemList);
				((ListView) channel.findViewById(R.id.channelList)).setAdapter(adapter);

				/**
				 * check/uncheck all buttons
				 */
				((Button) channel.findViewById(R.id.allButtons)).setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						boolean checked = adapter.getChecked(0);

						for(int x=0;x < adapter.getCount();x++) {
							adapter.setChecked(x,!checked);
						}
					}
				});
				
				/**
				 * back button has been pressed
				 */
				dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						userFeeds.clear();
						for(int x=0;x < adapter.getCount();x++) {
							if(adapter.getItem(x).checked) {
								userFeeds.add(channelList.get(x));
							}
						}
					}
				});
				dlg.setTitle(R.string.rssFeeds);
				dlg.setIcon(R.drawable.ic_launcher);
				dlg.show();
			}
		});
    }
    
    @Override
    public void onBackPressed() {
    	//super.onBackPressed();
    	
    	/* get remote view from widget screen */
		RemoteViews views = new RemoteViews(Configure.this.getPackageName(),R.layout.main);
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(Configure.this);

		/* create button procedure */
//		Intent intent = new Intent(rsswidget.CHANGE_TEXT);
//		intent.putExtra("line",1);
//		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
//		PendingIntent pendingIntent = PendingIntent.getBroadcast(Configure.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//		views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);

		/* update view */
		appWidgetManager.updateAppWidget(mAppWidgetId, views);

		/* set name of rss location */
		SharedPreferences prefs = getSharedPreferences(MyWidget.PREFS_DB, 0);
		SharedPreferences.Editor prefset = prefs.edit();
		prefset.putString("rssfeed",userFeeds.toJSONString());
		prefset.putInt("updateTimeout",interval);
		prefset.putInt("maxFeeds",maxFeeds);
		prefset.commit();
		
		/* start widget */
		Intent start = new Intent(MyWidget.START_WIDGET);
		start.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		sendBroadcast(start);

		/* set result */
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);
		
		/* finish the activity */
		finish();
    }
    
    public class ChannelItem {
    	public	String		name;
    	public	String		url;
    	public	boolean		checked;
    	public ChannelItem(String u,String n,boolean c) {
    		url = u;
    		name = n;
    		checked = c;
    	}
    }
    
    public class ChannelAdapter extends ArrayAdapter<ChannelItem> {
    	private Context ctxt;
    	private ChannelItem [] itemList;

    	public ChannelAdapter(Context context, ChannelItem [] itemList) {
    		super(context,android.R.layout.simple_list_item_1,itemList);
    		ctxt = context;
    		this.itemList = itemList;
    	}
    	
    	public void setChecked(int position,boolean checked) {
    		itemList[position].checked = checked;
    		super.notifyDataSetChanged();
    	}

    	public boolean getChecked(int position) {
    		return itemList[position].checked;
    	}
    	
    	@Override
    	public int getCount() {
    		return itemList.length;
    	}

    	@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View			checkView;
			
			LayoutInflater inflater = (LayoutInflater) ctxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			checkView =  inflater.inflate(R.layout.checkrow,parent,false);
			ChannelItem item = itemList[position];	
			Log.d("Create View","postion " + position + " Checked: " + item.checked + " name: " + item.name);
			CheckBox cb=(CheckBox) checkView.findViewById(R.id.checked);
			if(cb != null) {
				cb.setId(position);
				cb.setChecked(item.checked);
				cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton button, boolean checked) {
						CheckBox cb = (CheckBox) button;
						ChannelItem item = itemList[cb.getId()];
						item.checked = checked;
						Log.d("Create View","postion " + cb.getId() + " Checked: " + item.checked + " name: " + item.name);
					}
				});
			}
			TextView tv = (TextView) checkView.findViewById(R.id.title);
			if(tv != null) {
				tv.setText(item.name);
			}
			return checkView;
		}
   }
    
    
}