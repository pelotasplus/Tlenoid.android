package pl.com.nic.android.tlen;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.Thread.State;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Log;

public class
AvatarManager
{
	private static final String TAG = "Tlenoid/AvatarManager";

	File root = null;
	File avatarsDir = null;
	// ArrayList<String> avatarsToGet = new ArrayList<String>();
	private final ArrayList<QueueItem> queue = new ArrayList<QueueItem>();

	private String token   = null;
	private String urlRoot = "http://mini10.tlen.pl";

	private Thread thread;

	private QueueRunner runner = new QueueRunner();;

	private final class QueueItem {
		public String md5;
		public String url;
	}

	private HashMap<String, QueueItem> cache = new HashMap<String, QueueItem>();

	public
	AvatarManager(Context ctx)
	{
		/* requires 2.2 */
/*
		root = ctx.getExternalFilesDir(null);
		if (root == null) {
			Log.d(TAG, "no external files dir");
			return;
		}
 */

		root = Environment.getExternalStorageDirectory();
		if (root == null) {
			Log.d(TAG, "no external files dir");
			return;
		}

		root = new File(root.getAbsolutePath(), "/Android/data/" + ctx.getPackageName() + "/files/");
		if (root == null) {
			Log.d(TAG, "no external files dir -- problem with path");
			return;
		}



		avatarsDir = new File(root.getAbsolutePath(), "avatars/");
		if (avatarsDir == null) {
			Log.d(TAG, "avatars dir not available");
			return;
		}

		avatarsDir.mkdirs();

		if (! avatarsDir.exists()) {
			Log.d(TAG, "Dir " + avatarsDir + " doesn't exist -- not starting up worker thread");
			avatarsDir = null;
			return;
		}
		
		// Log.d("AvatarManager", "PATH=" + avatarsDir.getAbsolutePath());
	
		thread = new Thread(runner);

		String fls[] = avatarsDir.list();
		if (fls == null) {
			Log.d(TAG, "no avatars in avatarsDir, nothing to add to cache");
			return;
		}
			
		for (String s: fls) {
			// Log.d(TAG, "fl " + s);

			String md5 = s;

			int pos = s.indexOf(".");
			if (pos != -1) {
				md5 = s.substring(0, pos);
			}


			QueueItem i = new QueueItem();
			i.md5 = md5;
			i.url = avatarsDir.getAbsolutePath() + "/" + s;

			// Log.d(TAG, "s=" + md5 + ", url=" + i.url);

			cache.put(md5, i);
		}
	}


	public void
	set_token(String token_)
	{
		token = token_;
	}

	public void
	retrive(String name, String type, String md5)
	{
		// Log.d("AvatarManager", "retrive: " + "name= " + name + ", type= " + type + ", md5= " + md5);

		if (md5 == null || md5.equals("")) {
			return;
		}
		
		if (cache.containsKey(md5)) {
			// Log.d(TAG, "not downloading as its in cache");
			return;
		}

		if (avatarsDir == null) {
			// Log.d(TAG, "avatars dir not available -- not downloading");
			return;
		}
	
		// dodajemy do kolejki do pobrania
		// jesli nie ma go aktualnie na dysku

		String from = name;
		int pos = from.indexOf("@");
		if (pos != -1) {
			from = from.substring(0, pos);
		}

		QueueItem item = new QueueItem();
		item.md5 = md5;
		item.url = urlRoot + "/avatar/" + from + "/" + type + "?" + token;

		queue.add(item);

		if (thread.getState() == State.NEW) {
			thread.start();
		} else if (thread.getState() == State.TERMINATED) {
			thread = new Thread(runner);
			thread.start();
		}
	}

	
	public String
	get(String md5)
	{
		// Log.d("AvatarManager", "get: md5=" + md5);

		if (md5 == null)
			return null;

		if (! cache.containsKey(md5)) {
			return null;
		}

		QueueItem i = (QueueItem) cache.get(md5);

		return i.url;
	}


	private class
	QueueRunner implements Runnable
	{
		public void
		run()
		{
			Log.d(TAG, "RUN");
		synchronized(this) {
			while (queue.size() > 0) {
				final QueueItem item = queue.remove(0);
				final Bitmap bmp = readBitmapFromNetwork(item.url);
				if (bmp != null) {
					File file;

					try {
						file = new File(avatarsDir, item.md5 + ".png");
						OutputStream fOut = new FileOutputStream(file);
						bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
						fOut.flush();
						fOut.close();
					} catch (Exception exc) {
						Log.d(TAG, "storing error", exc);
						continue;
					}


					// skoro sciagnelismy, to podmieniamy url na lokanlna sciezke
					item.url = file.getAbsolutePath();
					cache.put(item.md5, item);

					dump_chache();

					// Cache.put(item.url.toString(), new SoftReference<Bitmap>(bmp));

					// Use a handler to get back onto the UI thread for the update
/*
					handler.post(new Runnable() {
						public void run() {
							if( item.listener != null ) {
								item.listener.imageLoaded(bmp);
							}
						}
					});
 */
				}
			}
		}
		}
	}

	private static Bitmap
	readBitmapFromNetwork(String address)
	{
		InputStream is = null;
		BufferedInputStream bis = null;
		Bitmap bmp = null;
		try {
			Log.d(TAG, "tid = " + android.os.Process.myTid() + " pobieram " + address);
			URL url = new URL(address);
			URLConnection conn = url.openConnection();
			conn.connect();
			is = conn.getInputStream();
			bis = new BufferedInputStream(is);
			bmp = BitmapFactory.decodeStream(bis);
		} catch (MalformedURLException e) {
			Log.e(TAG, "Bad URL", e);
		} catch (IOException e) {
			Log.e(TAG, "Could not get remote image", e);
		} finally {
			try {
				if (is != null)
					is.close();
				if (bis != null)
					bis.close();
			} catch (IOException e) {
				Log.w(TAG, "Error closing stream.");
			}
		}

		return bmp;
	}

	private void
	dump_chache()
	{
		Set<String> keys = cache.keySet();
		Iterator<String> iter = keys.iterator();
		while (iter.hasNext()) {
			String key = (String) iter.next();
			QueueItem val = (QueueItem) cache.get(key);

			Log.d(TAG, "key=" + key + ", md5=" + val.md5 + ", url=" + val.url);
		}
	}
}
