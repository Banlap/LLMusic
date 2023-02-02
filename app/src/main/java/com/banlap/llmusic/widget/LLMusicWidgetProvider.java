package com.banlap.llmusic.widget;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.banlap.llmusic.R;
import com.banlap.llmusic.service.MusicIsPauseService;
import com.banlap.llmusic.service.MusicLastService;
import com.banlap.llmusic.service.MusicNextService;
import com.banlap.llmusic.ui.MainActivity;
import com.banlap.llmusic.utils.Base64;
import com.banlap.llmusic.utils.NotificationHelper;

import java.io.ByteArrayOutputStream;

public class LLMusicWidgetProvider extends AppWidgetProvider {

    public static final String WIDGET_PROVIDER_REFRESH_MUSIC_MSG = "WIDGET_PROVIDER_REFRESH_MUSIC_MSG";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        String musicName, musicSinger;
        musicName = MainActivity.currentMusicName;
        musicSinger = MainActivity.currentMusicSinger;
        Log.e("LogByAB", "update success: musicName:" + musicName + " musicSinger: " + musicSinger );
        setRemoteViews(context, appWidgetManager, null);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        Log.e("LogByAB", "send success: " + intent.getStringExtra("MusicName") );

        if (action.equals(WIDGET_PROVIDER_REFRESH_MUSIC_MSG)) {
            setRemoteViews(context, appWidgetManager, intent);
        }
        super.onReceive(context, intent);
    }


    /** 展示小组件视图 */
    private void setRemoteViews(Context context, AppWidgetManager appWidgetManager, Intent intent) {
        final ComponentName mComponentName = new ComponentName(context, LLMusicWidgetProvider.class);

        String musicName = "LLMusic", musicSinger = "LLSinger";
        boolean isDefault = true, isLoading = false;
        Bitmap bitmap = null;

        if(intent != null) {
            isLoading = intent.getBooleanExtra("IsLoading", false);
        }

        //当小组件重新加入时 获取上次音乐信息
        String musicNameTemp = MainActivity.currentMusicName;
        String musicSingerTemp = MainActivity.currentMusicSinger;
        Bitmap bitmapTemp;
        if(musicNameTemp != null && !musicNameTemp.equals("")) {
            if(musicSingerTemp != null && !musicSingerTemp.equals("")) {
                musicName = musicNameTemp;
                musicSinger = musicSingerTemp;

                bitmapTemp = MainActivity.currentBitmap;
                if(bitmapTemp != null) {
                    bitmap = bitmapTemp;
                }
                isDefault = !MainActivity.isPlay;
            }
        }

        @SuppressLint("RemoteViewLayout")
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_widget_llmusic);

        remoteViews.setTextViewText(R.id.tv_music_name, musicName);
        remoteViews.setTextViewText(R.id.tv_music_singer, musicSinger);

        remoteViews.setImageViewResource(R.id.bt_play, context.getResources().getIdentifier(isDefault ? "selector_play_black_selected" : "selector_pause_black_selected", "drawable", context.getPackageName()));
        remoteViews.setViewVisibility(R.id.pb_loading_music, isLoading? View.VISIBLE : View.INVISIBLE);

        Intent intentServiceIsPause = new Intent(context, MusicIsPauseService.class);
        intentServiceIsPause.putExtra("IsPauseMusic", true);
        intentServiceIsPause.putExtra("MusicName", musicName);
        intentServiceIsPause.putExtra("MusicSinger", musicSinger);

        if(bitmap !=null) {
            remoteViews.setImageViewBitmap(R.id.iv_music_img, bitmap);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);  //压缩图片50% 防止显示不到图片
            byte[] bitmapByte = baos.toByteArray();
            intentServiceIsPause.putExtra("MusicBitmap", bitmapByte);
        } else {
            remoteViews.setImageViewResource(R.id.iv_music_img, context.getResources().getIdentifier("ic_llmp_2", "mipmap", context.getPackageName()));
            intentServiceIsPause.putExtra("MusicBitmap", (byte[]) null);
        }

        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pIntentIsPause = PendingIntent.getService(context, NotificationHelper.LL_MUSIC_PLAYER, intentServiceIsPause, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.bt_play, pIntentIsPause);


        Intent intentServiceLast = new Intent(context, MusicLastService.class);
        intentServiceLast.putExtra("LastMusic", true);
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pIntentLast = PendingIntent.getService(context, NotificationHelper.LL_MUSIC_PLAYER, intentServiceLast, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.bt_last, pIntentLast);

        Intent intentServiceNext = new Intent(context, MusicNextService.class);
        intentServiceNext.putExtra("NextMusic", true);
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pIntentNext = PendingIntent.getService(context, NotificationHelper.LL_MUSIC_PLAYER, intentServiceNext, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.bt_next, pIntentNext);

        Intent intentMain = new Intent(context, MainActivity.class);
        intentMain.addCategory(Intent.CATEGORY_LAUNCHER);
        //FLAG_ACTIVITY_RESET_TASK_IF_NEEDED 按需启动的关键,如果任务队列中已经存在,则重建程序
        intentMain.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_NEW_TASK);
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent pIntentMain = PendingIntent.getActivity(context, NotificationHelper.LL_MUSIC_PLAYER, intentMain, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.ll_widget_music, pIntentMain);

        appWidgetManager.updateAppWidget(mComponentName, remoteViews);
    }


}
