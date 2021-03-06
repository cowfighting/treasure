package com.xycoding.treasure.view.headerviewpager;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.EdgeEffectCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebView;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Scroller;

import com.xycoding.treasure.R;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Created by xuyang on 2017/3/31.
 */
public class HeaderViewPager extends LinearLayout {

    private static final int INVALID_POINTER = -1;
    //快速回到顶部最大时长
    private static final int FAST_RETURN_TOP_TIME = 1000;
    private int mPreScrollBarTop;
    private int mScrollBarHeight;
    private int mScrollBarMarginTop;
    private int mScrollbarMarginBottom;
    private OnScrollBarListener mScrollBarListener;
    private OnScrollBarVisibleListener mScrollBarVisibleListener;
    private OnFastBackVisibleListener mFastBackVisibleListener;

    private Rect mHeaderViewPagerRect = new Rect();
    private final Scroller mScroller;
    private VelocityTracker mVelocityTracker;
    //向上滑动偏移量
    private int mTopOffset;
    //向上滑动最大距离（等于header高度减去偏移量）
    private int mMaxScrollY;
    private final int mTouchSlop;
    private final int mMinFlingVelocity;
    private final int mMaxFlingVelocity;
    private int mActivePointerId = INVALID_POINTER;
    private int mInitMotionY;
    private int mLastTouchY;
    private int mLastScrollerY;
    //标志是否向上滑动
    private boolean mScrollUp;
    //标志是否向上fling
    private boolean mFlingUp = false;
    //标志是否fling内容
    private boolean mFlingContent = false;
    //标志是否fling到顶部
    private boolean mFlingToTop = false;
    private ViewPager mViewPager;
    private FragmentTagPagerAdapter mViewPagerAdapter;
    private ArrayList<OnScrollHeaderListener> mOnScrollHeaderListeners = new ArrayList<>();

    private EdgeEffectCompat mEdgeEffectTop;
    private EdgeEffectCompat mEdgeEffectBottom;
    private boolean mEdgeEffectTopActive;
    private boolean mEdgeEffectBottomActive;

    //pull to refresh
    private BaseRefreshView mBaseRefreshView;
    private OnRefreshListener mRefreshListener;
    private final static float DECELERATE_INTERPOLATION_FACTOR = 2f;
    private final static int DRAG_MAX_DISTANCE = 65;
    private final static float DRAG_RATE = .5f;
    private DecelerateInterpolator mDecelerateInterpolator;

    public HeaderViewPager(Context context) {
        this(context, null);
    }

