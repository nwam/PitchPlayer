/*
 * Copyright (C) 2016 Adrian Ulrich <adrian@blinkenlights.ch>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>. 
 */

package ch.blinkenlights.android.vanilla;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.animation.DecelerateInterpolator;
import android.view.GestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import java.util.ArrayList;
import android.util.Log;


public class SlidingView extends FrameLayout
	implements View.OnTouchListener
	{
	/**
	 * Ignore drag until we made 30 px progress.
	 */
	private final float MAX_PROGRESS = 30;
	/**
	 * Our current offset
	 */
	private float mViewOffsetY = 0;
	/**
	 * The maximum (initial) offset of the view
	 */
	private float mMaxOffsetY = 0;
	/**
	 * The previous Y coordinate, used to calculate the movement diff.
	 */
	private float mPreviousY = 0;
	/**
	 * The total progress in pixels of this drag
	 */
	private float mProgressPx = 0;
	/**
	 * Signals the direction of the fling
	 */
	private int mFlingDirection = 0;
	/**
	 * TRUE if we started to move this view
	 */
	private boolean mDidScroll = false;
	/**
	 * Reference to the gesture detector
	 */
	private GestureDetector mDetector;
	/**
	 * An external View we are managing during layout changes.
	 */
	private View mSlaveView;
	/**
	 * The resource id to listen for touch events
	 */
	private int mSliderHandleId = 0;
	/**
	 * The current expansion stage
	 */
	int mCurrentStage = 0;
	/**
	 * List with all possible stages and their offsets
	 */
	ArrayList<Integer> mStages = new ArrayList<Integer>();


	public SlidingView(Context context) {
		this(context, null);
	}

	public SlidingView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SlidingView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setBackgroundColor(ThemeHelper.getDefaultCoverColors(context)[0]);

		mDetector = new GestureDetector(new GestureListener());
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingViewPreferences);
		mSliderHandleId = a.getResourceId(R.styleable.SlidingViewPreferences_slider_handle_id, 0);
		int slaveId = a.getResourceId(R.styleable.SlidingViewPreferences_slider_slave_id, 0);
		a.recycle();

		// This is probably a parent view: so we need the context but can search
		// it before we got inflated:
		mSlaveView = ((Activity)context).findViewById(slaveId);
	}

	/**
	 * Fully expands the slide
	 */
	public void expandSlide() {
		jumpToStage(mCurrentStage + 1);
	}

	/**
	 * Hides the slide
	 */
	public void hideSlide() {
		jumpToStage(mCurrentStage - 1);
	}

	/**
	 * Transforms to the new expansion state
	 *
	 * @param stage the stage to transform to
	 */
	private void jumpToStage(int stage) {
		if (stage >= mStages.size())
			stage = mStages.size() - 1;
		if (stage < 0)
			stage = 0;

		Log.v("VanillaMusic", "Transforming to stage "+stage+" -> "+mStages.get(stage));
		int pxOff = mStages.get(stage);
		this.animate().translationY(pxOff).setInterpolator(new DecelerateInterpolator());
		mCurrentStage = stage;

		if (mSlaveView != null) {
			int totalOffset = 0;
			for (int i = 0; i <= mCurrentStage; i++) {
				totalOffset += getChildAt(i).getHeight();
			}
			FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)mSlaveView.getLayoutParams();
			Log.v("VanillaMusic", "Margin was: "+totalOffset);
			params.bottomMargin = totalOffset;
			mSlaveView.setLayoutParams(params);
		}
	}

	/**
	 * Returns true if the slide is currently fully expanded
	 */
	public boolean isExpanded() {
		return (getTranslationY() == 0 ? true : false);
	}

	/**
	 * Called after the view was inflated, binds an onTouchListener to all child
	 * elements of the child view
	 */
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		View handle = findViewById(mSliderHandleId);

		if (handle != null) {
			if (handle instanceof ViewGroup) {
				ViewGroup group = (ViewGroup)handle;
				for (int i = 0; i < group.getChildCount(); i++) {
					group.getChildAt(i).setOnTouchListener(this);
				}
			} else {
				handle.setOnTouchListener(this);
			}
		}
	}


	/**
	 * Attempts to stack all views orizontally in the available space
	 */
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);

		int viewHeight = getMeasuredHeight();
		int childCount = getChildCount();
		int topOffset = 0;
		View lastChild = null;

		mStages.clear();

		for (int i = 0; i < childCount ; i++) {
			lastChild = getChildAt(i);
			int childWidth = lastChild.getMeasuredWidth();
			int childHeight = lastChild.getMeasuredHeight();
			int childBottom = childHeight + topOffset;

			// No child should consume space outside of our view
			if (topOffset > viewHeight)
				topOffset = viewHeight;
			if (childBottom > viewHeight)
				childBottom = viewHeight;

Log.v("VanillaMusic", "Stacked child "+i+" at "+topOffset +" up to "+childBottom);
			lastChild.layout(0, topOffset, childWidth, childBottom);
			mStages.add(viewHeight - childBottom);
			topOffset += childHeight;
		}

		if (lastChild != null && mMaxOffsetY == 0) {
			// Sizes are now fixed: Overwrite any (possible) FILL_PARENT or WRAP_CONTENT
			// value with the measured size
			// This should only happen on the first run (mMaxOffsetY == 0)
			for (int i = 0; i < childCount ; i++) {
				View child = getChildAt(i);
				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)child.getLayoutParams();
				params.height = child.getHeight();
				params.width = child.getWidth();
				child.setLayoutParams(params);
			}
		}

		if (changed) {
			mMaxOffsetY = mStages.get(0);
			mViewOffsetY = mMaxOffsetY;
			Log.v("VanillaMusic", "Set to: "+mViewOffsetY);
			setTranslationY(mViewOffsetY);
			jumpToStage(0);
		}
	}


	@Override
	public boolean onTouch(View v, MotionEvent event){
		// Fix up the event offset as we are moving the view itself.
		// This is required to get flings correctly detected
		event.setLocation(event.getRawX(), event.getRawY());

		mDetector.onTouchEvent(event);
		float y = event.getRawY();
		float dy = y - mPreviousY;

		switch(event.getActionMasked()) {
			case MotionEvent.ACTION_UP : {
				if (mDidScroll == false) { // Dispatch event if we never scrolled
					v.onTouchEvent(event);
				} else if (mFlingDirection < 0) {
					expandSlide();
				} else if (mFlingDirection > 0) {
					hideSlide();
				} else if (mViewOffsetY < (mMaxOffsetY/2)) {
					expandSlide();
				} else {
					hideSlide();
				}
				break;
			}
			case MotionEvent.ACTION_DOWN : {
				v.onTouchEvent(event);

				mViewOffsetY = getTranslationY();
				mProgressPx = 0;
				mFlingDirection = 0;
				mDidScroll = false;
				break;
			}
			case MotionEvent.ACTION_MOVE : {
				mViewOffsetY += dy;
				mProgressPx += Math.abs(dy);

				float usedY = mViewOffsetY;
				if (usedY < 0)
					usedY = 0;
				if (usedY > mMaxOffsetY)
					usedY = mMaxOffsetY;

				if (mProgressPx < MAX_PROGRESS) {
					// we did not reach a minimum of progress: do not scroll yet
					usedY = getTranslationY();
				} else {
					if (mDidScroll == false) {
						event.setAction(MotionEvent.ACTION_CANCEL);
						v.onTouchEvent(event);
					}
					mDidScroll = true;
				}

				setTranslationY(usedY);
				break;
			}
		}
		mPreviousY = y;
		return true;
	}

	class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onFling(MotionEvent event1, MotionEvent event2,  float velocityX, float velocityY) {
			mFlingDirection = (velocityY > 0 ? 1 : -1);
			return true;
		}
	}


}