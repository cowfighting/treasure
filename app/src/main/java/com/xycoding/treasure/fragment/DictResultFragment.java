package com.xycoding.treasure.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;
import android.widget.Toast;

import com.xycoding.treasure.R;
import com.xycoding.treasure.databinding.FragmentDictResultBinding;
import com.xycoding.treasure.rx.RxViewWrapper;
import com.xycoding.treasure.view.QuickPositioningDialog;
import com.xycoding.treasure.view.headerviewpager.FragmentTagPagerAdapter;
import com.xycoding.treasure.view.headerviewpager.HeaderViewPager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.functions.Consumer;

/**
 * Created by xuyang on 2017/3/22.
 */
public class DictResultFragment extends BaseBindingFragment {

    private FragmentDictResultBinding mBinding;
    private FragmentTagPagerAdapter mPagerAdapter;
    private List<DictContentFragment> mFragments;
    private QuickPositioningDialog mDialog;
    private Runnable mScrollBarHideRunnable;

    public static DictResultFragment createInstance() {
        return new DictResultFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_dict_result;
    }

    @Override
    protected void initControls(Bundle savedInstanceState) {
        mBinding = (FragmentDictResultBinding) binding;
        initViews();
    }

    @Override
    protected void setListeners() {
        RxViewWrapper.clicks(mBinding.layoutHeader.fitTextView).subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                Toast.makeText(getContext(), mBinding.layoutHeader.fitTextView.getText(), Toast.LENGTH_SHORT).show();
            }
        });
        RxViewWrapper.clicks(mBinding.fab).subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                mBinding.headerViewPager.scrollToTop();
            }
        });
        RxViewWrapper.clicks(mBinding.layoutHeader.tvAd).subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                Toast.makeText(getContext(), mBinding.layoutHeader.tvAd.getText(), Toast.LENGTH_SHORT).show();
            }
        });
        RxViewWrapper.clicks(mBinding.ivScrollBar).subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                showQuickPositioningDialog();
            }
        });
        mBinding.headerViewPager.setScrollBarVisibleListener(new HeaderViewPager.OnScrollBarVisibleListener() {
            @Override
            public void onVisible(boolean visible) {
                mBinding.ivScrollBar.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        });
        mBinding.headerViewPager.setScrollBarListener(new HeaderViewPager.OnScrollBarListener() {
            @Override
            public void onScroll(float top, boolean scrollUp) {
                if (mBinding.ivScrollBar.isShown()) {
                    mBinding.ivScrollBar.setVisibility(View.VISIBLE);
                    mBinding.ivScrollBar.setTranslationY(top);
                    mBinding.ivScrollBar.removeCallbacks(mScrollBarHideRunnable);
                    if (mScrollBarHideRunnable == null) {
                        mScrollBarHideRunnable = new Runnable() {
                            @Override
                            public void run() {
                                mBinding.ivScrollBar.setVisibility(View.INVISIBLE);
                            }
                        };
                    }
                    mBinding.ivScrollBar.postDelayed(mScrollBarHideRunnable, 2000);
                }
            }
        });
    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }

    private void initViews() {
        mFragments = new ArrayList<>();
        mFragments.add(DictContentFragment.createInstance(20, true));
        mFragments.add(DictContentFragment.createInstance(2, false));
        mFragments.add(DictContentFragment.createInstance(10, false));

        mPagerAdapter = new FragmentTagPagerAdapter(getChildFragmentManager()) {

            public String[] titles = new String[]{"详解", "朗文", "柯林斯"};

            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }

            @Override
            public String makeFragmentName(int position) {
                return "tag:" + position;
            }

            @Override
            public int getCount() {
                return mFragments.size();
            }

            @Override
            public CharSequence getPageTitle(int position) {
                return titles[position];
            }
        };
        mBinding.viewPager.setAdapter(mPagerAdapter);
        mBinding.layoutTabDict1.tabLayoutDict.setupWithViewPager(mBinding.viewPager);
        mBinding.headerViewPager.setupViewPager(mBinding.viewPager, mPagerAdapter);
        mPagerAdapter.notifyDataSetChanged();
    }

    private void showQuickPositioningDialog() {
        if (mDialog == null) {
            mDialog = new QuickPositioningDialog(getContext());
            mDialog.setOnQuickClickListener(new QuickPositioningDialog.OnQuickClickListener() {
                @Override
                public void onClick(int position) {
                    mBinding.headerViewPager.scrollToPosition(position);
                }
            });
        }
        String[] data = {"词典1", "词典2", "词典3", "词典4"};
        //计算当前scroll bar全局中心点
        int[] location = new int[2];
        mBinding.headerViewPager.getLocationOnScreen(location);
        mDialog.show(
                Arrays.asList(data),
                Math.round(location[1] + mBinding.ivScrollBar.getTranslationY() + mBinding.ivScrollBar.getHeight() / 2),
                location[1] + mBinding.headerViewPager.getScrollBarStart(),
                location[1] + mBinding.headerViewPager.getScrollBarEnd());
    }

}
