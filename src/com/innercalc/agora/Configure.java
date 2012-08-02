package com.innercalc.agora;

import java.util.Arrays;

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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
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
		{"Folha.com - Ciência","http://feeds.folha.uol.com.br/ciencia/rss091.xml"},
		{"G1 - São Paulo","http://g1.globo.com/dynamo/sao-paulo/rss2.xml"},
		{"G1 - Ciência e Saúde","http://g1.globo.com/dynamo/ciencia-e-saude/rss2.xml"},
		{"G1 - Mundo","http://g1.globo.com/dynamo/mundo/rss2.xml"},
		{"G1 - Planeta Bizarro","http://g1.globo.com/dynamo/planeta-bizarro/rss2.xml"},
		{"G1 - Pop Arte","http://g1.globo.com/dynamo/pop-arte/rss2.xml"},
		{"G1 - Rio de Janeiro","http://g1.globo.com/dynamo/rio-de-janeiro/rss2.xml"},
		{"G1 - Tecnologia","http://g1.globo.com/dynamo/tecnologia/rss2.xml"},
		{"G1 - Amazônia","http://www.globoamazonia.com/Rss2/0,,AS0-16052,00.xml"},
		{"G1 - Carros","http://g1.globo.com/dynamo/carros/rss2.xml"},
		{"G1 - Economia","http://g1.globo.com/dynamo/economia/rss2.xml"},
		{"G1 - Home","http://g1.globo.com/dynamo/rss2.xml"},				
		{"G1 - Política","http://g1.globo.com/dynamo/politica/rss2.xml"},
		{"G1 - Vestibular e Educação","http://g1.globo.com/dynamo/vestibular-e-educacao/rss2.xml"}};
								
	private ChannelList channelList = Channel.addChannel(staticChannel);
	private ChannelList userFeeds,userChannels;
	private int interval = ViewProvider.THIRTY_MINUTES;
	private int maxFeeds = ViewProvider.MAXFEEDS_FIFTEEN;
	private int	textColor;

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
		userChannels =  ChannelList.fromJSONString(prefs.getString("userChannels",channelList.toJSONString()));
		interval = prefs.getInt("updateTimeout",interval);
		maxFeeds = prefs.getInt("maxFeeds",maxFeeds);
		textColor = prefs.getInt("textColor",0);
		
		/* incorporate user channels */
		for(LocalChannel l : userChannels) {
			channelList.add(l);
		}
		
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
		
		((SeekBar) findViewById(R.id.textColor)).setProgress(textColor);
		((SeekBar) findViewById(R.id.textColor)).setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				textColor = arg1;
			}
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			public void onStopTrackingTouch(SeekBar seekBar) {
				switch(textColor) {
				case 0:
					Toast.makeText(Configure.this,getString(R.string.lightGray),Toast.LENGTH_SHORT).show();
					break;
				case 1:
					Toast.makeText(Configure.this,getString(R.string.darkGray),Toast.LENGTH_SHORT).show();
					break;
				case 2:
					Toast.makeText(Configure.this,getString(R.string.white),Toast.LENGTH_SHORT).show();
					break;
				case 3:
					Toast.makeText(Configure.this,getString(R.string.black),Toast.LENGTH_SHORT).show();
					break;
				}
			}
		});
		
		/**
		 * edit list of channels
		 */
		((Button) findViewById(R.id.rssFeeds)).setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				final AlertDialog.Builder dlg = new AlertDialog.Builder(Configure.this);
				final LayoutInflater layoutInflater = LayoutInflater.from(Configure.this);
				final View channel = layoutInflater.inflate(R.layout.channel, null);
				dlg.setView(channel);

				/**
				 * set list adapter
				 */
				for(int x=0;x < channelList.size();x++) {
					LocalChannel cl = channelList.get(x);
					for(int y=0;y < userFeeds.size();y++) {
						if(userFeeds.get(y).url.equals(cl.url)) {
							cl.checked = true;
						}
					}
				}
				final ChannelAdapter adapter = new ChannelAdapter(Configure.this,channelList);
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
				 * new channel button
				 */
				((Button) channel.findViewById(R.id.newChannel)).setOnClickListener(new OnClickListener() {
					
					public void onClick(View arg0) {
						final AlertDialog.Builder newdlg = new AlertDialog.Builder(Configure.this);
						final LayoutInflater layoutInflater = LayoutInflater.from(Configure.this);
						final View newchannel = layoutInflater.inflate(R.layout.newchannel, null);
						newdlg.setView(newchannel);

						/**
						 * ok button pressed
						 */
						newdlg.setPositiveButton(getString(R.string.add),new DialogInterface.OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								final String channelName = ((EditText) newchannel.findViewById(R.id.channelName)).getText().toString();
								final String url =  ((EditText) newchannel.findViewById(R.id.url)).getText().toString();
								
								if(!channelName.isEmpty() && !url.isEmpty()) {
									adapter.add(new LocalChannel(channelName,url,false));
								} else {
									Toast.makeText(Configure.this,getString(R.string.errorAddingChannel),Toast.LENGTH_LONG).show();
								}
							}
						});

						/**
						 * back button pressed
						 */
						newdlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
							public void onCancel(DialogInterface dialog) {
							}
						});
						
						newdlg.setTitle(R.string.newChannel);
						newdlg.setIcon(R.drawable.ic_launcher);
						newdlg.show();
					}
				});
				
				/**
				 * back button has been pressed
				 */
				dlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						userFeeds.clear();
						userChannels.clear();
						for(int x=0;x < adapter.getCount();x++) {
							/* add checked channels */
							if(adapter.getItem(x).checked) {
								Log.d("Channel List","Channel: " + adapter.getItem(x).name);
								userFeeds.add(channelList.get(x));
							}
							/* add user channels */
							if(!adapter.getItem(x).staticChannel) {
								userChannels.add(adapter.getItem(x));
							}
						}
						channelList = adapter.itemList;
					}
				});
				dlg.setTitle(R.string.rssChannels);
				dlg.setIcon(R.drawable.ic_launcher);
				dlg.show();
			}
		});
    }
    
    @Override
    public void onBackPressed() {
    	//super.onBackPressed();
    	
    	/* get remote view from widget screen */
		//RemoteViews views = new RemoteViews(Configure.this.getPackageName(),R.layout.main);
		//AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(Configure.this);

		/* create button procedure */
//		Intent intent = new Intent(rsswidget.CHANGE_TEXT);
//		intent.putExtra("line",1);
//		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
//		PendingIntent pendingIntent = PendingIntent.getBroadcast(Configure.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
//		views.setOnClickPendingIntent(R.id.imageButton1, pendingIntent);

		/* set name of rss location */
		SharedPreferences prefs = getSharedPreferences(MyWidget.PREFS_DB, 0);
		SharedPreferences.Editor prefset = prefs.edit();
		prefset.putString("rssfeed",userFeeds.toJSONString());
		prefset.putString("userChannels",userChannels.toJSONString());
		prefset.putInt("updateTimeout",interval);
		prefset.putInt("maxFeeds",maxFeeds);
		prefset.putInt("textColor",textColor);
		prefset.commit();
		
		/* start widget */
		Intent start = new Intent(MyWidget.START_WIDGET);
		start.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		sendBroadcast(start);

		/* set result */
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);

		/* update view */
		//appWidgetManager.updateAppWidget(mAppWidgetId, views);

		/* finish the activity */
		finish();
    }
    
    public class ChannelAdapter extends ArrayAdapter<LocalChannel> {
    	private Context ctxt;
    	private ChannelList itemList;

    	public ChannelAdapter(Context context, ChannelList itemList) {
    		super(context,android.R.layout.simple_list_item_1,itemList);
    		ctxt = context;
    		this.itemList = itemList;
    	}
    	
    	public void setChecked(int position,boolean checked) {
    		itemList.get(position).checked = checked;
    		super.notifyDataSetChanged();
    	}

    	public boolean getChecked(int position) {
    		return itemList.get(position).checked;
    	}

    	@Override
    	public LocalChannel getItem(int position) {
    		return itemList.get(position);
    	}

    	@Override
    	public void remove(LocalChannel object) {
    		itemList.remove(object);
    		super.notifyDataSetChanged();
    	}
    	
    	@Override
    	public void add(LocalChannel object) {
    		object.checked = true;
    		itemList.add(object);
    		super.notifyDataSetChanged();
    	}

    	@Override
    	public int getCount() {
    		return itemList.size();
    	}

    	@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View			checkView;
			
			LayoutInflater inflater = (LayoutInflater) ctxt.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			checkView =  inflater.inflate(R.layout.checkrow,parent,false);
			LocalChannel item = itemList.get(position);
			checkView.setTag(item);
			Log.d("Create View","postion " + position + " Checked: " + item.checked + " name: " + item.name);
			CheckBox cb=(CheckBox) checkView.findViewById(R.id.checked);
			if(cb != null) {
				cb.setTag(item);
				cb.setChecked(item.checked);
				cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
					public void onCheckedChanged(CompoundButton button, boolean checked) {
						LocalChannel item = (LocalChannel) button.getTag();
						item.checked = checked;
					}
				});
			}
			TextView tv = (TextView) checkView.findViewById(R.id.title);
			if(tv != null) {
				tv.setText(item.name);
				tv.setTag(item);
				if(!item.staticChannel) tv.setOnLongClickListener(new OnLongClickListener() {
					
					public boolean onLongClick(View v) {
						final LocalChannel item = (LocalChannel) v.getTag();
						final AlertDialog.Builder newdlg = new AlertDialog.Builder(Configure.this);
						final LayoutInflater layoutInflater = LayoutInflater.from(Configure.this);
						final View editchannel = layoutInflater.inflate(R.layout.newchannel, null);
						newdlg.setView(editchannel);

						/**
						 * set current values
						 */
						((EditText) editchannel.findViewById(R.id.channelName)).setText(item.name);
						((EditText) editchannel.findViewById(R.id.url)).setText(item.url);
						
						/**
						 * ok button pressed
						 */
						newdlg.setPositiveButton(getString(R.string.ok),new DialogInterface.OnClickListener() {
							
							public void onClick(DialogInterface dialog, int which) {
								final String channelName = ((EditText) editchannel.findViewById(R.id.channelName)).getText().toString();
								final String url =  ((EditText) editchannel.findViewById(R.id.url)).getText().toString();
								
								if(!channelName.isEmpty() && !url.isEmpty()) {
									item.name = channelName;
									item.url = url;
									ChannelAdapter.this.notifyDataSetChanged();
								} else {
									Toast.makeText(Configure.this,getString(R.string.errorAddingChannel),Toast.LENGTH_LONG).show();
								}
							}
						});

						/**
						 * back button pressed
						 */
						newdlg.setOnCancelListener(new DialogInterface.OnCancelListener() {
							public void onCancel(DialogInterface dialog) {
							}
						});
						
						newdlg.setTitle(R.string.editChannel);
						newdlg.setIcon(R.drawable.ic_launcher);
						newdlg.show();
						return false;
					}
				});
			}
			ImageView im = (ImageView) checkView.findViewById(R.id.deleteChannel);
			if(im != null && !item.staticChannel) {
				im.setTag(item);
				im.setBackgroundResource(R.drawable.delpress);
				im.setOnClickListener(new OnClickListener() {
					
					public void onClick(View v) {
						LocalChannel item = (LocalChannel) v.getTag();
						ChannelAdapter.this.remove(item);
					}
				});
			}
			return checkView;
		}
   }
    
    
}