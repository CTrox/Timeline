package ctrox.ch.timeline;

import android.content.Context;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by cyrill on 12/18/16.
 */

public class ScrollingCalendarBehavior extends AppBarLayout.Behavior {

  public ScrollingCalendarBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean onInterceptTouchEvent(CoordinatorLayout parent, AppBarLayout child, MotionEvent ev) {
    return false;/*super.onInterceptTouchEvent(parent, child, ev);*/
  }
}