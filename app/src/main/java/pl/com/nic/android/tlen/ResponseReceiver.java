package pl.com.nic.android.tlen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class
ResponseReceiver extends BroadcastReceiver
{
	public static final String ACTION_RESP = "pl.com.nic.android.tlen.TLEN_PROTO_MESSAGE";
	private final String TAG = "ResponseReceiver";
 
	@Override
	public void
	onReceive(Context context, Intent intent)
	{
		Log.d(TAG, "onReceive "  + android.os.Process.myTid());

		Bundle b = intent.getExtras();

		Protocol.dump_extras(TAG, b);

		String cmd = b.getString("cmd");

		if ("got-session-start".equals(cmd)) {
			Bundle b_ = new Bundle();

			b_.putString("user-name", "");
			b_.putString("password",  "");

			Intent i = new Intent(context, TlenService.class);
			i.putExtras(b_);
			i.putExtra("cmd", "send-auth-query");
			context.startService(i);
		}
	}
}
