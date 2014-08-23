package pl.com.nic.android.tlen;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class
PresenceSetting extends Activity
	implements AdapterView.OnItemSelectedListener, View.OnClickListener
{
	private Spinner sp;
	private int p;
	private String d;
	private boolean preference_mode = false;

	public void
	onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.presence);

		Bundle extras = getIntent().getExtras();

		/* przekazane parametry -- zostalismy przeslani przez menu
		 * zmiany prezencji.
		 */
		if (extras != null) {
			this.p = extras.getInt("presence");
			this.d = extras.getString("desc");
		/* w przypadku braku paramtru znaczy ze ktos nas wywoluje przez
		 * okno konfiguracji.
		 */
		} else {
                	SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
			this.p = settings.getInt("default_presence", 0);
			this.d = settings.getString("default_desc", "");
			
			this.preference_mode = true;
		}


 		this.sp = (Spinner) this.findViewById(R.id.presence_spinner);

		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
			this, R.array.Presences, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		this.sp.setAdapter(adapter);
		this.sp.setOnItemSelectedListener(this);

		this.sp.setSelection(this.p);

		Button btn;

		btn = (Button) this.findViewById(R.id.cancel_button);
		btn.setOnClickListener(this);

		btn = (Button) this.findViewById(R.id.apply_button);
		btn.setOnClickListener(this);

		
		EditText edit = (EditText) findViewById(R.id.presence_desc);
		edit.setText(this.d);
	}

	public void
	onDestroy()
	{
		super.onDestroy();
	}

	public void
	onClick(View v)
	{
		if (v.getId() == R.id.apply_button) {
			if (this.preference_mode == true) {
                		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
				SharedPreferences.Editor e = settings.edit();

				e.putInt("default_presence", this.p);

				EditText edit = (EditText) findViewById(R.id.presence_desc);
				String msg = edit.getText().toString();

				e.putString("default_desc", msg);

				e.commit();
			} else {
				Intent data = new Intent();

				data.putExtra("presence", this.p);

				EditText edit = (EditText) findViewById(R.id.presence_desc);
				String msg = edit.getText().toString();

				data.putExtra("desc", msg);

				setResult(RESULT_OK, data);
			} 
		}

		super.finish();
	}

	public void
	onItemSelected(AdapterView<?> parent, View view, int pos, long id)
	{
		this.p = pos;
	}

	public void
	onNothingSelected(AdapterView<?> parent)
	{
	}
}
