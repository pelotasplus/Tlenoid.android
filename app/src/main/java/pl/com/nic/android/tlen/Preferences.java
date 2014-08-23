package pl.com.nic.android.tlen;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

public class
Preferences extends PreferenceActivity
{
	public void
	onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);

		Toast.makeText(getApplicationContext(),
			R.string.pref_save_note,
			Toast.LENGTH_SHORT).show();
	}

	public void
	onDestroy()
	{
		Log.d("Preferences", "onDestroy()");

		super.onDestroy();
	}
}
