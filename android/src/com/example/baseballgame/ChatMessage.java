package com.example.baseballgame;

import java.util.Comparator;

import android.os.Parcel;
import android.os.Parcelable;

public class ChatMessage implements Parcelable{
	public String name;
	public String msg;
	public long time;
	static public String me;
	@Override
	protected Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}
	public static class MessageComparator implements Comparator<ChatMessage>{
		public int compare(ChatMessage o1, ChatMessage o2) {
			if(o1.time == o2.time)
				return 0;
			else if(o1.time > o2.time)
				return 1;
			else
				return -1;
	    }
	}
	public boolean isMe() {
		return (name.equals(me));
	}
	
	public ChatMessage() {
		super();
	}

	private ChatMessage(Parcel in){
		super();
	    name = in.readString();
	    msg = in.readString();
	    time = in.readInt();
	}
	
	@Override
	public int describeContents() {
	    // TODO Auto-generated method stub
	    return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
	    // TODO Auto-generated method stub
	    out.writeString(name);
	    out.writeString(msg);
	    out.writeLong(time);
	}

	public static final Parcelable.Creator<ChatMessage> CREATOR = new Parcelable.Creator<ChatMessage>() {
	    public ChatMessage createFromParcel(Parcel in) {
	        return new ChatMessage(in);
	    }

	    public ChatMessage[] newArray(int size) {
	        return new ChatMessage[size];
	    }
	};
	
	@Override
	public String toString() {
		return this.name + "ดิ : " + this.msg;
	};
}
