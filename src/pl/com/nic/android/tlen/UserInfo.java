package pl.com.nic.android.tlen;

import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class
UserInfo extends Activity
	implements View.OnClickListener
{
	private final String TAG = "UserInfo";

	private Bundle extras = null;

	private String action = "";
	private boolean yes_do_remove = false;

	public void
	onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.user_info_activity);

		extras = getIntent().getExtras();

		//Log.d(TAG, "onCreate");
		Protocol.dump_extras(TAG, extras);

		/* user id */
		TextView tv;
		
		tv = (TextView) findViewById(R.id.user_info_user_name);
		tv.setText(extras.getString("username"));
		
		/* current alias */
		tv = (TextView) findViewById(R.id.user_info_alias);
		tv.setText(extras.getString("alias"));

		/* avatar */
		ImageView avtImg = (ImageView) findViewById(R.id.user_info_img);

		String path = extras.getString("avatar_path");
		if (path != null) {
		//	Log.d(TAG, "path=" + path);
			avtImg.setImageDrawable(Drawable.createFromPath(path));
		} else {
			avtImg.setImageResource(CommonUtils.buddy_status2drawable(extras.getString("status")));
		}

		TableLayout table = (TableLayout) findViewById(R.id.user_info_table);

		action = extras.getString("action");
		if (action == null)
			action = "";

		//Log.d(TAG, "ACTION= " + action);

		if (action.equals("delete")) {
			LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = vi.inflate(R.layout.user_info_remove, null);

			table.addView(v);
		} else if (action.equals("edit")) {
			LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View v = vi.inflate(R.layout.user_info_edit, null);

			/* alias */
			EditText alias = (EditText) v.findViewById(R.id.user_info_new_alias);
			alias.setText(extras.getString("alias"));

			table.addView(v);
		} else if (action.equals("info")) {
			if (extras.getString("empty-response") != null) {
				table.addView(create_row(getString(R.string.user_info_not_available)));
			} else {
				table.addView(create_row(getString(R.string.user_info_available)));
				table.addView(create_row(""));

				/* poszczegolne cechy */
				Iterator<String> iter = Protocol.tlen_get_user_info_keys().iterator();
				while (iter.hasNext()) {
					String key = (String) iter.next();

					String value = extras.getString(key);
					if (value == null)
						continue;

					String pretty_name = getString(Protocol.tlen_get_str_for_user_info_key(key));

					if (key.equals("s")) {
						if (value.equals("1"))
							value = getString(R.string.male);
						else
							value = getString(R.string.female);
					}

					table.addView(create_double_row(pretty_name, value));
				}
			}
		}
	}

	@Override
	public void
	finish()
	{
		Log.d(TAG, "finish: action=" + action);

		if (action.equals("edit")) {
			EditText edit = (EditText) findViewById(R.id.user_info_new_alias);
			String new_alias = edit.getText().toString();

			String orig_alias = extras.getString("alias");

			Log.d(TAG, "new_alias='" + new_alias + "' orig_alias='" + orig_alias + "'");

			if (! orig_alias.equals(new_alias)) {
				Intent data = new Intent();
				data.putExtra("id", extras.getString("username"));
				data.putExtra("new-alias", new_alias);
				data.putExtra("action", "edit");

				setResult(RESULT_OK, data);
			}
		} else if (action.equals("delete") && (yes_do_remove == true)) {
			Log.d(TAG, "FFFF");
			Intent data = new Intent();
			data.putExtra("id", extras.getString("username"));
			data.putExtra("action", "delete");

			setResult(RESULT_OK, data);
		}
	
		super.finish();
	}

	
	private TableRow
	create_double_row(String field, String value)
	{
		TableRow row = new TableRow(this);

		TextView text = new TextView(this);
		text.setText(field);

		int padding_in_dp = 10;
		final float scale = getResources().getDisplayMetrics().density;
		int padding_in_px = (int) (padding_in_dp * scale + 0.5f);

		text.setPadding(0, 0, padding_in_px, 0);

		row.addView(text);

		TextView text2 = new TextView(this);
		text2.setText(value);
		text2.setGravity(Gravity.LEFT);
		text2.setTextSize(14);
		text2.setTypeface(null, Typeface.BOLD);

		row.addView(text2);

		return row;
	}


/*	private TableRow
	create_delete_row(String field)
	{
		TableRow row = new TableRow(this);

		TableRow.LayoutParams params = new TableRow.LayoutParams();
		params.span = 2;

		Button okBtn = new Button(this);
		okBtn.setOnClickListener(this);
		okBtn.setText("OK");


		TextView text = new TextView(this);
		text.setText(field);
		text.setTextSize(14);

		text.setLayoutParams(params);

		row.addView(text);

		return row;
	}*/


	private TableRow
	create_row(String field)
	{
		TableRow row = new TableRow(this);

		TableRow.LayoutParams params = new TableRow.LayoutParams();
		params.span = 2;

		TextView text = new TextView(this);
		text.setText(field);
		text.setTextSize(14);
		text.setGravity(Gravity.CENTER_HORIZONTAL);

		text.setLayoutParams(params);

		row.addView(text);

		return row;
	}


	public boolean
	onKey(View v, int keyCode, KeyEvent event)
	{
		return false;
	}


	public void 
	onClick(View v)
	{
		Log.d(TAG, "onClick " + v.toString());

		if (v.getId() == R.id.user_remove_ok) {
			yes_do_remove = true;
			finish();
		} else if (v.getId() == R.id.user_remove_cancel) {
			finish();
		}
	}
}
