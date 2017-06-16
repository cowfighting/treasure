package com.xycoding.treasure.activity;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.NotificationCompat;
import android.view.Menu;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.xycoding.treasure.R;
import com.xycoding.treasure.databinding.ActivityMainBinding;
import com.xycoding.treasure.rx.RxViewWrapper;
import com.xycoding.treasure.service.LocalIntentService;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class MainActivity extends BaseBindingActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private ActivityMainBinding mBinding;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initControls(Bundle savedInstanceState) {
        mBinding = (ActivityMainBinding) binding;
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        showNotify();
    }

    @Override
    protected void setListeners() {
        subscriptions.add(RxViewWrapper.clicks(mBinding.fab).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                Snackbar.make(mBinding.fab, "Replace with your own action", Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.cardViewCollapsingToolbar).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                startActivity(new Intent(MainActivity.this, CollapsingToolbarActivity.class));
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.cardViewDialog).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                startActivity(new Intent(MainActivity.this, DialogActivity.class));
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.cardViewCustom).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                startActivity(new Intent(MainActivity.this, ViewActivity.class));
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.cardViewMode).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                startActivity(new Intent(MainActivity.this, ActionModeActivity.class));
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.cardViewHandwriting).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                startActivity(new Intent(MainActivity.this, HandwritingActivity.class));
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.cardViewSpeech).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                startActivity(new Intent(MainActivity.this, SpeechActivity.class));
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.cardViewDict).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                startActivity(new Intent(MainActivity.this, DictActivity.class));
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.cardViewChart).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                startActivity(new Intent(MainActivity.this, ChartActivity.class));
            }
        }));
        subscriptions.add(RxViewWrapper.clicks(mBinding.cardViewImmersiveMode).subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                startActivity(new Intent(MainActivity.this, ImmersiveModeActivity.class));
            }
        }));
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        startService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Toast.makeText(this, "我没打开新页面哦", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onScreenshot(Uri uri) {
        Snackbar.make(mBinding.getRoot(), "截屏：" + uri, Snackbar.LENGTH_INDEFINITE)
                .setAction("好的", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                    }
                })
                .show();
    }

    private void showNotify() {
        //MainActivity需设置singleTask
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews contentView = new RemoteViews(getPackageName(), R.layout.layout_notification);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_search_white_24dp)
                .setContent(contentView);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, builder.build());
    }

    private Handler mHandler = new Handler();

    private void startService() {
        final Intent service = new Intent(this, LocalIntentService.class);
        service.putExtra(LocalIntentService.BUNDLE_KEY_TASK, "task1");
        startService(service);
        service.putExtra(LocalIntentService.BUNDLE_KEY_TASK, "task2");
        startService(service);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                service.putExtra(LocalIntentService.BUNDLE_KEY_TASK, "task3");
                startService(service);
            }
        }, 5000);
    }

    private void testRxJava() {
        final Observable<String> networkDictObservable = Observable.defer(
                new Func0<Observable<String>>() {
                    @Override
                    public Observable<String> call() {
                        try {
                            Thread.sleep(52);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return Observable.just("network");
                    }
                })
                .flatMap(new Func1<String, Observable<String>>() {
                    @Override
                    public Observable<String> call(String str) {
                        return Observable.just(str);
                    }
                })
                //设置网络查词超时
                .timeout(2000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .share();
        final Observable<String> localDictObservable = Observable.defer(
                new Func0<Observable<String>>() {
                    @Override
                    public Observable<String> call() {
                        try {
                            Thread.sleep(51);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return Observable.just("local");
                    }
                })
                .map(new Func1<String, String>() {
                    @Override
                    public String call(String s) {
                        return s;
                    }
                })
                .takeUntil(networkDictObservable.onErrorResumeNext(new Func1<Throwable, Observable<? extends String>>() {
                    @Override
                    public Observable<? extends String> call(Throwable throwable) {
                        return Observable.never();
                    }
                }))
                .subscribeOn(Schedulers.io());
        Observable.mergeDelayError(localDictObservable, networkDictObservable)
                .observeOn(AndroidSchedulers.mainThread(), true)
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String str) {
                        System.out.println("RxJava:" + str);
                    }
                });
    }

}
