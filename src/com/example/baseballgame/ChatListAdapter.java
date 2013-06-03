package com.example.baseballgame;

import java.util.List;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ChatListAdapter extends ArrayAdapter<ChatMessage> {
	
	public ChatListAdapter(Context context, int textViewResourceId, List<ChatMessage> arr) {
		super(context, textViewResourceId, arr);
		// TODO Auto-generated constructor stub
	}

	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		
        // your drawable here for this element 
		View _convertView = super.getView(position, convertView, parent);
        // maybe here's more to do with the view
		
		ChatMessage msg = this.getItem(position);
		if(msg.isMe()) {
			_convertView.setBackgroundResource(R.drawable.me_chat_balloon);
			((TextView) _convertView).setGravity(Gravity.LEFT);
			_convertView.setPadding(10, 10, 40, 15);
		}
		else {
			_convertView.setBackgroundResource(R.drawable.other_chat_balloon);
			((TextView) _convertView).setGravity(Gravity.RIGHT);
			_convertView.setPadding(40, 10, 10, 15);
		}

        return _convertView;
    }
}
