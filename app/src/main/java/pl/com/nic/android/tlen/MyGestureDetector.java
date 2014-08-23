package pl.com.nic.android.tlen;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class
MyGestureDetector extends GestureDetector.SimpleOnGestureListener
{
	// private Chats chats;
	private Roster buddies;
		
	
	public
	MyGestureDetector(Roster buddies_)
	{
		buddies = buddies_;
	}
		
		
	@Override
	public boolean
	onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
	{
		//Log.d("Tlenoid", "onFling");

		int SWIPE_MIN_DISTANCE = 120;
		int SWIPE_THRESHOLD_VELOCITY = 200;

		if (buddies.getChatsCount() == 0) {
			return false;
		}

		try {
			if (Math.abs(e1.getX() - e2.getX()) < 100.0f) // { //> SWIPE_MAX_OFF_PATH)
				return false;

			// right to left swipe
			if(e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				// Toast.makeText(getApplicationContext(), "Left Swipe", Toast.LENGTH_SHORT).show();
				buddies.showNext();
				return true;
			}

			if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
				// Toast.makeText(getApplicationContext(), "Right Swipe", Toast.LENGTH_SHORT).show();
				buddies.showPrev();
				return true;
			}
		} catch (Exception e) {
			// nothing
		}

		return false;
	}
}