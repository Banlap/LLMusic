package com.banlap.llmusic.uivm;

import android.app.Application;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.banlap.llmusic.request.ThreadEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SettingsVM extends AndroidViewModel {

    private SettingsCallBack callBack;
    private boolean isDownloadStop = false;  //是否取消下载app

    public SettingsVM(@NonNull Application application) {
        super(application);
    }

    public void setCallBack(SettingsCallBack callBack) {
        this.callBack = callBack;
    }

    public void viewBack() { callBack.viewBack(); }

    /** 下载新版本App */
    public void downloadUrl(String dataSource) {
        isDownloadStop = false;
        EventBus.getDefault().post(new ThreadEvent(ThreadEvent.DOWNLOAD_APP_START2));
        HttpURLConnection connection=null;
        //数据缓冲
        byte[] bs = new byte[1024];
        int len;
        long total = 0;
        try {
            URL url = new URL(dataSource);
            connection = (HttpURLConnection) url.openConnection();
            int contentLength = connection.getContentLength();
            InputStream is = connection.getInputStream();
            if (is == null) {
                throw new RuntimeException("stream is null");
            }
            String path="";
            if (Build.VERSION.SDK_INT > 29) {
                path = getApplication().getExternalFilesDir(null).getAbsolutePath() + "/LLMusic.apk";
            } else {
                path = Environment.getExternalStorageDirectory().getPath() + "/LLMusic.apk";
            }
            File file = new File(path);
            if(file.exists()){    //如果目标文件已经存在
                file.delete();    //则删除旧文件
            }
            OutputStream os = new FileOutputStream(file);
            //开始读取
            while((len = is.read(bs)) != -1){
                total += len;
                int progress = (int) (100 * (total / (double) contentLength));
                EventBus.getDefault().post(new ThreadEvent(ThreadEvent.DOWNLOAD_APP_LOADING2, progress));
                //Log.e("LogByAB","Download progress: " + (100 * (total / (double) contentLength)));
                if(isDownloadStop) {
                    file.delete(); //取消下载则删除文件
                    EventBus.getDefault().post(new ThreadEvent(ThreadEvent.DOWNLOAD_APP_SUCCESS2, false));
                    os.close();
                    is.close();
                    return;
                }
                os.write(bs,0,len);
            }
            //完毕关闭所有连接
            os.close();
            is.close();
            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.DOWNLOAD_APP_SUCCESS2, true, file));
        }catch (Exception e) {
            Log.e("ABMediaPlay", "error " + e.getMessage());
            EventBus.getDefault().post(new ThreadEvent(ThreadEvent.DOWNLOAD_APP_ERROR2));
        } finally {
            if(connection!=null) {
                connection.disconnect();
            }
        }

    }

    public void changeDownloadApp(boolean status){
        isDownloadStop = status;
    }

    public interface SettingsCallBack {
        void viewBack();
    }
}
