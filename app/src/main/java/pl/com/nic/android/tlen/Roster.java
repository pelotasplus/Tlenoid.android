package pl.com.nic.android.tlen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import android.content.Context;
import android.gesture.GestureOverlayView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ViewFlipper;

public class
Roster
{
	private ArrayList<RosterItem> buddies = null;
	private ArrayList<RosterItem> chats = null;
	private RosterAdapter rosterAdapter;
	
	private Context ctx;
	private ViewFlipper flipper;
	
	private final String TAG = "Tlenoid";
	
	public
	Roster(Context context, View flipper_)
	{	
		ctx = context;
		flipper = (ViewFlipper) flipper_;
		
		buddies = new ArrayList<RosterItem>();
		
		chats = new ArrayList<RosterItem>();
		
		rosterAdapter = new RosterAdapter(ctx, R.layout.roster_item, buddies);
	}
	
	
	public int
	getChatsCount()
	{
		return chats.size();
	}
	
	public RosterAdapter
	getRosterAdapter()
	{
		return rosterAdapter;
	}
	
	public void
	setAvatarToken(String token)
	{
	}


	public void
	updateBuddy(String username, String alias)
	{
		// Log.d(TAG, "updateBuddy " + username);
		
		RosterItem ri = getBuddyByName(username);
		if (ri == null) {
			Log.d(TAG, "unknown buddy to update: " + username +
				", alias=" + alias);
			return;
		}
		
		ri.setAlias(alias);
		ri.updateChatHeader();
	}
	
	
	// TODO merge with updateBuddy above
	public void
	updateBuddy(String username, String show, String status, String avatar_md5, String avatar_type)
	{
		// Log.d(TAG, "updateBuddy " + username);
		
		RosterItem ri = getBuddyByName(username);
		if (ri == null) {
			Log.d(TAG, "unknown buddy to update");
			return;
		}
		
		ri.updateStatus(show);
		ri.updateDescription(status);
		ri.updateAvatar(avatar_md5);
		ri.updateChatHeader();
	}
	
	public ArrayList<RosterItem>
	getArrayList()
	{
		return buddies;
	}
	
	public void
	addBuddy(RosterItem ri)
	{
		// Log.d(TAG, "adding " + ri.toString());
		
		buddies.add(ri);
		Collections.sort(buddies);
		
		rosterAdapter.notifyDataSetChanged();
	}

	public void
	clearUnreadMessages(RosterItem ri)
	{
		Log.d(TAG, "clearUnreadMessages: ri=" + ri);
		
		ri.clear_unread_messages();
		rosterAdapter.notifyDataSetChanged();
	}
	
	public void
	addUnreadMessage(RosterItem ri)
	{
		ri.add_unread_message();
		rosterAdapter.notifyDataSetChanged();
	}
	
	/* pobiera buddy po uzytkownik@tlen.pl */
	private RosterItem
	getBuddyByName(String id)
	{
		RosterItem ri;
		Iterator<RosterItem> iter;

		iter = buddies.iterator();
		while (iter.hasNext()) {
			ri = (RosterItem) iter.next();

			if (ri.getUsername().equals(id)) {
				return ri;
			}
		}

		return null;
	}
	
	public RosterItem
	getBuddy(String jid_or_username)
	{		
		if (jid_or_username == null) {
			return null;
		}
		
		String name = jid_or_username;
		int pos = name.indexOf("@");
		if (pos == -1) {
			name = name + "@tlen.pl";
		}

		return getBuddyByName(name);
	}
	
	public void
	removeBuddy(RosterItem ri)
	{
		removeChat(ri);
		buddies.remove(ri);
		rosterAdapter.notifyDataSetChanged();
	}
	
	public void
	clearAllBuddies()
	{
		buddies.clear();
		chats.clear();
		// 1 stands for view -- list view with buddes
		flipper.removeViews(1, flipper.getChildCount() - 1);
		rosterAdapter.notifyDataSetChanged();
	}
	
	
	public void
	setChatFormat(int new_format)
	{
		for (RosterItem ri: chats) {
			Chat c = ri.getChat();
			c.setStyle(new_format);
		}
	}
	
	
	public void
	setAllOffline()
	{
		Log.d(TAG, "setAllOffline");
		for (RosterItem ri: buddies) {
			ri.updateStatus("unavailable");
		}
		for (RosterItem ri: chats) {
			ri.updateChatHeader();
			
			Chat c = ri.getChat();
			CommonUtils.blockSendButton(c.getView());
		}
		rosterAdapter.notifyDataSetChanged();
	}
	
	
	/* enable send button for chats */
	public void
	enableChats()
	{
		for (RosterItem ri: chats) {
			Chat c = ri.getChat();
			CommonUtils.unblockSendButton(c.getView());
		}
	}
	
	
	public Chat
	startChatWithBuddy(RosterItem ri, Tlenoid obj, int format)
	{
		Log.d(TAG, "start chat with format " + format);
		
		if (! buddies.contains(ri)) {
			Log.e(TAG, "starting chat with nonexistent buddy? ri=" + ri);
			return null;
		}
		
		if (chats.contains(ri)) {
			Log.d(TAG, "already running chat with " + ri);
			clearUnreadMessages(ri);
			return ri.getChat();
		}
		
		LayoutInflater vi = (LayoutInflater) obj.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View v = vi.inflate(R.layout.chat, null);
		
		
		View hdr = v.findViewById(R.id.header);
		obj.registerForContextMenu(hdr);

		GestureOverlayView v1 = (GestureOverlayView) v.findViewById(R.id.chat_container);
		v1.setOnTouchListener(obj);

		ListView v2 = (ListView) v.findViewById(R.id.chat_lv);
		v2.setOnTouchListener(obj);
		
		/*
		ScrollView v2 = (ScrollView) v.findViewById(R.id.chat_sv);
		v2.setOnTouchListener(obj);
		 */

		Button btn = (Button) v.findViewById(R.id.send_button);
		btn.setOnClickListener(obj);

		EditText edit = (EditText) v.findViewById(R.id.edittext);
		edit.setOnKeyListener(obj);

		/*
		TextView chatMsg = (TextView) v.findViewById(R.id.chat_msgs);
		chatMsg.setText("");
		 */

		Chat c = new Chat(ctx, v, ri, format);
		
		Log.d(TAG, "created chat " + c + " for " + ri);
		
		ri.setChat(c);
		ri.updateChatHeader();
		
		chats.add(ri);
		
		flipper.addView(v);
		
		c.setActive();
		
		return c;
	}
	
	
	public boolean
	isShowingRoster()
	{
		return flipper.getDisplayedChild() == 0;
	}
	
	
	public void
	removeChat(RosterItem ri)
	{
		if (ri == null) {
			return;
		}
		
		if (! chats.contains(ri)) {
			return;
		}
		
		Chat c = ri.getChat();
		
		flipper.removeView(c.getView());
		ri.setChat(null);
		chats.remove(ri);
		
		c = null;
	}
	
	/* f-cja pokazuje jesli ma co pokazac.
	 * jesli czat nie istnieje, to nic nie robi -- nie sypie tez bledem.
	 * klienci tej f-cji oczekuja tego!
	 */
	public void
	showChat(RosterItem buddy)
	{
		int i = 1;
		for (RosterItem c: chats) {
			if (c.equals(buddy)) {
				flipper.setOutAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.fade_out));
				flipper.setInAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.fade_in));
				
				flipper.setDisplayedChild(i);
				
				c.getChat().setActive();
				
				clearUnreadMessages(c);
				break;
			}
			i = i + 1;
		}
	}
	
	
	public void
	flipToRoster()
	{
		flipper.setDisplayedChild(0);
	}
	
	
	public Chat
	getChatCurrent()
	{
		int pos = flipper.getDisplayedChild();
		
		// remove one as there is extra main view in filppper with roster */
		pos = pos - 1;
		
		if (pos < 0 || pos >= chats.size())
			return null;
		
		return chats.get(pos).getChat();
	}
	
	
	/* get buddy for current chat being shown (if any) */
	public RosterItem
	getBuddyCurrent()
	{
		int pos = flipper.getDisplayedChild();
		
		// remove one as there is extra main view in filppper with roster */
		pos = pos - 1;
		
		if (pos < 0 || pos >= chats.size())
			return null;
		
		return chats.get(pos);
	}
	
	
	public void
	showNext()
	{
		Animation fade = AnimationUtils.loadAnimation(ctx, android.R.anim.fade_out);
		flipper.setOutAnimation(fade);
		flipper.setInAnimation(AnimationUtils.loadAnimation(ctx, R.anim.slide_right));

		flipper.showNext();

		flipper.setInAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.fade_in));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.fade_out));
		
		Chat c = getChatCurrent();
		if (c != null) {
			c.setActive();
			clearUnreadMessages(getBuddyCurrent());
		}
	}
				
	public void
	showPrev()
	{
		flipper.setOutAnimation(AnimationUtils.loadAnimation(ctx, R.anim.slide_left));
		Animation fade = AnimationUtils.loadAnimation(ctx, android.R.anim.fade_in);
		fade.setDuration(2000);
		flipper.setInAnimation(fade);

		flipper.showPrevious();

		flipper.setInAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.fade_in));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(ctx, android.R.anim.fade_out));

		Chat c = getChatCurrent();
		if (c != null) {
			c.setActive();
			clearUnreadMessages(getBuddyCurrent());
		}
	}
	
	
	public void
	sort()
	{
		Collections.sort((List<RosterItem>) buddies);
		rosterAdapter.notifyDataSetChanged();
	}
}
