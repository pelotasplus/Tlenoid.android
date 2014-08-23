package pl.com.nic.android.tlen;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class
RosterAdapter extends ArrayAdapter<RosterItem>
{
	private ArrayList<RosterItem> roster;
	private Context ctx;

	public
	RosterAdapter(Context context, int textViewResourceId, ArrayList<RosterItem> items)
	{
		super(context, textViewResourceId, items);
		roster = items;
		ctx = context;
	}

	@Override
	public View
	getView(int position, View convertView, ViewGroup parent)
	{
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.roster_item, null);
			
			TextView t = (TextView) v.findViewById(R.id.status);
			t.setSelected(true);
		}

		RosterItem ri = this.roster.get(position);

		CommonUtils.setBuddyDesc(v, ri);

		v.setBackgroundColor(ctx.getResources().getColor(android.R.color.white));

		TextView alias = (TextView) v.findViewById(R.id.alias);
		alias.setTextColor(ctx.getResources().getColor(android.R.color.black));

		TextView status = (TextView) v.findViewById(R.id.status);
		status.setTextColor(ctx.getResources().getColor(android.R.color.black));

		ImageView img = (ImageView) v.findViewById(R.id.unreadImg);

		if (ri.get_unread_msg_count() > 0) {
			img.setImageResource(android.R.drawable.stat_notify_chat);
			img.setVisibility(View.VISIBLE);

			int padding_in_dp = 59 + 32;
			final float scale = ctx.getResources().getDisplayMetrics().density;
			int padding_in_px = (int) (padding_in_dp * scale + 0.5f);

			alias.setPadding(0, 0, padding_in_px, 0);
			status.setPadding(0, 0, padding_in_px, 0);
		} else {
			img.setVisibility(View.INVISIBLE);

			int padding_in_dp = 59;
			final float scale = ctx.getResources().getDisplayMetrics().density;
			int padding_in_px = (int) (padding_in_dp * scale + 0.5f);

			alias.setPadding(0, 0, padding_in_px, 0);
			status.setPadding(0, 0, padding_in_px, 0);
		}

		return v;
	}
}