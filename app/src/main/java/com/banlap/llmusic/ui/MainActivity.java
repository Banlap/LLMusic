package com.banlap.llmusic.ui;

import static android.view.View.GONE;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.banlap.llmusic.R;
import com.banlap.llmusic.base.BaseActivity;
import com.banlap.llmusic.databinding.ActivityMainBinding;
import com.banlap.llmusic.databinding.DialogCharacterMenuBinding;
import com.banlap.llmusic.databinding.DialogDeleteListAllBinding;
import com.banlap.llmusic.databinding.DialogDownloadBinding;
import com.banlap.llmusic.databinding.DialogLocalFileBinding;
import com.banlap.llmusic.databinding.DialogMainMenuBinding;
import com.banlap.llmusic.databinding.DialogMessageBinding;
import com.banlap.llmusic.databinding.DialogSortMenuBinding;
import com.banlap.llmusic.databinding.ItemLyricListBinding;
import com.banlap.llmusic.databinding.ItemMusicListBinding;
import com.banlap.llmusic.databinding.ItemPlayListBinding;
import com.banlap.llmusic.fixed.LiellaMusic;
import com.banlap.llmusic.model.Message;
import com.banlap.llmusic.model.Music;
import com.banlap.llmusic.model.MusicLyric;
import com.banlap.llmusic.model.Version;
import com.banlap.llmusic.request.ThreadEvent;
import com.banlap.llmusic.service.CharacterService;
import com.banlap.llmusic.service.MusicPlayService;
import com.banlap.llmusic.sql.MysqlHelper;
import com.banlap.llmusic.uivm.MainVM;
import com.banlap.llmusic.utils.Base64;
import com.banlap.llmusic.utils.BluetoothUtil;
import com.banlap.llmusic.utils.CharacterHelper;
import com.banlap.llmusic.utils.MyAnimationUtil;
import com.banlap.llmusic.utils.NotificationHelper;
import com.banlap.llmusic.utils.PxUtil;
import com.banlap.llmusic.utils.SPUtil;
import com.banlap.llmusic.widget.CenterLayoutManager;
import com.banlap.llmusic.widget.LyricScrollView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import jp.wasabeef.glide.transformations.CropCircleWithBorderTransformation;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class MainActivity extends BaseActivity<MainVM, ActivityMainBinding>
        implements MainVM.MainCallBack {

    private final Context context = MainActivity.this;
    private List<Music> musicList;                      //按类型的所有歌曲
    private List<Music> tempMusicList;                  //临时音乐列表
    private List<Music> playList;                       //当前播放的列表
    private List<MusicLyric> musicLyricList;            //当前播放歌曲的歌词列表
    private List<Message> messageList;                  //消息列表
    private List<Version> versionList;                  //版本列表
    private MusicListAdapter musicListAdapter;          //音乐列表适配器
    private PlayMusicListAdapter playMusicListAdapter;  //播放列表适配器
    private ObjectAnimator objectAnimator;              //动画效果
    private MusicPlayService.MusicBinder binder;        //用于绑定服务
    private ServiceConn conn;                           //用于绑定服务
    private Intent intentService;                       //音乐服务
    private Intent intentCharacterService;              //角色服务
    private boolean isSelect = false;                   //查询一次数据
    private boolean isClick = false;                    //判断是否点击按钮
    private boolean isNotMain = false;                  //判断是否在主界面
    private boolean isShowMusicPanel = false;           //判断是否显示音乐面板
    private boolean isShowMusicList = false;            //判断是否显示音乐清单
    public static boolean isPlay = false;               //判断是否播放音乐
    public static String currentMusicName ="";          //当前歌曲的歌名
    public static String currentMusicSinger ="";        //当前歌曲的歌手
    private String currentMusicImg ="";                 //当前歌曲的图片
    public static Bitmap currentBitmap = null;          //当前歌曲Bitmap图
    private AlertDialog mAlertDialog;                   //弹窗
    private int musicListSize = 0;                      //获取总播放列表数
    private int playMode = 0;                           //播放模式: 0顺序播放 1随机播放 2单曲循环
    private int currentAllTime = 0;                     //当前歌曲总时间
    private final int panelMoveAxis=750;                 //面板移动值
    private int rThemeId =0;                             //当前主题
    /** 角色视图 */
    private String mCharacterName;                       //当前角色
    private ActivityResultLauncher<Intent> intentActivityResultLauncher;
    private ActivityResultLauncher<Intent> intentTakePhotoLauncher;
    private DialogDownloadBinding downloadBinding;
    private boolean isExistNewVersion = false;            //是否存在新版本app
    private boolean isFinishAnimator = true;              //是否执行标题栏文本显示动画
    private boolean isChangeScrollRange = false;          //解决重复高度时刷新appBar的标题文字动画
    private ObjectAnimator textAnimator;                  //标题栏文本动画
    private RequestOptions requestOptions;
    private boolean isUpSortByTime = false;
    private boolean isUpSortByName = false;
    private boolean isUpSortBySinger = false;
    private int clickSortType = 0;                        //当前点击的排序类型
    private LyricScrollView lyricScrollView;              //
    private MediaSession mSession;                        //用于获取按键事件
    public boolean isFirstBluetoothControl = true;
    private MainFragmentStateAdapter  mainFragmentStateAdapter;
    public static final int REQUEST_CODE_DOWNLOAD_APP = 101;           //检查下载app时需要的权限
    public static final int REQUEST_CODE_SCAN_LOCAL_FILE = 102;       //检查扫描文件所需要的权限
    public static final int REQUEST_CODE_SELECT_LOCAL_FILE = 103;       //检查选择文件所需要的权限

    private DialogLocalFileBinding dialogLocalFileBinding;

    /** MediaSession框架回掉 用于返回耳机实体按钮操作 */
    private final MediaSession.Callback mSessionCallback = new MediaSession.Callback() {
        @Override
        public boolean onMediaButtonEvent(@NonNull Intent intent) {
            if (mSession == null) {
                return false;
            }
            if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
                KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
                if (event != null && event.getAction() == KeyEvent.ACTION_DOWN) {
                    onKeyDown(event.getKeyCode(),event);
                    onKeyUp(event.getKeyCode(),event);
                    return true;
                }
            }
            return false;
        }
    };

    @Override
    protected int getLayoutId() { return R.layout.activity_main; }

    /** 初始化主页数据 */
    @SuppressLint("CheckResult")
    @Override
    protected void initData() {
        musicList = new ArrayList<>();
        playList = new ArrayList<>();
        musicLyricList = new ArrayList<>();
        messageList = new ArrayList<>();
        versionList = new ArrayList<>();
        //本地缓存列表
        List<Music> spList = SPUtil.getListValue(this, "PlayListData", Music.class);
        if(spList.size()>0){
            playList.addAll(spList);
            setPlayListDefault(playList);
        }

        if(SPUtil.getStrValue(this, "SavePlayMode")!=null) {
            if (!(SPUtil.getStrValue(this, "SavePlayMode").equals(""))) {
                playMode = Integer.parseInt(SPUtil.getStrValue(this, "SavePlayMode"));
            }
        }

        requestOptions = new RequestOptions();
        requestOptions.diskCacheStrategy(DiskCacheStrategy.ALL);
        //requestOptions.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL); //关键代码，加载原始大小
        requestOptions.format(DecodeFormat.PREFER_RGB_565); //设置为这种格式去掉透明度通道，可以减少内存占有

        mCharacterName = CharacterHelper.CHARACTER_NAME_KEKE;
    }

    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        getViewDataBinding().setVm(getViewModel());
        getViewModel().setCallBack(this);
        //初始化主页内容
        initMainView();
        //连接数据库
        EventBus.getDefault().post(new ThreadEvent(ThreadEvent.CONNECT_MYSQL));
        //各种监听
        initListener();
        //开启所有相关服务
        startAllService();
        //广播监听蓝牙连接状态
        BluetoothUtil.getInstance().registerBluetoothReceiver(this);
        //初始化碎片
        initFragment();

    }


    /** 初始化主页内容 */
    private void initMainView(){
        getViewDataBinding().clMain.setVisibility(View.VISIBLE);
        getViewDataBinding().clAlbumDetail.setVisibility(View.VISIBLE);
        getViewDataBinding().pbLoadingMusic.setVisibility(View.INVISIBLE);
        getViewDataBinding().tvVersion.setVisibility(View.GONE);

        /** 动画：初始化将详细页面移走 */
        ObjectAnimator detailPanelDefault = MyAnimationUtil.objectAnimatorLeftOrRight(this, false, false, getViewDataBinding().clAlbumDetail);
        detailPanelDefault.start();

        textAnimator = MyAnimationUtil.objectAnimatorShowOrHide(MainActivity.this,  0, 0, getViewDataBinding().tvTitleBar);
        textAnimator.start();

        getViewDataBinding().llShowNormalBar.setVisibility(View.VISIBLE);
        getViewDataBinding().llShowSearchBar.setVisibility(View.GONE);

        musicListAdapter = new MusicListAdapter(this, musicList);
        getViewDataBinding().rvMusicList.setLayoutManager(new LinearLayoutManager(this));
        //setMusicListHeader(getViewDataBinding().rvMusicList);
        getViewDataBinding().rvMusicList.setAdapter(musicListAdapter);
        musicListAdapter.notifyDataSetChanged();

        playMusicListAdapter = new PlayMusicListAdapter(this, playList);
        getViewDataBinding().rvPlayList.setLayoutManager(new LinearLayoutManager(this));
        getViewDataBinding().rvPlayList.setAdapter(playMusicListAdapter);
        playMusicListAdapter.notifyDataSetChanged();


        Glide.with(getApplication())
                .setDefaultRequestOptions(requestOptions)
                .load(getResources().getIdentifier("ic_music_3", "mipmap", getPackageName()))
                .transform(new RoundedCornersTransformation(30, 0, RoundedCornersTransformation.CornerType.ALL))
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(getViewDataBinding().ivMusicImg);

        getViewDataBinding().tvListSize.setText("("+ playList.size() + ")");

        //歌词滚动
        lyricScrollView = getViewDataBinding().lvShowLyric;
    }

    /** 初始化碎片 */
    private void initFragment() {

        if(null == mainFragmentStateAdapter) {
            List<Fragment> fragmentList = new ArrayList<>();
            fragmentList.add(new MainListFragment());
            fragmentList.add(new LocalListFragment());
            mainFragmentStateAdapter = new MainFragmentStateAdapter(this, fragmentList);
        }

        getViewDataBinding().vp2Main.setOffscreenPageLimit(2);
        getViewDataBinding().vp2Main.setAdapter(mainFragmentStateAdapter);

        getViewDataBinding().vp2Main.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if(0 == position) {
                    AnimatorSet animatorSet = MyAnimationUtil.animatorSetEnlarge(getViewDataBinding().tvDiscover, 1, (float) 1.3);
                    AnimatorSet animatorSet2 = MyAnimationUtil.animatorSetEnlarge(getViewDataBinding().tvLocal, (float) 1.3, 1);
                    animatorSet.start();
                    animatorSet2.start();

                    AnimatorSet animatorSetMove = MyAnimationUtil.animatorSetMove(getViewDataBinding().vLine, false);
                    animatorSetMove.start();

                } else if(1 == position) {
                    AnimatorSet animatorSet = MyAnimationUtil.animatorSetEnlarge(getViewDataBinding().tvDiscover,  (float) 1.3, 1);
                    AnimatorSet animatorSet2 = MyAnimationUtil.animatorSetEnlarge(getViewDataBinding().tvLocal, 1, (float) 1.3);
                    animatorSet.start();
                    animatorSet2.start();

                    AnimatorSet animatorSetMove = MyAnimationUtil.animatorSetMove(getViewDataBinding().vLine, true);
                    animatorSetMove.start();
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                //Log.e("PageScrolled", "position: " + position + " positionOffset: " + positionOffset + " positionOffsetPixels: " + positionOffsetPixels);

            }
        });

        getViewDataBinding().tvDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getViewDataBinding().vp2Main.setCurrentItem(0);
            }
        });

        getViewDataBinding().tvLocal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getViewDataBinding().vp2Main.setCurrentItem(1);
            }
        });

    }


    /** 初始化所有功能监听 */
    private void initListener() {
        getViewDataBinding().sbMusicBar.setOnSeekBarChangeListener(new MusicBarChangerListener());
        getViewDataBinding().sbMusicBar.setOnTouchListener(new ProgressBarTouchListener());
        getViewDataBinding().rlPlayController.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().rlShowLoading.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().rlDisableClick.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().llAllPlay.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().llSearch.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().llSort.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().llCancel.setOnClickListener(new ButtonClickListener());

        getViewDataBinding().llBack.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().ablAppBar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                //Toast.makeText(MainActivity.this, "vo: " + verticalOffset, Toast.LENGTH_SHORT).show();
                if(Math.abs(verticalOffset) == appBarLayout.getTotalScrollRange()) {
                    if(isChangeScrollRange) {
                        textAnimator = MyAnimationUtil.objectAnimatorShowOrHide(MainActivity.this,  0, 1, getViewDataBinding().tvTitleBar);
                        textAnimator.start();
                        isFinishAnimator = false;
                        isChangeScrollRange = false;
                    }
                } else {
                    isChangeScrollRange = true;
                    if(!isFinishAnimator) {
                        textAnimator = MyAnimationUtil.objectAnimatorShowOrHide(MainActivity.this, 1, 0, getViewDataBinding().tvTitleBar);
                        isFinishAnimator = true;
                        textAnimator.start();
                    }
                }
            }
        });


        getViewDataBinding().etSearchMusic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String name = getViewDataBinding().etSearchMusic.getText().toString();
                if (!name.equals("")) {
                    List<Music> nullData = new ArrayList<>();
                    for (Music music : tempMusicList) {
                        if (music.getMusicType().equals(" ")) {
                            nullData.add(music);
                        }
                    }
                    List<Music> searchList = new ArrayList<>();
                    int size = tempMusicList.size();
                    for (int i = 0; i < size; i++) {
                        if (tempMusicList.get(i).musicName.contains(name)) {
                            searchList.add(tempMusicList.get(i));
                        }
                    }
                    if (searchList.size() > 0) {
                        musicList.clear();
                        musicList.addAll(searchList);
                        musicList.addAll(nullData);
                        musicListAdapter.notifyDataSetChanged();
                    }
                } else {
                    musicList.clear();
                    if(tempMusicList!=null) {
                        musicList.addAll(tempMusicList);
                    }
                    musicListAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        intentActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        //此处是跳转的result回调方法
                        if(isCanDrawOverlays()) {
                            if(null != mAlertDialog) {
                                mAlertDialog.dismiss();
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                startCharacterService(mCharacterName);
                            } else {
                                intentCharacterService.putExtra("CharacterName", mCharacterName);
                                startService(intentCharacterService);
                            }
                            getViewDataBinding().ivCharacterStatus.setVisibility(View.VISIBLE);
                        }
                    }
                });

        intentTakePhotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        try {
                            Intent intent = result.getData();
                            Uri uri = intent.getData();
                            //Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                            //getViewDataBinding().ivBg.setImageBitmap(bitmap);

                            if(null != uri) {
                                SPUtil.setStrValue(context,"BackgroundUri",uri.toString());
                            }

                            Glide.with(getApplication())
                                    .setDefaultRequestOptions(requestOptions)
                                    .load(uri)
                                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                    .into(getViewDataBinding().ivBg);
                            Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {

                        }
                    }
                });

    }

    /** 开启所有相关服务 */
    private void startAllService() {
        conn = new ServiceConn();
        intentService = new Intent(this, MusicPlayService.class);           //创建音乐播放服务
        intentCharacterService = new Intent(this, CharacterService.class);  //创建角色服务
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMusicService(false, "LLMusic", "Singer", null);
        } else {
            startService(intentService);
            initNotificationHelper("LLMusic", "Singer", "");
        }
        bindService(intentService, conn, BIND_AUTO_CREATE);
    }

    /** 初始化音乐服务*/
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startMusicService(boolean isPlayMusic, String musicName, String musicSinger, Bitmap bitmap) {
        intentService.putExtra("IsPlayMusic", isPlayMusic);
        intentService.putExtra("MusicName", musicName);
        intentService.putExtra("MusicSinger", musicSinger);
        if(bitmap!=null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);//把bitmap100%高质量压缩 到 output对象里
            byte[] result = bos.toByteArray();
            intentService.putExtra("MusicBitmap", result);
        } else {
            intentService.putExtra("MusicBitmap", (byte[]) null);
        }
        startForegroundService(intentService);
    }


    /** 是否已经开启弹窗权限*/
    private boolean isCanDrawOverlays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                return false;
            }
        }
        return true;
    }

    /** 初始化角色服务*/
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startCharacterService(String characterName) {
        intentCharacterService.putExtra("IsPlayMusic", isPlay);
        intentCharacterService.putExtra("CharacterName", characterName);
        startForegroundService(intentCharacterService);
    }

    /**
     * 判断服务是否开启
     *
     * @param mContext 上下文
     * @param className 服务class名
     * @return true:开启 false:未开启
     */
    public static boolean isWorked(Context mContext, String className) {
        ActivityManager myManager = (ActivityManager) mContext
                .getApplicationContext().getSystemService(
                        Context.ACTIVITY_SERVICE);
        ArrayList<ActivityManager.RunningServiceInfo> runningService = (ArrayList<ActivityManager.RunningServiceInfo>) myManager
                .getRunningServices(30);
        for (int i = 0; i < runningService.size(); i++) {
            if (runningService.get(i).service.getClassName().toString()
                    .equals(className)) {
                return true;
            }
        }
        return false;
    }

    /** 初始化通知栏消息 */
    @SuppressLint("RemoteViewLayout")
    private void initNotificationHelper(String musicName, String musicSinger, String imgUrl) {
        if(!imgUrl.equals("")) {
            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.SHOW_IMAGE_URL, musicName, musicSinger, imgUrl));
        } else {
            NotificationHelper.getInstance().createRemoteViews(this, musicName, musicSinger, null, true);
            sendWidgetBroadcastReceiver(false);
        }
    }

    /** 绑定服务需要ServiceConnection对象 */
    private class ServiceConn implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            binder = (MusicPlayService.MusicBinder) service;
            changePlayMode(playMode);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) { }
    }

    @Override
    public void onResume(){
        super.onResume();
        //获取壁纸数据
        String strBackgroundUri = SPUtil.getStrValue(getApplicationContext(), "BackgroundUri");
        if(null != strBackgroundUri && !"".equals(strBackgroundUri)) {
            Glide.with(getApplication())
                    .setDefaultRequestOptions(requestOptions)
                    .load(Uri.parse(strBackgroundUri))
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(getViewDataBinding().ivBg);
        }

        String strThemeId = SPUtil.getStrValue(getApplicationContext(), "SaveThemeId");
        if(strThemeId!=null) {
            if(!strThemeId.equals("")) {
                rThemeId = Integer.parseInt(strThemeId);
                changeTheme(rThemeId);
                lyricScrollView.setThemeId(rThemeId);
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_CHANGE_THEME));
            }
        }
        //创建媒体MediaSession框架 用于蓝牙耳机按钮控制
        createMediaSession();
    }

    @Override
    public void onPause() {
        super.onPause();
        //releaseMediaSession();
    }

    /** 创建媒体MediaSession框架 */
    private void createMediaSession() {
        if (mSession == null) {
            mSession = new MediaSession(context, MainActivity.class.getSimpleName());
            mSession.setCallback(mSessionCallback);
            mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
            mSession.setActive(true);
        }
    }

    private void releaseMediaSession() {
        if (mSession != null) {
            mSession.setCallback(null);
            mSession.setActive(false);
            mSession.release();
            mSession = null;
        }
    }

    @SuppressLint({"SetTextI18n", "RemoteViewLayout"})
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void MessageEvent(ThreadEvent event) {
        switch (event.msgCode) {
            case ThreadEvent.CONNECT_MYSQL_LOADING:
                getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                break;
            case ThreadEvent.CONNECT_MYSQL_SUCCESS:
                Log.e("MYSQL", "mysql connect success");
                if (!isSelect) {
                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_DATA_APP_VERSION));
                    //EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_DATA_LIST_MESSAGE));
                    //EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_DATA_LIST_COUNT));
                    //EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_DATA_LIST));
                    isSelect = true;
                }
                break;
            case ThreadEvent.CONNECT_MYSQL_ERROR:
                getViewDataBinding().rlShowLoading.setVisibility(View.GONE);
                Toast.makeText(this, "connect error", Toast.LENGTH_SHORT).show();
                break;
            case ThreadEvent.GET_SUCCESS:
                musicList.clear();
                musicList.addAll(event.musicList);
                musicList.addAll(LiellaMusic.getInstance().getNullMusicData());
                musicListAdapter.notifyDataSetChanged();
                sortList(0);
                clickSortType = 0;
                ObjectAnimator mainPanelChangeObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(MainActivity.this, true, false, getViewDataBinding().clMain);
                ObjectAnimator detailPanelChangeObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(MainActivity.this, false, true, getViewDataBinding().clAlbumDetail);
                mainPanelChangeObjectAnimator.start();
                detailPanelChangeObjectAnimator.start();
                getViewDataBinding().rlShowLoading.setVisibility(View.GONE);
                isNotMain = true;
                break;
            case ThreadEvent.GET_ALBUM_SUCCESS:
                if(ThreadEvent.ALBUM_LIELLA.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    getViewDataBinding().ivLogo.setBackgroundResource(R.mipmap.ic_album_liella_3);
                    getViewDataBinding().tvTitleBar.setText("Liella!");
                    getViewDataBinding().tvListMsgName1.setText("Liella!");
                    getViewDataBinding().tvListMsgName2.setText("LoveLive!Superstar!!");
                } else if(ThreadEvent.ALBUM_FOUR_YUU.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    getViewDataBinding().ivLogo.setBackgroundResource(R.mipmap.ic_album_liyuu);
                    getViewDataBinding().tvTitleBar.setText("Liyuu");
                    getViewDataBinding().tvListMsgName1.setText("Liyuu");
                    getViewDataBinding().tvListMsgName2.setText("Liyuu");
                } else if(ThreadEvent.ALBUM_SUNNY_PASSION.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    getViewDataBinding().ivLogo.setBackgroundResource(R.mipmap.ic_album_sunny_passion);
                    getViewDataBinding().tvTitleBar.setText("SunnyPassion");
                    getViewDataBinding().tvListMsgName1.setText("SunnyPassion");
                    getViewDataBinding().tvListMsgName2.setText("サニーパッション");
                } else if(ThreadEvent.ALBUM_NIJIGASAKI.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    getViewDataBinding().ivLogo.setBackgroundResource(R.mipmap.ic_album_nijigasaki_3);
                    getViewDataBinding().tvTitleBar.setText("虹ヶ咲学園スクールアイドル同好会");
                    getViewDataBinding().tvListMsgName1.setText("Nijigasaki HighSchool IdolClub");
                    getViewDataBinding().tvListMsgName2.setText("虹ヶ咲学園スクールアイドル同好会");
                } else if(ThreadEvent.ALBUM_AQOURS.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    getViewDataBinding().ivLogo.setBackgroundResource(R.mipmap.ic_album_aqours_3);
                    getViewDataBinding().tvTitleBar.setText("Aqours");
                    getViewDataBinding().tvListMsgName1.setText("Aqours");
                    getViewDataBinding().tvListMsgName2.setText("LoveLive!Sunshine!!");
                } else if(ThreadEvent.ALBUM_US.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    getViewDataBinding().ivLogo.setBackgroundResource(R.mipmap.ic_album_us_3);
                    getViewDataBinding().tvTitleBar.setText("μ's");
                    getViewDataBinding().tvListMsgName1.setText("μ's");
                    getViewDataBinding().tvListMsgName2.setText("国立音ノ木坂学院");
                }
                break;
            case ThreadEvent.GET_COUNT_SUCCESS:
                getViewDataBinding().tvCount.setText(""+event.i);
                getViewDataBinding().tvMusicCount.setText(""+event.i);
                musicListSize = event.i;
                break;
            case ThreadEvent.GET_APP_VERSION_SUCCESS:
                isExistNewVersion = false;
                versionList.clear();
                versionList.addAll(event.tList);
                if(versionList.size()>0) {
                    int versionCode = Integer.parseInt(versionList.get(0).getVersionCode());
                    if(getAppVersionCode(this) < versionCode) {
                        isExistNewVersion = true;
                        String versionType = versionList.get(0).getVersionType();
                        if("1".equals(versionType)) {
                            getViewDataBinding().tvVersion.setVisibility(View.VISIBLE);
                        } else if ("2".equals(versionType)) {
                            getViewDataBinding().tvVersion.setVisibility(View.VISIBLE);
                            showUpgradeApp();
                        } else {
                            isExistNewVersion = false;
                        }
                    }
                }
                break;
            case ThreadEvent.GET_MESSAGE_SUCCESS:
                messageList.clear();
                messageList.addAll(event.tList);
                getViewDataBinding().tvMessageCount.setText(""+messageList.size());
                break;
            case ThreadEvent.GET_CURRENT_TIME:
                if(currentAllTime!=0 && currentAllTime > event.i) {
                    getViewDataBinding().tvStartTime.setText(event.str2);
                }
                break;
            case ThreadEvent.PLAY_FINISH_SUCCESS:
                if(playList.size()>0) {
                    //判断播放模式
                    switch (playMode) {
                        case 0: //顺序播放
                            Log.e("ABMediaPlay","0");
                            boolean isInto = false;
                            for (int i = 0; i < playList.size(); i++) {
                                if (playList.get(i).isPlaying) {
                                    if(i+1<playList.size()) {
                                        isInto = true;
                                        playList.get(i).isPlaying = false;
                                        //getViewModel().player(playList.get(i+1), false);
                                        //binder.player(playList.get(i+1), (playMode == 2));
                                        binder.showLyric(playList.get(i+1), (playMode == 2));
                                        playList.get(i+1).isPlaying = true;
                                        playMusicListAdapter.notifyDataSetChanged();
                                        break;
                                    }
                                }
                            }

                            if(!isInto){
                                setPlayListDefault(playList);
                                binder.showLyric(playList.get(0), (playMode == 2));
                                playList.get(0).isPlaying = true;
                                playMusicListAdapter.notifyDataSetChanged();
                            }
                            if(rThemeId!=0) {
                                if(rThemeId == R.id.ll_theme_normal) {
                                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_black_selected);
                                } else if(rThemeId == R.id.ll_theme_dark) {
                                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.ic_play_2_white);
                                } else if(rThemeId == R.id.ll_theme_white) {
                                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_purple_selected);
                                } else if(rThemeId == R.id.ll_theme_orange) {
                                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_orange_selected);
                                } else if(rThemeId == R.id.ll_theme_light) {
                                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_light_selected);
                                }

                            }

                            if (objectAnimator != null) {
                                objectAnimator.pause();
                            }
                            break;
                        case 1: //随机播放
                            Log.e("ABMediaPlay","1");
                            boolean isIntoRand = false;
                            for (int i = 0; i < playList.size(); i++) {
                                if(playList.get(i).isPlaying) {
                                    isIntoRand = true;
                                    playList.get(i).isPlaying = false;
                                    int rand = new Random().nextInt(playList.size());
                                    while(i == rand) {
                                        rand = new Random().nextInt(playList.size());
                                    }
                                    binder.showLyric(playList.get(rand), (playMode == 2));
                                    playList.get(rand).isPlaying = true;
                                    playMusicListAdapter.notifyDataSetChanged();
                                    break;
                                }
                            }

                            if(!isIntoRand) {
                                int rand = new Random().nextInt(playList.size());
                                binder.showLyric(playList.get(rand), (playMode == 2));
                                playList.get(rand).isPlaying = true;
                                playMusicListAdapter.notifyDataSetChanged();
                            }
                            break;
                        case 2: //单曲循环
                            Log.e("ABMediaPlay","2");
                            break;
                    }
                } else {
                    if(rThemeId!=0) {
                        if(rThemeId == R.id.ll_theme_normal) {
                            getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_black_selected);
                        } else if(rThemeId == R.id.ll_theme_dark) {
                            getViewDataBinding().btPlay.setBackgroundResource(R.drawable.ic_play_2_white);
                        } else if(rThemeId == R.id.ll_theme_white) {
                            getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_purple_selected);
                        } else if(rThemeId == R.id.ll_theme_orange) {
                            getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_orange_selected);
                        } else if(rThemeId == R.id.ll_theme_light) {
                            getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_light_selected);
                        }
                    }
                    if (objectAnimator != null) {
                        objectAnimator.pause();
                    }
                }
                break;
            case ThreadEvent.PLAY_ERROR:
                Toast.makeText(this, "播放失败", Toast.LENGTH_SHORT).show();
                getViewDataBinding().pbLoadingMusic.setVisibility(View.INVISIBLE);
                break;
            case ThreadEvent.PLAY_MUSIC:
                if(binder!=null) {
                    binder.pause(this, currentMusicName, currentMusicSinger, currentBitmap);
                }
                break;
            case ThreadEvent.PLAY_LOCAL_MUSIC:   //点击了本地歌曲并添加
                //binder.showLyric(event.music, event.b);
                if(playList.size()>0){
                    for(int i=0; i<playList.size(); i++) {
                        if(playList.get(i).isPlaying) {
                            playList.get(i).isPlaying = false;
                            binder.showLyric(event.music, (playMode == 2));
                            playList.add(i+1, setMusicMsg(event.music, true));
                            playMusicListAdapter.notifyDataSetChanged();

                            SPUtil.setListValue(context, "PlayListData", playList);
                            return;
                        }
                    }
                    binder.showLyric(event.music, (playMode == 2));
                    playList.add(playList.size(), setMusicMsg(event.music, true));
                } else {
                    binder.showLyric(event.music, (playMode == 2));
                    playList.add(setMusicMsg(event.music, true));
                }
                playMusicListAdapter.notifyDataSetChanged();
                SPUtil.setListValue(context, "PlayListData", playList);
                break;
            case ThreadEvent.ADD_LOCAL_MUSIC:   //添加了本地歌曲并添加
                playList.add(setMusicMsg(event.music, false));

                if(playList.size()==1) {
                    playList.get(0).isPlaying = true;
                    binder.showLyric(playList.get(0), (playMode == 2));
                } else {
                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_ADD_MUSIC));
                }
                playMusicListAdapter.notifyDataSetChanged();

                SPUtil.setListValue(context, "PlayListData", playList);

                break;
            case ThreadEvent.MUSIC_IS_PAUSE:
                if(binder!=null) {
                    binder.pause(this, event.str, event.str2, event.bitmap);
                }
                break;
            case ThreadEvent.MUSIC_IS_NEXT:
                lastOrNextMusic(true);
                break;
            case ThreadEvent.MUSIC_IS_LAST:
                lastOrNextMusic(false);
                break;
            case ThreadEvent.PLAY_LIST_FIRST:
                if(playList.size()>0) {
                    binder.showLyric(playList.get(0), (playMode == 2));
                    playList.get(0).isPlaying = true;
                    playMusicListAdapter.notifyDataSetChanged();
                }
                break;

            case ThreadEvent.VIEW_SEEK_BAR_POS:
                lyricScrollView.setMusicPlayerPos(event.i);
                getViewDataBinding().sbMusicBar.setProgress(event.i);
                getViewDataBinding().hpvProgress.setCurrentCount(event.i);

                break;
            case ThreadEvent.VIEW_SEEK_BAR_RESUME:
                lyricScrollView.posLock(true);
                getViewDataBinding().sbMusicBar.setProgress(0);
                getViewDataBinding().pbLoadingMusic.setVisibility(View.VISIBLE);
                sendWidgetBroadcastReceiver(true);
                break;

            case ThreadEvent.VIEW_PAUSE:
                isPlay = !event.b;
                lyricScrollView.posLock(event.b);
                //角色服务存在时 对角色服务做处理
                if(isWorked(this, CharacterService.class.getPackage().getName()
                        + "." + CharacterService.class.getSimpleName())) {
                    //根据播放或暂停 对角色状态变更
                    MainVM.stopHandler();
                    if(event.b) {
                        MainVM.initAnimatedCharacter(mCharacterName);
                    } else {
                        MainVM.animatedListenCharacter(mCharacterName);
                    }
                    //控制角色系统里面的播放按钮
                    CharacterService.isPlayMusic(!event.b);
                }

                if(rThemeId!=0) {
                    Log.e("LogByAB","rThemeId: " + rThemeId);
                    if(rThemeId == R.id.ll_theme_normal) {
                        getViewDataBinding().btPlay.setBackgroundResource(event.b ? R.drawable.selector_play_black_selected : R.drawable.selector_pause_black_selected);
                        getViewDataBinding().ivPanelPlay.setBackgroundResource(event.b ? R.drawable.selector_play_circle_black_selected : R.drawable.selector_pause_circle_black_selected);
                    } else if(rThemeId == R.id.ll_theme_dark) {
                        getViewDataBinding().btPlay.setBackgroundResource(event.b ? R.drawable.ic_play_2_white : R.drawable.ic_pause_2_white);
                        getViewDataBinding().ivPanelPlay.setBackgroundResource(event.b ? R.drawable.ic_play_circle_white : R.drawable.ic_pause_circle_white);
                    } else if(rThemeId == R.id.ll_theme_white) {
                        getViewDataBinding().btPlay.setBackgroundResource(event.b ? R.drawable.selector_play_purple_selected : R.drawable.selector_pause_purple_selected);
                        getViewDataBinding().ivPanelPlay.setBackgroundResource(event.b ? R.drawable.selector_play_circle_purple_selected : R.drawable.selector_pause_circle_purple_selected);
                    } else if(rThemeId == R.id.ll_theme_orange) {
                        getViewDataBinding().btPlay.setBackgroundResource(event.b ? R.drawable.selector_play_orange_selected : R.drawable.selector_pause_orange_selected);
                        getViewDataBinding().ivPanelPlay.setBackgroundResource(event.b ? R.drawable.selector_play_circle_orange_selected : R.drawable.selector_pause_circle_orange_selected);
                    } else if(rThemeId == R.id.ll_theme_light) {
                        getViewDataBinding().btPlay.setBackgroundResource(event.b ? R.drawable.selector_play_light_selected : R.drawable.selector_pause_light_selected);
                        getViewDataBinding().ivPanelPlay.setBackgroundResource(event.b ? R.drawable.selector_play_circle_light_selected : R.drawable.selector_pause_circle_light_selected);
                    } else {
                        getViewDataBinding().btPlay.setBackgroundResource(event.b ? R.drawable.selector_play_black_selected : R.drawable.selector_pause_black_selected);
                        getViewDataBinding().ivPanelPlay.setBackgroundResource(event.b ? R.drawable.selector_play_circle_black_selected : R.drawable.selector_pause_circle_black_selected);
                    }
                } else {
                    getViewDataBinding().btPlay.setBackgroundResource(event.b ? R.drawable.selector_play_black_selected : R.drawable.selector_pause_black_selected);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(event.b ? R.drawable.selector_play_circle_black_selected : R.drawable.selector_pause_circle_black_selected);
                }
                if (objectAnimator != null) {
                    if (!event.b) {
                        objectAnimator.resume();
                    } else {
                        objectAnimator.pause();
                    }
                }
                break;
            case ThreadEvent.VIEW_ADD_MUSIC:
            case ThreadEvent.VIEW_DELETE_MUSIC:
                getViewDataBinding().tvListSize.setText("("+ playList.size() + ")");
                break;
            case ThreadEvent.VIEW_MUSIC_MSG:
                lyricScrollView.initView();

                getViewDataBinding().hpvProgress.setMaxCount(event.i);

                getViewDataBinding().pbLoadingMusic.setVisibility(View.INVISIBLE);
                getViewDataBinding().sbMusicBar.setMax(event.i);
                getViewDataBinding().tvAllTime.setText(getViewModel().rebuildTime(event.i));
                String musicMsg = event.music.musicName;
                getViewDataBinding().tvMusicName.setText(musicMsg);
                getViewDataBinding().tvSingerName.setText(event.music.musicSinger);
                getViewDataBinding().tvListSize.setText("("+ playList.size() + ")");

                currentMusicImg = event.music.getMusicImg();

                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_normal) {

                        Glide.with(getApplication())
                                .setDefaultRequestOptions(requestOptions)
                                .load(event.music.isLocal?
                                                (null != event.music.musicImgByte?
                                                        BitmapFactory.decodeByteArray(event.music.musicImgByte, 0, event.music.musicImgByte.length) : R.drawable.ic_music_default) : event.music.getMusicImg()
                                )
                                .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.light_ea)))
                                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .into(getViewDataBinding().ivMusicImg);
                    } else if(rThemeId == R.id.ll_theme_dark) {

                        Glide.with(getApplication())
                                .setDefaultRequestOptions(requestOptions)
                                .load(event.music.isLocal?
                                        (null != event.music.musicImgByte?
                                                BitmapFactory.decodeByteArray(event.music.musicImgByte, 0, event.music.musicImgByte.length) : R.drawable.ic_music_default) : event.music.getMusicImg()
                                )
                                .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.white)))
                                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .into(getViewDataBinding().ivMusicImg);
                    } else if(rThemeId == R.id.ll_theme_white) {
                        Glide.with(getApplication())
                                .setDefaultRequestOptions(requestOptions)
                                .load(event.music.isLocal?
                                        (null != event.music.musicImgByte?
                                                BitmapFactory.decodeByteArray(event.music.musicImgByte, 0, event.music.musicImgByte.length) : R.drawable.ic_music_default) : event.music.getMusicImg()
                                )
                                .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.purple_light)))
                                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .into(getViewDataBinding().ivMusicImg);
                    } else if(rThemeId == R.id.ll_theme_orange) {
                        Glide.with(getApplication())
                                .setDefaultRequestOptions(requestOptions)
                                .load(event.music.isLocal?
                                        (null != event.music.musicImgByte?
                                                BitmapFactory.decodeByteArray(event.music.musicImgByte, 0, event.music.musicImgByte.length) : R.drawable.ic_music_default) : event.music.getMusicImg()
                                )
                                .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.orange_0b)))
                                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .into(getViewDataBinding().ivMusicImg);
                    } else if(rThemeId == R.id.ll_theme_light) {
                        Glide.with(getApplication())
                                .setDefaultRequestOptions(requestOptions)
                                .load(event.music.isLocal?
                                        (null != event.music.musicImgByte?
                                                BitmapFactory.decodeByteArray(event.music.musicImgByte, 0, event.music.musicImgByte.length) : R.drawable.ic_music_default) : event.music.getMusicImg()
                                )
                                .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.light_b5)))
                                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .into(getViewDataBinding().ivMusicImg);
                    } else {
                        Glide.with(getApplication())
                                .setDefaultRequestOptions(requestOptions)
                                .load(event.music.isLocal?
                                        (null != event.music.musicImgByte?
                                                BitmapFactory.decodeByteArray(event.music.musicImgByte, 0, event.music.musicImgByte.length) : R.drawable.ic_music_default) : event.music.getMusicImg()
                                )
                                .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.light_ea)))
                                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                .into(getViewDataBinding().ivMusicImg);
                    }
                } else {
                    Glide.with(getApplication())
                            .load(event.music.isLocal?
                                    (null != event.music.musicImgByte?
                                            BitmapFactory.decodeByteArray(event.music.musicImgByte, 0, event.music.musicImgByte.length) : R.drawable.ic_music_default) : event.music.getMusicImg()
                            )
                            .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.light_ea)))
                            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                            .into(getViewDataBinding().ivMusicImg);
                }

                if (objectAnimator != null) {
                    objectAnimator.cancel();
                }
                objectAnimator = ObjectAnimator.ofFloat(getViewDataBinding().ivMusicImg, "rotation", 0f, 360.0f);
                objectAnimator.setDuration(15000);
                objectAnimator.setInterpolator(new LinearInterpolator());//不停顿
                objectAnimator.setRepeatCount(-1);//设置动画重复次数
                objectAnimator.setRepeatMode(ValueAnimator.RESTART);//动画重复模式
                objectAnimator.start();

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if(event.music.isLocal) {
                        if(null != event.music.musicImgByte) {
                            Bitmap bitmap = BitmapFactory.decodeByteArray(event.music.musicImgByte, 0, event.music.musicImgByte.length);
                            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.SHOW_IMAGE_URL, event.music.musicName, event.music.musicSinger, event.music.musicImg, bitmap, true));
                        } else {
                            startMusicService(true, event.music.musicName, event.music.musicSinger, null);
                            sendWidgetBroadcastReceiver(false);
                        }
                    } else {
                        if(!event.music.musicImg.equals("")) {
                            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.SHOW_IMAGE_URL, event.music.musicName, event.music.musicSinger, event.music.musicImg, null, false));
                        } else {
                            startMusicService(true, event.music.musicName, event.music.musicSinger, null);
                            sendWidgetBroadcastReceiver(false);
                        }
                    }
                } else {
                    initNotificationHelper(event.music.musicName, event.music.musicSinger, event.music.musicImg);
                }
                break;
            case ThreadEvent.VIEW_IMAGE_URL:
                currentMusicName = event.str;
                currentMusicSinger = event.str2;
                currentBitmap = event.bitmap;

                NotificationHelper.getInstance().createRemoteViews(this, event.str, event.str2, event.bitmap, false);
                sendWidgetBroadcastReceiver(false);
                break;
            case ThreadEvent.VIEW_LYRIC:
                if(null != event.tList) {
                    //将歌词数据方法lyricScrollView显示
                    lyricScrollView.setMusicLyrics(event.tList);

                    musicLyricList.clear();
                    musicLyricList.addAll(event.tList);
                }
                if(binder !=null) {
                    binder.player(event.music, event.b, event.str, musicLyricList);
                }
                break;
            case ThreadEvent.DOWNLOAD_APP_START:
                showLoadingApp();
                break;
            case ThreadEvent.DOWNLOAD_APP_LOADING:
                if(null != downloadBinding) {
                    downloadBinding.tvValue.setText(""+event.i);
                }
                break;
            case ThreadEvent.DOWNLOAD_APP_SUCCESS:
                if(null != mAlertDialog) {
                    mAlertDialog.dismiss();
                }
                //是否完成下载app
                if(event.b) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    Log.e("LogByAB","file: " + event.file.toString());
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Uri contentUri = FileProvider.getUriForFile(getApplicationContext(),"com.banlap.llmusic.fileProvider", event.file);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setDataAndType(contentUri, "application/vnd.android.package-archive");//设置类型
                    } else {
                        intent.setDataAndType(Uri.parse("file://"+event.file.toString()), "application/vnd.android.package-archive");//设置类型
                    }
                    startActivity(intent);
                } else {
                    event.file.delete();
                }
                break;
            case ThreadEvent.DOWNLOAD_APP_ERROR:
                if(null != mAlertDialog) {
                    mAlertDialog.dismiss();
                }
                Toast.makeText(this, "app下载失败，请重新下载", Toast.LENGTH_SHORT).show();
                break;
            case ThreadEvent.BLUETOOTH_DISCONNECT:
                if(binder!=null) {
                    binder.pauseImm(this, currentMusicName, currentMusicSinger, currentBitmap);
                }
                break;
            case ThreadEvent.ACTION_MEDIA_BUTTON:
                if(null != event.kt) {
                    if(binder!=null) {
                        if(KeyEvent.KEYCODE_MEDIA_NEXT == event.kt.getKeyCode() && KeyEvent.ACTION_DOWN == event.kt.getKeyCode()) {
                            lastOrNextMusic(true);
                        } else if(KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.kt.getKeyCode() && KeyEvent.ACTION_DOWN == event.kt.getKeyCode()) {
                            lastOrNextMusic(false);
                        } else if(KeyEvent.KEYCODE_MEDIA_PLAY == event.kt.getKeyCode() && KeyEvent.ACTION_DOWN == event.kt.getKeyCode()) {
                            binder.playImm(this, currentMusicName, currentMusicSinger, currentBitmap);
                        } else if(KeyEvent.KEYCODE_MEDIA_PAUSE == event.kt.getKeyCode() && KeyEvent.ACTION_DOWN == event.kt.getKeyCode()) {
                            binder.pauseImm(this, currentMusicName, currentMusicSinger, currentBitmap);
                        }
                    }
                }
                break;

            case ThreadEvent.VIEW_NORMAL_STATUS_CHARACTER:
                if(MainVM.CHARACTER_NAME_KEKE_INT == event.i) {
                    CharacterHelper.showNormalStatusCharacter(CharacterHelper.CHARACTER_NAME_KEKE);
                } else if(MainVM.CHARACTER_NAME_KANON_INT == event.i) {
                    CharacterHelper.showNormalStatusCharacter(CharacterHelper.CHARACTER_NAME_KANON);
                }
                break;
            case ThreadEvent.VIEW_MOVE_STATUS_CHARACTER:
                if(MainVM.CHARACTER_NAME_KEKE_INT == event.i) {
                    CharacterHelper.showMoveStatusCharacter(CharacterHelper.CHARACTER_NAME_KEKE);
                } else if(MainVM.CHARACTER_NAME_KANON_INT == event.i) {
                    CharacterHelper.showMoveStatusCharacter(CharacterHelper.CHARACTER_NAME_KANON);
                }
                break;
            case ThreadEvent.VIEW_LISTEN_STATUS_CHARACTER_LEFT:
                if(MainVM.CHARACTER_NAME_KEKE_INT == event.i) {
                    CharacterHelper.showListenStatusCharacter(CharacterHelper.CHARACTER_NAME_KEKE,true);
                } else if(MainVM.CHARACTER_NAME_KANON_INT == event.i) {
                    CharacterHelper.showListenStatusCharacter(CharacterHelper.CHARACTER_NAME_KANON,true);
                }
                break;
            case ThreadEvent.VIEW_LISTEN_STATUS_CHARACTER_RIGHT:
                if(MainVM.CHARACTER_NAME_KEKE_INT == event.i) {
                    CharacterHelper.showListenStatusCharacter(CharacterHelper.CHARACTER_NAME_KEKE, false);
                } else if(MainVM.CHARACTER_NAME_KANON_INT == event.i) {
                    CharacterHelper.showListenStatusCharacter(CharacterHelper.CHARACTER_NAME_KANON, false);
                }
                break;

            case ThreadEvent.SCAN_LOCAL_FILE_SUCCESS:
                getViewDataBinding().rlShowLoading.setVisibility(View.GONE);
                if(null != dialogLocalFileBinding) {
                    dialogLocalFileBinding.prLocalMusicLoading.setVisibility(View.GONE);
                    dialogLocalFileBinding.llSelectFile.setClickable(true);
                    dialogLocalFileBinding.llScanFile.setClickable(true);
                }
                break;
            case ThreadEvent.SELECT_LOCAL_FILE_SUCCESS:
                if(null != dialogLocalFileBinding) {
                    dialogLocalFileBinding.prLocalMusicLoading.setVisibility(View.GONE);
                    dialogLocalFileBinding.llSelectFile.setClickable(true);
                    dialogLocalFileBinding.llScanFile.setClickable(true);
                }
                break;

        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void ThreadEvent(final ThreadEvent event) {
        switch (event.msgCode) {
            case ThreadEvent.CONNECT_MYSQL:
                MysqlHelper.connectDB();
                break;
            case ThreadEvent.DOWNLOAD_APP:
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                    String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
                    //验证是否许可权限
                    for (String str : permissions) {
                        if (checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
                            //申请权限
                            requestPermissions(permissions, REQUEST_CODE_DOWNLOAD_APP);
                            return;
                        }
                    }
                    getViewModel().downloadUrl(event.str);
                } else {
                    getViewModel().downloadUrl(event.str);
                }
                break;
            case ThreadEvent.GET_DATA_LIST:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicSql()));
                break;
            case ThreadEvent.GET_DATA_LIST_COUNT:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicCount()));
                break;
            case ThreadEvent.GET_DATA_APP_VERSION:
                EventBus.getDefault().post(new ThreadEvent<Version>(ThreadEvent.GET_APP_VERSION_SUCCESS, MysqlHelper.getInstance().findVersionSql(),""));
                break;
            case ThreadEvent.GET_DATA_LIST_MESSAGE:
                EventBus.getDefault().post(new ThreadEvent<Message>(ThreadEvent.GET_MESSAGE_SUCCESS, MysqlHelper.getInstance().findMessageSql(), ""));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_LIELLA:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_LIELLA));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql("Liella")));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount("Liella")));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_FOUR_YUU:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_FOUR_YUU));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql("Fo(u)rYuU")));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount("Fo(u)rYuU")));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_SUNNY_PASSION:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_SUNNY_PASSION));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql("SunnyPassion")));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount("SunnyPassion")));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_NIJIGASAKI:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_NIJIGASAKI));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql("Nijigasaki")));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount("Nijigasaki")));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_AQOURS:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_AQOURS));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql("Aqours")));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount("Aqours")));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_US:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_US));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql("μs")));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount("μs")));
                break;
            case ThreadEvent.GET_MUSIC_LYRIC:
                getViewModel().showLyric(event.music, event.b);
                break;
            case ThreadEvent.SCROLL_LYRIC:

                break;
            case ThreadEvent.SHOW_IMAGE_URL:  //设置状态栏显示对应图片
                if(event.b) {
                    getViewModel().showImageBitmap(event.str, event.str2, event.bitmap);
                } else {
                    getViewModel().showImageURL(event.str, event.str2, event.str3);
                }
                break;

        }
    }

    /** 点击按钮事件 */
    public class ButtonClickListener implements View.OnClickListener {
        @SuppressLint("SetTextI18n")
        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.rl_play_controller) {
                Log.e("ABMediaPlay", "clickController");
            } else if(v.getId() == R.id.rl_show_loading) {
                Log.e("ABMediaPlay", "showLoading");
            } else if (v.getId() == R.id.rl_disable_click) {
                Log.e("ABMediaPlay", "clickDisableView");
                hideAllMusicView();
            } else if (v.getId() == R.id.ll_all_play) {
                allPlayMusic();
            } else if (v.getId() == R.id.ll_search) {
                searchMusic();
            } else if (v.getId() == R.id.ll_sort) {
                showSortMenuDialog(v);
            } else if (v.getId() == R.id.ll_cancel) {
                searchCancel();
            } else if (v.getId() == R.id.ll_settings) {
                intoSettings();

            } else if(v.getId() == R.id.ll_back) {
                searchCancel();
                ObjectAnimator mainPanelChangeObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(MainActivity.this, true, true, getViewDataBinding().clMain);
                ObjectAnimator detailPanelChangeObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(MainActivity.this, false, false, getViewDataBinding().clAlbumDetail);
                mainPanelChangeObjectAnimator.start();
                detailPanelChangeObjectAnimator.start();
                isNotMain = false;


            }
        }
    }

    /** 获取版本code */
    public int getAppVersionCode(Context context) {
        int versionCode=0;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionCode = pi.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /** 发送广播给小组件 更新视图 */
    private void sendWidgetBroadcastReceiver(boolean isLoading) {
        Intent intent = new Intent("WIDGET_PROVIDER_REFRESH_MUSIC_MSG");
        intent.setPackage(getPackageName());
        intent.putExtra("IsLoading", isLoading);

        sendBroadcast(intent);
    }

    /** 显示弹窗更新App */
    private void showUpgradeApp(){

        DialogMessageBinding messageBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.dialog_message, null, false);

        messageBinding.tvTitle.setText(null != versionList.get(0).versionTitle? versionList.get(0).versionTitle : "");
        messageBinding.tvContent.setText(null != versionList.get(0).versionContent? versionList.get(0).versionContent : "");
        messageBinding.btSelectIconCancel.setText("以后再说");
        messageBinding.btSelectIconCommit.setText("立即体验");
        messageBinding.btSelectIconCommit.setTextColor(getResources().getColor(R.color.white));
        messageBinding.btSelectIconCommit.setBackgroundResource(R.drawable.selector_button_selected3);

        messageBinding.btSelectIconCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialog.dismiss();
            }
        });

        messageBinding.btSelectIconCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(null != versionList.get(0).versionUrl && !"".equals(versionList.get(0).versionUrl)) {
                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.DOWNLOAD_APP, versionList.get(0).versionUrl));
                }
            }
        });

        mAlertDialog = new AlertDialog.Builder(this)
                .setView(messageBinding.getRoot())
                .create();
        Objects.requireNonNull(mAlertDialog.getWindow()).setBackgroundDrawableResource(R.drawable.shape_button_white_2);
        mAlertDialog.show();
    }

    /** 显示弹窗正在下载App */
    private void showLoadingApp() {
        if(null != mAlertDialog) {
            mAlertDialog.dismiss();
        }

        downloadBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.dialog_download, null, false);

        downloadBinding.btSelectIconCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getViewModel().changeDownloadApp(true);
            }
        });

        mAlertDialog = new AlertDialog.Builder(this)
                .setView(downloadBinding.getRoot())
                .create();

        mAlertDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK) {
                    return true;
                }
                return false;
            }
        });

        mAlertDialog.setCanceledOnTouchOutside(false);
        Objects.requireNonNull(mAlertDialog.getWindow()).setBackgroundDrawableResource(R.drawable.shape_button_white_2);
        mAlertDialog.show();

    }

    /** 改变主题 */
    private void changeTheme(int rId) {
        if(rId == R.id.ll_theme_normal) {
            getViewDataBinding().rlPlayControllerIn.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().clCurrentMusicPanel.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().clCurrentMusicList.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().tvDiscover.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvLocal.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().vLine.setBackgroundResource(R.drawable.shape_button_white);
            getViewDataBinding().tvTitleBar.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvListMsgName1.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvPlayAll.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvCancel.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvListMsgName2.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvSingle.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvCount.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvMusicCount.setTextColor(getResources().getColor(R.color.light_ff));

            getViewDataBinding().tvMusicName.setTextColor(getResources().getColor(R.color.black));
            getViewDataBinding().tvSingerName.setTextColor(getResources().getColor(R.color.black));
            getViewDataBinding().tvPlayMode.setTextColor(getResources().getColor(R.color.black));
            getViewDataBinding().tvListSize.setTextColor(getResources().getColor(R.color.black));
            getViewDataBinding().tvStartTime.setTextColor(getResources().getColor(R.color.black));
            getViewDataBinding().tvAllTime.setTextColor(getResources().getColor(R.color.black));

            getViewDataBinding().clBg.setBackgroundResource(R.mipmap.ic_gradient_color5);
            //getViewDataBinding().rlPlayController.setBackgroundResource(R.drawable.shape_button_alpha_50);
            getViewDataBinding().btCurrentList.setBackgroundResource(R.drawable.selector_music_list_2_selected);

            getViewDataBinding().llAllPlay.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSearch.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSort.setBackgroundResource(R.drawable.selector_normal_selected);
            //getViewDataBinding().llSettings.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSearchBar.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llCancel.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llBack.setBackgroundResource(R.drawable.selector_normal_selected);

            getViewDataBinding().ivAllPlay.setBackgroundResource(R.drawable.ic_play_mini_light);
            getViewDataBinding().ivSearch.setBackgroundResource(R.drawable.ic_search_light);
            getViewDataBinding().ivSort.setBackgroundResource(R.drawable.ic_sort_light);
            //getViewDataBinding().ivSettings.setBackgroundResource(R.drawable.ic_settings_light);
            getViewDataBinding().ivDeleteAll.setBackgroundResource(R.drawable.ic_delete_black);
            getViewDataBinding().ivSearchMusic.setBackgroundResource(R.drawable.ic_search_light);
            getViewDataBinding().ivBack.setBackgroundResource(R.drawable.ic_arrow_back_light);
            getViewDataBinding().ivPanelLast.setBackgroundResource(R.drawable.selector_last_2_selected);
            getViewDataBinding().ivPanelNext.setBackgroundResource(R.drawable.selector_next_2_selected);
            getViewDataBinding().ivBgMode.setBackgroundResource(R.drawable.ic_bg_mode_black);

            getViewDataBinding().etSearchMusic.setHintTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().etSearchMusic.setTextColor(getResources().getColor(R.color.light_ff));

            getViewDataBinding().llMainMenuBt.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().ivMainMenuBt.setBackgroundResource(R.drawable.ic_menu);

            if(playMode == 0) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
            } else if (playMode == 1) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
            } else if (playMode == 2) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
            }

            if(binder!=null) {
                if (binder.isPlay()) {
                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_pause_black_selected);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_pause_circle_black_selected);
                } else {
                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_black_selected);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_play_circle_black_selected);
                }
            } else {
                getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_black_selected);
                getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_play_circle_black_selected);
            }
            if(currentMusicImg!=null) {
                if (!currentMusicImg.equals("")) {
                    Glide.with(getApplication())
                            .setDefaultRequestOptions(requestOptions)
                            .load(currentMusicImg)
                            .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.light_ea)))
                            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                            .into(getViewDataBinding().ivMusicImg);
                }
            }
            //解决seekBar滚动条变形问题
            Rect r = getViewDataBinding().sbMusicBar.getProgressDrawable().getBounds();
            getViewDataBinding().sbMusicBar.setThumb(getResources().getDrawable(R.drawable.shape_seek_bar_thumb));
            getViewDataBinding().sbMusicBar.setProgressDrawable(getResources().getDrawable(R.drawable.layer_seek_bar));
            getViewDataBinding().sbMusicBar.getProgressDrawable().setBounds(r);
            //getViewDataBinding().sbMusicBar.setProgressTintMode(PorterDuff.Mode.SRC_ATOP);
            //loading加载颜色
            getViewDataBinding().pbLoadingMusic.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.light_ea), PorterDuff.Mode.SRC_IN);
            getViewDataBinding().prLoading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.light_ea), PorterDuff.Mode.SRC_IN);
            getViewDataBinding().hpvProgress.setLinearGradient(R.color.light_ea);
        } else if(rId == R.id.ll_theme_dark) {
            getViewDataBinding().rlPlayControllerIn.setBackgroundResource(R.drawable.shape_button_black_2);
            getViewDataBinding().clCurrentMusicPanel.setBackgroundResource(R.drawable.shape_button_black_2);
            getViewDataBinding().clCurrentMusicList.setBackgroundResource(R.drawable.shape_button_black_2);
            getViewDataBinding().pbLoadingMusic.setProgressDrawable(getResources().getDrawable(R.color.gray_36));
            getViewDataBinding().tvDiscover.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvLocal.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().vLine.setBackgroundResource(R.drawable.shape_button_white);
            getViewDataBinding().tvTitleBar.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvListMsgName1.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvPlayAll.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvCancel.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvMusicCount.setTextColor(getResources().getColor(R.color.white));

            getViewDataBinding().tvListMsgName2.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvSingle.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvCount.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvMusicName.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvSingerName.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvPlayMode.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvListSize.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvStartTime.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvAllTime.setTextColor(getResources().getColor(R.color.white));

            getViewDataBinding().clBg.setBackgroundResource(R.mipmap.ic_gradient_color6);
            //getViewDataBinding().rlPlayController.setBackgroundResource(R.drawable.shape_button_orange_alpha_50);
            getViewDataBinding().btCurrentList.setBackgroundResource(R.drawable.ic_music_list);

            getViewDataBinding().llAllPlay.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSearch.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSort.setBackgroundResource(R.drawable.selector_normal_selected);
            //getViewDataBinding().llSettings.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSearchBar.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llCancel.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llBack.setBackgroundResource(R.drawable.selector_normal_selected);

            getViewDataBinding().ivAllPlay.setBackgroundResource(R.drawable.ic_play_mini_white);
            getViewDataBinding().ivSearch.setBackgroundResource(R.drawable.ic_search);
            getViewDataBinding().ivSort.setBackgroundResource(R.drawable.ic_sort);
            //getViewDataBinding().ivSettings.setBackgroundResource(R.drawable.ic_settings);
            getViewDataBinding().ivDeleteAll.setBackgroundResource(R.drawable.ic_delete);
            getViewDataBinding().ivSearchMusic.setBackgroundResource(R.drawable.ic_search);
            getViewDataBinding().ivBack.setBackgroundResource(R.drawable.ic_arrow_back);
            getViewDataBinding().ivPanelLast.setBackgroundResource(R.drawable.ic_last);
            getViewDataBinding().ivPanelNext.setBackgroundResource(R.drawable.ic_next);
            getViewDataBinding().ivBgMode.setBackgroundResource(R.drawable.ic_bg_mode);

            getViewDataBinding().etSearchMusic.setHintTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().etSearchMusic.setTextColor(getResources().getColor(R.color.white));

            getViewDataBinding().llMainMenuBt.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().ivMainMenuBt.setBackgroundResource(R.drawable.ic_menu);

            if(playMode == 0) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play);
            } else if (playMode == 1) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play);
            } else if (playMode == 2) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play);
            }

            if(binder!=null) {
                if (binder.isPlay()) {
                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.ic_pause_2_white);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.ic_pause_circle_white);
                } else {
                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.ic_play_2_white);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.ic_play_circle_white);
                }
            } else {
                getViewDataBinding().btPlay.setBackgroundResource(R.drawable.ic_play_2_white);
                getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.ic_play_circle_white);
            }
            if(currentMusicImg!=null) {
                if(!currentMusicImg.equals("")) {
                    Glide.with(getApplication())
                            .setDefaultRequestOptions(requestOptions)
                            .load(currentMusicImg)
                            .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.white)))
                            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                            .into(getViewDataBinding().ivMusicImg);
                }
            }
            //解决seekBar滚动条变形问题
            Rect r = getViewDataBinding().sbMusicBar.getProgressDrawable().getBounds();
            getViewDataBinding().sbMusicBar.setThumb(getResources().getDrawable(R.drawable.shape_seek_bar_thumb));
            getViewDataBinding().sbMusicBar.setProgressDrawable(getResources().getDrawable(R.drawable.layer_seek_bar_dark));
            getViewDataBinding().sbMusicBar.getProgressDrawable().setBounds(r);
            //getViewDataBinding().sbMusicBar.setProgressTintMode(PorterDuff.Mode.SRC_ATOP);
            //loading加载颜色
            getViewDataBinding().pbLoadingMusic.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.gray_36), PorterDuff.Mode.SRC_IN);
            getViewDataBinding().prLoading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.gray_36), PorterDuff.Mode.SRC_IN);
            getViewDataBinding().hpvProgress.setLinearGradient(R.color.white);
        } else if(rId == R.id.ll_theme_white) {
            getViewDataBinding().rlPlayControllerIn.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().clCurrentMusicPanel.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().clCurrentMusicList.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().tvDiscover.setTextColor(getResources().getColor(R.color.purple));
            getViewDataBinding().tvLocal.setTextColor(getResources().getColor(R.color.purple));
            getViewDataBinding().vLine.setBackgroundResource(R.drawable.shape_button_purple);
            getViewDataBinding().tvTitleBar.setTextColor(getResources().getColor(R.color.purple));
            getViewDataBinding().tvListMsgName1.setTextColor(getResources().getColor(R.color.purple));
            getViewDataBinding().tvMusicName.setTextColor(getResources().getColor(R.color.purple));
            getViewDataBinding().tvPlayAll.setTextColor(getResources().getColor(R.color.purple));
            getViewDataBinding().tvPlayMode.setTextColor(getResources().getColor(R.color.purple));
            getViewDataBinding().tvListSize.setTextColor(getResources().getColor(R.color.purple));
            getViewDataBinding().tvCancel.setTextColor(getResources().getColor(R.color.purple));

            getViewDataBinding().tvListMsgName2.setTextColor(getResources().getColor(R.color.gray_purple_ac));
            getViewDataBinding().tvSingle.setTextColor(getResources().getColor(R.color.gray_purple_ac));
            getViewDataBinding().tvCount.setTextColor(getResources().getColor(R.color.gray_purple_ac));
            getViewDataBinding().tvSingerName.setTextColor(getResources().getColor(R.color.gray_purple_ac));
            getViewDataBinding().tvStartTime.setTextColor(getResources().getColor(R.color.gray_purple_ac));
            getViewDataBinding().tvAllTime.setTextColor(getResources().getColor(R.color.gray_purple_ac));
            getViewDataBinding().tvMusicCount.setTextColor(getResources().getColor(R.color.gray_purple_ac));

            getViewDataBinding().clBg.setBackgroundResource(R.color.background_color_F2);
            //getViewDataBinding().rlPlayController.setBackgroundResource(R.drawable.shape_button_white_alpha_50);
            getViewDataBinding().btCurrentList.setBackgroundResource(R.drawable.ic_music_list_purple);

            getViewDataBinding().llAllPlay.setBackgroundResource(R.drawable.selector_white_theme_selected);
            getViewDataBinding().llSearch.setBackgroundResource(R.drawable.selector_white_theme_selected);
            getViewDataBinding().llSort.setBackgroundResource(R.drawable.selector_white_theme_selected);
            //getViewDataBinding().llSettings.setBackgroundResource(R.drawable.selector_white_theme_selected);
            getViewDataBinding().llSearchBar.setBackgroundResource(R.drawable.selector_white_theme_selected);
            getViewDataBinding().llCancel.setBackgroundResource(R.drawable.selector_white_theme_selected);
            getViewDataBinding().llBack.setBackgroundResource(R.drawable.selector_white_theme_selected);


            getViewDataBinding().ivAllPlay.setBackgroundResource(R.drawable.ic_play_mini_purple);
            getViewDataBinding().ivSearch.setBackgroundResource(R.drawable.ic_search_purple);
            getViewDataBinding().ivSort.setBackgroundResource(R.drawable.ic_sort_purple);
            //getViewDataBinding().ivSettings.setBackgroundResource(R.drawable.ic_settings_purple);
            getViewDataBinding().ivDeleteAll.setBackgroundResource(R.drawable.ic_delete_purple);
            getViewDataBinding().ivSearchMusic.setBackgroundResource(R.drawable.ic_search_purple);
            getViewDataBinding().ivBack.setBackgroundResource(R.drawable.ic_arrow_back_purple);
            getViewDataBinding().ivPanelLast.setBackgroundResource(R.drawable.ic_last_purple);
            getViewDataBinding().ivPanelNext.setBackgroundResource(R.drawable.ic_next_purple);
            getViewDataBinding().ivBgMode.setBackgroundResource(R.drawable.ic_bg_mode_purple);

            getViewDataBinding().etSearchMusic.setHintTextColor(getResources().getColor(R.color.gray_purple_ac));
            getViewDataBinding().etSearchMusic.setTextColor(getResources().getColor(R.color.purple));

            getViewDataBinding().llMainMenuBt.setBackgroundResource(R.drawable.shape_button_white_4);
            getViewDataBinding().ivMainMenuBt.setBackgroundResource(R.drawable.ic_menu_purple);

            if(playMode == 0) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_purple);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_purple);
            } else if (playMode == 1) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_purple);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_purple);
            } else if (playMode == 2) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_purple);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_purple);
            }

            if(binder!=null) {
                if (binder.isPlay()) {
                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_pause_purple_selected);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_pause_circle_purple_selected);
                } else {
                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_purple_selected);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_play_circle_purple_selected);
                }
            } else {
                getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_purple_selected);
                getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_play_circle_purple_selected);
            }
            if(currentMusicImg!=null) {
                if (!currentMusicImg.equals("")) {
                    Glide.with(getApplication())
                            .setDefaultRequestOptions(requestOptions)
                            .load(currentMusicImg)
                            .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.purple)))
                            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                            .into(getViewDataBinding().ivMusicImg);
                }
            }
            //解决seekBar滚动条变形问题
            Rect r = getViewDataBinding().sbMusicBar.getProgressDrawable().getBounds();
            getViewDataBinding().sbMusicBar.setThumb(getResources().getDrawable(R.drawable.shape_seek_bar_thumb));
            getViewDataBinding().sbMusicBar.setProgressDrawable(getResources().getDrawable(R.drawable.layer_seek_bar_purple));
            getViewDataBinding().sbMusicBar.getProgressDrawable().setBounds(r);
            //getViewDataBinding().sbMusicBar.setProgressTintMode(PorterDuff.Mode.SRC_ATOP);
            //loading加载颜色
            getViewDataBinding().pbLoadingMusic.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.purple), PorterDuff.Mode.SRC_IN);
            getViewDataBinding().prLoading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.purple), PorterDuff.Mode.SRC_IN);
            getViewDataBinding().hpvProgress.setLinearGradient(R.color.purple);
        } else if(rId == R.id.ll_theme_orange) {
            getViewDataBinding().rlPlayControllerIn.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().clCurrentMusicPanel.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().clCurrentMusicList.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().pbLoadingMusic.setProgressDrawable(getResources().getDrawable(R.color.orange_0b));
            getViewDataBinding().tvDiscover.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvLocal.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().vLine.setBackgroundResource(R.drawable.shape_button_orange);
            getViewDataBinding().tvTitleBar.setTextColor(getResources().getColor(R.color.orange_0b));

            getViewDataBinding().tvListMsgName1.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvPlayAll.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvCancel.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvMusicCount.setTextColor(getResources().getColor(R.color.orange_0b));

            getViewDataBinding().tvListMsgName2.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvSingle.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvCount.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvMusicName.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvSingerName.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvPlayMode.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvListSize.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvStartTime.setTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().tvAllTime.setTextColor(getResources().getColor(R.color.orange_0b));

            getViewDataBinding().clBg.setBackgroundResource(R.mipmap.ic_gradient_color7);
            //getViewDataBinding().rlPlayController.setBackgroundResource(R.drawable.shape_button_orange_alpha_50);
            getViewDataBinding().btCurrentList.setBackgroundResource(R.drawable.ic_music_list_orange);

            getViewDataBinding().llAllPlay.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSearch.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSort.setBackgroundResource(R.drawable.selector_normal_selected);
            //getViewDataBinding().llSettings.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSearchBar.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llCancel.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llBack.setBackgroundResource(R.drawable.selector_normal_selected);

            getViewDataBinding().ivAllPlay.setBackgroundResource(R.drawable.ic_play_mini_orange);
            getViewDataBinding().ivSearch.setBackgroundResource(R.drawable.ic_search_orange);
            getViewDataBinding().ivSort.setBackgroundResource(R.drawable.ic_sort_orange);
            //getViewDataBinding().ivSettings.setBackgroundResource(R.drawable.ic_settings_orange);
            getViewDataBinding().ivDeleteAll.setBackgroundResource(R.drawable.ic_delete_orange);
            getViewDataBinding().ivSearchMusic.setBackgroundResource(R.drawable.ic_search_orange);
            getViewDataBinding().ivBack.setBackgroundResource(R.drawable.ic_arrow_back_orange);
            getViewDataBinding().ivPanelLast.setBackgroundResource(R.drawable.selector_last_orange_selected);
            getViewDataBinding().ivPanelNext.setBackgroundResource(R.drawable.selector_next_orange_selected);
            getViewDataBinding().ivBgMode.setBackgroundResource(R.drawable.ic_bg_mode_orange);

            getViewDataBinding().etSearchMusic.setHintTextColor(getResources().getColor(R.color.orange_0b));
            getViewDataBinding().etSearchMusic.setTextColor(getResources().getColor(R.color.orange_0b));

            getViewDataBinding().llMainMenuBt.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().ivMainMenuBt.setBackgroundResource(R.drawable.ic_menu_orange);

            if(playMode == 0) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_orange);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_orange);
            } else if (playMode == 1) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_orange);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_orange);
            } else if (playMode == 2) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_orange);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_orange);
            }

            if(binder!=null) {
                if (binder.isPlay()) {
                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_pause_orange_selected);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_pause_circle_orange_selected);
                } else {
                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_orange_selected);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_play_circle_orange_selected);
                }
            } else {
                getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_orange_selected);
                getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_play_circle_orange_selected);
            }
            if(currentMusicImg!=null) {
                if(!currentMusicImg.equals("")) {
                    Glide.with(getApplication())
                            .setDefaultRequestOptions(requestOptions)
                            .load(currentMusicImg)
                            .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.orange_0b)))
                            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                            .into(getViewDataBinding().ivMusicImg);
                }
            }
            //解决seekBar滚动条变形问题
            Rect r = getViewDataBinding().sbMusicBar.getProgressDrawable().getBounds();
            getViewDataBinding().sbMusicBar.setThumb(getResources().getDrawable(R.drawable.shape_seek_bar_thumb));
            getViewDataBinding().sbMusicBar.setProgressDrawable(getResources().getDrawable(R.drawable.layer_seek_bar_orange));
            getViewDataBinding().sbMusicBar.getProgressDrawable().setBounds(r);
            //getViewDataBinding().sbMusicBar.setProgressTintMode(PorterDuff.Mode.SRC_ATOP);
            //loading加载颜色
            getViewDataBinding().pbLoadingMusic.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.orange_f4), PorterDuff.Mode.SRC_IN);
            getViewDataBinding().prLoading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.orange_f4), PorterDuff.Mode.SRC_IN);
            getViewDataBinding().hpvProgress.setLinearGradient(R.color.orange_0b);
        } else if(rId == R.id.ll_theme_light) {
            getViewDataBinding().rlPlayControllerIn.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().clCurrentMusicPanel.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().clCurrentMusicList.setBackgroundResource(R.drawable.shape_button_white_3);
            getViewDataBinding().tvDiscover.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().tvLocal.setTextColor(getResources().getColor(R.color.white));
            getViewDataBinding().vLine.setBackgroundResource(R.drawable.shape_button_white);
            getViewDataBinding().tvTitleBar.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvListMsgName1.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvPlayAll.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvCancel.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvListMsgName2.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvSingle.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvCount.setTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().tvMusicCount.setTextColor(getResources().getColor(R.color.light_ff));

            getViewDataBinding().tvPlayMode.setTextColor(getResources().getColor(R.color.light_b5));
            getViewDataBinding().tvListSize.setTextColor(getResources().getColor(R.color.light_b5));
            getViewDataBinding().tvMusicName.setTextColor(getResources().getColor(R.color.light_b5));
            getViewDataBinding().tvSingerName.setTextColor(getResources().getColor(R.color.light_b5));
            getViewDataBinding().tvStartTime.setTextColor(getResources().getColor(R.color.light_b5));
            getViewDataBinding().tvAllTime.setTextColor(getResources().getColor(R.color.light_b5));

            getViewDataBinding().clBg.setBackgroundResource(R.mipmap.ic_gradient_color4);
            //getViewDataBinding().rlPlayController.setBackgroundResource(R.drawable.shape_button_light_alpha_50);
            getViewDataBinding().btCurrentList.setBackgroundResource(R.drawable.ic_music_list_light);

            getViewDataBinding().llAllPlay.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSearch.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSort.setBackgroundResource(R.drawable.selector_normal_selected);
            //getViewDataBinding().llSettings.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llSearchBar.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llCancel.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().llBack.setBackgroundResource(R.drawable.selector_normal_selected);

            getViewDataBinding().ivAllPlay.setBackgroundResource(R.drawable.ic_play_mini_light);
            getViewDataBinding().ivSearch.setBackgroundResource(R.drawable.ic_search_light);
            getViewDataBinding().ivSort.setBackgroundResource(R.drawable.ic_sort_light);
            //getViewDataBinding().ivSettings.setBackgroundResource(R.drawable.ic_settings_light);
            getViewDataBinding().ivDeleteAll.setBackgroundResource(R.drawable.ic_delete_light);
            getViewDataBinding().ivSearchMusic.setBackgroundResource(R.drawable.ic_search_light);
            getViewDataBinding().ivBack.setBackgroundResource(R.drawable.ic_arrow_back_light);
            getViewDataBinding().ivPanelLast.setBackgroundResource(R.drawable.ic_last_purple_light);
            getViewDataBinding().ivPanelNext.setBackgroundResource(R.drawable.ic_next_light);
            getViewDataBinding().ivBgMode.setBackgroundResource(R.drawable.ic_bg_mode_light);
            getViewDataBinding().etSearchMusic.setHintTextColor(getResources().getColor(R.color.light_ff));
            getViewDataBinding().etSearchMusic.setTextColor(getResources().getColor(R.color.light_ff));

            getViewDataBinding().llMainMenuBt.setBackgroundResource(R.drawable.selector_normal_selected);
            getViewDataBinding().ivMainMenuBt.setBackgroundResource(R.drawable.ic_menu);

            if(playMode == 0) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_light);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_light);
            } else if (playMode == 1) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_light);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_light);
            } else if (playMode == 2) {
                getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_light);
                getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_light);
            }

            if(binder!=null) {
                if (binder.isPlay()) {
                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_pause_light_selected);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_pause_circle_light_selected);
                } else {
                    getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_light_selected);
                    getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_play_circle_light_selected);
                }
            } else {
                getViewDataBinding().btPlay.setBackgroundResource(R.drawable.selector_play_light_selected);
                getViewDataBinding().ivPanelPlay.setBackgroundResource(R.drawable.selector_play_circle_light_selected);
            }
            if(currentMusicImg!=null) {
                if (!currentMusicImg.equals("")) {
                    Glide.with(getApplication())
                            .setDefaultRequestOptions(requestOptions)
                            .load(currentMusicImg)
                            .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.light_b5)))
                            .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                            .into(getViewDataBinding().ivMusicImg);
                }
            }
            //解决seekBar滚动条变形问题
            Rect r = getViewDataBinding().sbMusicBar.getProgressDrawable().getBounds();
            getViewDataBinding().sbMusicBar.setThumb(getResources().getDrawable(R.drawable.shape_seek_bar_thumb));
            getViewDataBinding().sbMusicBar.setProgressDrawable(getResources().getDrawable(R.drawable.layer_seek_bar_light));
            getViewDataBinding().sbMusicBar.getProgressDrawable().setBounds(r);
            //getViewDataBinding().sbMusicBar.setProgressTintMode(PorterDuff.Mode.SRC_ATOP);
            //loading加载颜色
            getViewDataBinding().pbLoadingMusic.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.light_b5), PorterDuff.Mode.SRC_IN);
            getViewDataBinding().prLoading.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.light_b5), PorterDuff.Mode.SRC_IN);
            getViewDataBinding().hpvProgress.setLinearGradient(R.color.light_b5);
        }
        musicListAdapter.notifyDataSetChanged();
        playMusicListAdapter.notifyDataSetChanged();
    }

    /** 点击展示系统菜单 */
    public void showMainMenu(View view) {
        DialogMainMenuBinding mainMenuBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.dialog_main_menu, null, false);

        PopupWindow menuPopupWindow  = new PopupWindow(mainMenuBinding.getRoot(),
                PxUtil.getInstance().dp2px(110, this),  WindowManager.LayoutParams.WRAP_CONTENT, true);
        menuPopupWindow.setTouchable(true);

        if(rThemeId != 0) {
            if(rThemeId == R.id.ll_theme_normal) {
                menuPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_normal));
                mainMenuBinding.tvSettings.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvCharacter.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvBackground.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvLocalMusic.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                mainMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                mainMenuBinding.vLine3.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            } else if(rThemeId == R.id.ll_theme_dark) {
                menuPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu));
                mainMenuBinding.tvSettings.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvCharacter.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvBackground.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvLocalMusic.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                mainMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                mainMenuBinding.vLine3.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            } else if(rThemeId == R.id.ll_theme_white) {
                menuPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_white));
                mainMenuBinding.tvSettings.setTextColor(getResources().getColor(R.color.purple));
                mainMenuBinding.tvCharacter.setTextColor(getResources().getColor(R.color.purple));
                mainMenuBinding.tvBackground.setTextColor(getResources().getColor(R.color.purple));
                mainMenuBinding.tvLocalMusic.setTextColor(getResources().getColor(R.color.purple));
                mainMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.purple));
                mainMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.purple));
                mainMenuBinding.vLine3.setBackgroundColor(getResources().getColor(R.color.purple));
            } else if(rThemeId == R.id.ll_theme_orange) {
                menuPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_orange));
                mainMenuBinding.tvSettings.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvCharacter.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvBackground.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvLocalMusic.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                mainMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                mainMenuBinding.vLine3.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            } else if(rThemeId == R.id.ll_theme_light) {
                menuPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_light));
                mainMenuBinding.tvSettings.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvCharacter.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvBackground.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvLocalMusic.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                mainMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                mainMenuBinding.vLine3.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            } else {
                menuPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_normal));
                mainMenuBinding.tvSettings.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvCharacter.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvBackground.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.tvLocalMusic.setTextColor(getResources().getColor(R.color.white));
                mainMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                mainMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                mainMenuBinding.vLine3.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            }
        } else {
            menuPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_normal));
            mainMenuBinding.tvSettings.setTextColor(getResources().getColor(R.color.white));
            mainMenuBinding.tvCharacter.setTextColor(getResources().getColor(R.color.white));
            mainMenuBinding.tvBackground.setTextColor(getResources().getColor(R.color.white));
            mainMenuBinding.tvLocalMusic.setTextColor(getResources().getColor(R.color.white));
            mainMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            mainMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            mainMenuBinding.vLine3.setBackgroundColor(getResources().getColor(R.color.gray_c9));
        }
        menuPopupWindow.showAsDropDown(view,  PxUtil.getInstance().dp2px(-60, this),  PxUtil.getInstance().dp2px(10, this));

        mainMenuBinding.llSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPopupWindow.dismiss();
                intoSettings();
            }
        });

        mainMenuBinding.llCharacter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPopupWindow.dismiss();
                //展示角色菜单
                DialogCharacterMenuBinding characterMenuBinding = DataBindingUtil.inflate(LayoutInflater.from(v.getContext()),
                        R.layout.dialog_character_menu, null, false);

                characterMenuBinding.llCharacterByKeke.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showCharacterAuth(CharacterHelper.CHARACTER_NAME_KEKE);
                    }
                });

                characterMenuBinding.llCharacterByKanon.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        showCharacterAuth(CharacterHelper.CHARACTER_NAME_KANON);
                    }
                });

                PopupWindow characterPopupWindow  = new PopupWindow(characterMenuBinding.getRoot(),
                        PxUtil.getInstance().dp2px(110, v.getContext()),  WindowManager.LayoutParams.WRAP_CONTENT, true);
                characterPopupWindow.setTouchable(true);
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_normal) {
                        characterPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_normal));
                        characterMenuBinding.tvCharacterByKeke.setTextColor(getResources().getColor(R.color.white));
                        characterMenuBinding.tvCharacterByKanon.setTextColor(getResources().getColor(R.color.white));
                        characterMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                    } else if(rThemeId == R.id.ll_theme_dark) {
                        characterPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu));
                        characterMenuBinding.tvCharacterByKeke.setTextColor(getResources().getColor(R.color.white));
                        characterMenuBinding.tvCharacterByKanon.setTextColor(getResources().getColor(R.color.white));
                        characterMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                    } else if(rThemeId == R.id.ll_theme_white) {
                        characterPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_white));
                        characterMenuBinding.tvCharacterByKeke.setTextColor(getResources().getColor(R.color.purple));
                        characterMenuBinding.tvCharacterByKanon.setTextColor(getResources().getColor(R.color.purple));
                        characterMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.purple));
                    } else if(rThemeId == R.id.ll_theme_orange) {
                        characterPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_orange));
                        characterMenuBinding.tvCharacterByKeke.setTextColor(getResources().getColor(R.color.white));
                        characterMenuBinding.tvCharacterByKanon.setTextColor(getResources().getColor(R.color.white));
                        characterMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                    } else if(rThemeId == R.id.ll_theme_light) {
                        characterPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_light));
                        characterMenuBinding.tvCharacterByKeke.setTextColor(getResources().getColor(R.color.white));
                        characterMenuBinding.tvCharacterByKanon.setTextColor(getResources().getColor(R.color.white));
                        characterMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                    } else {
                        characterPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_normal));
                        characterMenuBinding.tvCharacterByKeke.setTextColor(getResources().getColor(R.color.white));
                        characterMenuBinding.tvCharacterByKanon.setTextColor(getResources().getColor(R.color.white));
                        characterMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                    }
                } else {
                    characterPopupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_normal));
                    characterMenuBinding.tvCharacterByKeke.setTextColor(getResources().getColor(R.color.white));
                    characterMenuBinding.tvCharacterByKanon.setTextColor(getResources().getColor(R.color.white));
                    characterMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                }
                characterPopupWindow.showAsDropDown(view,  PxUtil.getInstance().dp2px(-60, v.getContext()),  PxUtil.getInstance().dp2px(10, v.getContext()));
            }
        });

        mainMenuBinding.llBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPopupWindow.dismiss();
                Intent intentPhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                // 获得当前手机版本
                if(Build.VERSION.SDK_INT<=19) {
                    intentPhoto.setAction(Intent.ACTION_PICK);
                    intentPhoto.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                } else{

                    intentPhoto.setAction(Intent.ACTION_OPEN_DOCUMENT);
                    intentPhoto.addCategory(Intent.CATEGORY_OPENABLE);
                    intentPhoto.setType("image/*");
                }
                intentTakePhotoLauncher.launch(intentPhoto);
            }
        });

        mainMenuBinding.llLocalMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPopupWindow.dismiss();
                if(null != dialogLocalFileBinding) {
                    dialogLocalFileBinding =null;
                }
                dialogLocalFileBinding = DataBindingUtil.inflate(LayoutInflater.from(context),
                        R.layout.dialog_local_file, null, false);

                dialogLocalFileBinding.llSelectFile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialogLocalFileBinding.llSelectFile.setClickable(false);
                        dialogLocalFileBinding.llScanFile.setClickable(false);
                        EventBus.getDefault().post(new ThreadEvent(ThreadEvent.SCAN_LOCAL_FILE_BY_CHECK_PERMISSION,  "select"));
                        mAlertDialog.dismiss();
                    }
                });

                dialogLocalFileBinding.llScanFile.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAlertDialog.dismiss();
                        getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);

                        dialogLocalFileBinding.prLocalMusicLoading.setVisibility(View.VISIBLE);
                        dialogLocalFileBinding.llSelectFile.setClickable(false);
                        dialogLocalFileBinding.llScanFile.setClickable(false);
                        EventBus.getDefault().post(new ThreadEvent(ThreadEvent.SCAN_LOCAL_FILE_BY_CHECK_PERMISSION,  "scan"));
                    }
                });

                mAlertDialog = new AlertDialog.Builder(context)
                        .setView(dialogLocalFileBinding.getRoot())
                        .create();
                Objects.requireNonNull(mAlertDialog.getWindow()).setBackgroundDrawableResource(R.drawable.shape_button_white_2);
                mAlertDialog.show();

            }
        });

    }

    /** 点击展示授权悬浮窗显示角色系统 */
    public void showCharacterAuth(final String characterName) {
        //判断是否已经授权显示悬浮窗
        if(isCanDrawOverlays()) {
             if(isWorked(this, CharacterService.class.getPackage().getName()
                        + "." + CharacterService.class.getSimpleName())) {

                 stopService(intentCharacterService);
                 MainVM.stopHandler();           //关闭角色并停止handler
                 MainVM.stopTalkHandler();
                 getViewDataBinding().ivCharacterStatus.setVisibility(View.GONE);

                 if(!mCharacterName.equals(characterName)) {
                     showCharacterAuth(characterName);
                 }
             } else {
                 mCharacterName = characterName;
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                     startCharacterService(mCharacterName);
                 } else {
                     intentCharacterService.putExtra("CharacterName", mCharacterName);
                     startService(intentCharacterService);
                 }
                 getViewDataBinding().ivCharacterStatus.setVisibility(View.VISIBLE);
             }
        } else {
            DialogDeleteListAllBinding deleteListAllBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                    R.layout.dialog_delete_list_all, null, false);

            deleteListAllBinding.dialogSelectTitle.setText("开启悬浮窗权限以展示角色系统");

            //取消
            deleteListAllBinding.btSelectIconCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAlertDialog.dismiss();
                }
            });

            //授权开启
            deleteListAllBinding.btSelectIconCommit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    }
                    mCharacterName = characterName;
                    intentActivityResultLauncher.launch(intent);
                }
            });

            mAlertDialog = new AlertDialog.Builder(this)
                    .setView(deleteListAllBinding.getRoot())
                    .create();
            Objects.requireNonNull(mAlertDialog.getWindow()).setBackgroundDrawableResource(R.drawable.shape_button_white_2);
            mAlertDialog.show();
        }

    }

    /** 点击当前音乐img */
    public void musicImgClick(View view) {
        //Toast.makeText(this, "click img", Toast.LENGTH_SHORT).show();
        showOrHideMusicPlayerPanel();
    }

    /** 壁纸模式 */
    public void backgroundModeClick(View view) {
        if(View.VISIBLE == getViewDataBinding().clBgMode.getVisibility()) {
            getViewDataBinding().clMain.setVisibility(View.VISIBLE);
            getViewDataBinding().clAlbumDetail.setVisibility(View.VISIBLE);
            getViewDataBinding().clBgMode.setVisibility(View.GONE);
        } else {
            getViewDataBinding().clMain.setVisibility(View.GONE);
            getViewDataBinding().clAlbumDetail.setVisibility(View.GONE);
            getViewDataBinding().clBgMode.setVisibility(View.VISIBLE);
        }
    }

    /** 点击播放按钮 */
    public void playButtonClick(View view)  {
        //getViewModel().pause();
        binder.pause(this, currentMusicName, currentMusicSinger, currentBitmap);
    }

    /** 点击切换播放模式按钮 */
    public void changePlayModeButtonClick(View view)  {
        playModeButtonClick(view);
    }

    /** 点击当前列表按钮 */
    public void currentListButtonClick(View view)  {
        showOrHideMusicPlayerList();
    }

    /** 播放上一首 */
    public void lastMusicButtonClick(View view) {
        lastOrNextMusic(false);
    }

    /** 播放下一首 */
    public void nextMusicButtonClick(View view) {
        lastOrNextMusic(true);
    }

    /** 播放上一首或下一首歌曲 */
    public void lastOrNextMusic(boolean isNext) {
        if(playList.size()>0) {
            if(playList.size() == 1) {
                binder.showLyric(playList.get(0), (playMode == 2));
            } else {
                for(int i=0; i<playList.size(); i++) {
                    if(playList.get(i).isPlaying) {
                        //如果当前模式为随机模式，点击播放下一首为随机歌曲
                        if(playMode ==1) {
                            playList.get(i).isPlaying = false;
                            int rand = new Random().nextInt(playList.size());
                            while (i == rand) {
                                rand = new Random().nextInt(playList.size());
                            }
                            binder.showLyric(playList.get(rand), false);
                            playList.get(rand).isPlaying = true;
                            playMusicListAdapter.notifyDataSetChanged();
                        } else if(playMode == 2) {
                            binder.showLyric(playList.get(i), true);
                        } else {
                            if (isNext) {
                                if (i + 1 < playList.size()) {
                                    playList.get(i).isPlaying = false;
                                    //getViewModel().player(playList.get(i + 1), (playMode == 2));
                                    //binder.player(playList.get(i+1), (playMode == 2));
                                    binder.showLyric(playList.get(i + 1), false);
                                    playList.get(i + 1).isPlaying = true;
                                    playMusicListAdapter.notifyDataSetChanged();
                                } else {
                                    playList.get(i).isPlaying = false;
                                    //getViewModel().player(playList.get(0), (playMode == 2));
                                    //binder.player(playList.get(0), (playMode == 2));
                                    binder.showLyric(playList.get(0), false);
                                    playList.get(0).isPlaying = true;
                                }
                            } else {
                                if (i == 0) {
                                    playList.get(0).isPlaying = false;
                                    binder.showLyric(playList.get(playList.size() - 1), false);
                                    playList.get(playList.size() - 1).isPlaying = true;
                                } else {
                                    playList.get(i).isPlaying = false;
                                    binder.showLyric(playList.get(i - 1), false);
                                    playList.get(i - 1).isPlaying = true;
                                }
                            }
                        }
                        playMusicListAdapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
        }
    }

    /** 显示当前播放列表 */
    public void showOrHideMusicPlayerList() {
        //Log.e("CLICK:", "isClick: " +isClick + " isShowMusicPanel: " + isShowMusicPanel + " isShowMusicList: " + isShowMusicList);
        //clCurrentAllPanel隐藏会首次没有高度，需给固定值
        int moveAxis = getViewDataBinding().clCurrentAllPanel.getHeight() == 0 ? panelMoveAxis : getViewDataBinding().clCurrentAllPanel.getHeight();

        if(isClick){
            if(isShowMusicList&&!isShowMusicPanel){
                ObjectAnimator allPanelObjectAnimator = MyAnimationUtil.objectAnimatorUpOrDown(this, true, moveAxis, getViewDataBinding().rlPlayController);
                allPanelObjectAnimator.start();

                getViewDataBinding().btPlay.setVisibility(View.VISIBLE);
                getViewDataBinding().btChangePlayMode.setVisibility(View.INVISIBLE);
                getViewDataBinding().rlDisableClick.setVisibility(isClick ? View.GONE : View.VISIBLE);
                isClick = !isClick;
            } else {
                isShowMusicPanel=!isShowMusicPanel;

                ObjectAnimator musicPanelObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(this, true, false, getViewDataBinding().clCurrentMusicPanel);
                ObjectAnimator musicListObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(this, false, true, getViewDataBinding().clCurrentMusicList);
                musicPanelObjectAnimator.start();
                musicListObjectAnimator.start();

                getViewDataBinding().clCurrentMusicPanel.setVisibility(View.VISIBLE);
                getViewDataBinding().clCurrentMusicList.setVisibility(View.VISIBLE);
                getViewDataBinding().btPlay.setVisibility(View.VISIBLE);
                getViewDataBinding().btChangePlayMode.setVisibility(View.INVISIBLE);
            }
        } else {
            ObjectAnimator allPanelObjectAnimator = MyAnimationUtil.objectAnimatorUpOrDown(this, false, moveAxis, getViewDataBinding().rlPlayController);
            allPanelObjectAnimator.start();
            ObjectAnimator musicListObjectAnimator = MyAnimationUtil.objectAnimatorInit(this, getViewDataBinding().clCurrentMusicList);
            musicListObjectAnimator.start();

            getViewDataBinding().clCurrentAllPanel.setVisibility(View.VISIBLE);
            getViewDataBinding().btPlay.setVisibility(View.VISIBLE);
            getViewDataBinding().btChangePlayMode.setVisibility(View.INVISIBLE);
            getViewDataBinding().clCurrentMusicList.setVisibility(isClick ? View.GONE : View.VISIBLE);
            getViewDataBinding().clCurrentMusicPanel.setVisibility(isClick ? View.VISIBLE: View.GONE);
            getViewDataBinding().rlDisableClick.setVisibility(isClick ? View.GONE : View.VISIBLE);
            isClick = !isClick;
        }
        isShowMusicList = !isShowMusicList;


    }

    /** 显示当前播放面板 */
    public void showOrHideMusicPlayerPanel() {
        //Log.e("CLICK:", "isClick: " +isClick + " isShowMusicPanel: " + isShowMusicPanel + " isShowMusicList: " + isShowMusicList);
        //clCurrentAllPanel隐藏会首次没有高度，需给固定值
        int moveAxis = getViewDataBinding().clCurrentAllPanel.getHeight() == 0 ? panelMoveAxis : getViewDataBinding().clCurrentAllPanel.getHeight();

        if(isClick) {
            if(isShowMusicPanel&&!isShowMusicList) {
                ObjectAnimator allPanelObjectAnimator = MyAnimationUtil.objectAnimatorUpOrDown(this, true, moveAxis, getViewDataBinding().rlPlayController);
                allPanelObjectAnimator.start();

                getViewDataBinding().btPlay.setVisibility(View.INVISIBLE);
                getViewDataBinding().btChangePlayMode.setVisibility(View.VISIBLE);
                getViewDataBinding().rlDisableClick.setVisibility(isClick ? View.GONE : View.VISIBLE);
                isClick = !isClick;
            } else {
                isShowMusicList = !isShowMusicList;
                ObjectAnimator musicPanelObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(this, true, true, getViewDataBinding().clCurrentMusicPanel);
                ObjectAnimator musicListObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(this, false, false, getViewDataBinding().clCurrentMusicList);
                musicPanelObjectAnimator.start();
                musicListObjectAnimator.start();

                getViewDataBinding().clCurrentMusicPanel.setVisibility(View.VISIBLE);
                getViewDataBinding().clCurrentMusicList.setVisibility(View.VISIBLE);
                getViewDataBinding().btPlay.setVisibility(View.INVISIBLE);
                getViewDataBinding().btChangePlayMode.setVisibility(View.VISIBLE);
            }
        } else {
            ObjectAnimator allPanelObjectAnimator = MyAnimationUtil.objectAnimatorUpOrDown(this, false, moveAxis, getViewDataBinding().rlPlayController);
            allPanelObjectAnimator.start();
            ObjectAnimator musicPanelObjectAnimator = MyAnimationUtil.objectAnimatorInit(this, getViewDataBinding().clCurrentMusicPanel);
            musicPanelObjectAnimator.start();

            getViewDataBinding().clCurrentAllPanel.setVisibility(View.VISIBLE);
            getViewDataBinding().btPlay.setVisibility(View.INVISIBLE);
            getViewDataBinding().btChangePlayMode.setVisibility(View.VISIBLE);
            getViewDataBinding().clCurrentMusicList.setVisibility(isClick ? View.VISIBLE: View.GONE);
            getViewDataBinding().clCurrentMusicPanel.setVisibility(isClick ? View.GONE : View.VISIBLE);
            getViewDataBinding().rlDisableClick.setVisibility(isClick ? View.GONE : View.VISIBLE);
            isClick = !isClick;
        }
        isShowMusicPanel = !isShowMusicPanel;


    }

    /** 隐藏所有播放View */
    public void hideAllMusicView() {
        int moveAxis = getViewDataBinding().clCurrentAllPanel.getHeight();
        ObjectAnimator allPanelObjectAnimator = MyAnimationUtil.objectAnimatorUpOrDown(this, true, moveAxis, getViewDataBinding().rlPlayController);
        allPanelObjectAnimator.start();

        getViewDataBinding().btPlay.setVisibility(View.VISIBLE);
        getViewDataBinding().btChangePlayMode.setVisibility(View.INVISIBLE);
        getViewDataBinding().rlDisableClick.setVisibility(isClick ? View.GONE : View.VISIBLE);
        isClick = false;
        isShowMusicPanel = false;
        isShowMusicList = false;
    }

    /** 点击播放全部歌曲 */
    public void allPlayMusic() {
        if(musicListSize!=0) {
            playList.clear();
            List<Music> list = new ArrayList<>();
            for(Music music: musicList) {
                if(!music.getMusicType().equals(" ")) {
                    list.add(music);
                }
            }
            playList.addAll(list);
            playMusicListAdapter.notifyDataSetChanged();
            //保存当前列表数据
            SPUtil.setListValue(this, "PlayListData", playList);
            //播放当前第一首音乐
            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.PLAY_LIST_FIRST));
            getViewDataBinding().tvListSize.setText("("+ playList.size() + ")");
        }
    }

    /** 点击查找歌曲按钮 */
    public void searchMusic() {
        tempMusicList = new ArrayList<>();
        tempMusicList.addAll(musicList);
        getViewDataBinding().llShowNormalBar.setVisibility(View.GONE);
        getViewDataBinding().llShowSearchBar.setVisibility(View.VISIBLE);
    }

    /** 点击排序按钮 */
    public void showSortMenuDialog(View view) {
        final DialogSortMenuBinding sortMenuBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.dialog_sort_menu, null, false);

        // 0:按时间   1:按名称   2:按歌手
        if(0 == clickSortType) {
            sortMenuBinding.ivSortByTimeType.setVisibility(View.VISIBLE);
            sortMenuBinding.ivSortByNameType.setVisibility(View.INVISIBLE);
            sortMenuBinding.ivSortBySingerType.setVisibility(View.INVISIBLE);
        } else if(1 == clickSortType) {
            sortMenuBinding.ivSortByTimeType.setVisibility(View.INVISIBLE);
            sortMenuBinding.ivSortByNameType.setVisibility(View.VISIBLE);
            sortMenuBinding.ivSortBySingerType.setVisibility(View.INVISIBLE);
        } else {
            sortMenuBinding.ivSortByTimeType.setVisibility(View.INVISIBLE);
            sortMenuBinding.ivSortByNameType.setVisibility(View.INVISIBLE);
            sortMenuBinding.ivSortBySingerType.setVisibility(View.VISIBLE);
        }

        if(rThemeId!=0) {
            if(rThemeId == R.id.ll_theme_white) {
                sortMenuBinding.ivSortByTimeType.setBackgroundResource(isUpSortByTime? R.drawable.ic_sort_up_purple : R.drawable.ic_sort_down_purple);
                sortMenuBinding.ivSortByNameType.setBackgroundResource(isUpSortByName? R.drawable.ic_sort_up_purple : R.drawable.ic_sort_down_purple);
                sortMenuBinding.ivSortBySingerType.setBackgroundResource(isUpSortBySinger? R.drawable.ic_sort_up_purple : R.drawable.ic_sort_down_purple);
            } else {
                sortMenuBinding.ivSortByTimeType.setBackgroundResource(isUpSortByTime? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
                sortMenuBinding.ivSortByNameType.setBackgroundResource(isUpSortByName? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
                sortMenuBinding.ivSortBySingerType.setBackgroundResource(isUpSortBySinger? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
            }
        } else {
            sortMenuBinding.ivSortByTimeType.setBackgroundResource(isUpSortByTime? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
            sortMenuBinding.ivSortByNameType.setBackgroundResource(isUpSortByName? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
            sortMenuBinding.ivSortBySingerType.setBackgroundResource(isUpSortBySinger? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
        }


        /* 按时间排序 */
        sortMenuBinding.llSortByTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isUpSortByTime = !isUpSortByTime;
                sortList(0);
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_white) {
                        sortMenuBinding.ivSortByTimeType.setBackgroundResource(isUpSortByTime? R.drawable.ic_sort_up_purple : R.drawable.ic_sort_down_purple);
                    } else {
                        sortMenuBinding.ivSortByTimeType.setBackgroundResource(isUpSortByTime? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
                    }
                } else {
                    sortMenuBinding.ivSortByTimeType.setBackgroundResource(isUpSortByTime? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
                }
                sortMenuBinding.ivSortByTimeType.setVisibility(View.VISIBLE);
                sortMenuBinding.ivSortByNameType.setVisibility(View.INVISIBLE);
                sortMenuBinding.ivSortBySingerType.setVisibility(View.INVISIBLE);
                clickSortType = 0;
            }
        });

        /* 按名称排序 */
        sortMenuBinding.llSortByName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isUpSortByName = !isUpSortByName;
                sortList(1);
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_white) {
                        sortMenuBinding.ivSortByNameType.setBackgroundResource(isUpSortByName? R.drawable.ic_sort_up_purple : R.drawable.ic_sort_down_purple);
                    } else {
                        sortMenuBinding.ivSortByNameType.setBackgroundResource(isUpSortByName? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
                    }
                } else {
                    sortMenuBinding.ivSortByNameType.setBackgroundResource(isUpSortByName? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
                }
                sortMenuBinding.ivSortByTimeType.setVisibility(View.INVISIBLE);
                sortMenuBinding.ivSortByNameType.setVisibility(View.VISIBLE);
                sortMenuBinding.ivSortBySingerType.setVisibility(View.INVISIBLE);
                clickSortType = 1;
            }
        });



        /* 按歌手名排序 */
        sortMenuBinding.llSortBySinger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isUpSortBySinger = !isUpSortBySinger;
                sortList(2);
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_white) {
                        sortMenuBinding.ivSortBySingerType.setBackgroundResource(isUpSortBySinger? R.drawable.ic_sort_up_purple : R.drawable.ic_sort_down_purple);
                    } else {
                        sortMenuBinding.ivSortBySingerType.setBackgroundResource(isUpSortBySinger? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
                    }
                } else {
                    sortMenuBinding.ivSortBySingerType.setBackgroundResource(isUpSortBySinger? R.drawable.ic_sort_up : R.drawable.ic_sort_down);
                }
                sortMenuBinding.ivSortByTimeType.setVisibility(View.INVISIBLE);
                sortMenuBinding.ivSortByNameType.setVisibility(View.INVISIBLE);
                sortMenuBinding.ivSortBySingerType.setVisibility(View.VISIBLE);
                clickSortType = 2;
            }
        });

        PopupWindow popupWindow  = new PopupWindow(sortMenuBinding.getRoot(),
                PxUtil.getInstance().dp2px(150, this),  WindowManager.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setTouchable(true);
        if(rThemeId!=0) {
            if(rThemeId == R.id.ll_theme_normal) {
                popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_normal));
                sortMenuBinding.tvSortByTime.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.tvSortByName.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.tvSortBySinger.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.ivSortByTime.setBackgroundResource(R.drawable.ic_sort_by_time);
                sortMenuBinding.ivSortByName.setBackgroundResource(R.drawable.ic_sort_by_name);
                sortMenuBinding.ivSortBySinger.setBackgroundResource(R.drawable.ic_sort_by_singer);
                sortMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                sortMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            } else if(rThemeId == R.id.ll_theme_dark) {
                popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu));
                sortMenuBinding.tvSortByTime.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.tvSortByName.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.tvSortBySinger.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.ivSortByTime.setBackgroundResource(R.drawable.ic_sort_by_time);
                sortMenuBinding.ivSortByName.setBackgroundResource(R.drawable.ic_sort_by_name);
                sortMenuBinding.ivSortBySinger.setBackgroundResource(R.drawable.ic_sort_by_singer);
                sortMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                sortMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            } else if(rThemeId == R.id.ll_theme_white) {
                popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_white));
                sortMenuBinding.tvSortByTime.setTextColor(getResources().getColor(R.color.purple));
                sortMenuBinding.tvSortByName.setTextColor(getResources().getColor(R.color.purple));
                sortMenuBinding.tvSortBySinger.setTextColor(getResources().getColor(R.color.purple));
                sortMenuBinding.ivSortByTime.setBackgroundResource(R.drawable.ic_sort_by_time_purple);
                sortMenuBinding.ivSortByName.setBackgroundResource(R.drawable.ic_sort_by_name_purple);
                sortMenuBinding.ivSortBySinger.setBackgroundResource(R.drawable.ic_sort_by_singer_purple);
                sortMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.purple));
                sortMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.purple));
            } else if(rThemeId == R.id.ll_theme_orange) {
                popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_orange));
                sortMenuBinding.tvSortByTime.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.tvSortByName.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.tvSortBySinger.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.ivSortByTime.setBackgroundResource(R.drawable.ic_sort_by_time);
                sortMenuBinding.ivSortByName.setBackgroundResource(R.drawable.ic_sort_by_name);
                sortMenuBinding.ivSortBySinger.setBackgroundResource(R.drawable.ic_sort_by_singer);
                sortMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                sortMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            } else if(rThemeId == R.id.ll_theme_light) {
                popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_light));
                sortMenuBinding.tvSortByTime.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.tvSortByName.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.tvSortBySinger.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.ivSortByTime.setBackgroundResource(R.drawable.ic_sort_by_time);
                sortMenuBinding.ivSortByName.setBackgroundResource(R.drawable.ic_sort_by_name);
                sortMenuBinding.ivSortBySinger.setBackgroundResource(R.drawable.ic_sort_by_singer);
                sortMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                sortMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            } else {
                popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_normal));
                sortMenuBinding.tvSortByTime.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.tvSortByName.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.tvSortBySinger.setTextColor(getResources().getColor(R.color.white));
                sortMenuBinding.ivSortByTime.setBackgroundResource(R.drawable.ic_sort_by_time);
                sortMenuBinding.ivSortByName.setBackgroundResource(R.drawable.ic_sort_by_name);
                sortMenuBinding.ivSortBySinger.setBackgroundResource(R.drawable.ic_sort_by_singer);
                sortMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
                sortMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            }
        } else {
            popupWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_menu_normal));
            sortMenuBinding.tvSortByTime.setTextColor(getResources().getColor(R.color.white));
            sortMenuBinding.tvSortByName.setTextColor(getResources().getColor(R.color.white));
            sortMenuBinding.tvSortBySinger.setTextColor(getResources().getColor(R.color.white));
            sortMenuBinding.ivSortByTime.setBackgroundResource(R.drawable.ic_sort_by_time);
            sortMenuBinding.ivSortByName.setBackgroundResource(R.drawable.ic_sort_by_name);
            sortMenuBinding.ivSortBySinger.setBackgroundResource(R.drawable.ic_sort_by_singer);
            sortMenuBinding.vLine.setBackgroundColor(getResources().getColor(R.color.gray_c9));
            sortMenuBinding.vLine2.setBackgroundColor(getResources().getColor(R.color.gray_c9));
        }
        popupWindow.showAsDropDown(view,  PxUtil.getInstance().dp2px(-105, this),  PxUtil.getInstance().dp2px(10, this));
    }

    /** 按类型排序歌曲 */
    public  void sortList(final int sortType) {
        //将空数据提取 排序后再放到列表最底部
        List<Music> nullData = new ArrayList<>();
        for (Music music : musicList) {
            if(music.getMusicType().equals(" ")) {
                nullData.add(music);
            }
        }
        if(nullData.size() >0) {
            for (Music nullMusic : nullData) {
                musicList.remove(nullMusic);
            }
        }
        Collections.sort(musicList, new Comparator<Music>() {
            @Override
            public int compare(Music o1, Music o2) {
                Collator collator = Collator.getInstance();
                if(1 == sortType) {
                    CollationKey key1 = collator
                            .getCollationKey(String.valueOf(((Music) o1).musicName));
                    CollationKey key2 = collator
                            .getCollationKey(String.valueOf(((Music) o2).musicName));
                    return isUpSortByName ? key1.compareTo(key2) : key2.compareTo(key1);
                } else if (2 == sortType) {
                    CollationKey key1 = collator
                            .getCollationKey(String.valueOf(((Music) o1).musicSinger));
                    CollationKey key2 = collator
                            .getCollationKey(String.valueOf(((Music) o2).musicSinger));
                    return isUpSortBySinger ? key1.compareTo(key2) : key2.compareTo(key1);
                } else {
                    CollationKey key1 = collator
                            .getCollationKey(String.valueOf(((Music) o1).musicId));
                    CollationKey key2 = collator
                            .getCollationKey(String.valueOf(((Music) o2).musicId));
                    return isUpSortByTime ? key1.compareTo(key2) : key2.compareTo(key1);
                }
            }
        });

        if(nullData.size() >0) {
            musicList.addAll(nullData);
        }
        musicListAdapter.notifyDataSetChanged();


    }

    /** 点击取消搜索 */
    public void searchCancel() {
        musicList.clear();
        if(tempMusicList!=null) {
            musicList.addAll(tempMusicList);
        }
        musicListAdapter.notifyDataSetChanged();
        getViewDataBinding().etSearchMusic.setText("");
        getViewDataBinding().llShowNormalBar.setVisibility(View.VISIBLE);
        getViewDataBinding().llShowSearchBar.setVisibility(View.GONE);
        hintKeyBoard();
    }

    /** 点击进入设置页面 */
    public void intoSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra("IsExistNewVersion", isExistNewVersion);
        if(isExistNewVersion) {
            if (null != versionList && versionList.size() > 0) {
                intent.putExtra("NewVersionUrl", versionList.get(0).getVersionUrl());
                intent.putExtra("NewVersionTitle", versionList.get(0).getVersionTitle());
                intent.putExtra("NewVersionContent", versionList.get(0).getVersionContent());
            }
        }
        startActivity(intent);
    }

    /** 指定改变播放模式 */
    public void changePlayMode(int changePlayMode) {
        switch (changePlayMode) {
            case 0: //顺序播放
                binder.setSingePlayMode(false);
                //getViewModel().setSingePlayMode(false);
                getViewDataBinding().tvPlayMode.setText("顺序播放");
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_normal) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                    } else if(rThemeId == R.id.ll_theme_dark) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play);
                    } else if(rThemeId == R.id.ll_theme_white) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_purple);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_purple);
                    } else if(rThemeId == R.id.ll_theme_orange) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_orange);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_orange);
                    } else if(rThemeId == R.id.ll_theme_light) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_light);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_light);
                    } else {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                    }
                } else {
                    getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                    getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                }
                break;
            case 1: //随机播放
                binder.setSingePlayMode(false);
                //getViewModel().setSingePlayMode(false);
                getViewDataBinding().tvPlayMode.setText("随机播放");
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_normal) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                    } else if(rThemeId == R.id.ll_theme_dark) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play);
                    } else if(rThemeId == R.id.ll_theme_white) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_purple);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_purple);
                    } else if(rThemeId == R.id.ll_theme_orange) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_orange);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_orange);
                    } else if(rThemeId == R.id.ll_theme_light) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_light);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_light);
                    } else {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                    }
                } else {
                    getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                    getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                }
                break;
            case 2: //单曲循环
                binder.setSingePlayMode(true);
                //getViewModel().setSingePlayMode(true);
                getViewDataBinding().tvPlayMode.setText("单曲循环");
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_normal) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                    } else if(rThemeId == R.id.ll_theme_dark) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play);
                    } else if(rThemeId == R.id.ll_theme_white) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_purple);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_purple);
                    } else if(rThemeId == R.id.ll_theme_orange) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_orange);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_orange);
                    } else if(rThemeId == R.id.ll_theme_light) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_light);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_light);
                    } else {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                    }
                } else {
                    getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                    getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                }
                break;
        }
    }

    /** 点击切换播放模式 */
    public void playModeButtonClick(View view) {
        playMode = playMode!=2 ? playMode+1 : 0;
        SPUtil.setStrValue(getApplicationContext(), "SavePlayMode", String.valueOf(playMode));
        switch (playMode) {
            case 0: //顺序播放
                binder.setSingePlayMode(false);
                //getViewModel().setSingePlayMode(false);
                getViewDataBinding().tvPlayMode.setText("顺序播放");
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_normal) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                    } else if(rThemeId == R.id.ll_theme_dark) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play);
                    } else if(rThemeId == R.id.ll_theme_white) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_purple);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_purple);
                    } else if(rThemeId == R.id.ll_theme_orange) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_orange);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_orange);
                    } else if(rThemeId == R.id.ll_theme_light) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_light);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_light);
                    } else {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                    }
                } else {
                    getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                    getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_order_play_black);
                }
                break;
            case 1: //随机播放
                binder.setSingePlayMode(false);
                //getViewModel().setSingePlayMode(false);
                getViewDataBinding().tvPlayMode.setText("随机播放");
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_normal) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                    } else if(rThemeId == R.id.ll_theme_dark) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play);
                    } else if(rThemeId == R.id.ll_theme_white) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_purple);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_purple);
                    } else if(rThemeId == R.id.ll_theme_orange) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_orange);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_orange);
                    } else if(rThemeId == R.id.ll_theme_light) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_light);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_light);
                    } else {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                    }
                } else {
                    getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                    getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_random_play_black);
                }
                break;
            case 2: //单曲循环
                binder.setSingePlayMode(true);
                //getViewModel().setSingePlayMode(true);
                getViewDataBinding().tvPlayMode.setText("单曲循环");
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_normal) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                    } else if(rThemeId == R.id.ll_theme_dark) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play);
                    } else if(rThemeId == R.id.ll_theme_white) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_purple);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_purple);
                    } else if(rThemeId == R.id.ll_theme_orange) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_orange);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_orange);
                    } else if(rThemeId == R.id.ll_theme_light) {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_light);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_light);
                    } else {
                        getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                        getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                    }
                } else {
                    getViewDataBinding().ivPlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                    getViewDataBinding().btChangePlayMode.setBackgroundResource(R.drawable.ic_single_play_black);
                }
                break;
        }

    }

    /** 点击删除当前歌单列表所有歌曲 */
    public void deletePlayListAll(View view) {
        DialogDeleteListAllBinding deleteListAllBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.dialog_delete_list_all, null, false);

        //取消
        deleteListAllBinding.btSelectIconCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAlertDialog.dismiss();
            }
        });
        //删除
        deleteListAllBinding.btSelectIconCommit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playList.clear();
                playMusicListAdapter.notifyDataSetChanged();
                SPUtil.setListValue(getApplicationContext(), "PlayListData", playList);
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_DELETE_MUSIC));
                mAlertDialog.dismiss();
            }
        });
        mAlertDialog = new AlertDialog.Builder(this)
                .setView(deleteListAllBinding.getRoot())
                .create();
        Objects.requireNonNull(mAlertDialog.getWindow()).setBackgroundDrawableResource(R.drawable.shape_button_white_2);
        mAlertDialog.show();

       /* PopupWindow  deleteAllWindow  = new PopupWindow(deleteListAllBinding.getRoot(),
                WindowManager.LayoutParams.WRAP_CONTENT,  WindowManager.LayoutParams.WRAP_CONTENT, true);
        deleteAllWindow.setTouchable(true);
        deleteAllWindow.setBackgroundDrawable(getResources().getDrawable(R.drawable.shape_button_white_2));
        deleteAllWindow.showAsDropDown(view);*/


    }

    /** 音乐播放条调整 */
    private class MusicBarChangerListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //拖动时改变显示时间
            getViewDataBinding().tvStartTime.setText(getViewModel().rebuildTime(progress));
            //Log.e("LogByAB", "progress: " + progress);
            lyricScrollView.setMusicPlayerPos(progress);
            getViewDataBinding().hpvProgress.setCurrentCount(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            lyricScrollView.setIsRefreshDraw(true);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //跳到拖动位置播放
            binder.seekTo(seekBar);
            lyricScrollView.setIsRefreshDraw(false);
        }
    }

    /** 拖动进度条调整 （解决拖动时与进度条正常播放移动 显示错乱）*/
    private class ProgressBarTouchListener implements SeekBar.OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.e("ABMediaPlay", "Click: down ");
                    binder.posLock(true);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.e("ABMediaPlay", "Click: up ");
                    binder.posLock(false);
                    break;
            }
            return false;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("ABMusicPlayer", "Stop - UnbindService");
        EventBus.getDefault().unregister(this);
        if(binder!=null) {
            binder.pauseImm(this, currentMusicName, currentMusicSinger, currentBitmap);
            binder.clearMedia();
        }
        clearMediaSession();
        MainVM.stopHandler();
        MainVM.stopTalkHandler();
        unbindService(conn);
        stopService(intentService);
        stopService(intentCharacterService);
        BluetoothUtil.getInstance().unRegisterBluetoothReceiver(this);

    }

    /** 清除媒体会话参数 */
    public void clearMediaSession(){
        if (mSession != null) {
            mSession.setCallback(null);
            mSession.setActive(false);
            mSession.release();
            mSession = null;
        }
    }

    /** 屏蔽返回键 */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            if(isShowMusicPanel || isShowMusicList) {
                hideAllMusicView();
                return true;
            }
            if(isNotMain) {
                searchCancel();
                ObjectAnimator mainPanelChangeObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(MainActivity.this, true, true, getViewDataBinding().clMain);
                ObjectAnimator detailPanelChangeObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(MainActivity.this, false, false, getViewDataBinding().clAlbumDetail);
                mainPanelChangeObjectAnimator.start();
                detailPanelChangeObjectAnimator.start();
                isNotMain = false;
                return true;
            }
        } else if (KeyEvent.KEYCODE_MEDIA_PLAY == keyCode&& keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if(isFirstBluetoothControl){
                isFirstBluetoothControl = false;
                binder.clearMedia();
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.PLAY_LIST_FIRST));
            } else {
                binder.pause(this, currentMusicName, currentMusicSinger, currentBitmap);
            }
           // binder.pause(this, currentMusicName, currentMusicSinger, currentBitmap);
        } else if (KeyEvent.KEYCODE_MEDIA_PAUSE == keyCode && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            if(isFirstBluetoothControl){
                isFirstBluetoothControl = false;
                binder.clearMedia();
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.PLAY_LIST_FIRST));
            } else {
                binder.pause(this, currentMusicName, currentMusicSinger, currentBitmap);
            }
            //binder.pause(this, currentMusicName, currentMusicSinger, currentBitmap);
        } else if(KeyEvent.KEYCODE_MEDIA_NEXT == keyCode && KeyEvent.ACTION_DOWN == keyEvent.getAction()) {
            lastOrNextMusic(true);
        } else if(KeyEvent.KEYCODE_MEDIA_PREVIOUS == keyCode && KeyEvent.ACTION_DOWN == keyEvent.getAction()) {
            lastOrNextMusic(false);
        }


        return super.onKeyDown(keyCode, keyEvent);
    }

    /** 将当前界面设置为Task中第一个Activity启动 */
    @Override
    public void onBackPressed() {
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.addCategory(Intent.CATEGORY_HOME);
        startActivity(i);
    }


    /** 音乐进度条 */
    @Override
    public void viewSeekBarPos(int pos) {
        getViewDataBinding().sbMusicBar.setProgress(pos);
    }

    /** 音乐进度条进度初始化 */
    @Override
    public void viewSeekBarResume() {
        getViewDataBinding().sbMusicBar.setProgress(0);
    }

    /** 播放或暂停 */
    @Override
    public void viewPause(boolean isPause) {
        getViewDataBinding().btPlay.setBackgroundResource(isPause ? R.drawable.selector_play_black_selected : R.drawable.selector_pause_black_selected);
        getViewDataBinding().ivPanelPlay.setBackgroundResource(isPause ? R.drawable.selector_play_circle_black_selected : R.drawable.selector_pause_circle_black_selected);
        if (objectAnimator != null) {
            if (!isPause) {
                objectAnimator.resume();
            } else {
                objectAnimator.pause();
            }
        }
    }

    /** 点击歌曲后显示音乐信息 */
    @Override
    public void viewMusicMsg(Music source, int allPos) {
        currentAllTime = allPos;
        getViewDataBinding().sbMusicBar.setMax(allPos);
        getViewDataBinding().tvAllTime.setText(getViewModel().rebuildTime(allPos));
        String musicMsg = source.musicName;
        getViewDataBinding().tvMusicName.setText(musicMsg);
        getViewDataBinding().tvSingerName.setText(source.musicSinger);
        //变更主题
        if(rThemeId!=0) {
            if(rThemeId == R.id.ll_theme_normal) {
                Glide.with(getApplication())
                        .setDefaultRequestOptions(requestOptions)
                        .load(source.getMusicImg())
                        .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.light_ea)))
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(getViewDataBinding().ivMusicImg);
            } else if(rThemeId == R.id.ll_theme_dark) {
                Glide.with(getApplication())
                        .setDefaultRequestOptions(requestOptions)
                        .load(source.getMusicImg())
                        .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.white)))
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(getViewDataBinding().ivMusicImg);
            } else if(rThemeId == R.id.ll_theme_white) {
                Glide.with(getApplication())
                        .setDefaultRequestOptions(requestOptions)
                        .load(source.getMusicImg())
                        .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.purple)))
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(getViewDataBinding().ivMusicImg);
            } else if(rThemeId == R.id.ll_theme_orange) {
                Glide.with(getApplication())
                        .setDefaultRequestOptions(requestOptions)
                        .load(source.getMusicImg())
                        .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.orange_0b)))
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(getViewDataBinding().ivMusicImg);
            } else if(rThemeId == R.id.ll_theme_light) {
                Glide.with(getApplication())
                        .setDefaultRequestOptions(requestOptions)
                        .load(source.getMusicImg())
                        .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.light_b5)))
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(getViewDataBinding().ivMusicImg);
            } else {
                Glide.with(getApplication())
                        .setDefaultRequestOptions(requestOptions)
                        .load(source.getMusicImg())
                        .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.light_ea)))
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(getViewDataBinding().ivMusicImg);
            }
        } else {
            Glide.with(getApplication())
                    .setDefaultRequestOptions(requestOptions)
                    .load(source.getMusicImg())
                    .transform(new CropCircleWithBorderTransformation(5, getResources().getColor(R.color.light_ea)))
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(getViewDataBinding().ivMusicImg);
        }

        if (objectAnimator != null) {
            objectAnimator.cancel();
        }
        objectAnimator = ObjectAnimator.ofFloat(getViewDataBinding().ivMusicImg, "rotation", 0f, 360.0f);
        objectAnimator.setDuration(15000);
        objectAnimator.setInterpolator(new LinearInterpolator());//不停顿
        objectAnimator.setRepeatCount(-1);//设置动画重复次数
        objectAnimator.setRepeatMode(ValueAnimator.RESTART);//动画重复模式
        objectAnimator.start();
    }


    public static class MusicListViewHolder extends RecyclerView.ViewHolder {
        public MusicListViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
    /** 在线音乐列表Adapter */
    public class MusicListAdapter extends RecyclerView.Adapter<MusicListViewHolder> {

        private Context context;
        private List<Music> list;
        private View mViewHeader;

        public static final int ITEM_TYPE_HEADER =0;
        public static final int ITEM_TYPE_CONTENT =1;

        public MusicListAdapter(Context context, List<Music> list) {
            this.context = context;
            this.list = list;
        }

        public void setHeaderView(View viewHeader) {
            mViewHeader = viewHeader;
            notifyItemInserted(0);
        }

        @NonNull
        @Override
        public MusicListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if(mViewHeader !=null && viewType == ITEM_TYPE_HEADER) {
                return new MusicListViewHolder(mViewHeader);
            }
            ItemMusicListBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.item_music_list, parent, false);
            return new MusicListViewHolder(binding.getRoot());
        }

        @Override
        public void onBindViewHolder(@NonNull MusicListViewHolder holder, @SuppressLint("RecyclerView") final int position) {
            final ItemMusicListBinding binding = DataBindingUtil.getBinding(holder.itemView);
            if (binding != null) {

                //变更主题
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_normal) {
                        binding.rlMusicAll.setBackgroundResource(R.drawable.selector_tab_selected3);
                        binding.tvMusicName.setTextColor(getResources().getColor(R.color.light_ff));
                        binding.tvSingerName.setTextColor(getResources().getColor(R.color.light_ff));
                        binding.ivAdd.setBackgroundResource(R.drawable.ic_add_light);
                    } else if(rThemeId == R.id.ll_theme_dark) {
                        binding.rlMusicAll.setBackgroundResource(R.drawable.selector_tab_selected3);
                        binding.tvMusicName.setTextColor(getResources().getColor(R.color.white));
                        binding.tvSingerName.setTextColor(getResources().getColor(R.color.white));
                        binding.ivAdd.setBackgroundResource(R.drawable.ic_add);
                    } else if (rThemeId == R.id.ll_theme_white) {
                        binding.rlMusicAll.setBackgroundResource(R.drawable.selector_white_theme_selected2);
                        binding.tvMusicName.setTextColor(getResources().getColor(R.color.purple));
                        binding.tvSingerName.setTextColor(getResources().getColor(R.color.gray_purple_ac));
                        binding.ivAdd.setBackgroundResource(R.drawable.ic_add_gray_purple);
                    } else if (rThemeId == R.id.ll_theme_orange) {
                        binding.rlMusicAll.setBackgroundResource(R.drawable.selector_tab_selected3);
                        binding.tvMusicName.setTextColor(getResources().getColor(R.color.orange_0b));
                        binding.tvSingerName.setTextColor(getResources().getColor(R.color.orange_0b));
                        binding.ivAdd.setBackgroundResource(R.drawable.ic_add_orange);
                    } else if(rThemeId == R.id.ll_theme_light) {
                        binding.rlMusicAll.setBackgroundResource(R.drawable.selector_tab_selected3);
                        binding.tvMusicName.setTextColor(getResources().getColor(R.color.light_ff));
                        binding.tvSingerName.setTextColor(getResources().getColor(R.color.light_ff));
                        binding.ivAdd.setBackgroundResource(R.drawable.ic_add_light);
                    } else {
                        binding.rlMusicAll.setBackgroundResource(R.drawable.selector_tab_selected3);
                        binding.tvMusicName.setTextColor(getResources().getColor(R.color.light_ff));
                        binding.tvSingerName.setTextColor(getResources().getColor(R.color.light_ff));
                        binding.ivAdd.setBackgroundResource(R.drawable.ic_add_light);
                    }
                }
                binding.rlMusicAll.setVisibility(list.get(position).getMusicType().equals(" ") ? GONE : View.VISIBLE);
                Glide.with(getApplication())
                        .setDefaultRequestOptions(requestOptions)
                        .load(list.get(position).getMusicImg())
                        .transform(new RoundedCornersTransformation(20, 0, RoundedCornersTransformation.CornerType.ALL))
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(binding.ivMusicImg);
                binding.tvMusicName.setText(list.get(position).musicName);
                binding.tvSingerName.setText(list.get(position).musicSinger);
                //点击播放歌曲
                binding.rlMusicAll.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(playList.size()>0){
                            for(int i=0; i<playList.size(); i++) {
                                if(playList.get(i).isPlaying) {
                                    playList.get(i).isPlaying = false;
                                    binder.showLyric(list.get(position), (playMode == 2));
                                    playList.add(i+1, setMusicMsg(list.get(position), true));
                                    playMusicListAdapter.notifyDataSetChanged();

                                    SPUtil.setListValue(context, "PlayListData", playList);
                                    return;
                                }
                            }
                            binder.showLyric(list.get(position), (playMode == 2));
                            playList.add(playList.size(), setMusicMsg(list.get(position), true));
                            playMusicListAdapter.notifyDataSetChanged();

                            SPUtil.setListValue(context, "PlayListData", playList);
                        } else {
                            binder.showLyric(list.get(position), (playMode == 2));
                            playList.add(setMusicMsg(list.get(position), true));
                            playMusicListAdapter.notifyDataSetChanged();

                            SPUtil.setListValue(context, "PlayListData", playList);
                        }

                    }
                });

                //点击添加歌曲到列表
                binding.llAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        //变更主题
                        if(rThemeId!=0) {
                            if (rThemeId == R.id.ll_theme_normal) {
                                binding.ivAddAnimator.setVisibility(View.GONE);
                                binding.ivAddAnimatorBlack.setVisibility(View.GONE);
                                binding.ivAddAnimatorOrange.setVisibility(View.GONE);
                                binding.ivAddAnimatorLight1.setVisibility(View.VISIBLE);
                                binding.ivAddAnimatorLight2.setVisibility(View.GONE);
                                AnimatorSet animatorSet = MyAnimationUtil.animatorSetAddMusic(binding.ivAddAnimatorLight1);
                                animatorSet.start();
                            } else if(rThemeId == R.id.ll_theme_dark) {
                                binding.ivAddAnimator.setVisibility(View.GONE);
                                binding.ivAddAnimatorBlack.setVisibility(View.VISIBLE);
                                binding.ivAddAnimatorOrange.setVisibility(View.GONE);
                                binding.ivAddAnimatorLight1.setVisibility(View.GONE);
                                binding.ivAddAnimatorLight2.setVisibility(View.GONE);
                                AnimatorSet animatorSet = MyAnimationUtil.animatorSetAddMusic(binding.ivAddAnimatorBlack);
                                animatorSet.start();

                            } else if (rThemeId == R.id.ll_theme_white) {
                                binding.ivAddAnimator.setVisibility(View.VISIBLE);
                                binding.ivAddAnimatorBlack.setVisibility(View.GONE);
                                binding.ivAddAnimatorOrange.setVisibility(View.GONE);
                                binding.ivAddAnimatorLight1.setVisibility(View.GONE);
                                binding.ivAddAnimatorLight2.setVisibility(View.GONE);
                                AnimatorSet animatorSet = MyAnimationUtil.animatorSetAddMusic(binding.ivAddAnimator);
                                animatorSet.start();
                            } else if (rThemeId == R.id.ll_theme_orange) {
                                binding.ivAddAnimator.setVisibility(View.GONE);
                                binding.ivAddAnimatorBlack.setVisibility(View.GONE);
                                binding.ivAddAnimatorOrange.setVisibility(View.VISIBLE);
                                binding.ivAddAnimatorLight1.setVisibility(View.GONE);
                                binding.ivAddAnimatorLight2.setVisibility(View.GONE);
                                AnimatorSet animatorSet = MyAnimationUtil.animatorSetAddMusic(binding.ivAddAnimatorOrange);
                                animatorSet.start();
                            } else if(rThemeId == R.id.ll_theme_light) {
                                binding.ivAddAnimator.setVisibility(View.GONE);
                                binding.ivAddAnimatorBlack.setVisibility(View.GONE);
                                binding.ivAddAnimatorOrange.setVisibility(View.GONE);
                                binding.ivAddAnimatorLight1.setVisibility(View.GONE);
                                binding.ivAddAnimatorLight2.setVisibility(View.GONE);
                                AnimatorSet animatorSet = MyAnimationUtil.animatorSetAddMusic(binding.ivAddAnimatorLight2);
                                animatorSet.start();
                            } else {
                                binding.ivAddAnimator.setVisibility(View.GONE);
                                binding.ivAddAnimatorBlack.setVisibility(View.GONE);
                                binding.ivAddAnimatorOrange.setVisibility(View.GONE);
                                binding.ivAddAnimatorLight1.setVisibility(View.GONE);
                                binding.ivAddAnimatorLight2.setVisibility(View.GONE);
                                AnimatorSet animatorSet = MyAnimationUtil.animatorSetAddMusic(binding.ivAddAnimatorLight1);
                                animatorSet.start();
                            }
                        } else {
                            binding.ivAddAnimator.setVisibility(View.GONE);
                            binding.ivAddAnimatorBlack.setVisibility(View.GONE);
                            binding.ivAddAnimatorOrange.setVisibility(View.GONE);
                            binding.ivAddAnimatorLight1.setVisibility(View.GONE);
                            binding.ivAddAnimatorLight2.setVisibility(View.GONE);
                            AnimatorSet animatorSet = MyAnimationUtil.animatorSetAddMusic(binding.ivAddAnimatorLight1);
                            animatorSet.start();
                        }


                        playList.add(setMusicMsg(list.get(position), false));

                        if(playList.size()==1) {
                            playList.get(0).isPlaying = true;
                            binder.showLyric(playList.get(0), (playMode == 2));
                        } else {
                            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_ADD_MUSIC));
                        }
                        playMusicListAdapter.notifyDataSetChanged();

                        SPUtil.setListValue(context, "PlayListData", playList);
                    }
                });

                binding.llFavorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(context, "喜爱", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }

        @Override
        public int getItemViewType(int position){
            if(mViewHeader == null) return ITEM_TYPE_CONTENT;
            if(position == 0) return ITEM_TYPE_HEADER;
            return ITEM_TYPE_CONTENT;
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

    }

    /** 默认存储Music值 */
    private Music setMusicMsg(Music musicMsg, boolean isPlaying) {
        Music music = new Music();
        music.setMusicId(musicMsg.getMusicId());
        music.setMusicName(musicMsg.getMusicName());
        music.setMusicSinger(musicMsg.getMusicSinger());
        music.setMusicType(musicMsg.getMusicType());
        music.setMusicImg(musicMsg.getMusicImg());
        music.setMusicURL(musicMsg.getMusicURL());
        music.setMusicFavorite(musicMsg.getMusicFavorite());
        music.setMusicLyric(musicMsg.getMusicLyric());
        music.setMusicImgByte(musicMsg.getMusicImgByte());
        music.setLocal(musicMsg.isLocal);
        music.isPlaying = isPlaying;
        return music;
    }

    /** 存储列表时默认所有歌单为未播放状态 */
    private void setPlayListDefault(List<Music> playList) {
        List<Music> list = playList;
        int size = list.size();
        for(int i=0; i<size; i++) {
            list.get(i).isPlaying = false;
        }
    }

    public static class PlayMusicListViewHolder extends RecyclerView.ViewHolder {
        public PlayMusicListViewHolder(@NonNull View itemView) { super(itemView); }
    }
    /** 播放列表Adapter */
    public class PlayMusicListAdapter extends RecyclerView.Adapter<PlayMusicListViewHolder> {

        private Context context;
        private List<Music> list;

        public PlayMusicListAdapter(Context context, List<Music> list) {
            this.context = context;
            this.list = list;
        }

        @NonNull
        @Override
        public PlayMusicListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemPlayListBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context),
                    R.layout.item_play_list, parent, false);
            return new PlayMusicListViewHolder(binding.getRoot());
        }

        @Override
        public void onBindViewHolder(@NonNull PlayMusicListViewHolder holder, @SuppressLint("RecyclerView") final int position) {
            ItemPlayListBinding binding = DataBindingUtil.getBinding(holder.itemView);
            if(binding != null) {

                binding.tvOrderNum.setVisibility(list.get(position).isPlaying? GONE : View.VISIBLE);

                int num = position+1;
                binding.tvOrderNum.setText(num<10? "0"+num : ""+num);
                binding.tvMusicName.setText(list.get(position).musicName);
                binding.tvSingerName.setText(list.get(position).musicSinger);

                //变更主题
                if(rThemeId!=0) {
                    if(rThemeId == R.id.ll_theme_normal) {
                        binding.tvOrderNum.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_ea) : getResources().getColor(R.color.black));
                        binding.tvMusicName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_ea) : getResources().getColor(R.color.black));
                        binding.tvSingerName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_ea) : getResources().getColor(R.color.black));
                        binding.ivDelete.setBackgroundResource(R.drawable.selector_delete_selected_2);
                        binding.ivMusicRail.setVisibility(list.get(position).isPlaying? View.VISIBLE : GONE);
                        binding.ivMusicRailDark.setVisibility(View.GONE);
                        binding.ivMusicRailPurple.setVisibility(View.GONE);
                        binding.ivMusicRailOrange.setVisibility(View.GONE);
                        binding.ivMusicRailLight.setVisibility(View.GONE);
                    } else if (rThemeId == R.id.ll_theme_dark) {
                        binding.tvOrderNum.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.black) : getResources().getColor(R.color.white));
                        binding.tvMusicName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.black) : getResources().getColor(R.color.white));
                        binding.tvSingerName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.black) : getResources().getColor(R.color.white));
                        binding.ivDelete.setBackgroundResource(R.drawable.ic_delete);
                        binding.ivMusicRail.setVisibility(View.GONE);
                        binding.ivMusicRailDark.setVisibility(list.get(position).isPlaying? View.VISIBLE : GONE);
                        binding.ivMusicRailPurple.setVisibility(View.GONE);
                        binding.ivMusicRailOrange.setVisibility(View.GONE);
                        binding.ivMusicRailLight.setVisibility(View.GONE);
                    } else if (rThemeId == R.id.ll_theme_white) {
                        binding.tvOrderNum.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.purple_light) : getResources().getColor(R.color.purple));
                        binding.tvMusicName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.purple_light) : getResources().getColor(R.color.purple));
                        binding.tvSingerName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.purple_light) : getResources().getColor(R.color.gray_purple_ac));
                        binding.ivDelete.setBackgroundResource(R.drawable.ic_delete_purple);
                        binding.ivMusicRail.setVisibility(View.GONE);
                        binding.ivMusicRailDark.setVisibility(View.GONE);
                        binding.ivMusicRailPurple.setVisibility(list.get(position).isPlaying? View.VISIBLE : GONE);
                        binding.ivMusicRailOrange.setVisibility(View.GONE);
                        binding.ivMusicRailLight.setVisibility(View.GONE);
                    } else if (rThemeId == R.id.ll_theme_orange) {
                        binding.tvOrderNum.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.orange_f4) : getResources().getColor(R.color.orange_0b));
                        binding.tvMusicName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.orange_f4) : getResources().getColor(R.color.orange_0b));
                        binding.tvSingerName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.orange_f4) : getResources().getColor(R.color.orange_0b));
                        binding.ivDelete.setBackgroundResource(R.drawable.ic_delete_orange);
                        binding.ivMusicRail.setVisibility(View.GONE);
                        binding.ivMusicRailDark.setVisibility(View.GONE);
                        binding.ivMusicRailPurple.setVisibility(View.GONE);
                        binding.ivMusicRailOrange.setVisibility(list.get(position).isPlaying? View.VISIBLE : GONE);
                        binding.ivMusicRailLight.setVisibility(View.GONE);
                    } else if(rThemeId == R.id.ll_theme_light) {
                        binding.tvOrderNum.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_8a) : getResources().getColor(R.color.light_b5));
                        binding.tvMusicName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_8a) : getResources().getColor(R.color.light_b5));
                        binding.tvSingerName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_8a) : getResources().getColor(R.color.light_b5));
                        binding.ivDelete.setBackgroundResource(R.drawable.ic_delete_light);
                        binding.ivMusicRail.setVisibility(View.GONE);
                        binding.ivMusicRailDark.setVisibility(View.GONE);
                        binding.ivMusicRailPurple.setVisibility(View.GONE);
                        binding.ivMusicRailOrange.setVisibility(View.GONE);
                        binding.ivMusicRailLight.setVisibility(list.get(position).isPlaying? View.VISIBLE : GONE);

                    } else {
                        binding.tvOrderNum.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_ea) : getResources().getColor(R.color.black));
                        binding.tvMusicName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_ea) : getResources().getColor(R.color.black));
                        binding.tvSingerName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_ea) : getResources().getColor(R.color.black));
                        binding.ivMusicRail.setVisibility(list.get(position).isPlaying? View.VISIBLE : GONE);
                        binding.ivDelete.setBackgroundResource(R.drawable.selector_delete_selected_2);
                        binding.ivMusicRail.setVisibility(list.get(position).isPlaying? View.VISIBLE : GONE);
                        binding.ivMusicRailDark.setVisibility(View.GONE);
                        binding.ivMusicRailPurple.setVisibility(View.GONE);
                        binding.ivMusicRailOrange.setVisibility(View.GONE);
                        binding.ivMusicRailLight.setVisibility(View.GONE);
                    }
                } else {
                    binding.tvOrderNum.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_ea) : getResources().getColor(R.color.black));
                    binding.tvMusicName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_ea) : getResources().getColor(R.color.black));
                    binding.tvSingerName.setTextColor(list.get(position).isPlaying? getResources().getColor(R.color.light_ea) : getResources().getColor(R.color.black));
                    binding.ivMusicRail.setVisibility(list.get(position).isPlaying? View.VISIBLE : GONE);
                    binding.ivDelete.setBackgroundResource(R.drawable.selector_delete_selected_2);
                    binding.ivMusicRail.setVisibility(list.get(position).isPlaying? View.VISIBLE : GONE);
                    binding.ivMusicRailDark.setVisibility(View.GONE);
                    binding.ivMusicRailPurple.setVisibility(View.GONE);
                    binding.ivMusicRailOrange.setVisibility(View.GONE);
                    binding.ivMusicRailLight.setVisibility(View.GONE);
                }

                //点击播放列表的歌曲
                binding.getRoot().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!list.get(position).isPlaying) {
                            setPlayListDefault(list);
                            binder.showLyric(list.get(position), (playMode == 2));
                            list.get(position).isPlaying = true;
                            notifyDataSetChanged();
                        }
                    }
                });
                //当前播放的歌曲列表不显示删除按钮
                binding.llDelete.setVisibility(list.get(position).isPlaying? View.GONE : View.VISIBLE);
                binding.llDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!list.get(position).isPlaying) {
                            list.remove(position);
                            playMusicListAdapter.notifyDataSetChanged();
                            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_DELETE_MUSIC));
                            SPUtil.setListValue(context, "PlayListData", list);
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    /** 关闭软键盘 */
    public void hintKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        View v = getWindow().peekDecorView();
        if (null != v) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    /** 根据权限对应实现功能 */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(REQUEST_CODE_DOWNLOAD_APP== requestCode) {
            for (int i=0; i<permissions.length; i++) {
                if(permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        EventBus.getDefault().post(new ThreadEvent(ThreadEvent.DOWNLOAD_APP, versionList.get(0).versionUrl));
                    }
                }
            }
        } else if(REQUEST_CODE_SCAN_LOCAL_FILE == requestCode) {
            for (int i=0; i<permissions.length; i++) {
                if(permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)){
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        EventBus.getDefault().post(new ThreadEvent(ThreadEvent.SCAN_LOCAL_FILE_BY_CHECK_PERMISSION,  "scan"));
                    }
                }
            }
        } else if(REQUEST_CODE_SELECT_LOCAL_FILE == requestCode) {
            for (int i=0; i<permissions.length; i++) {
                if(permissions[i].equals(Manifest.permission.READ_EXTERNAL_STORAGE)){
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        EventBus.getDefault().post(new ThreadEvent(ThreadEvent.SCAN_LOCAL_FILE_BY_CHECK_PERMISSION,  "select"));
                    }
                }
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /** 在线与本地页面碎片切换 */
    private static class MainFragmentStateAdapter extends FragmentStateAdapter {

        private List<Fragment> fragmentList;

        public MainFragmentStateAdapter(FragmentActivity fragmentActivity, List<Fragment> fragmentList) {
            super(fragmentActivity);
            this.fragmentList = fragmentList;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment = fragmentList.get(position);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return fragmentList.size();
        }

    }

}