    public HeaderViewPager(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HeaderViewPager(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        setWillNotDraw(false);

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.HeaderViewPager);
        mTopOffset = typedArray.getDimensionPixelSize(R.styleable.HeaderViewPager_hvp_topOffset, 0);
        mScrollBarHeight = typedArray.getDimensionPixelSize(R.styleable.HeaderViewPager_hvp_scrollbar_height, 0);
        mScrollBarMarginTop = typedArray.getDimensionPixelSize(R.styleable.HeaderViewPager_hvp_scrollbar_marginTop, 0);
        mScrollbarMarginBottom = typedArray.getDimensionPixelSize(R.styleable.HeaderViewPager_hvp_scrollbar_marginBottom, 0);
        typedArray.recycle();

        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();

        mScroller = new Scroller(context);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (onlyOneDirectChildVisible()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        //测量需加上header高度
        View header = getChildAt(0);
        if (header != null) {
            measureChildWithMargins(header, widthMeasureSpec, 0, MeasureSpec.UNSPECIFIED, 0);
            mMaxScrollY = header.getMeasuredHeight() - mTopOffset;
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec) + mMaxScrollY, MeasureSpec.EXACTLY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (onlyOneDirectChildVisible()) {
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
            return;
        }
        if (mScroller.computeScrollOffset()) {
            //强制child view不拦截事件
            super.requestDisallowInterceptTouchEvent(false);
        } else {
            super.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (onlyOneDirectChildVisible()) {
            return super.onInterceptTouchEvent(ev);
        }

        ensureVelocityTracker(ev);
        int pointerIndex;
        switch (MotionEventCompat.getActionMasked(ev)) {
            case MotionEvent.ACTION_DOWN:
                resetEdgeEffects();
                mActivePointerId = ev.getPointerId(0);
                mInitMotionY = mLastTouchY = Math.round(ev.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final int y = Math.round(ev.getY(pointerIndex));
                final int dy = y - mInitMotionY;
                if (Math.abs(dy) > mTouchSlop) {
                    mLastTouchY = dy > 0 ? mInitMotionY + mTouchSlop : mInitMotionY - mTouchSlop;
                    return true;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int actionIndex = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = ev.getPointerId(actionIndex);
                mInitMotionY = mLastTouchY = Math.round(ev.getY(actionIndex));
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                //未拦截事件时：手指滑动微小距离，需fling header
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
        if (onlyOneDirectChildVisible()) {
            return super.onTouchEvent(ev);
        }

        ensureVelocityTracker(ev);
        int pointerIndex;
        switch (MotionEventCompat.getActionMasked(ev)) {
            case MotionEvent.ACTION_DOWN:
                resetEdgeEffects();
                mActivePointerId = ev.getPointerId(0);
                mLastTouchY = Math.round(ev.getY());
                break;
            case MotionEvent.ACTION_MOVE:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
                final int y = Math.round(ev.getY(pointerIndex));
                final int deltaY = mLastTouchY - y;
                scroll(deltaY);
                edgeEffectPull(ev.getX(pointerIndex), deltaY);
                mLastTouchY = y;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                final int actionIndex = MotionEventCompat.getActionIndex(ev);
                mActivePointerId = ev.getPointerId(actionIndex);
                mLastTouchY = Math.round(ev.getY(actionIndex));
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
            case MotionEvent.ACTION_UP:
                pointerIndex = ev.findPointerIndex(mActivePointerId);
                if (pointerIndex < 0) {
                    return false;
                }
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

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = MotionEventCompat.getActionIndex(ev);
        if (ev.getPointerId(pointerIndex) == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mActivePointerId = ev.getPointerId(newPointerIndex);
            mInitMotionY = mLastTouchY = Math.round(ev.getY(newPointerIndex));
        }
    }

    private void dispatchFastBackVisibleEvent() {
        if (mFastBackVisibleListener != null) {
            mFastBackVisibleListener.onVisible(mScrollUp && isHeaderCollapseCompletely());
        }
    }

    private void dispatchScrollBarVisibleEvent(boolean visible) {
        if (mScrollBarVisibleListener != null) {
            mScrollBarVisibleListener.onVisible(visible);
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

        if (mEdgeEffectTop != null) {
            mEdgeEffectTop.onRelease();
        }
        if (mEdgeEffectBottom != null) {
            mEdgeEffectBottom.onRelease();
        }
    }

    private void resetEdgeEffects() {
        mEdgeEffectTopActive = false;
        mEdgeEffectBottomActive = false;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            if (mFlingToTop) {
                //快速滑到顶部
                final int deltaY = mLastScrollerY - mScroller.getCurrY();
                final View view = getCurrentScrollableView();
                if (isScrollContainerTop() || view instanceof WebView) {
                    scrollTo(0, getScrollY() + deltaY);
                    if (view instanceof WebView) {
                        view.scrollTo(0, 0);
                    }
                } else {
                    scrollContent(deltaY);
                }
            } else {
                final int distance = mScroller.getFinalY() - mScroller.getCurrY();
                final int duration = mScroller.getDuration() - mScroller.timePassed();
                final float currVelocity = mScroller.getCurrVelocity();
                if (mFlingUp) {
                    //向上fling时，若header已完全隐藏，则向上fling底部内容
                    if (isHeaderCollapseCompletely() && !mFlingContent) {
                        //保证fling一次
                        mFlingContent = true;
                        flingContent(Math.round(currVelocity), distance, duration);
                    } else {
                        final int deltaY = mScroller.getCurrY() - mLastScrollerY;
                        scrollTo(0, getScrollY() + deltaY);
                    }
                } else {
                    boolean isScrollContainerTop = isScrollContainerTop();
                    //向下fling时，若底部内容未滑动到顶部时，则向下fling底部内容
                    if (!isScrollContainerTop && !mFlingContent) {
                        //保证fling一次
                        mFlingContent = true;
                        flingContent(-Math.round(currVelocity), distance, duration);
                    }
                    //向下fling时，若header未完全展开时，则滑动header
                    if ((!isHeaderCollapseCompletely() && !isHeaderExpandCompletely())
                            || (isScrollContainerTop && !isHeaderExpandCompletely())) {
                        final int deltaY = mScroller.getCurrY() - mLastScrollerY;
                        scrollTo(0, getScrollY() + deltaY);
                    }
                }
                dispatchScrollBarPosition();
            }
            mLastScrollerY = mScroller.getCurrY();
            invalidate();

            if (canOverScroll()) {
                ensureGlows();
                if (mFlingToTop || mFlingUp) {
                    //向上fling时，若已滑动到底部，触发底部边界动效
                    if (isScrollBottom() && !mEdgeEffectBottomActive) {
                        mScroller.abortAnimation();
                        mEdgeEffectBottomActive = true;
                        mEdgeEffectBottom.onAbsorb((int) mScroller.getCurrVelocity());
                    }
                } else {
                    //向下fling时，若已滑动到顶部，触发顶部边界动效
                    if (isScrollTop() && !mEdgeEffectTopActive) {
                        mScroller.abortAnimation();
                        mEdgeEffectTopActive = true;
                        mEdgeEffectTop.onAbsorb((int) mScroller.getCurrVelocity());
                        dispatchFastBackVisibleEvent();
                    }
                }
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (onlyOneDirectChildVisible()) {
            return;
        }

        if (mEdgeEffectTop != null) {
            final int width = getWidth() - getPaddingLeft() - getPaddingRight();
            final int height = getHeight();
            if (!mEdgeEffectTop.isFinished()) {
                //绘制顶部边界动效
                final int restoreCount = canvas.save();
                canvas.translate(getPaddingLeft(), getPaddingTop());
                mEdgeEffectTop.setSize(width, height);
                if (mEdgeEffectTop.draw(canvas)) {
                    invalidate();
                }
                canvas.restoreToCount(restoreCount);
            }
            if (!mEdgeEffectBottom.isFinished()) {
                //绘制底部边界动效
                final int restoreCount = canvas.save();
                canvas.translate(-width + getPaddingLeft(), height);
                canvas.rotate(180, width, 0);
                mEdgeEffectBottom.setSize(width, height);
                if (mEdgeEffectBottom.draw(canvas)) {
                    invalidate();
                }
                canvas.restoreToCount(restoreCount);
            }
        }
    }

    @Override
    protected int computeVerticalScrollRange() {
        if (onlyOneDirectChildVisible()) {
            return super.computeVerticalScrollRange();
        }

        int range = 0;
        final View view = getCurrentScrollableView();
        if (view != null) {
            int scrollExtent = compute(view, "computeVerticalScrollExtent");
            int scrollRange = compute(view, "computeVerticalScrollRange");
            if (scrollExtent == scrollRange) {
                //底部内容不能滚动，直接返回当前view高度
                return getHeight() - mTopOffset;
            }
            range = scrollRange;
        }
        range += mMaxScrollY + getContentHeaderGap();
        return range;
    }

    @Override
    protected int computeVerticalScrollOffset() {
        if (onlyOneDirectChildVisible()) {
            return super.computeVerticalScrollOffset();
        }

        int offset = getScrollY();
        final View view = getCurrentScrollableView();
        if (view != null) {
            offset += compute(view, "computeVerticalScrollOffset");
        }
        return offset;
    }

    @Override
    protected int computeVerticalScrollExtent() {
        if (onlyOneDirectChildVisible()) {
            return super.computeVerticalScrollExtent();
        }

        if (mHeaderViewPagerRect.isEmpty()) {
            getGlobalVisibleRect(mHeaderViewPagerRect);
        }
        return mHeaderViewPagerRect.height() - mTopOffset;
    }

    private int compute(View view, String methodName) {
        try {
            Method method = findMethod(view.getClass(), methodName);
            if (method != null) {
                method.setAccessible(true);
                return (int) method.invoke(view);
            }
        } catch (Exception e) {
            //do nothing.
        }
        return 0;
    }

    @Nullable
    private Method findMethod(Class<?> klass, String methodName) {
        if (klass == null) {
            return null;
        }
        try {
            return klass.getDeclaredMethod(methodName);
        } catch (Exception e) {
            Method method = findMethod(klass.getSuperclass(), methodName);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    private boolean onlyOneDirectChildVisible() {
        int visibleCount = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == VISIBLE) {
                visibleCount++;
                if (visibleCount > 1) {
                    return false;
                }
            }
        }
        return true;
    }

    private void ensureGlows() {
        final View view = getCurrentScrollableView();
        if (view != null) {
            //隐藏recycler view滑到顶部和底部时的效果
            view.setOverScrollMode(OVER_SCROLL_NEVER);
        }
        if (getOverScrollMode() != View.OVER_SCROLL_NEVER) {
            if (mEdgeEffectTop == null) {
                Context context = getContext();
                mEdgeEffectTop = new EdgeEffectCompat(context);
                mEdgeEffectBottom = new EdgeEffectCompat(context);
            }
        } else {
            mEdgeEffectTop = null;
            mEdgeEffectBottom = null;
        }
    }

    private void edgeEffectPull(float touchX, int deltaY) {
        if (canOverScroll()) {
            ensureGlows();
            if (isScrollTop() && deltaY < 0) {
                mEdgeEffectTopActive = true;
                //滑到顶部且向下滑时，触发顶部边界动效
                mEdgeEffectTop.onPull((float) deltaY / getHeight(), touchX / getWidth());
                if (!mEdgeEffectBottom.isFinished()) {
                    mEdgeEffectBottom.onRelease();
                }
            } else if (isScrollBottom() && deltaY > 0) {
                mEdgeEffectBottomActive = true;
                //滑到底部且向上滑时，触发底部边界动效
                mEdgeEffectBottom.onPull((float) deltaY / getHeight(), 1.f - touchX / getWidth());
                if (!mEdgeEffectTop.isFinished()) {
                    mEdgeEffectTop.onRelease();
                }
            }
            if (mEdgeEffectTop != null && mEdgeEffectBottom != null
                    && (!mEdgeEffectTop.isFinished() || !mEdgeEffectBottom.isFinished())) {
                invalidate();
            }
        }
    }

    private boolean canOverScroll() {
        final int overScrollMode = getOverScrollMode();
        return overScrollMode == View.OVER_SCROLL_ALWAYS || overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS;
    }

    @Override
    public void scrollTo(@Px int x, @Px int y) {
        if (onlyOneDirectChildVisible()) {
            super.scrollTo(x, y);
            return;
        }

        if (y > mMaxScrollY) {
            y = mMaxScrollY;
        } else if (y < 0) {
            y = 0;
        }
        if (mOnScrollHeaderListeners.size() != 0 && getScrollY() != y) {
            for (OnScrollHeaderListener listener : mOnScrollHeaderListeners) {
                listener.onScroll(y, mMaxScrollY);
            }
        }
        if (!isHeaderCollapseCompletely()) {
            dispatchScrollBarVisibleEvent(false);
        }
        super.scrollTo(x, y);
    }

    public void scrollToTop() {
        int distance = getCurrentScrollY();
        int duration = FAST_RETURN_TOP_TIME;
        final int screenHeight = getResources().getDisplayMetrics().heightPixels;
        if (distance <= screenHeight) {
            //滑动距离小于屏幕，时间减少
            duration = FAST_RETURN_TOP_TIME / 3;
        }
        //hacky: 极端情况计算的距离总要差点，手动增加屏幕高度
        distance += screenHeight;
        mLastScrollerY = 0;
        mFlingToTop = true;
        mScroller.startScroll(0, 0, 0, distance, duration);
        invalidate();
        scrollToTopViewPagerChildren();
    }

    public void scrollToTopImmediately() {
        mLastScrollerY = 0;
        scrollTo(0, 0);

        final View view = getCurrentScrollableView();
        if (view != null) {
            if (view instanceof RecyclerView) {
                ((RecyclerView) view).scrollToPosition(0);
            } else {
                view.scrollTo(0, 0);
            }
        }
        scrollToTopViewPagerChildren();
    }

    private void scrollToTopViewPagerChildren() {
        if (mViewPager != null) {
            final int count = mViewPager.getChildCount();
            for (int i = 0; i < count; i++) {
                if (i != mViewPager.getCurrentItem()) {
                    final View child = findFragmentView(mViewPagerAdapter, i);
                    if (child != null) {
                        final View scrollableView = findScrollableView(child);
                        if (scrollableView != null) {
                            if (scrollableView instanceof RecyclerView) {
                                ((RecyclerView) scrollableView).scrollToPosition(0);
                            } else {
                                scrollableView.scrollTo(0, 0);
                            }
                        }
                        child.scrollTo(0, 0);
                    }
                }
            }
        }
    }

    public void scrollToPosition(int position) {
        if (!isHeaderCollapseCompletely()) {
            //若header未完全隐藏，则smooth隐藏header
            mFlingUp = true;
            mFlingContent = true;
            mFlingToTop = false;
            mLastScrollerY = 0;
            mScroller.startScroll(0, 0, 0, mMaxScrollY - getScrollY(), 100);
            invalidate();
        }
        final View view = getCurrentScrollableView();
        if (view instanceof RecyclerView) {
            ((RecyclerView) view).smoothScrollToPosition(position);
        }
    }

    private int getCurrentScrollY() {
        int distance = 0;
        final View view = getCurrentScrollableView();
        if (view != null) {
            distance = compute(view, "computeVerticalScrollOffset");
            //大于0说明底部内容已滚动
            if (distance > 0) {
                distance += getContentHeaderGap();
            }
        }
        //加上header滚动距离
        distance += getScrollY();
        return distance;
    }

    private int getContentHeaderGap() {
        try {
            //计算底部内容和header之间距离
            int height = 0;
            final int count = getChildCount();
            final ViewParent parent = getCurrentScrollableView().getParent();
            for (int i = 1; i < count; i++) {
                final View child = getChildAt(i);
                if (isViewParent(child, parent)) {
                    break;
                } else {
                    height += child.getHeight();
                }
            }
            return height;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 判断child的parent是否为view
     *
     * @param view
     * @param child
     * @return
     */
    private boolean isViewParent(View view, ViewParent child) {
        if (child == this) {
            return false;
        }
        if (view == child) {
            return true;
        }
        if (child.getParent() != null) {
            return isViewParent(view, child.getParent());
        }
        return false;
    }

    private boolean ensureRefreshView() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            mBaseRefreshView = findRefreshView(getChildAt(i));
            if (mBaseRefreshView != null) {
                return true;
            }
        }
        return false;
    }

    private BaseRefreshView findRefreshView(View view) {
        if (view instanceof BaseRefreshView) {
            return (BaseRefreshView) view;
        }
        if (view instanceof ViewGroup) {
            final int count = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < count; i++) {
                final BaseRefreshView refreshView = findRefreshView(view);
                if (refreshView != null) {
                    return refreshView;
                }
            }
        }
        return null;
    }

    private void scroll(int dy) {
        //dy为0或首屏展示内容完毕，则禁止滚动
        if (dy == 0 || !canVerticalScroll()) {
            return;
        }
        mScrollUp = dy < 0;
        boolean isScrollContainerTop = isScrollContainerTop();
        if (isScrollContainerTop || !isHeaderCollapseCompletely()) {
            //当底部内容滑动到顶部或header未完全隐藏时，滑动header
            scrollBy(0, dy);
        }
        if ((!isScrollContainerTop && isHeaderExpandCompletely()) || isHeaderCollapseCompletely()) {
            //当底部内容未滑动到顶部且header完全展开或header完全隐藏时，滑动底部内容
            scrollContent(dy);
        }
        dispatchScrollBarPosition();
        if (Math.abs(dy) >= mTouchSlop) {
            dispatchFastBackVisibleEvent();
        }
        invalidate();
    }

    private void scrollContent(int dy) {
        final View view = getCurrentScrollableView();
        if (view != null) {
            view.scrollBy(0, dy);
        }
    }

    private void fling(float vy) {
        //vy为0或首屏展示内容完毕，则禁止滚动
        if (vy == 0 || !canVerticalScroll()) {
            return;
        }
        mScroller.abortAnimation();
        //上滑速度小于0，下滑速度大于0
        mFlingUp = vy < 0;
        mScrollUp = !mFlingUp;
        mFlingContent = false;
        mFlingToTop = false;
        mLastScrollerY = getScrollY();
        if ((mFlingUp && isScrollBottom()) || (!mFlingUp && isScrollTop())) {
            //上滑且滑到底部，或下滑且滑到顶部，不用fling
            return;
        }
        mScroller.fling(0, getScrollY(), 0, -Math.round(vy), 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        if (Math.abs(vy) >= 1000) {
            dispatchScrollBarVisibleEvent(true);
            dispatchFastBackVisibleEvent();
        }
        invalidate();
    }

    /**
     * fling底部内容
     *
     * @param vy
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void flingContent(int vy, int distance, int duration) {
        final View view = getCurrentScrollableView();
        if (view instanceof RecyclerView) {
            ((RecyclerView) view).fling(0, vy);
        } else if (view instanceof WebView) {
            ((WebView) view).flingScroll(0, vy);
        } else if (view instanceof ScrollView) {
            ((ScrollView) view).fling(vy);
        } else if (view instanceof AbsListView) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ((AbsListView) view).fling(vy);
            } else {
                ((AbsListView) view).smoothScrollBy(distance, duration);
            }
        }
    }

    private void dispatchScrollBarPosition() {
        if (mScrollBarListener != null) {
            //绘制scroll bar
            final int scrollRange = computeVerticalScrollRange();
            final int scrollExtent = computeVerticalScrollExtent();
            final int scrollbarRange = scrollExtent + mTopOffset - mScrollBarMarginTop - mScrollbarMarginBottom - mScrollBarHeight;
            float currentPercent = computeVerticalScrollOffset() * 1.f / (scrollRange - scrollExtent);
            currentPercent = currentPercent > 1.f ? 1.f : currentPercent;
            int scrollBarTop = (int) (mScrollBarMarginTop + currentPercent * scrollbarRange);
            if (scrollBarTop == mPreScrollBarTop) {
                return;
            }
            mPreScrollBarTop = scrollBarTop;
            mScrollBarListener.onScroll(scrollBarTop, mScrollUp);
        }
    }

    public int getScrollBarStart() {
        return mScrollBarMarginTop;
    }

    public int getScrollBarEnd() {
        return computeVerticalScrollExtent() - mScrollbarMarginBottom;
    }

    private boolean canVerticalScroll() {
        return realHeight() > computeVerticalScrollExtent();
    }

    private int realHeight() {
        int realHeight = 0;
        for (int i = 0; i < getChildCount(); i++) {
            final View view = getChildAt(i);
            final ViewPager pager = findDirectViewPager(view);
            if (pager != null) {
                int maxHeight = 0;
                for (int j = 0; j < pager.getChildCount(); j++) {
                    if (pager.getAdapter() instanceof FragmentTagPagerAdapter) {
                        maxHeight = Math.max(maxHeight, compute(
                                findFragmentView((FragmentTagPagerAdapter) pager.getAdapter(), j), "computeVerticalScrollExtent"));
                    }
                }
                realHeight += maxHeight;
            } else {
                realHeight += view.getHeight();
            }
        }
        return realHeight;
    }

    public int leftVisibleHeight() {
        return computeVerticalScrollExtent() - realHeight();
    }

    @Nullable
    private ViewPager findDirectViewPager(View parent) {
        if (parent instanceof ViewPager) {
            return (ViewPager) parent;
        }
        if (parent instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) parent).getChildCount(); i++) {
                View child = ((ViewGroup) parent).getChildAt(i);
                if (child instanceof ViewPager) {
                    return (ViewPager) child;
                }
            }
        }
        return null;
    }

    /**
     * header是否完全隐藏
     *
     * @return
     */
    private boolean isHeaderCollapseCompletely() {
        return getScrollY() == mMaxScrollY;
    }

    /**
     * header是否完全展开
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
        final View view = getCurrentScrollableView();
        return view == null || !canViewScrollUp(view);
    }

    /**
     * 是否已滑动到顶部
     *
     * @return
     */
    private boolean isScrollTop() {
        return isScrollContainerTop() && isHeaderExpandCompletely();
    }

    /**
     * 是否已滑动到底部
     *
     * @return
     */
    private boolean isScrollBottom() {
        final View view = getCurrentScrollableView();
        return isHeaderCollapseCompletely()
                && view != null
                && !canViewScrollDown(view);
    }

    private boolean canViewScrollUp(View view) {
        return ViewCompat.canScrollVertically(view, -1);
    }

    private boolean canViewScrollDown(View view) {
        return ViewCompat.canScrollVertically(view, 1);
    }

    @Nullable
    private View getCurrentScrollableView() {
        if (mViewPager != null) {
            return findScrollableView(findFragmentView(mViewPagerAdapter, mViewPager.getCurrentItem()));
        }
        return null;
    }

    private View findScrollableView(View view) {
        if (view instanceof RecyclerView
                || view instanceof WebView
                || view instanceof ListView
                || view instanceof GridView) {
            return view;
        }
        if (view instanceof ViewGroup) {
            int count = ((ViewGroup) view).getChildCount();
            for (int i = 0; i < count; i++) {
                View childView = findScrollableView(((ViewGroup) view).getChildAt(i));
                if (childView != null) {
                    return childView;
                }
            }
        }
        return null;
    }

    @Nullable
    private View findFragmentView(FragmentTagPagerAdapter adapter, int position) {
        try {
            final String tag = adapter.makeFragmentName(position);
            return adapter.getFragmentManager().findFragmentByTag(tag).getView();
        } catch (Exception e) {
            //do nothing.
        }
        return null;
    }

    public void setupViewPager(@NonNull ViewPager viewPager, @NonNull FragmentTagPagerAdapter pagerAdapter) {
        mViewPager = viewPager;
        mViewPagerAdapter = pagerAdapter;
    }

    public void addOnScrollHeaderListener(@NonNull OnScrollHeaderListener listener) {
        if (!mOnScrollHeaderListeners.contains(listener)) {
            mOnScrollHeaderListeners.add(listener);
        }
    }

    public void setScrollBarListener(@NonNull OnScrollBarListener listener) {
        mScrollBarListener = listener;
    }

    public void setScrollBarVisibleListener(@NonNull OnScrollBarVisibleListener listener) {
        mScrollBarVisibleListener = listener;
    }

    public void setOnFastBackVisibleListener(@NonNull OnFastBackVisibleListener listener) {
        mFastBackVisibleListener = listener;
    }

    public void setOnRefreshListener(@NonNull OnRefreshListener listener) {
        mRefreshListener = listener;
    }

    public interface OnScrollHeaderListener {
        void onScroll(int currentPosition, int maxPosition);
    }

    public interface OnScrollBarVisibleListener {
        void onVisible(boolean visible);
    }

    public interface OnScrollBarListener {
        void onScroll(float top, boolean scrollUp);
    }

    public interface OnFastBackVisibleListener {
        void onVisible(boolean visible);
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

}
