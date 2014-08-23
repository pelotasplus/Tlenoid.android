package pl.com.nic.android.tlen;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

final class
Message
{
	private String from;
	private String msg;
	private Date tstamp;
	private final String TAG = "Tlenoid";
	
	Message(String from_, String msg_, String tstamp_)
	{
		from   = from_;
		msg    = msg_;
		
		if (tstamp_ == null) {
			tstamp = new Date(System.currentTimeMillis());
		} else {
			// 20120223T21:49:11
			SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
			try { 
				tstamp = (Date) formatter.parse(tstamp_);
			} catch (Exception exc) {
				Log.e(TAG, "got error while processing date: " + exc.toString());
				tstamp = new Date(System.currentTimeMillis());
			}
		}
	}
	
	public String
	getFrom()
	{
		return from;
	}
	
	public String
	getMsg()
	{
		return msg;
	}
	
	public void
	appendMsg(String msg_)
	{
		msg += "\n" + msg_;
	}
	
	public String
	getTstamp()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return sdf.format(tstamp);
	}
	
	
	public boolean
	isMyMessage()
	{
		return from.equals("me") || from.equals("ja");
	}
	
	@Override
	public String
	toString()
	{
		return from + ": " + msg;
	}
}

final class
ChatAdapter extends ArrayAdapter<Message>
	implements View.OnCreateContextMenuListener
{
	private Context ctx;
	private LayoutInflater inflater;
	private int fmt;
	
	public
	ChatAdapter(Context context, ArrayList<Message> items, int format)
	{
		super(context, R.layout.message_view, items);
		
		fmt = format;
		
		Log.d("Tlenoid", "format is " + fmt);
		
		ctx = context;
		inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	
	public void
	setStyle(int new_style)
	{
		fmt = new_style;
		notifyDataSetChanged();
	}
	
	
	public void
	onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo)
	{
		AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

		Message m = (Message) getItem(info.position);
		
		CommonUtils.extendContextMenu(ctx, menu, m.getMsg());
	}
	
	
	@Override
	public View
	getView(int position, View convertView, ViewGroup parent)
	{
		Log.d("Tlenoid", "getView fmt " + fmt);
		if (fmt == 1) {
			return getViewPlain(position, convertView, parent);
		} else {
			return getViewBubbles(position, convertView, parent);
		}
	}
	
	
	public View
	getViewBubbles(int position, View convertView, ViewGroup parent)
	{
		View row = inflater.inflate(R.layout.message_view, null);
		TextView textView = (TextView) row.findViewById(R.id.message);
		TextView time = (TextView) row.findViewById(R.id.time);
		
		Message m = (Message) getItem(position);
		textView.setText(m.getMsg());
		
		int padding_in_dp = 8;
		final float scale = ctx.getResources().getDisplayMetrics().density;
		int padding_in_px = (int) (padding_in_dp * scale + 0.5f);
		
		if (m.isMyMessage()) {
			textView.setBackgroundResource(R.drawable.bubble_out);
			
			LinearLayout.LayoutParams lllp= (LinearLayout.LayoutParams) textView.getLayoutParams(); 
			lllp.gravity=Gravity.RIGHT; 
			textView.setLayoutParams(lllp); 
			
			lllp= (LinearLayout.LayoutParams) time.getLayoutParams(); 
			lllp.gravity=Gravity.RIGHT; 
			time.setLayoutParams(lllp); 
			
			time.setPadding(time.getPaddingLeft(), time.getPaddingTop(), padding_in_px, time.getPaddingBottom());
		} else {
			textView.setBackgroundResource(R.drawable.bubble_in);
			textView.setTextColor(ctx.getResources().getColor(android.R.color.black));
			
			time.setPadding(padding_in_px, time.getPaddingTop(), time.getPaddingRight(), time.getPaddingBottom());
		}
		
		time.setText(m.getTstamp());
		
		return row;
	}
	
	
	public View
	getViewPlain(int position, View convertView, ViewGroup parent)
	{
		View row = inflater.inflate(R.layout.message_plain, null);
		
		TextView textView = (TextView) row.findViewById(R.id.message);
		TextView timeView = (TextView) row.findViewById(R.id.time);
		
		Message m = (Message) getItem(position);
		textView.setText(Html.fromHtml("<b>" + m.getFrom() + ":</b> " + m.getMsg().replace("\n", "<br />")));
		
		if (! m.isMyMessage()) {
			textView.setBackgroundColor(ctx.getResources().getColor(R.color.Gainsboro));
			timeView.setBackgroundColor(ctx.getResources().getColor(R.color.Gainsboro));
		}
		
		timeView.setText(m.getTstamp());
		
		return row;
	}
}


public class
Chat
{
	private View v;
	private RosterItem user;
	private ListView lv;
	private ChatAdapter adapter;
	private ArrayList<Message> msgs;
	
	public
	Chat(Context context, View v_, RosterItem user_, int format)
	{
		v = v_;
		user = user_;
		msgs = new ArrayList<Message>();
		
		adapter = new ChatAdapter(context, msgs, format);
			
		lv = (ListView) v.findViewById(R.id.chat_lv);
		lv.setAdapter(adapter);
		lv.setOnCreateContextMenuListener(adapter);
		lv.setSelector(android.R.color.transparent);
		lv.setCacheColorHint(Color.TRANSPARENT);
	}
	
	
	public View
	getView()
	{
		return v;
	}
	
	
	public String
	getMessageToSend()
	{
		EditText edit = (EditText) v.findViewById(R.id.edittext);
		String msg = edit.getText().toString();
		edit.setText("");
		return msg;
	}
	
	public RosterItem
	getUser()
	{
		return user;
	}
	
	
	public void
	setStyle(int new_style)
	{
		adapter.setStyle(new_style);
	}
	
	
	/* dodajemy wiadomosc do okna rozmowy */
	public void
	addMessage(RosterItem from, String stamp, String msg)
	{
		addMessage(from.getShortUsername(), stamp, msg);
	}
	
	
	public void
	addMessage(String from, String stamp, String msg)
	{
		if (msgs.size() > 0) {
			int pos = msgs.size() - 1;
			Message last = msgs.get(pos);
			if (last.getFrom().equals(from)) {
				last.appendMsg(msg);
				adapter.notifyDataSetChanged();
				
				return;
			}
		}
		
		msgs.add(new Message(from, msg, stamp));
		adapter.notifyDataSetChanged();
	}
	
	public void
	setActive()
	{
		EditText et = (EditText) v.findViewById(R.id.edittext);
		et.requestFocus();
		
		TextView t = (TextView) v.findViewById(R.id.status);
		t.setSelected(true);
	}
}
