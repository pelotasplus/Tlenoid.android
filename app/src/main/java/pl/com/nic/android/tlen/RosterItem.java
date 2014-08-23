package pl.com.nic.android.tlen;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;

import android.content.Context;
import android.util.Log;

public class
RosterItem implements Comparable<RosterItem>
{
	private String username = null;
	private String alias = null;
	private String avatar = null;
	private String group_name = null;

	private String subscription = "none";
	private String description = "";
	private String status = "unavailable";
	
	private String TAG = "Tlenoid";
	
	private Chat chat = null;
	
	private int unread_messages = 0;

	private Hashtable<String, Integer> statusPri = new Hashtable<String, Integer>();
	
	private final String STATUS_UNAUTHORIZED;

    private Context context;

	public
	RosterItem(Context ctx, String username_, String alias_, String subscription_, String group_name_)
	{
        context = ctx;
		username = username_;
		alias = alias_;
		subscription = subscription_;
		group_name = group_name_;
		STATUS_UNAUTHORIZED = ctx.getString(R.string.status_unauthorized);

		statusPri.put("available", 0);
		statusPri.put("chat", 1);
		statusPri.put("away", 2);
		statusPri.put("xa", 3);
		statusPri.put("dnd", 4);
		statusPri.put("unauthorized", 5);
		statusPri.put("unavailable", 5);
	}


	public
	RosterItem(Context ctx, ArrayList<String> params)
	{
		this(ctx, params.get(0), params.get(1), params.get(2), params.get(3));
	}
	
	
	public
	RosterItem(Context ctx, String username_)
	{
		this(ctx, username_, "", "both", "");
	}
	
	public Chat
	getChat()
	{
		return chat;
	}
	
	
	public void
	setChat(Chat c)
	{
		Log.d(TAG, "setChat: for " + toString() + " " + chat + " -> " + c);
		
		chat = c;
	}
	
	
	/* zwraca pelotasplus (bez @tlen.pl) */
	public String
	getShortUsername()
	{
		int pos = username.indexOf("@");
		if (pos != -1) {
			return username.substring(0, pos);
		} else {
			return username;
		}
	}
	
	/* zwraca pelotasplus@tlen.pl */
	public String
	getUsername()
	{
		return username;
	}

	
	public void
	updateStatus(String status)
	{
		this.status = status;
	}

	
	public void
	updateDescription(String description)
	{
		this.description = description;
	}


	public void
	updateAvatar(String avatar)
	{
		this.avatar = avatar;
	}

	
	public String
	getPrettyAlias()
	{
		if ("".equals(getAlias()))
			return getUsername();
		else
			return getAlias();
	}


	public String
	getAlias()
	{
		return alias;
	}


	public void
	setAlias(String alias_)
	{
		alias = alias_;
	}


	public String
	getSubscription()
	{
		return subscription;
	}


	public String
	getStatus()
	{
		if (! subscription.equals("both"))
			return "unauthorized";

		return status;
	}

	
	public String
	getDescription()
	{
		try {
			return URLDecoder.decode(description, "UTF-8");
		} catch (Exception exc) {
			Log.e("RosterItem", "decode description=" + description, exc);
			return description;
		}
	}

	
	public String
	getAvatar()
	{
		return avatar;
	}
	
	public String
	getAvatarPath()
    {
        return "http://poczta.o2.pl/avatar/" + username + "/0/";
	}


	public String
	getPrettyDescription()
	{
		if (! getSubscription().equals("both")) {
			return STATUS_UNAUTHORIZED;
		} else {
			return getDescription().replace("\n", " ");
		}
	}

	
	public int
	getStatusPri()
	{
		if (statusPri.containsKey(getStatus()))
			return (Integer) statusPri.get(getStatus());
		else
			return 9999;
	}
	
	
	@Override
	public boolean
	equals(Object other)
	{
		if (this == other) return true;
		if (! (other instanceof RosterItem)) return false;
		
		RosterItem o = (RosterItem) other;
		return o.getUsername().equals(getUsername());
	}


	public String
	toString()
	{
		return "RosterItem@" + hashCode() + "{username="+getUsername()+", chat=" + chat + ", alias="+getAlias()+", subscription="+getSubscription()
		        +", status="+getStatus()+", description="+getDescription()+", avatar=" + getAvatar() + ", group_name=" + group_name + "}";
	}

	public int
	compare(RosterItem other)
	{
		if (this.getStatusPri() < other.getStatusPri())
			return -1;

		if (this.getStatusPri() > other.getStatusPri())
			return 1;

		/* jesli sa takie same statusy to porownujemy alias */

		return this.getPrettyAlias().compareToIgnoreCase(other.getPrettyAlias());
	}

	public void
	add_unread_message()
	{
		this.unread_messages = this.unread_messages + 1;
	}

	public void
	clear_unread_messages()
	{
		this.unread_messages = 0;
	}

	public int
	get_unread_msg_count()
	{
		return this.unread_messages;
	}


	public String
	getGroup()
	{
		return group_name;
	}

	public int
	compareTo(RosterItem another) {
		return compare(another);
	}
	
	
	/* if user has active chat -- updates user's status + description */
	public void
	updateChatHeader()
	{
		if (chat == null)
			return;
		
		CommonUtils.setBuddyDesc(context, chat.getView(), this);
	}
}
