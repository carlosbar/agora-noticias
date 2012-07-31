package com.innercalc.agora;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

public class Channel {

	/**
	 * statically creates a list of channels
	 * @param channels Array of String arrays containing the name and feed url
	 * @return a list of channels
	 */
	public static ChannelList addChannel(String [] channels []) {
		ChannelList channelList = new ChannelList();
		
		for(String [] channel : channels) {
			LocalChannel cl = new LocalChannel(channel[0],channel[1],true);
			channelList.add(cl);
		}
		return channelList;
    }
	/**
	 * contains information about the given channel
	 * @author carlosbar
	 */
	public static class LocalChannel {
		public String	name;
		public String	url;
		public boolean	staticChannel;
		boolean checked;

		/**
		 * default constructor for a local channel
		 * @param name channel name
		 * @param url channel feed url
		 * @param staticChannel is this a static or user created channel?
		 */
		public LocalChannel(String name,String url,boolean staticChannel) {
			this.name = name;
			this.url = url;
			this.staticChannel = staticChannel;
		}
	}
	
	public static class ChannelList extends ArrayList<LocalChannel> {

		/**
		 * 
		 */
		private static final long serialVersionUID = -6933337135308184074L;

		/**
		 * retrieve a list of channels in JSON notation
		 * @return the url list
		 */
		public String toJSONString() {
			JSONObject json = new JSONObject();

			for(int x=0;x < this.size();x++) {
				JSONObject child = new JSONObject();

				try {
					child.put("name",this.get(x).name);
					child.put("url",this.get(x).url);
					child.put("staticChannel",this.get(x).staticChannel);
					json.put("channel"+x,child);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return json.toString();
		}
		
		/**
		 * create a list of channels from JSON object
		 * @param string in json notation
		 * @return a new channel list
		 */
		public static ChannelList fromJSONString(String str) {
			JSONObject json;
			ChannelList list = new ChannelList();

			try {
				json = new JSONObject(str);
			} catch (JSONException e) {
				json = null;
			}
			for(int x=0;json != null && x < json.length();x++) {
				try {
					JSONObject o = (JSONObject) json.get("channel"+x);
					LocalChannel cl = new LocalChannel((String) o.get("name"),(String) o.get("url"),o.getBoolean("staticChannel"));
					list.add(cl);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return list;
		}
	
	}
	
}	
