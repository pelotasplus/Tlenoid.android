package pl.com.nic.android.tlen;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.Toast;

public class
Tlenoid extends ListActivity
	implements View.OnClickListener, AdapterView.OnItemClickListener,
	           View.OnTouchListener, DialogInterface.OnClickListener, DialogInterface.OnCancelListener,
	           View.OnKeyListener, AdapterView.OnItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener
{
	private static final String TAG = "Tlenoid";

	private Roster buddies = null;

	private BroadcastReceiver receiver;

	private	boolean being_dispayed = false;
	private NotificationManager mNotificationManager = null;

	private boolean reload_config = false;
	private boolean start_network = true;

	private String user_name = null;
	private String password = null;
	private int chat_format = 1;

	/* show toast or preferences window?
	 * for the first time we show pref window, later just toast notice.
	 */
	private boolean no_configuration_just_toast = false;

	/* aktualna prezencja i opis */
	private int current_presence = 0;
	private String current_desc = "";
	
	private boolean auto_reconnect = true;
	
	private boolean auto_away = true;
	private boolean auto_away_status_set = false;
	private int AUTO_AWAY_PERIOD = 30;
	
//	private String k1, k2, k3;

//	private float oldX, oldY;

	private	AlertDialog incorrectDlg = null;
	private	AlertDialog userInfoDlg = null;
	private boolean     userInfo_waiting_for = false;
	private	AlertDialog signOffDlg = null;
	private	AlertDialog signingInDlg = null;
	private	AlertDialog gettingRosterDlg = null;
	private String      gotErrorMsg = null;
	private AlertDialog gotErrorDlg = null;
	private AlertDialog sessionEndDlg = null;

	private HashSet<Integer> currentDialogs = new HashSet<Integer>();
	private final int DIALOG_SIGN_OFF = 0;
	private final int DIALOG_INCORRECT_PASS = 2;
	private final int DIALOG_SIGNING_IN = 3;
	private final int DIALOG_GETTING_ROSTER = 4;
	private final int DIALOG_GETTING_USER_INFO = 6;
	private final int DIALOG_GOT_ERROR = 9;

	private GestureDetector gestDetect = null;

	/* tlen service state */
	private TlenState state = new TlenState();
	
	/* is roster already fetched? */
	private boolean got_roster;
	
	private ListView lv;
	private View roster_header;
	
	private final int PRESENCE_CHANGE_CODE = 10;
	private final int USER_INFO_EDIT_CODE  = 11;

	private Handler uiHandler = new Handler();
	
	public boolean
	onTouch(View v, MotionEvent event)
	{
		if (this.gestDetect.onTouchEvent(event)) {
			return true;
		}

		return false;
	}


	protected Dialog
	onCreateDialog(int id)
	{
		Dialog dialog;
		AlertDialog.Builder builder;
		ProgressDialog pd;

		switch(id) {
			case DIALOG_INCORRECT_PASS:
				builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.wrong_pass);
				builder.setCancelable(true);
				builder.setPositiveButton(
					android.R.string.ok,
					this
				);

				this.incorrectDlg = builder.create();
				dialog = (Dialog) this.incorrectDlg;

				break;
			case DIALOG_SIGN_OFF:
				builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.do_you_want_to_sign_off);
				builder.setCancelable(false);
				builder.setPositiveButton(
					android.R.string.yes,
					this
				);
				builder.setNegativeButton(
					android.R.string.no,
					this
				);

				this.signOffDlg = builder.create();
				dialog = (Dialog) this.signOffDlg;

				break;
			case DIALOG_GOT_ERROR:
				builder = new AlertDialog.Builder(this);
				builder.setMessage(getString(R.string.got_fatal_error) + ": " + this.gotErrorMsg);
				builder.setCancelable(false);
				builder.setPositiveButton(
					R.string.quit,
					this
				);

				this.gotErrorDlg = builder.create();
				dialog = (Dialog) this.gotErrorDlg;

				break;
			case DIALOG_GETTING_USER_INFO:
				pd = ProgressDialog.show(this, "", getString(R.string.getting_user_info), true, true, this);

				this.userInfoDlg = (AlertDialog) pd;
				dialog = (Dialog) this.userInfoDlg;

				break;
			case DIALOG_SIGNING_IN:
				pd = ProgressDialog.show(this, "", getString(R.string.singing_in), true, true, this);

				this.signingInDlg = (AlertDialog) pd;
				dialog = (Dialog) this.signingInDlg;

				break;
			case DIALOG_GETTING_ROSTER:
				pd = ProgressDialog.show(this, "", getString(R.string.getting_roster), true, true, this);

				this.gettingRosterDlg = (AlertDialog) pd;
				dialog = (Dialog) this.gettingRosterDlg;

				break;
			default:
				dialog = null;
		}

		return dialog;
	}



	public boolean
	onKey(View v, int keyCode, KeyEvent event)
	{
		// Log.d(TAG, "onKey: v=" + v.toString());

		// If the event is a key-down event on the "enter" button
		if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
			doSendMessage();
			return true;
		}

		return false;
	}


	@Override
	public boolean
	onKeyDown(int keyCode, KeyEvent event)
	{
		// Log.d(TAG, "onKeyDown");
		
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			if (buddies.isShowingRoster()) {
				myShowDialog(DIALOG_SIGN_OFF);
			} else {
				buddies.flipToRoster();
			}

			return true;
		}

		return super.onKeyDown(keyCode, event);
	}


	@Override
	public void
	onCreate(Bundle savedInstanceState)
	{
		Log.d(TAG, "onCreate");

		super.onCreate(savedInstanceState);

		this.mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		this.roster_header = vi.inflate(R.layout.roster_item, null);

		/* before first setPresence */
		load_preferences();
		
		/* nad setContentView() zeby nie pokazac domyslnych wartosci z XML */
		/* domyślnie, na starcie, jesteśmy offline */
		setPresence(new Presence("Invisible", "offline"), true /* skip network */);
		
		setContentView(R.layout.main);
		
		buddies = new Roster(getBaseContext(), findViewById(R.id.flipper));

		this.gestDetect = new GestureDetector(new MyGestureDetector(buddies));

		this.lv = getListView();
		this.lv.addHeaderView(this.roster_header);

		setListAdapter(buddies.getRosterAdapter());
		
		this.lv.setTextFilterEnabled(true);
		this.lv.setOnItemClickListener(this);
		this.lv.setOnTouchListener(this);

		registerForContextMenu(this.lv);

		// lv.setClickable(true);
		// lv.setLongClickable(true);
		// lv.setOnItemLongClickListener(this);

		prepare_broadcast_receiver();
	}
	
	
	private void
	myDismissDialog()
	{
		myDismissDialog(-1);
	}
	
	
	private void
	dumpDialogs()
	{
		String s = "dialogs [";
		for (Integer i: currentDialogs) {
			s += i.toString() + ", ";
		}
		s += "]";
		Log.d(TAG, s);
	}
	
	
	private void
	myDismissDialog(int dialog_id)
	{
		Iterator<Integer> it = currentDialogs.iterator();
		
		while (it.hasNext()) {
			Integer i = it.next();
			if (i == dialog_id || dialog_id == -1) {
				Log.d(TAG, "myDismissDialog: removing " + i.toString());
				dismissDialog(i);
				it.remove();
			}
		}
	}
	
	
	private void
	myShowDialog(int dialog_id)
	{
		if (currentDialogs.contains(dialog_id)) {
			return;
		}
		showDialog(dialog_id);
		currentDialogs.add(dialog_id);
		dumpDialogs();
	}

	private void
	prepare_broadcast_receiver()
	{
		receiver = new BroadcastReceiver()
		{
			@Override
			public void
			onReceive(Context ctx, Intent intent)
			{
				Bundle b = intent.getExtras();
				String cmd = b.getString("cmd");

				Log.d(TAG, "onReceive " + cmd);
				if (! cmd.equals("got-presence") && ! cmd.equals("got-roster")) {
					Protocol.dump_extras(TAG, b);
				}
				
				if ("got-avatar-token".equals(cmd)) {
					buddies.setAvatarToken(b.getString("token"));
				} else if ("got-error".equals(cmd)) {
					Presence p = new Presence("Invisible", getString(R.string.offline_tap_reconnect));
					setPresence(p, true /* skip network */);
					
					buddies.setAllOffline();
					
					myDismissDialog();
					
					if (being_dispayed) {
						Toast.makeText(
								getApplicationContext(),
								getString(R.string.offline_reason),
								Toast.LENGTH_LONG
						).show();
					}
				} else if ("got-user-info".equals(cmd) &&
				           userInfo_waiting_for) {
					myDismissDialog(DIALOG_GETTING_USER_INFO);
					userInfo_waiting_for = false;

					RosterItem ri = buddies.getBuddy(b.getString("id"));
					user_info_activity(ri, "info", b);
				} else if ("got-password-correct".equals(cmd)) {
					myDismissDialog(DIALOG_SIGNING_IN);

					if (! got_roster) {
						myShowDialog(DIALOG_GETTING_ROSTER);
						postToNT("send-get-roster");
					} else {
						/* na koniec ustawiamy presence i sie logujemy */
						setPresence(new Presence(current_presence, current_desc));
					}
				} else if ("got-presence".equals(cmd)) {
					String username = b.getString("username");
					String status = b.getString("status");
					String show = b.getString("show");
					String avatar_md5 = b.getString("avatar_md5");
					String avatar_type = b.getString("avatar_type");

					// RosterItem ri;
					
					// ri = 
							
					buddies.updateBuddy(username, show, status, avatar_md5, avatar_type);
					buddies.sort();
				} else if ("got-message".equals(cmd)) {
					String from = b.getString("from");
					String msg = b.getString("msg");
					String stamp = b.getString("stamp");

					RosterItem ri = buddies.getBuddy(from);
					if (ri == null) {
						Log.e(TAG, "message '" + msg + "' from unknown user " + from);
						return;
					}

					addIncomingMessage(ri, msg, stamp);
				} else if ("got-password-incorrect".equals(cmd)) {
					CommonUtils.stopTlenService(getApplicationContext()); //postToNT("stop-session");

					myDismissDialog();
					myShowDialog(DIALOG_INCORRECT_PASS);
				} else if ("got-user-removed".equals(cmd)) {
					RosterItem ri = buddies.getBuddy(b.getString("jid"));
					if (ri == null) {
						return;
					}

					buddies.removeBuddy(ri);
				/* OK */
				} else if ("got-roster".equals(cmd)) {
					myDismissDialog(DIALOG_GETTING_ROSTER);
					
					buddies.clearAllBuddies();

					Set<String> keys = b.keySet();
					Iterator<String> iter = keys.iterator();
					while (iter.hasNext()) {
						String key = (String) iter.next();

						if (! key.startsWith("ri:", 0))
							continue;

						RosterItem ri = new RosterItem(getApplicationContext(),
							b.getStringArrayList(key));

						buddies.addBuddy(ri);
					}

					/* na koniec ustawiamy presence i sie logujemy */
					setPresence(new Presence(current_presence, current_desc));

					got_roster = true;
					// get_user_info("pelotaspriv");
				} else if ("state-is".equals(cmd)) {
					state.set(b.getString("data"));
					
					if (state.isOnline()) {
						buddies.enableChats();
					} else if(state.isConnecting()) {
						Presence p = new Presence("Invisible", getString(R.string.offline_connecting));
						setPresence(p, true /* skip network */);
					}
				} else {
					Log.d(TAG, "nieznana komenda");
					Protocol.dump_extras(TAG, b);
				}
			}
		};

		IntentFilter filter = new IntentFilter(ResponseReceiver.ACTION_RESP);
		filter.addCategory(Intent.CATEGORY_DEFAULT);

		registerReceiver(receiver, filter);
	}


	@Override
	public void
	onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		RosterItem ri = null;
		
		if (v.getId() == android.R.id.list) {
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
			HeaderViewListAdapter hv = (HeaderViewListAdapter) this.lv.getAdapter();
			@SuppressWarnings("unchecked")
			ArrayAdapter<RosterItem> aa = (ArrayAdapter<RosterItem>) hv.getWrappedAdapter();
			
			ri = (RosterItem) aa.getItem(info.position - 1);
		} else if (v.getId() == R.id.header) {
			ri = buddies.getBuddyCurrent();
		}
		
		if (ri == null)
			return;
		
		MenuItem mi;
		Intent i;
		
		mi= menu.add(-1, -1, 0, getString(R.string.menu_user_info));
		i = new Intent(this, Tlenoid.class);
		i.putExtra("action", "get_contact_info");
		i.putExtra("user", ri.getUsername());
		mi.setIcon(android.R.drawable.ic_menu_info_details);
		mi.setIntent(i);
		
		mi= menu.add(-1, -1, 0, getString(R.string.menu_user_edit));
		i = new Intent(this, Tlenoid.class);
		i.putExtra("action", "edit_contact_info");
		i.putExtra("user", ri.getUsername());
		mi.setIcon(android.R.drawable.ic_menu_edit);
		mi.setIntent(i);
		
		mi= menu.add(-1, -1, 0, getString(R.string.menu_user_remove));
		i = new Intent(this, Tlenoid.class);
		i.putExtra("action", "delete_contact");
		i.putExtra("user", ri.getUsername());
		mi.setIcon(android.R.drawable.ic_menu_delete);
		mi.setIntent(i);
		
		String desc = ri.getDescription();
		if (desc != null && ! desc.equals("")) {
			CommonUtils.extendContextMenu(getApplicationContext(), menu, desc);
		}
		
	}


	@Override
	public void
	onStart()
	{
		super.onStart();

		if ("".equals(this.user_name) || "".equals(this.password)) {
			if (no_configuration_just_toast) {
				Toast.makeText(getApplicationContext(), getString(R.string.please_setup_account), Toast.LENGTH_LONG).show();
				openOptionsMenu();
			} else {
				no_configuration_just_toast = true;
				showPreferences();
			}

			return;
		}

		if (start_network) {
			start_network = false;

			startNetwork();
		}
	}
	
	
	private void
	startNetwork()
	{
			myShowDialog(DIALOG_SIGNING_IN);
			// buddies.clearAllBuddies();
			
			Bundle b = new Bundle();
			b.putString("auto-reconnect", Boolean.toString(auto_reconnect));
			b.putString("user-name",  user_name);
			b.putString("password",   password);
			
			postToNT("start-session", b);
			
			Presence p = new Presence("Invisible", getString(R.string.offline_connecting));
			setPresence(p, true /* skip network */);
	}


	@Override
	public void
	onRestart()
	{
		Log.d(TAG, "onRestart");
 
		super.onRestart();

		if (reload_config) {
			reload_config = false;
			got_roster = false;
			buddies.clearAllBuddies();
			
			start_network = true;

			postToNT("stop-session");
			
			load_preferences();
		}
	}


	@Override
	public void
	onStop()
	{
		Log.d(TAG, "onStop");
		
		super.onStop();
		
		startAutoAway();
	}
	
	
	private Runnable autoAwayTask = new Runnable() {
		   public void run() {
			   if (being_dispayed)
				   return;

			   try {
				   Log.d(TAG, "doing auto away");
				   
				   setPresence(new Presence("Away", current_desc));
				   auto_away_status_set = true;
			   } catch (Exception exc) {
				   Log.e(TAG, "auto_away_timer", exc);
			   }
		   }
	};
	
	
	private void
	startAutoAway()
	{
		if (! auto_away) {
			return;
		}
		
        uiHandler.postDelayed(autoAwayTask, AUTO_AWAY_PERIOD * 1000);
	}

	
	private void
	stopAutoAway()
	{
        uiHandler.removeCallbacks(autoAwayTask);
	}
	

	@Override
	public void
	onResume()
	{
		Log.d(TAG, "onResume");

		super.onResume();

		if (auto_away) {
			if (auto_away_status_set) {
				setPresence(new Presence(this.current_presence,	this.current_desc));
			}
			auto_away_status_set = false;
		}
		
		this.being_dispayed = true;

		this.mNotificationManager.cancelAll();
	}

	@Override
	public void
	onPause()
	{
		super.onPause();

		this.being_dispayed = false;
	}


	public void
	onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		Log.d(TAG, "changed " + key);

		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

		reload_config = false;

		if ("account_username".equals(key)) {
			Log.d(TAG, "un " + settings.getString(key, "") + ", " + user_name);
			if (! settings.getString(key, "").equals(user_name)) {
				reload_config = true;
			}	
		} else if ("account_password".equals(key)) {
			if ( ! settings.getString(key, "").equals(password)) {
				reload_config = true;
			}
		} else if ("auto_reconnect".equals(key)) {
			if (settings.getBoolean(key, false) != auto_reconnect) {
				reload_config = true;
			}
		} else if ("auto_away".equals(key)) {
			if (settings.getBoolean(key, false) != auto_away) {
				reload_config = true;
			}
		} else if ("chat_style".equals(key)) {
			int new_chat_style = Integer.parseInt(settings.getString("chat_style", "1"));
			if (chat_format != new_chat_style) {
				chat_format = new_chat_style;
				buddies.setChatFormat(chat_format);
			}
		}
		
		Log.d(TAG, "reload " + reload_config);
	}
	

	private void
	load_preferences()
	{
		// Restore preferences
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		settings.registerOnSharedPreferenceChangeListener(this);

		user_name = settings.getString("account_username", "");
		password = settings.getString("account_password", "");

		current_presence = settings.getInt("default_presence", 0);
		current_desc = settings.getString("default_desc", "");
		
		auto_reconnect = settings.getBoolean("auto_reconnect", true);
		auto_away = settings.getBoolean("auto_away", true);
		
		chat_format = Integer.parseInt(settings.getString("chat_style", "1"));
		
		Log.d(TAG, "CHAT FORMAT IS " + chat_format);

	}


	private void
	remove_user(RosterItem ri)
	{
		user_info_activity(ri, "delete");
	}


	private void
	user_info_activity(RosterItem ri, String action)
	{
		user_info_activity(ri, action, null);
	}


	private void
	user_info_activity(RosterItem ri, String action, Bundle extras)
	{
		Bundle b;

		if (extras != null)
			b = new Bundle(extras);
		else
			b = new Bundle();
		
		if (ri != null) {
			b.putString("status", ri.getStatus());
			b.putString("avatar_path", ri.getAvatarPath());
			b.putString("username", ri.getUsername());
			b.putString("alias", ri.getAlias());
		}

		b.putString("action", action);

		Intent i = new Intent(Tlenoid.this, UserInfo.class);
		i.putExtras(b);
		startActivityForResult(i, this.USER_INFO_EDIT_CODE);
	}

	private void
	edit_user_info(RosterItem ri)
	{
		user_info_activity(ri, "edit");
	}


	private void
	get_user_info(RosterItem ri)
	{
		myShowDialog(DIALOG_GETTING_USER_INFO);
		userInfo_waiting_for = true;

		Bundle b = new Bundle();
		b.putString("user-name", ri.getUsername());
		postToNT("get-user-info", b);
	}


	private void
	spawnPresenceActivity()
	{
		Intent i = new Intent(Tlenoid.this, PresenceSetting.class);
		i.putExtra("presence", this.current_presence);
		i.putExtra("desc", this.current_desc);
		startActivityForResult(i, this.PRESENCE_CHANGE_CODE);
	}


	public void
	onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		HeaderViewListAdapter hv = (HeaderViewListAdapter) parent.getAdapter();
		@SuppressWarnings("unchecked")
		ArrayAdapter<RosterItem> aa = (ArrayAdapter<RosterItem>) hv.getWrappedAdapter();

		/* klik w pierwszy element rostera -- moj status */
		if (position == 0) {
			if (state.isOnline()) {
				spawnPresenceActivity();
			} else if (state.isOffline()) {
				startNetwork();
			}
		/* klik goscia z rostera */
		} else {
			/* na liscie na gorze jestem ja sam, wiec odejmujemy -1 od position */
			RosterItem ri = (RosterItem) aa.getItem(position - 1);
			
			/* w trybie tworzymy nowe czaty jesli trzeba */
			if (state.isOnline()) {
				buddies.startChatWithBuddy(ri, this, chat_format);
			}
			
			buddies.showChat(ri);
		}
	}

	
	@Override
	protected void
	onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "onActivityResult; result=" + resultCode + ", request=" +  requestCode);

		if (resultCode == RESULT_OK) {
			Log.d(TAG, "RESULT_OK");

			Bundle extras = data.getExtras();
			// Protocol.dump_extras(TAG, extras);

			if (requestCode == this.PRESENCE_CHANGE_CODE) {
				this.current_presence = data.getExtras().getInt("presence");
				this.current_desc = data.getExtras().getString("desc");
		
				setPresence(new Presence(this.current_presence, this.current_desc));
			} else if (requestCode == this.USER_INFO_EDIT_CODE) {
				String action = extras.getString("action");
				if (action == null)
					action = "";

				if (action.equals("edit")) {
					RosterItem ri = buddies.getBuddy(extras.getString("id"));

					String new_alias = extras.getString("new-alias");
			
					if (ri != null && new_alias != null) {
						extras.putString("group", ri.getGroup());

						postToNT("set-alias", extras);

						ri.setAlias(new_alias);

						buddies.updateBuddy(ri.getUsername(), new_alias);
						buddies.sort();
					} else {
						Log.e(TAG, "ri=" + ri + ", new_alias=" + new_alias);
					}
				} else if (action.equals("delete")) {
					postToNT("delete-user", extras);
				}
			}
		} else {
			Log.d(TAG, "RESULT NOT OK");
		}
	}

	public void
	onItemSelected(AdapterView<?> parent, View view, int pos, long id)
	{
		Presence p = new Presence(pos);

		setPresence(p);
	}

	public void
	onNothingSelected(AdapterView<?> parent)
	{
	}

	
