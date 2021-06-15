package org.easydarwin.easyplayer;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

//import com.tencent.bugly.Bugly;
//import com.tencent.bugly.beta.Beta;

import org.easydarwin.easyplayer.activity.PlayListActivity;
import org.easydarwin.easyplayer.data.EasyDBHelper;
import org.easydarwin.video.Client;
//import com.pgyersdk.crash.PgyCrashManager;
//import com.pgyersdk.update.PgyUpdateManager;
import com.pgyer.pgyersdk.PgyerSDKManager;

public class TheApp extends Application {

    public static SQLiteDatabase sDB;
    public static int activeDays = 9999;

    @Override
    public void onCreate() {
        super.onCreate();

        activeDays = Client.getActiveDays(this, BuildConfig.RTSP_KEY);
        sDB = new EasyDBHelper(this).getWritableDatabase();
//        PgyCrashManager.register();

//        Bugly.init(getApplicationContext(), "eb3d7319a8", false);
//        setBuglyInit();
//        new PgyUpdateManager.Builder()
//                .register();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //在attachBaseContext方法中调用初始化sdk
        initPgyerSDK(this);
    }

    /**
     *  初始化蒲公英SDK
     * @param application
     */
    private static void initPgyerSDK( TheApp application){
        new PgyerSDKManager.Init()
                .setContext(application) //设置上下问对象
                .start();
    }

    public void setBuglyInit(){
//        // 添加可显示弹窗的Activity
//        Beta.canShowUpgradeActs.add(PlayListActivity.class);
//
////        例如，只允许在MainActivity上显示更新弹窗，其他activity上不显示弹窗; 如果不设置默认所有activity都可以显示弹窗。
////        设置是否显示消息通知
//        Beta.enableNotification = true;
//
////        如果你不想在通知栏显示下载进度，你可以将这个接口设置为false，默认值为true。
////        设置Wifi下自动下载
//        Beta.autoDownloadOnWifi = false;v
//
////        如果你想在Wifi网络下自动下载，可以将这个接口设置为true，默认值为false。
////        设置是否显示弹窗中的apk信息
//        Beta.canShowApkInfo = true;
//
////        如果你使用我们默认弹窗是会显示apk信息的，如果你不想显示可以将这个接口设置为false。
////        关闭热更新能力
//        Beta.enableHotfix = true;
//
//        Beta.largeIconId = R.mipmap.ic_launcher_foreground;
//        Beta.smallIconId = R.mipmap.ic_launcher_foreground;
//
//        // 设置是否显示消息通知
//        Beta.enableNotification = true;
    }
}
