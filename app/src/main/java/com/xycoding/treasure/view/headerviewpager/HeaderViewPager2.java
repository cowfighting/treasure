package com.xycoding.treasure.view.headerviewpager;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Scroller;

/**
 * Created by xuyang on 2017/3/31.
 */
public class HeaderViewPager2 extends LinearLayout {

    private static final int INVALID_POINTER = -1;

    private final Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    private HeaderScrollHelper.ScrollableContainer mScrollableContainer;
    private int mHeaderHeight;
    private final int mTouchSlop;
    private final int mMinFlingVelocity;
    private final int mMaxFlingVelocity;
    private int mActivePointerId = INVALID_POINTER;
    private float mInitMotionY, mInitMotionDownY, mInitMotionPointerDownY;
    private float mLastTouchY;
    private boolean mIntercepted = false;
    private int mLastScrollerY;

    public HeaderViewPager2(Context context) {
        this(context, null);
    }

    public HeaderViewPager2(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeaderViewPager2(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);

        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();

        mScroller = new Scroller(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //测量需加上header高度
        View header = getChildAt(0);
        if (header != null) {
            measureChildWithMargins(header, widthMeasureSpec, 0, MeasureSpec.UNSPECIFIED, 0);
            mHeaderHeight = header.getMeasuredHeight();
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec) + mHeaderHeight, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        handleTouchEvent(ev, false);
        super.dispatchTouchEvent(ev);
        return true;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //header完全隐藏且底部内容未在顶部时，不拦截事件
        if (isHeaderCollapseCompletely() && !isScrollContainerTop()) {
            mIntercepted = false;
            return false;
        }
        ensureVelocityTracker(ev);
        int pointerIndex;
        switch (MotionEventCompat.getActionMasked(ev)) {
            case MotionEvent.ACTION_DOWN:
                mIntercepted = false;
                mActivePointerId = ev.getPointerId(0);
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mInitMotionY = mInitMotionDownY = ev.getY(pointerIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final float y = ev.getY(pointerIndex);
                final float dy = y - mInitMotionY;
                if (Math.abs(dy) > mTouchSlop) {
                    //header完全隐藏且向上滑动时，不拦截事件
                    if (isHeaderCollapseCompletely() && dy < 0) {
                        mIntercepted = false;
                        return false;
                    }
                    mLastTouchY = dy > 0 ? mInitMotionY + mTouchSlop : mInitMotionY - mTouchSlop;
                    mIntercepted = true;
                    return true;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    return false;
                }
                mInitMotionY = mInitMotionPointerDownY = ev.getY(pointerIndex);
                mActivePointerId = ev.getPointerId(pointerIndex);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                final int prePointerId = mActivePointerId;
                onSecondaryPointerUp(ev);
                if (prePointerId != mActivePointerId) {
                    //切换手指，改变起始位置
                    mInitMotionY = mInitMotionY == mInitMotionDownY ? mInitMotionPointerDownY : mInitMotionDownY;
                }
                break;
            case MotionEvent.ACTION_UP:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                //当手指滑动微小距离时，fling header
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                float vy = mVelocityTracker.getYVelocity(pointerIndex);
                if (Math.abs(vy) >= mMinFlingVelocity) {
                    fling(vy);
                }
                clearParams();
                break;
            case MotionEvent.ACTION_CANCEL:
                clearParams();
                break;
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return handleTouchEvent(ev, true);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            float currVelocity = mScroller.getCurrVelocity();
            if (currVelocity > 0 && mIntercepted) {
                //向上fling时，若header已完全隐藏，则开始fling底部内容
                if (isHeaderCollapseCompletely()) {
                    mScroller.abortAnimation();
                    int remainDistance = mScroller.getFinalY() - mScroller.getCurrY();
                    int remainDuration = mScroller.getDuration() - mScroller.timePassed();
                    flingContent(Math.round(mScroller.getCurrVelocity()), remainDistance, remainDuration);
                } else {
                    scrollTo(0, mScroller.getCurrY());
                }
            } else {
                //向下fling时，若底部内容已到顶部，则开始滚动header
                if (isScrollContainerTop()) {
                    final int deltaY = mScroller.getCurrY() - mLastScrollerY;
                    scrollTo(0, getScrollY() + deltaY);
                }
            }
            postInvalidate();
            mLastScrollerY = mScroller.getCurrY();
        }
    }

    @Override
    public void scrollTo(@Px int x, @Px int y) {
        if (y > mHeaderHeight) {
            y = mHeaderHeight;
        } else if (y < 0) {
            y = 0;
        }
        super.scrollTo(x, y);
    }

    private boolean handleTouchEvent(MotionEvent ev, boolean fromOnTouchEvent) {
        ensureVelocityTracker(ev);
        int pointerIndex;
        switch (MotionEventCompat.getActionMasked(ev)) {
            case MotionEvent.ACTION_DOWN:
                mActivePointerId = ev.getPointerId(0);
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mLastTouchY = ev.getY(pointerIndex);
                mScroller.abortAnimation();
                break;
            case MotionEvent.ACTION_MOVE:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mIntercepted = mIntercepted || fromOnTouchEvent;
                final float y = ev.getY(pointerIndex);
                scroll(Math.round(mLastTouchY - y));
                mLastTouchY = y;
                break;
            case MotionEventCompat.ACTION_POINTER_DOWN:
                pointerIndex = MotionEventCompat.getActionIndex(ev);
                if (pointerIndex < 0) {
                    return false;
                }
                mActivePointerId = ev.getPointerId(pointerIndex);
                mLastTouchY = ev.getY(pointerIndex);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastTouchY = ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
            case MotionEvent.ACTION_UP:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                mIntercepted = mIntercepted || fromOnTouchEvent;
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                fling(mVelocityTracker.getYVelocity(pointerIndex));
                clearParams();
                break;
            case MotionEvent.ACTION_CANCEL:
                clearParams();
                break;
        }
        return true;
    }

    private void scroll(int dy) {
        if (!mIntercepted) {
            return;
        }
        if (isScrollContainerTop() || !isHeaderCollapseCompletely()) {
            //当底部内容在顶部或header未完全隐藏时，滑动header
            scrollBy(0, dy);
        }
        if (isHeaderCollapseCompletely()) {
            //当header滑动到顶部后，滑动底部内容
            if (mScrollableContainer != null && mScrollableContainer.getScrollableView() != null) {
                mScrollableContainer.getScrollableView().scrollBy(0, dy);
            }
        }
    }

    private void fling(float vy) {
        mLastScrollerY = getScrollY();
        mScroller.fling(0, getScrollY(), 0, -Math.round(vy), 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        invalidate();
    }

    /**
     * fling底部内容
     *
     * @param vy
     * @param distance
     * @param duration
     */
    private void flingContent(int vy, int distance, int duration) {
        if (mScrollableContainer != null && mScrollableContainer.getScrollableView() != null) {
            if (mScrollableContainer.getScrollableView() instanceof AbsListView) {
                AbsListView absListView = (AbsListView) mScrollableContainer.getScrollableView();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    absListView.fling(vy);
                } else {
                    absListView.smoothScrollBy(distance, duration);
                }
            } else if (mScrollableContainer.getScrollableView() instanceof ScrollView) {
                ((ScrollView) mScrollableContainer.getScrollableView()).fling(vy);
            } else if (mScrollableContainer.getScrollableView() instanceof RecyclerView) {
                ((RecyclerView) mScrollableContainer.getScrollableView()).fling(0, vy);
            } else if (mScrollableContainer.getScrollableView() instanceof WebView) {
                ((WebView) mScrollableContainer.getScrollableView()).flingScroll(0, vy);
            }
        }
    }

    private void ensureVelocityTracker(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);
    }

    private void clearParams() {
        mActivePointerId = INVALID_POINTER;
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
        }
    }