/*
	@Override
	public boolean
	onItemLongClick(AdapterView<?> parent, View v, int position, long id)
	{
		Log.d("FFFF", "ALEK");

		// record position/id/whatever here
		ArrayAdapter<RosterItem> aa = (ArrayAdapter) parent.getAdapter();
		RosterItem ri = (RosterItem) aa.getItem(position);

		getUserInfo(ri);

		return true;
	}
 */


	private void
	showPreferences()
	{
		startActivity(new Intent(getBaseContext(), Preferences.class));
	}


	@Override
	public boolean
	onPrepareOptionsMenu(Menu menu)
	{
		if (! buddies.isShowingRoster()) {
			menu.setGroupVisible(R.id.chat_menu_group, true);
			menu.setGroupVisible(R.id.roster_menu_group, false);
		} else {
			menu.setGroupVisible(R.id.chat_menu_group, false);
			menu.setGroupVisible(R.id.roster_menu_group, true);
		}

		return true;
	}


	@Override
	public boolean
	onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.roster, menu);
		return true;
	}


	@Override
	public boolean
	onOptionsItemSelected(MenuItem item)
	{
		Log.d(TAG, "onOptionsItemSelected: " + item.toString());

		RosterItem ri;
		
		switch (item.getItemId())
		{
			case R.id.QuitMenu:
				myShowDialog(DIALOG_SIGN_OFF);
				return true;
			case R.id.PresenceMenu:
				spawnPresenceActivity();
				return true;
			case R.id.SettingsMenu:
				showPreferences();
				return true;
			case R.id.CloseChat:
				ri = buddies.getBuddyCurrent();
				buddies.removeChat(ri);
				return true;
			case R.id.GetUserInfo:
				ri = buddies.getBuddyCurrent();
				get_user_info(ri);
				return true;
			case R.id.EditUserInfo:
				ri = buddies.getBuddyCurrent();
				edit_user_info(ri);
				return true;
			case R.id.RemoveUser:
				ri = buddies.getBuddyCurrent();
				
				remove_user(ri);
				
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	
	/*
	 * from -> kto wyslal
	 * msg -> tresc wiadomosci
	 * stamp -> timestamp o ile jest)
	 */
	private void
	addIncomingMessage(RosterItem from, String msg, String stamp)
	{
		if (from == null) {
			return;
		}

		RosterItem current_buddy = buddies.getBuddyCurrent();

		Chat c = buddies.startChatWithBuddy(from, this, chat_format);
		c.addMessage(from, stamp, msg);
		
		/* jeśli apka jest w tle -- powiadomienie na pasku */
		if (! this.being_dispayed) {
			Intent notificationIntent = new Intent(this, Tlenoid.class);
			notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP); 
			notificationIntent.putExtra("from", from.getUsername());

			PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

			Notification notification = new Notification(
					android.R.drawable.stat_notify_chat,
					from.getPrettyAlias() + ": " + msg,
					System.currentTimeMillis()
					);
			notification.setLatestEventInfo(
					getApplicationContext(),
					from.getPrettyAlias() /* contentTitle */,
					msg /* contentText */,
					contentIntent);
			notification.flags |= Notification.FLAG_AUTO_CANCEL;

			this.mNotificationManager.notify(1, notification);
		/* toast tylko jesli jestesmy pokazywani a wiadomosc jest do okna innej rozmowy niz prowadzona
		 */
		} else if (
			this.being_dispayed && 
			(
				current_buddy == null ||
				! current_buddy.getUsername().equals(from.getUsername())
			)
		) {
			Toast.makeText(getApplicationContext(), from.getPrettyAlias() + ": " + msg, Toast.LENGTH_SHORT).show();
		}
	
		// jesli nie prowadzimy rozmowy, to dodajamy powiadomienie na liscie kontaktow
		// o tym ze w oknie rozowy jest nowa wiadomosc
		if (buddies.isShowingRoster()) {
			buddies.addUnreadMessage(from);
		}
	}
	
	public void
	onNewIntent(Intent i)
	{
		Log.d(TAG, "onNewIntent i=" + i.toString());

		Bundle b = i.getExtras();
		if (b == null) {
			Log.e(TAG, "onNewIntent no extras!");
			return;
		}
		
		Protocol.dump_extras(TAG, b);
		
		String action = b.getString("action");
		
		if (action == null) {
			String from = b.getString("from");
			if (from == null) {
				Log.e(TAG, "onNewIntent: from is null");
				return;
			} else {
				Log.d(TAG, "from=" + from);
				RosterItem buddy = buddies.getBuddy(from);
				buddies.showChat(buddy);
			}
		} else if (action.equals("copy_to_clipboard")) {
			String data = b.getString("data");
			ClipboardManager cb = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
			cb.setText(data);
			
			Toast.makeText(
					getApplicationContext(),
					getString(R.string.context_menu_copied),
					Toast.LENGTH_SHORT
			).show();
		} else if (action.equals("get_contact_info")) {
			String username = b.getString("user");
			get_user_info(buddies.getBuddy(username));
		} else if (action.equals("edit_contact_info")) {
			String username = b.getString("user");
			edit_user_info(buddies.getBuddy(username));
		} else if (action.equals("delete_contact")) {
			String username = b.getString("user");
			remove_user(buddies.getBuddy(username));
		}
	} 

	public void
	onClick(DialogInterface dialog, int id)
	{
		Log.d(TAG, "onClick (dialog): id=" + id);
		
		if (dialog == this.incorrectDlg) {
			dialog.cancel();
			showPreferences();
		} else if (dialog == this.signOffDlg) {
			if (id == -1) {
				finish();
			} else if (id == -2) {
				dialog.cancel();
			}
		} else if (dialog == this.gotErrorDlg) {
			this.finish();
		} else if (dialog == this.sessionEndDlg) {
			// this.finish();
		}
	}


	@Override
	public void
	onDestroy()
	{
		Log.d(TAG, "onDestroy()");

		// this.onDestroyCalled = true;

		super.onDestroy();

		stopAutoAway();
		unregisterReceiver(receiver);

		CommonUtils.stopTlenService(getApplicationContext()); //postToNT("stop-service");
	}


	private void
	doSendMessage()
	{
		Chat c = buddies.getChatCurrent();
		
		String msg = c.getMessageToSend();
		if (msg.length() == 0)
			return;
		
		c.addMessage(getString(R.string.me), null, msg);

		Bundle b = new Bundle();
		b.putString("message", msg);
		b.putString("to", c.getUser().getUsername());
		
		postToNT("send-message", b);
	}


	public void
	onClick(View v)
	{
		Log.d(TAG, "onClick (view): v=" + v.toString());

		if (v.getId() == R.id.send_button) {
			doSendMessage();
		}
	}


	public void
	postToNT(String line, Bundle b)
	{
		CommonUtils.postToTlenService(getApplicationContext(), line, b);
	}

	public void
	postToNT(String line)
	{
		CommonUtils.postToTlenService(getApplicationContext(), line, null);
	}

	private void
	setPresence(Presence p)
	{
		setPresence(p, false /* skip_network */);
	}

	private void
	setPresence(Presence p, boolean skip_network)
	{
		Log.d(TAG, "setPresence: " + p.toString());

		if (! skip_network) {
			Bundle b = new Bundle();
			b.putString("presence", p.getCode());
			b.putString("message", p.getDesc());

			postToNT("send-set-presence", b);
		}

		RosterItem ri = new RosterItem(getApplicationContext(), this.user_name);
		ri.updateStatus(p.getCode());
		ri.updateDescription(p.getDesc());

		CommonUtils.setBuddyDesc(this, this.roster_header, ri);
	}

	public void
	onCancel(DialogInterface dialog)
	{
		//Log.d(TAG, "onCancel");

		if (dialog == this.signingInDlg) {
			this.finish();
		} else if (dialog == this.userInfoDlg) {
			userInfo_waiting_for = false;
			myDismissDialog(DIALOG_GETTING_USER_INFO);
		}
	}
}