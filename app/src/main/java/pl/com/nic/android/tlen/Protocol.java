package pl.com.nic.android.tlen;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import android.os.Bundle;
import android.util.Log;

class
Protocol
{
	public static void
	dump_extras(String TAG, Bundle b)
	{
		Set<String> keys = b.keySet();  
		Iterator<String> iterate = keys.iterator();  

		while (iterate.hasNext()) {
			String key = iterate.next();  
			if (key.startsWith("ri:")) {
				Log.d(TAG, "\t" + key + " => roster-item");
				ArrayList<String> params = b.getStringArrayList(key);
				int i;
				for (i = 0; i < params.size(); i++) {
					Log.d(TAG, "\t\t " + i + " => " + params.get(i));
				}
			} else {
				Log.d(TAG, "\t" + key + " => " + b.getString(key));
			}
		}  
	}


	public static String
	decodeString(String msg)
	{
		if (msg == null)
			return null;

		try {
			return URLDecoder.decode(msg, "iso8859-2");
		} catch (Exception exc) {
			Log.e("XMLHandler", "decodeString: msg=" + msg, exc);
			return msg;
		}
	}

	public static String
	encodeString(String msg)
	{
		if (msg == null)
			return null;

		try {
			return URLEncoder.encode(msg, "iso8859-2");
		} catch (Exception exc) {
			Log.e("Protocol", "encodeString: msg=" + msg, exc);
			return msg;
		}
	}

	public static String
	tlen_ping_session()
	{
		return "  \t  ";
	}

	public static String
	tlen_message(String to, String msg)
	{
		
		return "<message to='" + to + "' type='chat'><body>" + encodeString(msg) + "</body></message>";
	}

	public static String
	tlen_close()
	{
		return "</s>";
	}

	public static String
	tlen_set_presence(String status, String message)
	{
		if (message == null) {
			return "<presence><show>" + status + "</show></presence>";
		} else {
			return "<presence><show>" + status + "</show><status>" + message + "</status></presence>";
		}
	}

	
	public static String
	tlen_get_roster()
	{
		return "<iq type='get' id='GetRoster'><query xmlns='jabber:iq:roster'></query></iq>";
	}

	public static String
	tlen_get_config()
	{
		return "<iq to='tcfg' type='get' id='TcfgGetAfterLoggedIn'></iq>";
	}
	
	public static String
	calc_passcode(String password)
	{
		int magic1 = 0x50305735;
		int magic2 = 0x12345671;
		int sum = 7;

		byte pass[] = password.getBytes();

		int j;
		for (j = 0; j < pass.length; j++) {
			if (pass[j] == ' ')
				continue;

			if (pass[j] == '\t')
				continue;

			magic1 ^= (((magic1 & 0x3f) + sum) * pass[j]) + (magic1 << 8);
			magic2 += (magic2 << 8) ^ magic1;
			sum += pass[j];
		}

		magic1 &= 0x7fffffff;
		magic2 &= 0x7fffffff;

		return String.format("%08x%08x", magic1, magic2);
	}

	public static String
	tlen_hash(String sess_id, String password)
	{
		String passcode;

		passcode = calc_passcode(password);

		Log.e("debug", "passcode="+passcode);
 
		MessageDigest md;

		try {
			md = MessageDigest.getInstance("SHA");
		} catch (Exception exc) {
			return null;
		}

		String prehash = sess_id + passcode;

		md.reset();
		md.update(prehash.getBytes());

		byte digest[] = md.digest();

		Log.e("debug", md.toString());


		StringBuffer ret = new StringBuffer();
		int i;
		for (i = 0; i < digest.length; i++) {
			String tmp = Integer.toHexString(0xFF & digest[i]);
			if (tmp.length() == 1)
				tmp = "0" + tmp;
			ret.append(tmp);
		}

		return ret.toString();
	}

	public static String
	tlen_auth_query(String sess_id, String username, String password)
	{
		String digest = tlen_hash(sess_id, password);

		return "<iq type='set' id='" +
		       sess_id +
		       "'><query xmlns='jabber:iq:auth'><username>" +
		       username +
		       "</username><host>tlen.pl</host><digest>" +
		       digest +
		       "</digest><resource>Tlenoid</resource></query></iq>";
	}

	public static String
	tlen_session_string()
	{
		// return "<s s='1' v='9' t='06000224'>";
		return "<s v='10'>";
	}

	public static String
	tlen_get_info(String username)
	{
		int pos;
		String userid = username;

		pos = username.indexOf("@");
		if (pos != -1) {
			userid = username.substring(0, pos);
		}

		String s = "<iq type='get' id='" + username + "' to='tuba'><query xmlns='jabber:iq:search'>" +
		       "<i>" + userid + "</i>" +
		       "</query></iq>";

		Log.d("Protocol", "s=" + s);

		return s;
	}

	public static ArrayList<String>
	tlen_get_user_info_keys()
	{
		ArrayList<String> ret = new ArrayList<String>();

		ret.add("first");
		ret.add("last");
		ret.add("nickname");
		ret.add("email");
		ret.add("c");
		ret.add("e");
		ret.add("s");
		ret.add("b");
		ret.add("j");
		ret.add("r");
		ret.add("g");
		ret.add("p");

		return ret;
	}

 	public static int
	tlen_get_str_for_user_info_key(String code)
	{
		if (code.equals("first")) {
			return R.string.user_info_first_name;
		} else if (code.equals("last")) {
			return R.string.user_info_last_name;
		} else if (code.equals("nick")) {
			return R.string.user_info_nick;
		} else if (code.equals("email")) {
			return R.string.user_info_email;
		} else if (code.equals("c")) {
			return R.string.user_info_city;
		} else if (code.equals("e")) {
			return R.string.user_info_school;
		} else if (code.equals("s")) {
			return R.string.user_info_sex;
		} else if (code.equals("b")) {
			return R.string.user_info_age;
		} else if (code.equals("j")) {
			return R.string.user_info_job;
		} else if (code.equals("r")) {
			return R.string.user_info_looking_for;
		} else if (code.equals("g")) {
			return R.string.user_info_voice;
		} else if (code.equals("p")) {
			return R.string.user_info_plans;
		} else if (code.equals("tlen-id")) {
			return R.string.user_info_tlen_id;
		} else {
			return R.string.user_info_unknown;
		}
	}

	
	public static String
	tlen_set_alias(String username, String alias, String group)
	{
		if (group == null)
			group = "";

		String ret;

		ret = "<iq type='set'><query xmlns='jabber:iq:roster'>";

		if (alias == null || alias.equals("")) {
			ret += "<item jid='" + username + "'>";
		} else 	{
			alias = encodeString(alias);
			ret += "<item jid='" + username + "' name='" + alias + "'>";
		}

		group = encodeString(group);
		ret += "<group>" + group + "</group></item></query></iq>";
		
		return ret;
	}

	
	public static String
	tlen_remove_user(String id)
	{
		String ret;

		ret = "<iq type='set'><query xmlns='jabber:iq:roster'><item jid='";

		ret += id;

		ret += "' subscription='remove'></item></query></iq>";

		return ret;
	}
}
