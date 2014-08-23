package pl.com.nic.android.tlen;

import android.util.Log;

public class
TlenState
{
	private final String TAG = "Tlenoid";
	
	private enum State {
		OFFLINE,
		CONNECTING,
		ONLINE,
	}
	
	private State state;
	
	TlenState()
	{
		setOffline();
	}
	
	
	public void
	setOffline()
	{
		state = State.OFFLINE;
	}
	
	
	public void
	setOnline()
	{
		state = State.ONLINE;
	}
	
	
	public void
	setConnecting()
	{
		state = State.CONNECTING;
	}
	
	
	public void
	set(String new_state)
	{
		Log.d(TAG, "new_state=" + new_state);
		
		if ("ONLINE".equals(new_state)) {
			state = State.ONLINE;
		} else if ("OFFLINE".equals(new_state)) {
			state = State.OFFLINE;
		} else if ("CONNECTING".equals(new_state)) {
			state = State.CONNECTING;
		}
	}
	
	
	public String
	toString()
	{
		switch (state) {
		case OFFLINE:
			return "OFFLINE";
		case ONLINE:
			return "ONLINE";
		case CONNECTING:
			return "CONNECTING";
		}
		
		return "UNKNOWN";
	}
	
	
	public boolean
	isOnline()
	{
		return state == State.ONLINE;
	}
	
	
	public boolean
	isConnecting()
	{
		return state == State.CONNECTING;
	}
	
	
	public boolean
	isOffline()
	{
		return state == State.OFFLINE;
	}
}