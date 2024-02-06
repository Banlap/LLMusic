package com.banlap.llmusic.ui.activity;

import static android.view.View.GONE;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.banlap.llmusic.R;
import com.banlap.llmusic.base.BaseActivity;
import com.banlap.llmusic.base.BaseApplication;
import com.banlap.llmusic.databinding.ActivityMainBinding;
import com.banlap.llmusic.databinding.DialogAddMusicToLocalListBinding;
import com.banlap.llmusic.databinding.DialogChangeModeMenuBinding;
import com.banlap.llmusic.databinding.DialogCharacterMenuBinding;
import com.banlap.llmusic.databinding.DialogDeleteListAllBinding;
import com.banlap.llmusic.databinding.DialogDownloadBinding;
import com.banlap.llmusic.databinding.DialogLocalFileBinding;
import com.banlap.llmusic.databinding.DialogMainMenuBinding;
import com.banlap.llmusic.databinding.DialogMessageBinding;
import com.banlap.llmusic.databinding.DialogMoreMenuBinding;
import com.banlap.llmusic.databinding.DialogPanelMoreMenuBinding;
import com.banlap.llmusic.databinding.DialogSortMenuBinding;
import com.banlap.llmusic.databinding.DialogTimePickerBinding;
import com.banlap.llmusic.databinding.DialogTimeTasksBinding;
import com.banlap.llmusic.databinding.ItemAddMusicLocalListBinding;
import com.banlap.llmusic.databinding.ItemMusicListBinding;
import com.banlap.llmusic.databinding.ItemPlayListBinding;
import com.banlap.llmusic.fixed.LiellaMusic;
import com.banlap.llmusic.model.LocalPlayList;
import com.banlap.llmusic.model.Message;
import com.banlap.llmusic.model.Music;
import com.banlap.llmusic.model.MusicLyric;
import com.banlap.llmusic.model.Version;
import com.banlap.llmusic.receiver.AlarmReceiver;
import com.banlap.llmusic.request.ThreadEvent;
import com.banlap.llmusic.service.CharacterService;
import com.banlap.llmusic.service.MusicPlayService;
import com.banlap.llmusic.sql.MysqlHelper;
import com.banlap.llmusic.ui.ThemeHelper;
import com.banlap.llmusic.ui.fragment.LocalListFragment;
import com.banlap.llmusic.ui.fragment.MainListFragment;
import com.banlap.llmusic.uivm.vm.MainVM;
import com.banlap.llmusic.utils.BluetoothUtil;
import com.banlap.llmusic.utils.CharacterHelper;
import com.banlap.llmusic.utils.CountDownHelper;
import com.banlap.llmusic.utils.FileUtil;
import com.banlap.llmusic.utils.LLActivityManager;
import com.banlap.llmusic.utils.MyAnimationUtil;
import com.banlap.llmusic.utils.NotificationHelper;
import com.banlap.llmusic.utils.PermissionUtil;
import com.banlap.llmusic.utils.PxUtil;
import com.banlap.llmusic.utils.SPUtil;
import com.banlap.llmusic.utils.SystemUtil;
import com.banlap.llmusic.utils.TimeUtil;
import com.banlap.llmusic.widget.LyricScrollView;
import com.banlap.llmusic.widget.SingleLyricScrollView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.android.material.appbar.AppBarLayout;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.text.CollationKey;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

/**
 * 主页界面
 * */
public class MainActivity extends BaseActivity<MainVM, ActivityMainBinding> implements MainVM.MainCallBack {
    private final String TAG = MainActivity.class.getSimpleName();
    private final Context context = MainActivity.this;
    private List<Music> musicList;                      //按类型的所有歌曲
    private List<Music> tempMusicList;                  //临时音乐列表
    private List<Music> playList;                       //当前播放的列表
    private List<Music> mFavoriteList;                  //收藏夹歌单数据
    private List<MusicLyric> musicLyricList;            //当前播放歌曲的歌词列表
    private List<Message> messageList;                  //消息列表
    private List<Version> versionList;                  //版本列表
    private List<LocalPlayList> localPlayList;          //自建歌单数据列表
    private MusicListAdapter musicListAdapter;          //音乐列表适配器
    private PlayMusicListAdapter playMusicListAdapter;  //播放列表适配器
    private AddMusicLocalListAdapter addMusicLocalListAdapter; //点击添加到自建列表 适配器
    private ObjectAnimator objectAnimator;              //动画效果
    private MusicPlayService.MusicBinder binder;        //用于绑定服务
    private ServiceConn conn;                           //用于绑定服务
    private Intent intentService;                       //音乐服务
    private Intent intentCharacterService;              //角色服务
    private boolean isSelect = false;                   //查询一次数据
    private boolean isClick = false;                    //判断是否点击按钮
    private boolean isNotMain = false;                  //判断是否在主界面
    private boolean isSearchMusic = false;              //判断是否处于搜索音乐状态

    private boolean isShowMusicPanel = false;           //判断是否显示音乐面板
    private boolean isShowMusicList = false;            //判断是否显示音乐清单
    private boolean isShowControllerModePanel = false;  //判断是否显示新版音乐控制模式
    private boolean isShowNewPlayController = false;    //判断是否显示新版音乐控制栏
    private boolean isShowNewMusicList = false;         //判断是否显新版音乐清单
    private boolean isClickNewSingleLyricView = false;  //判断是否点击新版音乐歌词展示
    public static boolean isPlay = false;               //判断是否播放音乐
    public Music currentMusicDetail;                    //当前播放的歌曲总数据
    public static String currentMusicName ="";          //当前歌曲的歌名
    public static String currentMusicSinger ="";        //当前歌曲的歌手
    public static String currentMusicImg ="";           //当前歌曲的图片
    public static Bitmap currentBitmap = null;          //当前歌曲Bitmap图
    public static boolean isFavorite = false;           //当前歌曲是否已收藏
    private AlertDialog mAlertDialog;                   //弹窗
    private PopupWindow mPopupWindow;                   //弹窗
    private int musicListSize = 0;                      //获取总播放列表数
    public static int playMode = 0;                     //播放模式: 0顺序播放 1随机播放 2单曲循环
    private final int panelMoveAxis = 750;              //面板移动值
    private int heightPixels = 0 ;                      //设备高度
    private int rThemeId =0;                            //当前主题
    /** 角色视图 */
    private String mCharacterName;                      //当前角色
    private ActivityResultLauncher<Intent> intentActivityResultLauncher;
    private ActivityResultLauncher<Intent> intentTakePhotoLauncher;
    private DialogDownloadBinding downloadBinding;
    private DialogLocalFileBinding dialogLocalFileBinding;
    private DialogTimeTasksBinding dialogTimeTasksBinding;
    private int clickLocalPlayListId = 0;                 //点击自建歌单数据列表的ID号
    private boolean isClickLocalPlayList = false;         //是否点击了自建歌单数据列表
    private boolean isClickLocalOrFavorite = false;       //是否点击了本地歌曲或收藏列表
    private boolean isExistNewVersion = false;            //是否存在新版本app
    private boolean isFinishAnimator = true;              //是否执行标题栏文本显示动画
    private boolean isChangeScrollRange = false;          //解决重复高度时刷新appBar的标题文字动画
    private boolean isCountDown = false;                  //倒计时是否完成
    private int selectCountDown;                          //选择的倒计时选项
    private int hour = 0, minute = 0, sec;
    private ObjectAnimator textAnimator;                  //标题栏文本动画
    private ObjectAnimator controllerModeAnimator;        //音乐氛围模式动画
    private ObjectAnimator musicControllerAnimator;       //音乐控制器动画
    private RequestOptions requestOptions;
    private boolean isUpSortByTime = false;               //是否向上排序：时间
    private boolean isUpSortByName = false;               //是否向上排序：歌曲名称
    private boolean isUpSortBySinger = false;             //是否向上排序：歌手名称
    private int clickSortType = 0;                        //当前点击的排序类型
    private int defaultLyricSize = 25;                        //默认歌词字体大小
    private int singleLyricSize = 25;                        //滚动行歌词字体大小
    private int detailLyricSize = 25;                        //明细歌词字体大小

    private LyricScrollView lyricScrollView;              //
    private LyricScrollView lyricNewScrollDetailView;              //
    private SingleLyricScrollView lyricNewScrollView;              //
    private MediaSession mSession;                        //用于获取按键事件
    public boolean isFirstBluetoothControl = true;        //首次蓝牙控制标记
    private MainFragmentStateAdapter  mainFragmentStateAdapter;     //主页与本地页面切换Adapter
    public static final int REQUEST_CODE_NEED_RUNNING_PERMISSION = 100; //检查进入APP需要的权限
    public static final int REQUEST_CODE_DOWNLOAD_APP = 101;            //检查下载app时需要的权限
    public static final int REQUEST_CODE_SCAN_LOCAL_FILE = 102;         //检查扫描文件所需要的权限
    public static final int REQUEST_CODE_SELECT_LOCAL_FILE = 103;       //检查选择文件所需要的权限
    public static final int REQUEST_CODE_SELECT_IMG_FILE = 104;       //检查选择图片所需要的权限
    public static final int REQUEST_CODE_CONTROLLER_MODE = 105;       //检查进入氛围模式时所需要的权限

    //专辑类型
    public static final String MUSIC_TYPE_LIELLA = "Liella";
    public static final String MUSIC_TYPE_LIYUU = "Fo(u)rYuU";
    public static final String MUSIC_TYPE_SUNNYPASSION = "SunnyPassion";
    public static final String MUSIC_TYPE_NIJIGASAKI = "Nijigasaki";
    public static final String MUSIC_TYPE_AQOURS = "Aqours";
    public static final String MUSIC_TYPE_US = "us";
    public static final String MUSIC_TYPE_HASUNOSORA = "Hasunosora";
    public static final String MUSIC_TYPE_SAINT_SNOW = "Saint Snow";
    public static final String MUSIC_TYPE_A_RISE = "A-RISE";

    private BaseApplication baseApplication;
    private HttpProxyCacheServer proxyCacheServer;
    private AudioManager audioManager;

    /** MediaSession框架回调 用于返回耳机实体按钮操作 */
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
        localPlayList = new ArrayList<>();
        mFavoriteList = new ArrayList<>();

        //本地缓存列表
        List<Music> spList = SPUtil.getListValue(this, SPUtil.PlayListData, Music.class);
        if(spList.size()>0){
            playList.addAll(spList);
            setPlayListDefault(playList);
        } else {
            SPUtil.setListValue(context, SPUtil.PlayListData, playList);
        }

        //最爱歌曲缓存列表
        List<Music> spList2 = SPUtil.getListValue(LLActivityManager.getInstance().getTopActivity(), SPUtil.FavoriteListData, Music.class);
        if(spList2.size()>0){
            mFavoriteList.addAll(spList2);
        } else {
            Music nullMusicFile = new Music();
            Music nullMusicFile2 = new Music();
            mFavoriteList.add(nullMusicFile);
            mFavoriteList.add(nullMusicFile2);
            SPUtil.setListValue(LLActivityManager.getInstance().getTopActivity(), SPUtil.FavoriteListData, mFavoriteList);
        }

        //播放列表使用图标显示收藏的歌曲
        setMusicFavorite(mFavoriteList);

        if(SPUtil.getStrValue(this, SPUtil.SavePlayMode) != null) {
            if (!(SPUtil.getStrValue(this, SPUtil.SavePlayMode).equals(""))) {
                playMode = Integer.parseInt(SPUtil.getStrValue(this, SPUtil.SavePlayMode));
            }
        }

        String controllerScene = SPUtil.getStrValue(MainActivity.this, SPUtil.SaveControllerScene);
        if(TextUtils.isEmpty(controllerScene)) {
            SPUtil.setStrValue(MainActivity.this, SPUtil.SaveControllerScene, SPUtil.SaveControllerSceneValue_DefaultScene);
        }