    /**
     * header是否完全隐藏
     *
     * @return
     */
    private boolean isHeaderCollapseCompletely() {
        return getScrollY() == mHeaderHeight;
    }

    /**
     * header是否完全可见
     *
     * @return
     */
    private boolean isHeaderExpandCompletely() {
        return getScrollY() == 0;
    }

    /**
     * scroll container是否已滑动到顶部
     *
     * @return
     */
    private boolean isScrollContainerTop() {
        return mScrollableContainer == null
                || mScrollableContainer.getScrollableView() == null
                || !canViewScrollUp(mScrollableContainer.getScrollableView());
    }

    /**
     * 判断当前view是否往上滑动
     *
     * @param view
     * @return
     */
    private boolean canViewScrollUp(View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            if (view instanceof AbsListView) {
                final AbsListView absListView = (AbsListView) view;
                return absListView.getChildCount() > 0
                        && (absListView.getFirstVisiblePosition() > 0 ||
                        absListView.getChildAt(0).getTop() < absListView.getPaddingTop());
            } else {
                return ViewCompat.canScrollVertically(view, -1) || view.getScrollY() > 0;
            }
        } else {
            return ViewCompat.canScrollVertically(view, -1);
        }
    }

    public void setCurrentScrollableContainer(@NonNull HeaderScrollHelper.ScrollableContainer scrollableContainer) {
        mScrollableContainer = scrollableContainer;
    }

}