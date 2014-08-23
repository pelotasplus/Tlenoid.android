package pl.com.nic.android.tlen;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

import org.xml.sax.SAXException;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Xml;

public class
TlenService extends Service
{
	private class
	Reconnect
	{
		private boolean auto_reconnect = false;
		private final int RECONNECT_DELAY = 10;
		
		private boolean task_scheduled = false;
		
		private	Runnable reconnectTask = new Runnable() {
			public void run()
			{
				Log.d(TAG, "Reconnect::reconnectTask");
				
				task_scheduled = false;
				
				if (! network_state.isOnline()) {
					Log.d(TAG, "skipping, network is not online");
					return;
				}
				
				if (! tlenservice_state.isOffline()) {
					Log.d(TAG, "skipping, tlen service is not offline");
					return;
				}
			
				startSession();
			}
		};
		
		public void
		set(boolean val)
		{
			auto_reconnect = val;
		}
		
		
		public void
		schedule()
		{
			Log.d(TAG, "Reconnect::schedule");

			if (! auto_reconnect) {
				Log.d(TAG, "skipping, no auto_reconnect");
				return;
			}

			if (! task_scheduled) {
				Log.d(TAG, "scheduling reconnect");
				task_scheduled = tsHandler.postDelayed(reconnectTask, RECONNECT_DELAY * 1000);
			} else {
				Log.d(TAG, "reconnect already scheduled");
			}
		}
	}

	private final String TAG = "TlenService";

	private Socket s = null;
	private OutputStreamWriter os;
	
	private volatile Looper mServiceLooper;
	private volatile ServiceHandler mServiceHandler;
	
	private boolean mRedelivery;

	private ParseXML xmlParser = null;
		
	private Handler tsHandler = new Handler();
	
	// in seconds
	private final int PING_INTERVAL = 20;
	private	Runnable pingTask = new Runnable() {
		public void run()
		{
			if (tlenservice_state.isOffline()) {
				Log.d(TAG, "skipping ping as tlen state is offline");
				return;
			}
			
			try {
				send(Protocol.tlen_ping_session(), false /* verbose */);
			} catch (Exception exc) {
				Log.e(TAG, "sendPing: ", exc);
			}
			
			tsHandler.postDelayed(pingTask, PING_INTERVAL * 1000);
		}
	};
	
	/* tlen service state (connected or offline) */
	private TlenState tlenservice_state = new TlenState();
	
	/* state of android network connectivity (wifi, 3g) */
	private TlenState network_state = new TlenState();
	
	private Reconnect reconnect = new Reconnect();
	
	private BroadcastReceiver receiver;
	
	private String username = null;
	private String password = null;
	private String session_id = null;
	
	private final class ServiceHandler extends Handler
	{
		public ServiceHandler(Looper looper)
		{
			super(looper);
		}

		@Override
		public void handleMessage(Message msg)
		{
			onHandleIntent((Intent)msg.obj);
		}
	}

	public
	TlenService()
	{
		super();
	}

	@Override
	public void
	onCreate()
	{
		super.onCreate();

		try {
			Class.forName("android.os.AsyncTask");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
		}

		HandlerThread thread = new HandlerThread("TlenService thread");
		thread.start();

		mServiceLooper = thread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
		
		receiver = new BroadcastReceiver()
		{
			@Override
			public void
			onReceive(Context ctx, Intent intent)
			{
				boolean no_conn = intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
				
				Log.d(TAG, "intent=" + intent.toString() + ", no_connectivity=" + no_conn);
			
				/* no network connectivity */
				if (no_conn == true) {
					network_state.setOffline();
					stop_network();
					postToUI("got-error", "no network connectivity");
				} else {
					network_state.setOnline();
					reconnect.schedule();
				}
			}
		};
		
		registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
		
		postToUI("state-is", tlenservice_state.toString());
	}


	@Override
	public void
	onStart(Intent intent, int startId)
	{
		Message msg = mServiceHandler.obtainMessage();
		msg.arg1 = startId;
		msg.obj = intent;
		
		mServiceHandler.sendMessage(msg);
	}


	// bez @Override bo wchodzi w API 5 a my wspieramy API 4
	public int
	onStartCommand(Intent intent, int flags, int startId)
	{
		onStart(intent, startId);
		
		return mRedelivery ? 3 /* START_REDELIVER_INTENT */  : 2 /* START_NOT_STICKY */;
	}

	@Override
	public void
	onDestroy()
	{
		Log.d(TAG, "onDestroy");

		tsHandler.removeCallbacksAndMessages(null);
		tsHandler = null;
		
		mServiceLooper.quit();
		
		unregisterReceiver(receiver);
		
		close_network();
		
		super.onDestroy();
	}

	@Override
	public IBinder
	onBind(Intent intent)
	{
		return null;
	}

	
	private void
	postToUI(String cmd, Bundle b)
	{
		Intent broadcastIntent = new Intent();
		
		broadcastIntent.putExtra("cmd", cmd);
		broadcastIntent.putExtras(b);

		postToUI_send(broadcastIntent);
	}
	
	private void
	postToUI(String cmd, String data)
	{
		Intent broadcastIntent = new Intent();
		
		broadcastIntent.putExtra("cmd", cmd);
		broadcastIntent.putExtra("data", data);
		
		postToUI_send(broadcastIntent);
	}
	
	
	private void
	postToUI_send(Intent i)
	{
		String cmd = i.getStringExtra("cmd");
		Log.d(TAG, "postToUI_send cmd=" + cmd);
		
		i.setAction(ResponseReceiver.ACTION_RESP);
		i.addCategory(Intent.CATEGORY_DEFAULT);
		
		sendBroadcast(i);
	}


	class ParseXML extends AsyncTask<InputStream, Void, String>
	{
		protected String
		doInBackground(InputStream... is)
		{
			try {
				Xml.parse(is[0], Xml.Encoding.ISO_8859_1, new XMLHandler(getBaseContext()));
			} catch (SAXException e) {
				Log.d(TAG, "run: got SAXException", e);
			} catch (java.net.SocketException e) {
				Log.d(TAG, "run: got no net exception", e);
			} catch (java.lang.AssertionError e) {
				Log.d(TAG, "Xml.parser() died in async task");
			} catch (Exception e) {
				Log.e(TAG, "run: exc: ", e);	
			}

			return "xml-parse-end";
		}

		@Override
		protected void
		onPostExecute(String result)
		{
			Log.d(TAG, "onPostExec result is '" + result + "'");

			stop_network();
			postToUI("got-error", result);
		}
	}
	
	
	/* stops network
	 * doesn't send tlen protocol end of session tag -- it's not sure if network
	 * is working at this time.
	 */
	private void
	stop_network()
	{
		stop_network(false /* no reconnect */);
	}
	
	
	/* closes network descriptors + parsers */
	private void
	close_network()
	{
		if (s != null) {
			try {
				s.shutdownInput();
				s.shutdownOutput();
				s.close();
			} catch (IOException e) {
				// pass
			}

			s = null;
			os = null;
		}

		if (xmlParser != null) {
			xmlParser.cancel(true);
			xmlParser = null;
		}
	}	
	
	
	private void
	stop_network(boolean disable_reconnect)
	{
		close_network();
		
		tlenservice_state.setOffline();
		postToUI("state-is", tlenservice_state.toString());
		
		if (! disable_reconnect) {
			reconnect.schedule();
		}
	}

	
	/* starts both network and tlen session */
	private void
	startSession()
	{
		try {
			s = new Socket("193.17.41.53", 443);	
			os = new OutputStreamWriter(s.getOutputStream());
			
			xmlParser = new ParseXML();
			xmlParser.execute(s.getInputStream());
		} catch (Exception e) {
			Log.e(TAG, "startSession: " + e.toString());

			stop_network();
			postToUI("got-error", "cannot start session");
			
			tlenservice_state.setOffline();
			postToUI("state-is", tlenservice_state.toString());
			
			return;
		}
		
		send(Protocol.tlen_session_string());
		
		tlenservice_state.setConnecting();
		postToUI("state-is", tlenservice_state.toString());
	}
	

	public void
	onHandleIntent(Intent intent)
	{
		Bundle b = intent.getExtras();
		String cmd = b.getString("cmd");

		Log.d(TAG, "onHandleIntent: cmd= " + cmd);
		if (! cmd.equals("got-presence") && ! cmd.equals("got-roster")) {
			Protocol.dump_extras(TAG, b);
		}
		
		/* got-* messages are from XML Handler */
		if (cmd.startsWith("got-")) {
			if (cmd.equals("got-password-correct")) {
				pingTask.run();
				
				tlenservice_state.setOnline();
				postToUI("state-is", tlenservice_state.toString());
				
				postToUI(cmd, b);
			} else if (cmd.equals("got-session-start")) {
				session_id = b.getString("session-id");

				send(Protocol.tlen_auth_query(session_id, username, password));
				
				/* nie przesylamy do UI */
			} else if (! tlenservice_state.isOffline()) {
				postToUI(cmd, b);
			}
		/* messages from UI */
		} else {
			if ("start-session".equals(cmd)) {
				String reconn = b.getString("auto-reconnect");
				if (reconn != null) {
					reconnect.set(Boolean.parseBoolean(reconn));
				}
				
				username = b.getString("user-name");
				password = b.getString("password");
				
				startSession();
			} else if ("send-get-roster".equals(cmd)) {
				send(Protocol.tlen_get_roster());
			} else if ("send-message".equals(cmd)) {
				String m  = b.getString("message");
				String to = b.getString("to");

				send(Protocol.tlen_message(to, m));
			} else if (cmd.equals("stop-session")) {
				send(Protocol.tlen_close());

				stop_network(true /* disable reconnect */);
			} else if (cmd.equals("stop-service")) {
				stopSelf();
			} else if (cmd.equals("get-user-info")) {
				String username = b.getString("user-name");

				send(Protocol.tlen_get_info(username));
			} else if (cmd.equals("send-set-presence")) {
				String presence = b.getString("presence");
				String message = b.getString("message");

				send(Protocol.tlen_set_presence(presence, message));
			} else if (cmd.equals("delete-user")) {
				String id = b.getString("id");
				Log.d(TAG, "user to delete " + id);

				send(Protocol.tlen_remove_user(id));
			} else if (cmd.equals("set-alias")) {
				String group = b.getString("group");
				String username = b.getString("id");
				String alias = b.getString("new-alias");

				Log.d(TAG, "username=" + username + ", alias=" + alias + ", group=" + group);

				send(Protocol.tlen_set_alias(username, alias, group));
			} else if (cmd.equals("get-state")) {
				postToUI("state-is", tlenservice_state.toString());
			}
		}
	}


	private void
	send(String data)
	{
		send(data, true);
	}


	private void
	send(String data, boolean verbose)
	{
		if (s == null) {
			stop_network();
			postToUI("got-error", "send-error-no-socket");
			
			return;
		}

		try {
			os.write(data, 0, data.length());
			os.flush();
		} catch (IOException e) {
			Log.e(TAG, "send: data=" + data + " exc " + e.toString());
			
			stop_network();
			postToUI("got-error", "send-error-io-exception");
			
			return;
		}

		if (verbose)
			Log.d(TAG, "send: " + data);
	}
}