        requestOptions = new RequestOptions();
        requestOptions.diskCacheStrategy(DiskCacheStrategy.ALL);
        //requestOptions.override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL); //关键代码，加载原始大小
        requestOptions.format(DecodeFormat.PREFER_RGB_565); //设置为这种格式去掉透明度通道，可以减少内存占有

        mCharacterName = CharacterHelper.CHARACTER_NAME_KEKE;

        //
        baseApplication = (BaseApplication) getApplication();
        proxyCacheServer = baseApplication.getProxy(this);
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

    }



    @SuppressLint({"ClickableViewAccessibility", "SetTextI18n"})
    @Override
    protected void initView() {
        EventBus.getDefault().register(this);
        getViewDataBinding().setVm(getViewModel());
        getViewModel().setCallBack(this);
        initCheckPermission();
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
        //

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    /** 统一检查权限 */
    private void initCheckPermission() {

        String[] permission = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };

        //Android 13时 检查 Manifest.permission.POST_NOTIFICATIONS、READ_MEDIA_VIDEO权限等
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = new String[]{
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO,
            };
        }

        if(PermissionUtil.getInstance().checkPermission(this, permission)) {
            ActivityCompat.requestPermissions(MainActivity.this, permission, REQUEST_CODE_NEED_RUNNING_PERMISSION);
        }

    }

    /** 初始化主页内容 */
    @SuppressLint("SetTextI18n")
    private void initMainView(){
        getViewDataBinding().rlPlayController.setVisibility(View.VISIBLE);
        getViewDataBinding().rlNewPlayController.setVisibility(View.VISIBLE);
        getViewDataBinding().clMain.setVisibility(View.VISIBLE);
        getViewDataBinding().clAlbumDetail.setVisibility(View.VISIBLE);
        getViewDataBinding().clControllerMode.setVisibility(View.VISIBLE);
        getViewDataBinding().clNewCurrentMusicList.setVisibility(View.VISIBLE);

        getViewDataBinding().pbLoadingMusic.setVisibility(View.INVISIBLE);
        getViewDataBinding().tvVersion.setVisibility(View.GONE);

        //动画：初始化将详细页面移走
        ObjectAnimator detailPanelDefault = MyAnimationUtil.objectAnimatorLeftOrRight(this, false, false, getViewDataBinding().clAlbumDetail);
        detailPanelDefault.start();

        String controllerScene = SPUtil.getStrValue(MainActivity.this, SPUtil.SaveControllerScene);

        //动画：初始化将新版音乐控制器移走
        ObjectAnimator newPlayController = MyAnimationUtil.objectAnimatorUpOrDown(MainActivity.this, true, PxUtil.getInstance().dp2px(85, this), getViewDataBinding().rlNewPlayController);
        //动画：初始化将新版播放列表移走
        ObjectAnimator newCurrentMusicList = MyAnimationUtil.objectAnimatorUpOrDown(MainActivity.this, true, PxUtil.getInstance().dp2px(325, this), getViewDataBinding().clNewCurrentMusicList);

        if(!TextUtils.isEmpty(controllerScene)) {
            if(controllerScene.equals("NewScene")) {
                //隐藏默认音乐控制界面
                musicControllerAnimator = MyAnimationUtil.objectAnimatorUpOrDown(this, true, PxUtil.getInstance().dp2px(300, this), getViewDataBinding().rlPlayController);
                musicControllerAnimator.start();
                //有场景值时显示新版播放列表
                newPlayController = MyAnimationUtil.objectAnimatorUpOrDown(MainActivity.this, false, PxUtil.getInstance().dp2px(85, this), getViewDataBinding().rlNewPlayController);
            }
        }

        newPlayController.start();
        newCurrentMusicList.start();

        initDeviceMate();
        //获取设备高度
        DisplayMetrics dm = SystemUtil.getInstance().getDM(this);
        heightPixels = (dm != null)? PxUtil.getInstance().dp2px(dm.heightPixels, this) : 750;
        //
        controllerModeAnimator = MyAnimationUtil.objectAnimatorUpOrDown(this, true, heightPixels, getViewDataBinding().clControllerMode);
        controllerModeAnimator.start();

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

        //长按移动列表其中一个item
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(playMusicListAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(getViewDataBinding().rvPlayList);

        ItemTouchHelper.Callback touchNewCallback = new ItemTouchHelperCallback(playMusicListAdapter);
        ItemTouchHelper touchNewHelper = new ItemTouchHelper(touchNewCallback);
        touchNewHelper.attachToRecyclerView(getViewDataBinding().rvNewPlayList);

        getViewDataBinding().rvNewPlayList.setLayoutManager(new LinearLayoutManager(this));
        getViewDataBinding().rvNewPlayList.setAdapter(playMusicListAdapter);
        playMusicListAdapter.notifyDataSetChanged();

        Glide.with(getApplication())
                .setDefaultRequestOptions(requestOptions)
                .load(getResources().getIdentifier("ic_music_3", "mipmap", getPackageName()))
                .transform(new RoundedCornersTransformation(30, 0, RoundedCornersTransformation.CornerType.ALL))
                .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(getViewDataBinding().ivMusicImg);

        getViewDataBinding().tvListSize.setText("("+ playList.size() + ")");
        getViewDataBinding().tvNewListSize.setText("("+ playList.size() + ")");

        //歌词滚动
        lyricScrollView = getViewDataBinding().lvShowLyric;
        lyricNewScrollDetailView = getViewDataBinding().lvNewShowLyricDetail;
        lyricNewScrollView = getViewDataBinding().lvNewShowLyric;

    }

    /** 适配机型ui */
    private void initDeviceMate() {
        if(SystemUtil.getInstance().isSmallScaleDevice()) {
            // 设置宽度和高度
            int widthInDp = 120;  // 宽度（以dp为单位）
            int heightInDp = 120; // 高度（以dp为单位）

            // 将dp单位转换为像素
            int widthInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthInDp, getResources().getDisplayMetrics());
            int heightInPx = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightInDp, getResources().getDisplayMetrics());

            // 创建LayoutParams并设置宽度和高度
            ViewGroup.LayoutParams layoutParams = getViewDataBinding().civMusicImg.getLayoutParams();
            layoutParams.width = widthInPx;
            layoutParams.height = heightInPx;
            getViewDataBinding().civMusicImg.setLayoutParams(layoutParams);
        }
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
                //Log.i("PageScrolled", "position: " + position + " positionOffset: " + positionOffset + " positionOffsetPixels: " + positionOffsetPixels);
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
        getViewDataBinding().sbNewMusicBar.setOnSeekBarChangeListener(new MusicBarChangerListener());
        getViewDataBinding().sbMusicBar.setOnTouchListener(new ProgressBarTouchListener());
        getViewDataBinding().sbNewMusicBar.setOnTouchListener(new ProgressBarTouchListener());
        getViewDataBinding().rlPlayController.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().rlShowLoading.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().rlDisableClick.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().rlDisableClick3.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().llAllPlay.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().llSearch.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().llSort.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().llCancel.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().clControllerMode.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().flCloseControllerMode.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().flNewChangeMode.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().rlNewPlayController.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().ivNewMyFavorite.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().lvNewShowLyric.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().lvNewShowLyricDetail.setOnClickListener(new ButtonClickListener());
        getViewDataBinding().lvNewShowLyricDetail.setIsUseLyricDetailColor(true);
        getViewDataBinding().lvNewShowLyricDetail.setOnLyricClickListener(new LyricScrollView.LyricClickListener() {
            @Override
            public void onSingleTap() {
                showOrHideNewLyricDetailView(false);
            }
        });
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
                        getViewDataBinding().tvNoSearchMusic.setVisibility(View.GONE);
                    } else {
                        musicList.clear();
                        musicListAdapter.notifyDataSetChanged();
                        getViewDataBinding().tvNoSearchMusic.setVisibility(View.VISIBLE);
                    }
                } else {
                    musicList.clear();
                    if(tempMusicList!=null) {
                        musicList.addAll(tempMusicList);
                    }
                    musicListAdapter.notifyDataSetChanged();
                    getViewDataBinding().tvNoSearchMusic.setVisibility(View.GONE);
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
                        if(SystemUtil.getInstance().isCanDrawOverlays(getApplication())) {
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

                            if(uri != null) {
                                SPUtil.setStrValue(context,SPUtil.BackgroundUri,uri.toString());
                            }

                            Glide.with(getApplication())
                                    .setDefaultRequestOptions(requestOptions)
                                    .load(uri)
                                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                                    .into(getViewDataBinding().ivBg);
                            Toast.makeText(context, "设置成功", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.i(TAG, "e: " + e.getMessage());
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
        }
        initNotificationHelper("LLMusic", "Singer", "");
        bindService(intentService, conn, BIND_AUTO_CREATE);
    }

    /** 初始化音乐服务 */
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

    /** 初始化角色服务 */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startCharacterService(String characterName) {
        intentCharacterService.putExtra("IsPlayMusic", isPlay);
        intentCharacterService.putExtra("CharacterName", characterName);
        startForegroundService(intentCharacterService);
    }

    /** 初始化通知栏消息 */
    @SuppressLint("RemoteViewLayout")
    private void initNotificationHelper(String musicName, String musicSinger, String imgUrl) {
        if(!imgUrl.equals("")) {
            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.SHOW_IMAGE_URL, musicName, musicSinger, imgUrl));
        } else {
            NotificationHelper.getInstance().createRemoteViews(this, musicName, musicSinger, null, true);
            updateWidgetUI(false);
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
        String strBackgroundUri = SPUtil.getStrValue(getApplicationContext(), SPUtil.BackgroundUri);
        if(null != strBackgroundUri && !"".equals(strBackgroundUri)) {
            Glide.with(getApplication())
                    .setDefaultRequestOptions(requestOptions)
                    .load(Uri.parse(strBackgroundUri))
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                    .into(getViewDataBinding().ivBg);
        }

        String strThemeId = SPUtil.getStrValue(getApplicationContext(), SPUtil.SaveThemeId);
        if(strThemeId!=null) {
            if(!strThemeId.equals("")) {
                rThemeId = Integer.parseInt(strThemeId);
                changeTheme(rThemeId);
                lyricScrollView.setThemeId(rThemeId);
                lyricNewScrollDetailView.setThemeId(rThemeId);
                lyricNewScrollView.setThemeId(rThemeId);
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_CHANGE_THEME));
            }
        }
        //创建媒体MediaSession框架 用于蓝牙耳机按钮控制
        createMediaSession();
    }

    /** 创建媒体MediaSession框架 */
    private void createMediaSession() {
        if (mSession == null) {
            mSession = new MediaSession(context, MainActivity.class.getSimpleName());
            mSession.setCallback(mSessionCallback);
            mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

            mSession.setActive(true);
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
                Log.i("MYSQL", "mysql connect success");
                if (!isSelect) {
                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_DATA_APP_VERSION));
                    //每日推荐
                    MainVM.showRecommendData(getApplicationContext());
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
                isClickLocalOrFavorite = false;
                break;
            case ThreadEvent.GET_ALBUM_SUCCESS:
                if(ThreadEvent.ALBUM_LIELLA.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    updateMusicDetailMessage("Liella!", "Liella!", "LoveLive!Superstar!!", getViewDataBinding().ivLogo, R.mipmap.ic_album_liella_3, 120, 80);
                } else if(ThreadEvent.ALBUM_FOUR_YUU.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    updateMusicDetailMessage("Liyuu", "Liyuu", "Liyuu", getViewDataBinding().ivLogo, R.mipmap.ic_album_liyuu, 120, 80);
                } else if(ThreadEvent.ALBUM_SUNNY_PASSION.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    updateMusicDetailMessage("サニーパッション", "サニーパッション", "SunnyPassion", getViewDataBinding().ivLogo, R.mipmap.ic_album_sunny_passion, 120, 80);
                } else if(ThreadEvent.ALBUM_NIJIGASAKI.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    updateMusicDetailMessage("虹ヶ咲学園スクールアイドル同好会", "虹ヶ咲学園スクールアイドル同好会", "Nijigasaki HighSchool IdolClub", getViewDataBinding().ivLogo, R.mipmap.ic_album_nijigasaki_3, 120, 80);
                } else if(ThreadEvent.ALBUM_AQOURS.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    updateMusicDetailMessage("Aqours", "Aqours", "LoveLive!Sunshine!!", getViewDataBinding().ivLogo, R.mipmap.ic_album_aqours_3, 120, 80);
                } else if(ThreadEvent.ALBUM_US.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    updateMusicDetailMessage("μ's", "μ's", "LoveLive!", getViewDataBinding().ivLogo, R.mipmap.ic_album_us_3, 120, 80);
                } else if(ThreadEvent.ALBUM_HASUNOSORA.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    updateMusicDetailMessage("蓮ノ空女学院スクールアイドルクラブ", "蓮ノ空女学院スクールアイドルクラブ", "Hasunosora Jogakuin School Idol Club", getViewDataBinding().ivLogo, R.mipmap.ic_album_hasu_2, 120, 80);
                } else if(ThreadEvent.ALBUM_SAINT_SNOW.equals(event.str)) {
                        getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                        updateMusicDetailMessage("セイントスノー", "セイントスノー", "Saint Snow", getViewDataBinding().ivLogo, R.mipmap.ic_album_saint_snow_2, 120, 80);
                } else if(ThreadEvent.ALBUM_A_RISE.equals(event.str)) {
                    getViewDataBinding().rlShowLoading.setVisibility(View.VISIBLE);
                    updateMusicDetailMessage("アライズ", "アライズ", "A-RISE", getViewDataBinding().ivLogo, R.mipmap.ic_album_a_rise_2, 120, 50);
                }
                isClickLocalPlayList = false;
                break;
            case ThreadEvent.GET_LOCAL_PLAY_LIST_SUCCESS:
                updateMusicDetailMessage(event.str, event.str, "", getViewDataBinding().ivLogo, R.drawable.ic_music_cover, 80, 80);
                if(event.byteArray != null && event.byteArray.length >0) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(event.byteArray, 0, event.byteArray.length);
                    getViewDataBinding().ivLogo.setImageBitmap(bitmap);
                }
                getViewDataBinding().tvCount.setText("" + event.i);
                getViewDataBinding().tvMusicCount.setText("" + event.i);
                musicListSize = event.i;
                clickLocalPlayListId = event.i2;
                isClickLocalPlayList = true;
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
                    if(getAppVersionCode(this) < versionCode) { //判断当前是否有新版本
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
                break;
            case ThreadEvent.PLAY_FINISH_SUCCESS:
                String tsw = SPUtil.getStrValue(context, SPUtil.TaskAfterMusicSwitch);
                if(!TextUtils.isEmpty(tsw) && tsw.equals("1")) {
                    SPUtil.setStrValue(context, SPUtil.TaskAfterMusicSwitch, "0");
                    return;
                }
                if(playList.size()>0) {
                    //判断播放模式
                    switch (playMode) {
                        case 0: //顺序播放
                            Log.i("ABMediaPlay","0");
                            boolean isInto = false;
                            for (int i = 0; i < playList.size(); i++) {
                                if (playList.get(i).isPlaying) {
                                    if(i+1<playList.size()) {
                                        isInto = true;
                                        playList.get(i).isPlaying = false;
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
                            //变更主题
                            ThemeHelper.getInstance().playButtonTheme(rThemeId, getViewDataBinding());
                            if (objectAnimator != null) {
                                objectAnimator.pause();
                            }
                            break;
                        case 1: //随机播放
                            Log.i("ABMediaPlay","1");
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
                            Log.i("ABMediaPlay","2");
                            break;
                    }
                } else {
                    //变更主题
                    ThemeHelper.getInstance().playButtonTheme(rThemeId, getViewDataBinding());
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
            case ThreadEvent.PLAY_RECOMMEND_MUSIC:   //点击播放每日推荐的歌曲 并添加到播放列表
            case ThreadEvent.PLAY_LOCAL_MUSIC:   //点击播放本地歌曲 并添加到播放列表
                if(event.music != null) {
                    List<Music> list = new ArrayList<>();
                    list.add(event.music);
                    playMusic(list, 0);
                }
                break;
            case ThreadEvent.ADD_LOCAL_MUSIC:   //点击添加本地歌曲到播放列表
                if(event.music != null) {
                    List<Music> list = new ArrayList<>();
                    list.add(event.music);
                    addMusic(list, 0);
                }
                break;
            case ThreadEvent.PLAY_FAVORITE_MUSIC:    //点击播放收藏歌曲 并添加到播放列表
                if(event.tList != null & event.tList.size()>0) {
                    List<Music> list = event.tList;
                    playMusic(list, event.i);
                }
                break;
            case ThreadEvent.ADD_FAVORITE_MUSIC:     //点击添加收藏歌曲到播放列表
                if(event.tList != null & event.tList.size()>0) {
                    List<Music> list = event.tList;
                    addMusic(list, event.i);
                }
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
                lyricNewScrollDetailView.setMusicPlayerPos(event.i);
                lyricNewScrollView.setMusicPlayerPos(event.i);
                getViewDataBinding().sbMusicBar.setProgress(event.i);
                getViewDataBinding().sbNewMusicBar.setProgress(event.i);
                getViewDataBinding().hpvProgress.setCurrentCount(event.i);
                getViewDataBinding().pbNewProgress.setProgress(event.i);
                break;
            case ThreadEvent.VIEW_SEEK_BAR_RESUME:
                lyricScrollView.posLock(true);
                lyricNewScrollDetailView.posLock(true);
                lyricNewScrollView.posLock(true);
                getViewDataBinding().sbMusicBar.setProgress(0);
                getViewDataBinding().sbNewMusicBar.setProgress(0);
                getViewDataBinding().pbLoadingMusic.setVisibility(View.VISIBLE);
                updateWidgetUI(true);
                break;

            case ThreadEvent.VIEW_PAUSE:
                isPlay = !event.b;
                lyricScrollView.posLock(event.b);
                lyricNewScrollDetailView.posLock(event.b);
                lyricNewScrollView.posLock(event.b);
                //角色服务存在时 对角色服务做处理
                if(SystemUtil.getInstance().isServiceWorked(this, CharacterService.class.getPackage().getName()
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

                //变更主题
                ThemeHelper.getInstance().playButtonStatusTheme(rThemeId, getViewDataBinding(), event.b);

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
                getViewDataBinding().tvNewListSize.setText("("+ playList.size() + ")");
                break;
            case ThreadEvent.VIEW_MUSIC_MSG:
                lyricScrollView.initView();
                lyricNewScrollView.initView();
                lyricNewScrollDetailView.initView();

                String defaultSize = SPUtil.getStrValue(MainActivity.this, SPUtil.DefaultLyricSizeData);
                if(!TextUtils.isEmpty(defaultSize)) {
                    int defaultSizeInt = Integer.parseInt(defaultSize);
                    lyricScrollView.setLyricSize(defaultSizeInt);
                }

                String singleSize = SPUtil.getStrValue(MainActivity.this, SPUtil.SingleLyricSizeData);
                if(!TextUtils.isEmpty(singleSize)) {
                    int singleSizeInt = Integer.parseInt(singleSize);
                    lyricNewScrollView.setLyricSize(singleSizeInt);
                }

                String detailSize = SPUtil.getStrValue(MainActivity.this, SPUtil.DetailLyricSizeData);
                if(!TextUtils.isEmpty(detailSize)) {
                    int detailSizeInt = Integer.parseInt(detailSize);
                    lyricNewScrollDetailView.setLyricSize(detailSizeInt);
                }

                getViewDataBinding().hpvProgress.setMaxCount(event.i);
                getViewDataBinding().pbNewProgress.setMax(event.i);

                getViewDataBinding().pbLoadingMusic.setVisibility(View.INVISIBLE);
                getViewDataBinding().sbMusicBar.setMax(event.i);
                getViewDataBinding().sbNewMusicBar.setMax(event.i);
                getViewDataBinding().tvAllTime.setText(getViewModel().rebuildTime(event.i));
                getViewDataBinding().tvNewAllTime.setText(getViewModel().rebuildTime(event.i));
                String musicName = event.music.musicName;
                getViewDataBinding().tvMusicName.setText(musicName);
                getViewDataBinding().tvNewMusicName.setText(musicName);
                getViewDataBinding().tvNewPlayMusicName.setText(musicName);
                getViewDataBinding().tvSingerName.setText(event.music.musicSinger);
                getViewDataBinding().tvNewSingerName.setText(event.music.musicSinger);
                getViewDataBinding().tvListSize.setText("(" + playList.size() + ")");
                getViewDataBinding().tvNewListSize.setText("(" + playList.size() + ")");
                currentMusicDetail = event.music;
                currentMusicImg = event.music.getMusicImg();
                currentMusicName = event.music.musicName;

                //刷新播放列表的收藏歌曲显示ui
                EventBus.getDefault().post(new ThreadEvent<>(ThreadEvent.VIEW_FRESH_FAVORITE_MUSIC));

                Glide.with(context)
                        .setDefaultRequestOptions(requestOptions)
                        .load(event.music.isLocal?
                                (null != event.music.musicImgByte?
                                        BitmapFactory.decodeByteArray(event.music.musicImgByte, 0, event.music.musicImgByte.length) : R.mipmap.ic_llmp_2) : event.music.getMusicImg()
                        )
                        .transform(new RoundedCornersTransformation(20, 0, RoundedCornersTransformation.CornerType.ALL))
                        .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
                        .into(getViewDataBinding().civMusicImg);

                //变更主题
                ThemeHelper.getInstance().musicBarMusicImgTheme(this, rThemeId, getViewDataBinding(), event.music);

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
                            currentMusicName = event.music.musicName;
                            currentMusicSinger = event.music.musicSinger;
                            currentBitmap = null;
                            startMusicService(true, event.music.musicName, event.music.musicSinger, null);
                            updateWidgetUI(false);
                        }
                    } else {
                        if(!event.music.musicImg.equals("")) {
                            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.SHOW_IMAGE_URL, event.music.musicName, event.music.musicSinger, event.music.musicImg, null, false));
                        } else {
                            currentMusicName = event.music.musicName;
                            currentMusicSinger = event.music.musicSinger;
                            currentMusicImg = "";
                            startMusicService(true, event.music.musicName, event.music.musicSinger, null);
                            updateWidgetUI(false);
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

                updateWidgetUI(false);
                break;
            case ThreadEvent.VIEW_LYRIC:
                if(null != event.tList) {
                    //将歌词数据方法lyricScrollView显示
                    lyricScrollView.setMusicLyrics(event.tList);
                    lyricNewScrollDetailView.setMusicLyrics(event.tList);
                    lyricNewScrollView.setMusicLyrics(event.tList);
                    musicLyricList.clear();
                    musicLyricList.addAll(event.tList);
                }
                if(binder !=null) {
                    binder.player(event.music, event.b, proxyCacheServer, musicLyricList);
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
                    Log.i("ABMusicPlayer","file: " + event.file.toString());
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
            case ThreadEvent.VIEW_COUNT_DOWN_REFRESH:  //倒计时刷新
                if(dialogTimeTasksBinding != null) {
                    dialogTimeTasksBinding.tvTitleDetailTime.setText(event.str);
                    dialogTimeTasksBinding.pbLoadingTask.setVisibility(View.GONE);
                }
                break;
            case ThreadEvent.VIEW_COUNT_DOWN_FINISH:
                if(dialogTimeTasksBinding != null) {
                    isCountDown = false;
                    dialogTimeTasksBinding.tvTitleDetailTime.setText("00:00");
                    dialogTimeTasksBinding.pbLoadingTask.setVisibility(View.GONE);
                    dialogTimeTasksBinding.tvTitleDetail.setVisibility(View.VISIBLE);
                    dialogTimeTasksBinding.llTitleDetail.setVisibility(View.GONE);
                    dialogTimeTasksBinding.ivTasksClose.setVisibility(View.VISIBLE);
                    dialogTimeTasksBinding.ivTimeTasks1.setVisibility(View.GONE);
                    dialogTimeTasksBinding.ivTimeTasks2.setVisibility(View.GONE);
                    dialogTimeTasksBinding.ivTimeTasks3.setVisibility(View.GONE);
                    dialogTimeTasksBinding.ivTimeTasks4.setVisibility(View.GONE);
                    dialogTimeTasksBinding.ivTimeTasksCustom.setVisibility(View.GONE);
                }
                String taskSwitch = SPUtil.getStrValue(context, SPUtil.TaskAfterMusicSwitch);
                if(!TextUtils.isEmpty(taskSwitch) && taskSwitch.equals("1")) {
                    return;
                }
                if(binder!=null) {
                    binder.pauseImm(this, currentMusicName, currentMusicSinger, currentBitmap);
                }
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

            case ThreadEvent.VIEW_SHOW_OR_HIDE_MASKING_BACKGROUND:
                getViewDataBinding().rlDisableClick2.setVisibility(event.b? View.VISIBLE : GONE);
                break;
            case ThreadEvent.VIEW_ADD_MUSIC_TO_LOCAL_PLAY_LIST_SUCCESS:
                Toast.makeText(getApplication(), "添加成功", Toast.LENGTH_SHORT).show();
                break;
            case ThreadEvent.VIEW_DELETE_MUSIC_TO_LOCAL_PLAY_LIST_SUCCESS:
                if(musicListSize>0) {
                    --musicListSize;
                    getViewDataBinding().tvMusicCount.setText(""+ (musicListSize));
                    getViewDataBinding().tvCount.setText(""+ (musicListSize));
                }
                break;
            case ThreadEvent.VIEW_CLICK_LOCAL_OR_FAVORITE:
                isClickLocalOrFavorite = true;
                break;
            case ThreadEvent.VIEW_SHOW_VISUALIZER:
                //getViewDataBinding().ccvShowVisualizer.startVisualizer();
                break;
            case ThreadEvent.VIEW_SHOW_STOP_VISUALIZER:
                //getViewDataBinding().ccvShowVisualizer.posLock(event.b);
                break;
            case ThreadEvent.VIEW_CONTROLLER_MODE:
                Log.i(TAG, "rlPlayController h: " + getViewDataBinding().rlPlayController.getHeight());
                int moveAxis =  getViewDataBinding().clControllerMode.getHeight() == 0? heightPixels :  getViewDataBinding().clControllerMode.getHeight();

                //隐藏默认音乐控制界面
                musicControllerAnimator = MyAnimationUtil.objectAnimatorUpOrDown(this, true, getViewDataBinding().rlPlayController.getHeight(), getViewDataBinding().rlPlayController);
                musicControllerAnimator.start();

                controllerModeAnimator = MyAnimationUtil.objectAnimatorUpOrDown(this, isShowControllerModePanel, moveAxis, getViewDataBinding().clControllerMode);
                controllerModeAnimator.start();

                getViewDataBinding().rlDisableClick.setVisibility(View.GONE);
                getViewDataBinding().clBgMode.setVisibility(View.GONE);
                isShowControllerModePanel = !isShowControllerModePanel;
                SPUtil.setStrValue(this, SPUtil.SaveControllerScene, SPUtil.SaveControllerSceneValue_NewScene);
                break;

            case ThreadEvent.VIEW_FRESH_FAVORITE_MUSIC:
                List<Music> spTempList = SPUtil.getListValue(LLActivityManager.getInstance().getTopActivity(), SPUtil.FavoriteListData, Music.class);
                if(spTempList.size() >0){
                    setMusicFavorite(spTempList);
                }

                playMusicListAdapter.notifyDataSetChanged();
                //刷新新播放界面是否收藏歌曲
                setCurrentMusicFavorite(spTempList, currentMusicName);
                break;
        }
    }

    /** 更新专辑详细信息 */
    private void updateMusicDetailMessage(String titleName, String msgName1, String msgName2, ImageView imageView, int resId, int width, int height) {
        getViewDataBinding().ivLogo.setImageBitmap(null);
        getViewDataBinding().ivLogo.setBackgroundResource(resId);
        ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
        layoutParams.width = PxUtil.getInstance().dp2px(width, context);
        layoutParams.height = PxUtil.getInstance().dp2px(height, context);
        getViewDataBinding().ivLogo.setLayoutParams(layoutParams);

        getViewDataBinding().tvTitleBar.setText(titleName);
        getViewDataBinding().tvListMsgName1.setText(msgName1);
        getViewDataBinding().tvListMsgName2.setText(msgName2);
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
                }
                getViewModel().downloadUrl(event.str);
                break;
            case ThreadEvent.GET_DATA_LIST:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicAll()));
                break;
            case ThreadEvent.GET_DATA_LIST_COUNT:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicCount()));
                break;
            case ThreadEvent.GET_DATA_APP_VERSION:
                EventBus.getDefault().post(new ThreadEvent<Version>(ThreadEvent.GET_APP_VERSION_SUCCESS, MysqlHelper.getInstance().findVersionSql(),""));
                break;
            case ThreadEvent.GET_DATA_RECOMMEND:
                EventBus.getDefault().post(new ThreadEvent<>(ThreadEvent.GET_RECOMMEND_SUCCESS, MysqlHelper.getInstance().findMusicByRandomSql(3)));
                break;
            case ThreadEvent.GET_DATA_LIST_MESSAGE:
                EventBus.getDefault().post(new ThreadEvent<Message>(ThreadEvent.GET_MESSAGE_SUCCESS, MysqlHelper.getInstance().findMessageSql(), ""));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_LIELLA:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_LIELLA));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql(MUSIC_TYPE_LIELLA)));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount(MUSIC_TYPE_LIELLA)));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_FOUR_YUU:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_FOUR_YUU));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql(MUSIC_TYPE_LIYUU)));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount(MUSIC_TYPE_LIYUU)));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_SUNNY_PASSION:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_SUNNY_PASSION));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql(MUSIC_TYPE_SUNNYPASSION)));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount(MUSIC_TYPE_SUNNYPASSION)));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_NIJIGASAKI:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_NIJIGASAKI));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql(MUSIC_TYPE_NIJIGASAKI)));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount(MUSIC_TYPE_NIJIGASAKI)));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_AQOURS:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_AQOURS));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql(MUSIC_TYPE_AQOURS)));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount(MUSIC_TYPE_AQOURS)));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_US:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_US));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql(MUSIC_TYPE_US)));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount(MUSIC_TYPE_US)));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_HASUNOSORA:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_HASUNOSORA));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeSql(MUSIC_TYPE_HASUNOSORA)));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeCount(MUSIC_TYPE_HASUNOSORA)));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_SAINT_SNOW:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_SAINT_SNOW));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeAndMusicSingerSql(MUSIC_TYPE_AQOURS, MUSIC_TYPE_SAINT_SNOW)));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeAndMusicSingerCount(MUSIC_TYPE_AQOURS, MUSIC_TYPE_SAINT_SNOW)));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_A_RISE:
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_ALBUM_SUCCESS, ThreadEvent.ALBUM_A_RISE));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeAndMusicSingerSql(MUSIC_TYPE_US, MUSIC_TYPE_A_RISE)));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_COUNT_SUCCESS, MysqlHelper.getInstance().findMusicByMusicTypeAndMusicSingerCount(MUSIC_TYPE_US, MUSIC_TYPE_A_RISE)));
                break;
            case ThreadEvent.GET_DATA_LIST_BY_LOCAL_PLAY:
                if(event.t != null) {
                    LocalPlayList localPlayList = (LocalPlayList) event.t;
                    List<Music> musicList1 = new ArrayList<>();
                    if(localPlayList.getMusicList() != null && localPlayList.getMusicList().size() >0) {
                        musicList1.addAll(localPlayList.getMusicList());
                    }
                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_SUCCESS, musicList1));
                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.GET_LOCAL_PLAY_LIST_SUCCESS, localPlayList.getPlayListName(), localPlayList.getPlayListCount(), localPlayList.getPlayListId(), localPlayList.getPlayListImgByte()));
                }
                break;
            case ThreadEvent.GET_MUSIC_LYRIC:
                getViewModel().showLyric(event.music, event.b);
                break;
            case ThreadEvent.SHOW_IMAGE_URL:  //设置状态栏显示对应图片
                if(event.b) {
                    getViewModel().showImageBitmap(event.str, event.str2, event.bitmap);
                } else {
                    getViewModel().showImageURL(event.str, event.str2, event.str3);
                }
                break;
            case ThreadEvent.ADD_MUSIC_TO_LOCAL_PLAY_LIST:   //添加在线音乐到自建歌单里面
                if(event.music != null) {
                    List<LocalPlayList> localPlayLists = SPUtil.getListValue(this, SPUtil.LocalPlayListData, LocalPlayList.class);
                    if(localPlayLists.size() >0) {
                        for(int i=0; i<localPlayLists.size(); i++) {
                            if(localPlayLists.get(i).playListId == event.i) {
                                if(localPlayLists.get(i).getMusicList() != null) {
                                    List<Music> musicList1 = localPlayLists.get(i).getMusicList();
                                    musicList1.add(event.music);
                                    localPlayLists.get(i).setMusicList(musicList1);
                                    localPlayLists.get(i).setPlayListCount(localPlayLists.get(i).getMusicList().size());
                                    SPUtil.setListValue(this, SPUtil.LocalPlayListData, localPlayLists);
                                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_ADD_MUSIC_TO_LOCAL_PLAY_LIST));
                                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_ADD_MUSIC_TO_LOCAL_PLAY_LIST_SUCCESS));
                                }
                                return;
                            }
                        }
                    }
                }
                break;

            case ThreadEvent.DELETE_MUSIC_IN_LOCAL_PLAY_LIST:  //删除自建歌单里面到某一首歌曲
                if(event.tList != null) {
                    List<LocalPlayList> localPlayLists = SPUtil.getListValue(this, SPUtil.LocalPlayListData, LocalPlayList.class);
                    if (localPlayLists.size() > 0) {
                        for (int i = 0; i < localPlayLists.size(); i++) {
                            if (localPlayLists.get(i).playListId == event.i) {
                                if (localPlayLists.get(i).getMusicList() != null) {
                                    localPlayLists.get(i).setMusicList(event.tList);
                                    localPlayLists.get(i).setPlayListCount(localPlayLists.get(i).getPlayListCount()-1);
                                    SPUtil.setListValue(getApplicationContext(), SPUtil.LocalPlayListData, localPlayLists);
                                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_DELETE_MUSIC_TO_LOCAL_PLAY_LIST));
                                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_DELETE_MUSIC_TO_LOCAL_PLAY_LIST_SUCCESS));
                                }
                                return;
                            }
                        }
                    }
                }
                break;

            case ThreadEvent.SAVE_LOCAL_MUSIC_LIST:  //在子线程中保存列表数据
                List<Music> list = event.musicList;
                SPUtil.setListValue(context, SPUtil.PlayListData, list);
                break;
            case ThreadEvent.VIEW_SCREEN_LOCK:
                if(binder != null && binder.isPlay()) {
                    NotificationHelper.getInstance().createFullScreen(getApplicationContext());
                } else {
                    NotificationHelper.getInstance().cancelNotification(getApplicationContext(), NotificationHelper.LL_MUSIC_FULL_SCREEN);
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
                Log.i("ABMediaPlay", "clickController");
            } else if(v.getId() == R.id.rl_show_loading) {
                Log.i("ABMediaPlay", "showLoading");
            } else if (v.getId() == R.id.rl_disable_click) {
                Log.i("ABMediaPlay", "clickDisableView");
                hideAllMusicView();
            } else if (v.getId() == R.id.rl_disable_click_3) {
                showOrHideNewMusicPlayerList();
            } else if (v.getId() == R.id.ll_all_play) {
                if(isDoubleClick()) { return; }
                allPlayMusic();
            } else if (v.getId() == R.id.ll_search) {
                if(isDoubleClick()) { return; }
                isSearchMusic = true;
                searchMusic();
            } else if (v.getId() == R.id.ll_sort) {
                if(isDoubleClick()) { return; }
                showSortMenuDialog(v);
            } else if (v.getId() == R.id.ll_cancel) {
                if(isDoubleClick()) { return; }
                isSearchMusic = false;
                searchCancel();
            } else if (v.getId() == R.id.ll_settings) {
                intoSettings();
            } else if(v.getId() == R.id.ll_back) {
                if(isSearchMusic) {
                    searchCancel();
                    isSearchMusic = false;
                }
                ObjectAnimator mainPanelChangeObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(MainActivity.this, true, true, getViewDataBinding().clMain);
                ObjectAnimator detailPanelChangeObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(MainActivity.this, false, false, getViewDataBinding().clAlbumDetail);
                mainPanelChangeObjectAnimator.start();
                detailPanelChangeObjectAnimator.start();
                isNotMain = false;
            } else if(v.getId() == R.id.fl_new_change_mode) {
                controllerModeAnimator = MyAnimationUtil.objectAnimatorUpOrDown(MainActivity.this, true, getViewDataBinding().clControllerMode.getHeight(), getViewDataBinding().clControllerMode);
                controllerModeAnimator.start();

                musicControllerAnimator = MyAnimationUtil.objectAnimatorUpOrDown(MainActivity.this, false, getViewDataBinding().rlPlayController.getHeight(), getViewDataBinding().rlPlayController);
                musicControllerAnimator.start();

                if(isClick) { //是否点击了旧版的播放器 按钮其中一个
                    getViewDataBinding().rlDisableClick.setVisibility(View.VISIBLE);
                }
                isShowControllerModePanel = false;
                SPUtil.setStrValue(MainActivity.this, SPUtil.SaveControllerScene, SPUtil.SaveControllerSceneValue_DefaultScene);
            } else if(v.getId() == R.id.fl_close_controller_mode) {  //关闭新版音乐播放页面
                showOrHideNewController(true);
            } else if(v.getId() == R.id.rl_new_play_controller) { //打开新版音乐播放页面
                showOrHideNewController(false);
            } else if(v.getId() == R.id.iv_new_my_favorite) {
                if(currentMusicDetail != null) {
                    if(!isFavorite) {
                        EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_ADD_FAVORITE_MUSIC, currentMusicDetail));
                    } else {
                        EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_CANCEL_FAVORITE_MUSIC, currentMusicDetail));
                    }
                }
            } else if(v.getId() == R.id.lv_new_show_lyric) {
                showOrHideNewLyricDetailView(true);
            } else if(v.getId() == R.id.lv_new_show_lyric_detail) {
                showOrHideNewLyricDetailView(false);
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

    /** 更新小组件UI */
    private void updateWidgetUI(boolean isLoading) {
        if(binder != null && binder.isPlay()) {
            MusicPlayService.stopAppWidgetRunnable();
            MusicPlayService.appWidgetRunnable = new Runnable() {
                @Override
                public void run() {
                    sendWidgetBroadcastReceiver(isLoading);
                    MusicPlayService.appWidgetHandler.postDelayed(MusicPlayService.appWidgetRunnable, MusicPlayService.DELAY_MILLIS);
                }
            };

            MusicPlayService.appWidgetHandler.post(MusicPlayService.appWidgetRunnable);
        } else {
            if(MusicPlayService.stopAppWidgetRunnable()) {
                sendWidgetBroadcastReceiver(isLoading);
            }
        }

    }

    /** 发送广播给小组件 更新视图 */
    private void sendWidgetBroadcastReceiver(boolean isLoading) {
        Intent intent = new Intent("WIDGET_PROVIDER_REFRESH_MUSIC_MSG");
        intent.setPackage(getPackageName());
        intent.putExtra("IsLoading", isLoading);

        String startTime = TimeUtil.rebuildTime(MusicPlayService.mStartPosition);
        if(!TextUtils.isEmpty(startTime)) {
            intent.putExtra("StartTime", startTime);
        }
        String allTime = TimeUtil.rebuildTime(MusicPlayService.mAllPosition);
        if(!TextUtils.isEmpty(allTime)) {
            intent.putExtra("AllTime", allTime);
        }
        BigDecimal bd1 = new BigDecimal(TimeUtil.showSec(MusicPlayService.mStartPosition));
        BigDecimal bd2 = new BigDecimal(TimeUtil.showSec(MusicPlayService.mAllPosition));

        int progress = bd1.divide(bd2, 2, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)).intValue();
        intent.putExtra("MusicProgress", progress);

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
                if(!TextUtils.isEmpty(versionList.get(0).versionUrl)) {
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
        ThemeHelper.getInstance().changeTheme(this, rId, getViewDataBinding(), binder);
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

        //变更主题
        ThemeHelper.getInstance().menuPopupWindowTheme(this, rThemeId, menuPopupWindow, mainMenuBinding);

        menuPopupWindow.showAsDropDown(view,  PxUtil.getInstance().dp2px(-60, this),  PxUtil.getInstance().dp2px(10, this));

        mainMenuBinding.llSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPopupWindow.dismiss();
                intoSettings();
            }
        });
        //桌面角色
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

                //变更主题
                ThemeHelper.getInstance().characterPopupWindowTheme(v.getContext(), rThemeId, characterPopupWindow, characterMenuBinding);

                characterPopupWindow.showAsDropDown(view,  PxUtil.getInstance().dp2px(-60, v.getContext()),  PxUtil.getInstance().dp2px(10, v.getContext()));
            }
        });
        //设置背景
        mainMenuBinding.llBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuPopupWindow.dismiss();
                Intent intentPhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intentPhoto.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intentPhoto.addCategory(Intent.CATEGORY_OPENABLE);
                intentPhoto.setType("image/*");

                intentTakePhotoLauncher.launch(intentPhoto);
            }
        });
        //导入歌曲
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
        //定时关闭
        mainMenuBinding.llTimeTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menuPopupWindow.dismiss();
                dialogTimeTasksBinding = DataBindingUtil.inflate(LayoutInflater.from(context),
                        R.layout.dialog_time_tasks, null, false);

                dialogTimeTasksBinding.tvTitleDetail.setVisibility(isCountDown? View.GONE : View.VISIBLE);
                dialogTimeTasksBinding.llTitleDetail.setVisibility(isCountDown? View.VISIBLE : View.GONE);
                dialogTimeTasksBinding.pbLoadingTask.setVisibility(isCountDown? View.VISIBLE : View.GONE);
                dialogTimeTasksBinding.switchTask.setChecked(false);

                String taskSwitch = SPUtil.getStrValue(context, SPUtil.TaskAfterMusicSwitch);
                if(!TextUtils.isEmpty(taskSwitch) && taskSwitch.equals("1")) {
                    dialogTimeTasksBinding.switchTask.setChecked(true);
                }

                if(isCountDown) {
                    dialogTimeTasksBinding.ivTasksClose.setVisibility(View.GONE);
                    dialogTimeTasksBinding.ivTimeTasks1.setVisibility(selectCountDown == 10? View.VISIBLE : View.GONE);
                    dialogTimeTasksBinding.ivTimeTasks2.setVisibility(selectCountDown == 20? View.VISIBLE : View.GONE);
                    dialogTimeTasksBinding.ivTimeTasks3.setVisibility(selectCountDown == 30? View.VISIBLE : View.GONE);
                    dialogTimeTasksBinding.ivTimeTasks4.setVisibility(selectCountDown == 60? View.VISIBLE : View.GONE);
                    dialogTimeTasksBinding.ivTimeTasksCustom.setVisibility(selectCountDown == 0? View.VISIBLE : View.GONE);
                }
                dialogTimeTasksBinding.llTasksClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        clickTimeTasks(false, 0, dialogTimeTasksBinding);
                    }
                });

                dialogTimeTasksBinding.llTimeTasks1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        clickTimeTasks(true, 10, dialogTimeTasksBinding);
                    }
                });

                dialogTimeTasksBinding.llTimeTasks2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        clickTimeTasks(true, 20, dialogTimeTasksBinding);
                    }
                });

                dialogTimeTasksBinding.llTimeTasks3.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        clickTimeTasks(true, 30, dialogTimeTasksBinding);
                    }
                });

                dialogTimeTasksBinding.llTimeTasks4.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        clickTimeTasks(true, 60, dialogTimeTasksBinding);
                    }
                });

                dialogTimeTasksBinding.llTimeTasksCustom.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        clickTimeTasks(true, 0, dialogTimeTasksBinding);
                    }
                });

                dialogTimeTasksBinding.switchTask.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        SPUtil.setStrValue(getApplicationContext(), SPUtil.TaskAfterMusicSwitch, dialogTimeTasksBinding.switchTask.isChecked()? "1" : "0");
                    }
                });

                mAlertDialog = new AlertDialog.Builder(context)
                        .setView(dialogTimeTasksBinding.getRoot())
                        .create();
                Objects.requireNonNull(mAlertDialog.getWindow()).setBackgroundDrawableResource(R.drawable.shape_button_white_2);
                mAlertDialog.show();
            }
        });

    }

    /**
     * 点击选择定时
     * */
    private void clickTimeTasks(boolean isStart, int time, DialogTimeTasksBinding binding) {
        if(!isStart) {
            cancelTimeTasks(binding);
        } else {
            if(time != 0) {
                sec = time * 60;
            } else {
                //自定义时间
                DialogTimePickerBinding dialogTimePickerBinding = DataBindingUtil.inflate(LayoutInflater.from(context),
                        R.layout.dialog_time_picker, null, false);

                dialogTimePickerBinding.tpSelect.setIs24HourView(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    dialogTimePickerBinding.tpSelect.setHour(0);
                    dialogTimePickerBinding.tpSelect.setMinute(1);
                }
                hour = 0;
                minute = 1;
                dialogTimePickerBinding.tpSelect.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                    @Override
                    public void onTimeChanged(TimePicker timePicker, int h, int m) {
                        hour = h;
                        minute = m;
                    }
                });
                dialogTimePickerBinding.btSelectIconCommit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(hour == 0 && minute == 0) {
                            Toast.makeText(context, "请设置一个正确的时间值", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        mPopupWindow.dismiss();
                        sec = hour * 60 * 60 + minute * 60;
                        setTimeTasks(time, sec, binding);
                    }
                });
                dialogTimePickerBinding.btSelectIconCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mPopupWindow.dismiss();
                        cancelTimeTasks(binding);
                    }
                });

                if(mPopupWindow != null) {
                    mPopupWindow.dismiss();
                }
                mPopupWindow  = new PopupWindow(dialogTimePickerBinding.getRoot(),
                        WindowManager.LayoutParams.WRAP_CONTENT,  WindowManager.LayoutParams.WRAP_CONTENT, true);
                mPopupWindow.setTouchable(true);
                mPopupWindow.showAtLocation(binding.getRoot(), Gravity.CENTER, 0,0);

                mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
//                        if(!isCountDown) {
//                            cancelTimeTasks(binding);
//                        }
                    }
                });
                return;
            }
            setTimeTasks(time, sec, binding);
        }
    }

    /**
     * 开始倒计时
     * */
    private void setTimeTasks(int time, int sec, DialogTimeTasksBinding binding) {
        binding.ivTasksClose.setVisibility(GONE);
        binding.ivTimeTasks1.setVisibility(time == 10? View.VISIBLE : GONE);
        binding.ivTimeTasks2.setVisibility(time == 20? View.VISIBLE : GONE);
        binding.ivTimeTasks3.setVisibility(time == 30? View.VISIBLE : GONE);
        binding.ivTimeTasks4.setVisibility(time == 60? View.VISIBLE : GONE);
        binding.ivTimeTasksCustom.setVisibility(time == 0? View.VISIBLE : GONE);
        selectCountDown = time;

        binding.tvTitleDetail.setVisibility(GONE);
        binding.llTitleDetail.setVisibility(View.VISIBLE);
        isCountDown = true;
        CountDownHelper.startCountTime(sec, new CountDownHelper.CountDownCallBack() {
            @Override
            public void showTime(int countDown, String time) {
                runOnUiThread(() -> {
                    binding.tvTitleDetailTime.setText(time);
                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_COUNT_DOWN_REFRESH, time));
                });
            }

            @Override
            public void finish() {
                runOnUiThread(() -> {
                    isCountDown = false;
                    binding.tvTitleDetailTime.setText("00:00");
                    binding.tvTitleDetail.setVisibility(View.VISIBLE);
                    binding.llTitleDetail.setVisibility(GONE);
                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_COUNT_DOWN_FINISH));
                });

            }
        });
    }

    /**
     * 取消倒计时
     * */
    private void cancelTimeTasks(DialogTimeTasksBinding binding) {
        binding.ivTasksClose.setVisibility(View.VISIBLE);
        binding.ivTimeTasks1.setVisibility(GONE);
        binding.ivTimeTasks2.setVisibility(GONE);
        binding.ivTimeTasks3.setVisibility(GONE);
        binding.ivTimeTasks4.setVisibility(GONE);
        binding.ivTimeTasksCustom.setVisibility(GONE);
        CountDownHelper.pauseImm();
        isCountDown = false;
        binding.tvTitleDetail.setVisibility(View.VISIBLE);
        binding.llTitleDetail.setVisibility(GONE);
    }


    /** 点击展示授权悬浮窗显示角色系统 */
    public void showCharacterAuth(final String characterName) {
        //判断是否已经授权显示悬浮窗
        if(SystemUtil.getInstance().isCanDrawOverlays(this)) {
             if(SystemUtil.getInstance().isServiceWorked(this, CharacterService.class.getPackage().getName()
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
        showOrHideMusicPlayerPanel();
    }

    /** 更改模式 */
    public void changeModeClick(View view) {
        final DialogChangeModeMenuBinding changeModeMenuBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.dialog_change_mode_menu, null, false);

        PopupWindow changeModePopupWindow  = new PopupWindow(changeModeMenuBinding.getRoot(),
                PxUtil.getInstance().dp2px(120, this),  WindowManager.LayoutParams.WRAP_CONTENT, true);
        changeModePopupWindow.setTouchable(true);

        changeModeMenuBinding.llControllerMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeModePopupWindow.dismiss();

               /* String[] permissions = new String[]{ Manifest.permission.RECORD_AUDIO };
                *//*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions = new String[]{ Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_AUDIO };
                }*//*
                if(PermissionUtil.getInstance().checkPermission(v.getContext(), permissions)) {
                    ActivityCompat.requestPermissions(MainActivity.this, permissions, REQUEST_CODE_CONTROLLER_MODE);
                    return;
                }*/
                EventBus.getDefault().post(new ThreadEvent<>(ThreadEvent.VIEW_CONTROLLER_MODE));
            }
        });

        changeModeMenuBinding.llBgMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeModePopupWindow.dismiss();
                if(View.VISIBLE == getViewDataBinding().clBgMode.getVisibility()) {
                    getViewDataBinding().clBgMode.setVisibility(View.GONE);
                } else {
                    getViewDataBinding().clBgMode.setVisibility(View.VISIBLE);
                }
                getViewDataBinding().rlPlayController.setVisibility(View.VISIBLE);

            }
        });

        //变更主题
        ThemeHelper.getInstance().changeModePopupWindowTheme(this, rThemeId, changeModePopupWindow, changeModeMenuBinding);
        changeModePopupWindow.showAsDropDown(view,  PxUtil.getInstance().dp2px(0, this),  PxUtil.getInstance().dp2px(-140, this));
    }

    /** 面板里面的更多菜单 */
    public void panelMoreMenu(View view) {
        DialogPanelMoreMenuBinding panelMoreMenuBinding = DataBindingUtil.inflate(LayoutInflater.from(this),
                R.layout.dialog_panel_more_menu, null, false);

        panelMoreMenuBinding.sbLyricSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int size = 25 + (i * 5);
                if(SystemUtil.getInstance().isSmallScaleDevice()) {
                    size = 15 + (i * 5);
                }
                getViewDataBinding().lvShowLyric.setLyricSize(size);
                defaultLyricSize = size;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                getViewDataBinding().lvShowLyric.setIsResetLyricSize(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getViewDataBinding().lvShowLyric.setIsResetLyricSize(false);
                SPUtil.setStrValue(MainActivity.this, SPUtil.DefaultLyricSizeData, "" + defaultLyricSize);
            }
        });

        panelMoreMenuBinding.sbSingleLyricSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int size = 25 + (i * 5);
                if(SystemUtil.getInstance().isSmallScaleDevice()) {
                    size = 15 + (i * 5);
                }
                getViewDataBinding().lvNewShowLyric.setLyricSize(size);
                singleLyricSize = size;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                getViewDataBinding().lvNewShowLyric.setIsResetLyricSize(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getViewDataBinding().lvNewShowLyric.setIsResetLyricSize(false);
                SPUtil.setStrValue(MainActivity.this, SPUtil.SingleLyricSizeData, "" + singleLyricSize);
            }
        });

        panelMoreMenuBinding.sbDetailLyricSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int size = 25 + (i * 5);
                if(SystemUtil.getInstance().isSmallScaleDevice()) {
                    size = 15 + (i * 5);
                }
                getViewDataBinding().lvNewShowLyricDetail.setLyricSize(size);
                detailLyricSize = size;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                getViewDataBinding().lvNewShowLyricDetail.setIsResetLyricSize(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getViewDataBinding().lvNewShowLyricDetail.setIsResetLyricSize(false);
                SPUtil.setStrValue(MainActivity.this, SPUtil.DetailLyricSizeData, "" + detailLyricSize);
            }
        });

        panelMoreMenuBinding.llDefaultLyricSizePanel.setVisibility(View.VISIBLE);
        panelMoreMenuBinding.llNewLyricSizePanel.setVisibility(View.GONE);
        int popWindowY = -150;

        String defaultSize = SPUtil.getStrValue(MainActivity.this, SPUtil.DefaultLyricSizeData);
        if(!TextUtils.isEmpty(defaultSize)) {
            int defaultSizeInt = Integer.parseInt(defaultSize);
            if(SystemUtil.getInstance().isSmallScaleDevice()) {
                panelMoreMenuBinding.sbLyricSize.setProgress((defaultSizeInt - 15)/5);
            } else {
                panelMoreMenuBinding.sbLyricSize.setProgress((defaultSizeInt - 25)/5);
            }
        }

        //根据当前显示的歌词页面展示对应设置
        String controllerScene = SPUtil.getStrValue(MainActivity.this, SPUtil.SaveControllerScene);
        if(!TextUtils.isEmpty(controllerScene)) {
            if(controllerScene.equals("NewScene")) {
                panelMoreMenuBinding.llDefaultLyricSizePanel.setVisibility(View.GONE);
                panelMoreMenuBinding.llNewLyricSizePanel.setVisibility(View.VISIBLE);
                popWindowY = -170;

                String singleSize = SPUtil.getStrValue(MainActivity.this, SPUtil.SingleLyricSizeData);
                if(!TextUtils.isEmpty(singleSize)) {
                    int singleSizeInt = Integer.parseInt(singleSize);
                    if(SystemUtil.getInstance().isSmallScaleDevice()) {
                        panelMoreMenuBinding.sbSingleLyricSize.setProgress((singleSizeInt - 15)/5);
                    } else {
                        panelMoreMenuBinding.sbSingleLyricSize.setProgress((singleSizeInt - 25)/5);
                    }
                }

                String detailSize = SPUtil.getStrValue(MainActivity.this, SPUtil.DetailLyricSizeData);
                if(!TextUtils.isEmpty(detailSize)) {
                    int detailSizeInt = Integer.parseInt(detailSize);
                    if(SystemUtil.getInstance().isSmallScaleDevice()) {
                        panelMoreMenuBinding.sbDetailLyricSize.setProgress((detailSizeInt - 15)/5);
                    } else {
                        panelMoreMenuBinding.sbDetailLyricSize.setProgress((detailSizeInt - 25)/5);
                    }
                }
            }
        }



        PopupWindow panelMorePopupWindow  = new PopupWindow(panelMoreMenuBinding.getRoot(),
                WindowManager.LayoutParams.MATCH_PARENT,  WindowManager.LayoutParams.WRAP_CONTENT, true);

        panelMorePopupWindow.setTouchable(true);
        //变更主题
        ThemeHelper.getInstance().panelMenuPopupWindowTheme(this, rThemeId, panelMorePopupWindow, panelMoreMenuBinding);
        panelMorePopupWindow.showAsDropDown(view,  PxUtil.getInstance().dp2px(0, this),  PxUtil.getInstance().dp2px(popWindowY, this));

    }

    /** 点击播放按钮 */
    public void playButtonClick(View view)  {
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

    /** 点击当前列表按钮 新版 */
    public void newCurrentListButtonClick(View view) {
        showOrHideNewMusicPlayerList();
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
                                    binder.showLyric(playList.get(i + 1), false);
                                    playList.get(i + 1).isPlaying = true;
                                    playMusicListAdapter.notifyDataSetChanged();
                                } else {
                                    playList.get(i).isPlaying = false;
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
        //Log.i("CLICK:", "isClick: " +isClick + " isShowMusicPanel: " + isShowMusicPanel + " isShowMusicList: " + isShowMusicList);
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

    /** 新版显示当前播放列表 */
    public void showOrHideNewMusicPlayerList() {
        if(!isShowNewMusicList) {
            getViewDataBinding().rlDisableClick3.setVisibility(View.VISIBLE);
            ObjectAnimator newCurrentMusicList = MyAnimationUtil.objectAnimatorUpOrDown(MainActivity.this, false, PxUtil.getInstance().dp2px(325, this), getViewDataBinding().clNewCurrentMusicList);
            newCurrentMusicList.start();
        } else {
            getViewDataBinding().rlDisableClick3.setVisibility(View.GONE);
            ObjectAnimator newCurrentMusicList = MyAnimationUtil.objectAnimatorUpOrDown(MainActivity.this, true, PxUtil.getInstance().dp2px(325, this), getViewDataBinding().clNewCurrentMusicList);
            newCurrentMusicList.start();
        }
        isShowNewMusicList = !isShowNewMusicList;
    }
    /** 显示当前播放面板 */
    public void showOrHideMusicPlayerPanel() {
        //Log.i("CLICK:", "isClick: " +isClick + " isShowMusicPanel: " + isShowMusicPanel + " isShowMusicList: " + isShowMusicList);
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
    @SuppressLint("SetTextI18n")
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
            SPUtil.setListValue(this, SPUtil.PlayListData, playList);
            //播放当前第一首音乐
            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.PLAY_LIST_FIRST));
            getViewDataBinding().tvListSize.setText("("+ playList.size() + ")");
            getViewDataBinding().tvNewListSize.setText("("+ playList.size() + ")");
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

        //变更主题
        ThemeHelper.getInstance().sortMenuButtonTheme(rThemeId, sortMenuBinding, isUpSortByTime, isUpSortByName, isUpSortBySinger);

        /* 按时间排序 */
        sortMenuBinding.llSortByTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isUpSortByTime = !isUpSortByTime;
                sortList(0);
                //变更主题
                ThemeHelper.getInstance().sortTypeButtonTheme(rThemeId, sortMenuBinding.ivSortByTimeType, isUpSortByTime);

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
                //变更主题
                ThemeHelper.getInstance().sortTypeButtonTheme(rThemeId, sortMenuBinding.ivSortByNameType, isUpSortByName);

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
                //变更主题
                ThemeHelper.getInstance().sortTypeButtonTheme(rThemeId, sortMenuBinding.ivSortBySingerType, isUpSortBySinger);

                sortMenuBinding.ivSortByTimeType.setVisibility(View.INVISIBLE);
                sortMenuBinding.ivSortByNameType.setVisibility(View.INVISIBLE);
                sortMenuBinding.ivSortBySingerType.setVisibility(View.VISIBLE);
                clickSortType = 2;
            }
        });

        PopupWindow sortMenuPopupWindow  = new PopupWindow(sortMenuBinding.getRoot(),
                PxUtil.getInstance().dp2px(150, this),  WindowManager.LayoutParams.WRAP_CONTENT, true);
        sortMenuPopupWindow.setTouchable(true);
        //变更主题
        ThemeHelper.getInstance().sortMenuTheme(this, rThemeId, sortMenuPopupWindow, sortMenuBinding);

        sortMenuPopupWindow.showAsDropDown(view,  PxUtil.getInstance().dp2px(-105, this),  PxUtil.getInstance().dp2px(10, this));
    }

    /**
     * 按类型排序歌曲
     * @param sortType 1：顺序 2：反序
     * */
    public void sortList(final int sortType) {
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

    /** 是否展示新的音乐控制器 */
    public void showOrHideNewController(boolean isShow) {
        controllerModeAnimator = MyAnimationUtil.objectAnimatorUpOrDown(MainActivity.this, isShow, getViewDataBinding().clControllerMode.getHeight(), getViewDataBinding().clControllerMode);
        controllerModeAnimator.start();

        ObjectAnimator newPlayController = MyAnimationUtil.objectAnimatorUpOrDown(MainActivity.this, !isShow, PxUtil.getInstance().dp2px(85, MainActivity.this), getViewDataBinding().rlNewPlayController);
        newPlayController.start();
        isShowControllerModePanel = !isShow;
        isShowNewPlayController = isShow;
    }

    /**
     * 是否展示新的详细歌曲页面
     * */
    public void showOrHideNewLyricDetailView(boolean isShow) {
        isClickNewSingleLyricView = isShow;
        getViewDataBinding().llNewShowLyricDetail.setVisibility(isClickNewSingleLyricView? View.VISIBLE : View.GONE);
        getViewDataBinding().llNewShowLyric.setVisibility(isClickNewSingleLyricView? View.GONE : View.VISIBLE);

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
                getViewDataBinding().tvPlayMode.setText("顺序播放");
                getViewDataBinding().tvNewPlayMode.setText("顺序播放");
                //变更主题
                ThemeHelper.getInstance().sequentialPlayButtonTheme(rThemeId, getViewDataBinding(), 0);
                break;
            case 1: //随机播放
                binder.setSingePlayMode(false);
                getViewDataBinding().tvPlayMode.setText("随机播放");
                getViewDataBinding().tvNewPlayMode.setText("随机播放");
                //变更主题
                ThemeHelper.getInstance().sequentialPlayButtonTheme(rThemeId, getViewDataBinding(), 1);
                break;
            case 2: //单曲循环
                binder.setSingePlayMode(true);
                getViewDataBinding().tvPlayMode.setText("单曲循环");
                getViewDataBinding().tvNewPlayMode.setText("单曲循环");
                //变更主题
                ThemeHelper.getInstance().sequentialPlayButtonTheme(rThemeId, getViewDataBinding(), 2);
                break;
        }
    }

    /** 点击切换播放模式 */
    public void playModeButtonClick(View view) {
        playMode = playMode!=2 ? playMode+1 : 0;
        SPUtil.setStrValue(getApplicationContext(), SPUtil.SavePlayMode, String.valueOf(playMode));
        switch (playMode) {
            case 0: //顺序播放
                binder.setSingePlayMode(false);
                getViewDataBinding().tvPlayMode.setText("顺序播放");
                getViewDataBinding().tvNewPlayMode.setText("顺序播放");
                //变更主题
                ThemeHelper.getInstance().sequentialPlayButtonTheme(rThemeId, getViewDataBinding(), 0);
                break;
            case 1: //随机播放
                binder.setSingePlayMode(false);
                getViewDataBinding().tvPlayMode.setText("随机播放");
                getViewDataBinding().tvNewPlayMode.setText("随机播放");
                //变更主题
                ThemeHelper.getInstance().sequentialPlayButtonTheme(rThemeId, getViewDataBinding(), 1);
                break;
            case 2: //单曲循环
                binder.setSingePlayMode(true);
                getViewDataBinding().tvPlayMode.setText("单曲循环");
                getViewDataBinding().tvNewPlayMode.setText("单曲循环");
                //变更主题
                ThemeHelper.getInstance().sequentialPlayButtonTheme(rThemeId, getViewDataBinding(), 2);
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
                SPUtil.setListValue(getApplicationContext(), SPUtil.PlayListData, playList);
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_DELETE_MUSIC));
                mAlertDialog.dismiss();
            }
        });
        mAlertDialog = new AlertDialog.Builder(this)
                .setView(deleteListAllBinding.getRoot())
                .create();
        Objects.requireNonNull(mAlertDialog.getWindow()).setBackgroundDrawableResource(R.drawable.shape_button_white_2);
        mAlertDialog.show();

    }

    /** 音乐播放条调整 */
    private class MusicBarChangerListener implements SeekBar.OnSeekBarChangeListener {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            //拖动时改变显示时间
            getViewDataBinding().tvStartTime.setText(getViewModel().rebuildTime(progress));
            getViewDataBinding().tvNewStartTime.setText(getViewModel().rebuildTime(progress));
            //Log.i("ABMusicPlayer", "progress: " + progress);
            lyricScrollView.setMusicPlayerPos(progress);
            lyricNewScrollDetailView.setMusicPlayerPos(progress);
            lyricNewScrollView.setMusicPlayerPos(progress);
            getViewDataBinding().hpvProgress.setCurrentCount(progress);
            getViewDataBinding().pbNewProgress.setProgress(progress);

        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            lyricScrollView.setIsRefreshDraw(true);
            lyricNewScrollDetailView.setIsRefreshDraw(true);
            lyricNewScrollView.setIsRefreshDraw(true);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            //跳到拖动位置播放
            binder.seekTo(seekBar);
            lyricScrollView.setIsRefreshDraw(false);
            lyricNewScrollDetailView.setIsRefreshDraw(false);
            lyricNewScrollView.setIsRefreshDraw(false);
        }
    }


    /** 拖动进度条调整 （解决拖动时与进度条正常播放移动 显示错乱）*/
    private class ProgressBarTouchListener implements SeekBar.OnTouchListener {

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.i("ABMediaPlay", "Click: down ");
                    binder.posLock(true);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i("ABMediaPlay", "Click: up ");
                    binder.posLock(false);
                    break;
            }
            return false;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("ABMusicPlayer", "Stop - UnbindService");
        EventBus.getDefault().unregister(this);
        if(binder != null) {
            binder.pauseImm(this, currentMusicName, currentMusicSinger, currentBitmap);
            binder.clearMedia();
        }
        getViewDataBinding().ccvShowVisualizer.release();
        clearMediaSession();
        MainVM.stopHandler();
        MainVM.stopTalkHandler();
        unbindService(conn);
        stopService(intentService);
        stopService(intentCharacterService);
        //stopService(intentLockScreenService);
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
        if(keyCode == KeyEvent.KEYCODE_BACK) { //物理返回键

            if(isShowNewMusicList) { //在新版音乐控制界面时当打开播放列表
                showOrHideNewMusicPlayerList();
                return true;
            }

            if(isShowControllerModePanel) { //当在新版音乐控制界面时关闭当前页面
                showOrHideNewController(true);
                return true;
            }

            if(!isShowNewPlayController) {
                if(isShowMusicPanel || isShowMusicList) {
                    hideAllMusicView();
                    return true;
                }
            }

            if(isNotMain) { //不在主界面下处理ui
                if(isSearchMusic) {
                    searchCancel();
                    isSearchMusic = false;
                }
                ObjectAnimator mainPanelChangeObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(MainActivity.this, true, true, getViewDataBinding().clMain);
                ObjectAnimator detailPanelChangeObjectAnimator = MyAnimationUtil.objectAnimatorLeftOrRight(MainActivity.this, false, false, getViewDataBinding().clAlbumDetail);
                mainPanelChangeObjectAnimator.start();
                detailPanelChangeObjectAnimator.start();
                isNotMain = false;
                return true;
            }
            if(isClickLocalOrFavorite) {
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_HIDE_LOCAL_OR_FAVORITE));
                isClickLocalOrFavorite = false;
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
                ThemeHelper.getInstance().musicListAddButtonTheme(getApplication(), rThemeId, binding);

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
                        playMusic(list, position);
                    }
                });

                //点击添加歌曲到列表
                binding.llAdd.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //变更主题
                        ThemeHelper.getInstance().musicListAddButtonAnimatorTheme(rThemeId, binding);
                        addMusic(list, position);
                    }
                });

                binding.llMore.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showMoreMenu(v, list, position);
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

    /** 播放当前点击的歌曲 */
    private void playMusic(List<Music> list, int position) {
        if(playList.size()>0){
            for(int i=0; i<playList.size(); i++) {
                if(playList.get(i).isPlaying) {
                    playList.get(i).isPlaying = false;
                    binder.showLyric(list.get(position), (playMode == 2));
                    playList.add(i+1, MainVM.setMusicMsg(list.get(position), true));
                    playMusicListAdapter.notifyDataSetChanged();
                    EventBus.getDefault().post(new ThreadEvent<>(ThreadEvent.SAVE_LOCAL_MUSIC_LIST, playList));
                    //SPUtil.setListValue(context, SPUtil.PlayListData, playList);
                    return;
                }
            }
            binder.showLyric(list.get(position), (playMode == 2));
            playList.add(playList.size(), MainVM.setMusicMsg(list.get(position), true));
        } else {
            binder.showLyric(list.get(position), (playMode == 2));
            playList.add(MainVM.setMusicMsg(list.get(position), true));
        }
        playMusicListAdapter.notifyDataSetChanged();
        EventBus.getDefault().post(new ThreadEvent<>(ThreadEvent.SAVE_LOCAL_MUSIC_LIST, playList));
        //SPUtil.setListValue(context, SPUtil.PlayListData, playList);
    }

    /** 添加当前点击的歌曲 */
    private void addMusic(List<Music> list, int position) {
        playList.add(MainVM.setMusicMsg(list.get(position), false));

        if(playList.size()==1) {
            playList.get(0).isPlaying = true;
            binder.showLyric(playList.get(0), (playMode == 2));
        } else {
            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_ADD_MUSIC));
        }
        playMusicListAdapter.notifyDataSetChanged();
        EventBus.getDefault().post(new ThreadEvent<>(ThreadEvent.SAVE_LOCAL_MUSIC_LIST, playList));
        EventBus.getDefault().post(new ThreadEvent<>(ThreadEvent.VIEW_FRESH_FAVORITE_MUSIC));
        //SPUtil.setListValue(context, SPUtil.PlayListData, playList);
    }

    /** 在线歌曲item点击展示更多菜单 */
    private void showMoreMenu(View v, List<Music> list, int position) {
        getViewDataBinding().rlDisableClick2.setVisibility(View.VISIBLE);
        DialogMoreMenuBinding dialogMoreMenuBinding = DataBindingUtil.inflate(LayoutInflater.from(this), R.layout.dialog_more_menu, null, false);

        dialogMoreMenuBinding.llList.setVisibility(isClickLocalPlayList? View.GONE : View.VISIBLE);
        dialogMoreMenuBinding.llList2.setVisibility(isClickLocalPlayList? View.VISIBLE : View.GONE);

        if(mPopupWindow != null) {
            mPopupWindow.dismiss();
        }
        mPopupWindow  = new PopupWindow(dialogMoreMenuBinding.getRoot(),
                PxUtil.getInstance().dp2px(200, v.getContext()),  WindowManager.LayoutParams.WRAP_CONTENT, true);
        mPopupWindow.setTouchable(true);
        mPopupWindow.showAtLocation(v, Gravity.CENTER, 0,0);

        mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                getViewDataBinding().rlDisableClick2.setVisibility(View.GONE);
            }
        });

        dialogMoreMenuBinding.tvMusicName.setText(list.get(position).musicName);
        dialogMoreMenuBinding.tvMusicSinger.setText(list.get(position).musicSinger);

        dialogMoreMenuBinding.llAddFavorite.setOnClickListener(new View.OnClickListener() {  //在线歌曲添加到收藏列表
            @Override
            public void onClick(View v) {
                if(mPopupWindow != null) {
                    mPopupWindow.dismiss();
                }
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_ADD_FAVORITE_MUSIC, list.get(position)));

            }
        });

        dialogMoreMenuBinding.llAddLocalPlayList.setOnClickListener(new View.OnClickListener() {  //在线歌曲添加到自建列表
            @Override
            public void onClick(View v) {
                if(mPopupWindow != null) {
                    mPopupWindow.dismiss();
                }

                //展示自建列表
                getViewDataBinding().rlDisableClick2.setVisibility(View.VISIBLE);
                DialogAddMusicToLocalListBinding dialogAddMusicToLocalListBinding = DataBindingUtil.inflate(LayoutInflater.from(v.getContext()), R.layout.dialog_add_music_to_local_list, null, false);

                addMusicLocalListAdapter = new AddMusicLocalListAdapter(v.getContext(), localPlayList, list.get(position));
                dialogAddMusicToLocalListBinding.rvLocalMusicList.setLayoutManager(new LinearLayoutManager(v.getContext()));
                dialogAddMusicToLocalListBinding.rvLocalMusicList.setAdapter(addMusicLocalListAdapter);
                localPlayList.clear();
                List<LocalPlayList> list = SPUtil.getListValue(v.getContext(), SPUtil.LocalPlayListData, LocalPlayList.class);
                if(list.size() >0) {
                    localPlayList.addAll(list);
                }
                addMusicLocalListAdapter.notifyDataSetChanged();


                mPopupWindow  = new PopupWindow(dialogAddMusicToLocalListBinding.getRoot(),
                        PxUtil.getInstance().dp2px(300, v.getContext()),  WindowManager.LayoutParams.WRAP_CONTENT, true);
                mPopupWindow.setTouchable(true);
                mPopupWindow.showAtLocation(v, Gravity.CENTER, 0,0);

                mPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                    @Override
                    public void onDismiss() {
                        getViewDataBinding().rlDisableClick2.setVisibility(View.GONE);
                    }
                });

            }
        });

        dialogMoreMenuBinding.llDeleteMusic.setOnClickListener(new View.OnClickListener() { //自建歌曲列表中删除该歌曲
            @Override
            public void onClick(View v) {
                if(mPopupWindow != null) {
                    mPopupWindow.dismiss();
                }

                list.remove(position);
                musicListAdapter.notifyItemRemoved(position);
                musicListAdapter.notifyItemRangeChanged(position, musicList.size());

                List<Music> list1 = new ArrayList<>();
                list1.addAll(list);
                Iterator<Music> iterator = list1.listIterator();
                while (iterator.hasNext()) {
                    if(iterator.next().musicName.equals(" ")) {
                        iterator.remove();
                    }
                }
                if(clickLocalPlayListId != 0) {
                    EventBus.getDefault().post(new ThreadEvent<Music>(ThreadEvent.DELETE_MUSIC_IN_LOCAL_PLAY_LIST, list1, clickLocalPlayListId));
                }
            }
        });
    }

    /** 设置播放列表使用图标显示收藏的歌 */
    private void setMusicFavorite(List<Music> favoriteList) {
        for(int i = 0; i< playList.size(); i++) {
            boolean isShowMusicFavorite = false;
            for(int j = 0; j< favoriteList.size(); j++) {
                if(!TextUtils.isEmpty(playList.get(i).musicName) && !TextUtils.isEmpty(favoriteList.get(j).musicName)) {
                    if(playList.get(i).musicName.equals(favoriteList.get(j).musicName)) {
                        playList.get(i).setMusicFavorite(1);
                        isShowMusicFavorite =true;
                        break;
                    }
                }
            }
            if(!isShowMusicFavorite) {
                playList.get(i).setMusicFavorite(0);
            }
        }
    }

    private void setCurrentMusicFavorite(List<Music> favoriteMusicList, String currentMusic) {
        isFavorite = false;

        if(favoriteMusicList.size() >0) {
            for(int i=0; i < favoriteMusicList.size(); i++) {
                String favoriteMusicName = favoriteMusicList.get(i).musicName;
                if(!TextUtils.isEmpty(favoriteMusicName)) {
                    if(favoriteMusicName.equals(currentMusic)){
                        isFavorite = true;
                        break;
                    }
                }
            }
        }
        //变更主题
        ThemeHelper.getInstance().musicBarMusicFavoriteTheme(this, rThemeId, getViewDataBinding(), isFavorite);
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
    public class PlayMusicListAdapter extends RecyclerView.Adapter<PlayMusicListViewHolder>
            implements ItemTouchHelperAdapter {

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

                //展示各团Logo
                binding.ivShowLogo.setVisibility(list.get(position).isLocal? View.GONE : View.VISIBLE);
                binding.ivShowLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                if(MUSIC_TYPE_LIELLA.equals(list.get(position).musicType)) {
                    binding.ivShowLogo.setImageResource(R.mipmap.ic_album_liella_3);
                } else if (MUSIC_TYPE_LIYUU.equals(list.get(position).musicType)) {
                    binding.ivShowLogo.setImageResource(R.mipmap.ic_album_liyuu);
                } else if (MUSIC_TYPE_SUNNYPASSION.equals(list.get(position).musicType)) {
                    binding.ivShowLogo.setImageResource(R.mipmap.ic_album_sunny_passion);
                    binding.ivShowLogo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                } else if (MUSIC_TYPE_NIJIGASAKI.equals(list.get(position).musicType)) {
                    binding.ivShowLogo.setImageResource(R.mipmap.ic_album_nijigasaki_3);
                } else if (MUSIC_TYPE_AQOURS.equals(list.get(position).musicType)) {
                    binding.ivShowLogo.setImageResource(R.mipmap.ic_album_aqours_3);
                } else if (MUSIC_TYPE_US.equals(list.get(position).musicType)) {
                    binding.ivShowLogo.setImageResource(R.mipmap.ic_album_us_3);
                } else if (MUSIC_TYPE_HASUNOSORA.equals(list.get(position).musicType)) {
                    binding.ivShowLogo.setImageResource(R.mipmap.ic_album_hasu_2);
                }

                binding.llFavorite.setVisibility(list.get(position).musicFavorite == 1? View.VISIBLE : View.GONE);

                //变更主题
                ThemeHelper.getInstance().playListTheme(context, rThemeId, binding, list.get(position).isPlaying);

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
                binding.llDelete.setVisibility(list.get(position).isPlaying? View.INVISIBLE : View.VISIBLE);
                binding.llDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(!list.get(position).isPlaying) {
                            list.remove(position);
                            playMusicListAdapter.notifyDataSetChanged();
                            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.VIEW_DELETE_MUSIC));
                            SPUtil.setListValue(context, SPUtil.PlayListData, list);
                        }
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void onItemMove(int fromPosition, int toPosition) {
            Collections.swap(list, fromPosition, toPosition);
            notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onItemDismiss(int position) {

        }

    }

    private interface ItemTouchHelperAdapter {
        void onItemMove(int fromPosition, int toPosition);

        void onItemDismiss(int position);
    }

    /** item触摸移动处理 */
    private class ItemTouchHelperCallback extends ItemTouchHelper.Callback {

        private final ItemTouchHelperAdapter adapter;

        public ItemTouchHelperCallback(ItemTouchHelperAdapter adapter) {
            this.adapter = adapter;
        }

        @Override
        public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
            return makeMovementFlags(dragFlags, 0);

        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            adapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            adapter.onItemDismiss(viewHolder.getAdapterPosition());
        }

        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                if (viewHolder instanceof PlayMusicListViewHolder) {
                    PlayMusicListViewHolder itemViewHolder = (PlayMusicListViewHolder) viewHolder;
                    itemViewHolder.itemView.setBackgroundColor(getResources().getColor(R.color.alpha_30));
                }
            }
            super.onSelectedChanged(viewHolder, actionState);
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (viewHolder instanceof PlayMusicListViewHolder) {
                PlayMusicListViewHolder itemViewHolder = (PlayMusicListViewHolder) viewHolder;
                itemViewHolder.itemView.setBackgroundColor(0);
            }
        }
    }

    public static class AddMusicLocalListViewHolder extends RecyclerView.ViewHolder {
        public AddMusicLocalListViewHolder(@NonNull View itemView) { super(itemView); }
    }
    /** 点击添加到自建列表 适配器*/
    public class AddMusicLocalListAdapter extends RecyclerView.Adapter<AddMusicLocalListViewHolder>{

        private Context context;
        private List<LocalPlayList> list;
        private Music music;

        public AddMusicLocalListAdapter(Context context, List<LocalPlayList> list, Music music) {
            this.context = context;
            this.list = list;
            this.music = music;
        }

        @NonNull
        @Override
        public AddMusicLocalListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemAddMusicLocalListBinding binding = DataBindingUtil.inflate(LayoutInflater.from(context),
                    R.layout.item_add_music_local_list, parent, false);
            return new AddMusicLocalListViewHolder(binding.getRoot());
        }

        @Override
        public void onBindViewHolder(@NonNull AddMusicLocalListViewHolder holder, @SuppressLint("RecyclerView") int position) {
            ItemAddMusicLocalListBinding binding = DataBindingUtil.getBinding(holder.itemView);
            if(binding != null) {
                if(TextUtils.isEmpty(list.get(position).getPlayListName())) {
                    binding.llMusicAll.setVisibility(View.GONE);
                } else {
                    binding.llMusicAll.setVisibility(View.VISIBLE);
                    binding.tvMusicListName.setText(list.get(position).playListName);
                    if(list.get(position).playListImgByte != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(list.get(position).playListImgByte, 0, list.get(position).playListImgByte.length);
                        binding.civImage.setImageBitmap(bitmap);
                    } else {
                        binding.civImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_music_cover));
                    }
                    //点击添加到该自建歌单
                    binding.llMusicAll.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(mPopupWindow != null) {
                                mPopupWindow.dismiss();
                            }
                            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.ADD_MUSIC_TO_LOCAL_PLAY_LIST, music, list.get(position).playListId));
                        }
                    });
                }
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
        switch (requestCode) {
            case REQUEST_CODE_DOWNLOAD_APP:
                resultPost(permissions, ThreadEvent.DOWNLOAD_APP, versionList.get(0).versionUrl);
                break;
            case REQUEST_CODE_SCAN_LOCAL_FILE:
                resultPost(permissions, ThreadEvent.SCAN_LOCAL_FILE_BY_CHECK_PERMISSION, "scan");
                break;
            case REQUEST_CODE_SELECT_LOCAL_FILE:
                resultPost(permissions, ThreadEvent.SCAN_LOCAL_FILE_BY_CHECK_PERMISSION, "select");
                break;
            case REQUEST_CODE_SELECT_IMG_FILE:
                resultPost(permissions, ThreadEvent.SELECT_IMG_FILE_SUCCESS, null);
                break;
            case REQUEST_CODE_CONTROLLER_MODE:
                resultPost(permissions, ThreadEvent.VIEW_CONTROLLER_MODE, null);
                break;
            case REQUEST_CODE_NEED_RUNNING_PERMISSION:
                if(PermissionUtil.getInstance().checkPermission(this, permissions)) {
                    //System.exit(0);
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 处理权限并调用EventBus事件
     * */
    private void resultPost(String[] permissions, int EVENT_NAME, String value) {
        if(!PermissionUtil.getInstance().checkPermission(this, permissions)) {
            if(!TextUtils.isEmpty(value)) {
                EventBus.getDefault().post(new ThreadEvent<>(EVENT_NAME, value));
            } else {
                EventBus.getDefault().post(new ThreadEvent<>(EVENT_NAME));
            }
        }
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