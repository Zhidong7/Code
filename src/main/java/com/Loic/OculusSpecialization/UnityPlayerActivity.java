package com.Loic.OculusSpecialization;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;

import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class UnityPlayerActivity extends Activity
{
    /**
     * unity3d
     */
    protected UnityPlayer mUnityPlayer; // don't change the name of this variable; referenced from native code


    /**
     * music player
     */
    private MusicPlayerMgr musicPlayerMgr = new MusicPlayerMgr(this);

    public MusicPlayerMgr getMusicPlayerMgr() {
        return musicPlayerMgr;
    }

    public void setMusicPlayerMgr(MusicPlayerMgr musicPlayerMgr) {
        this.musicPlayerMgr = musicPlayerMgr;
    }

    /**
     * 蓝牙
     */
    private BluetoothMgr bluetoothMgr=new BluetoothMgr();


    // Setup activity layout
    @Override protected void onCreate (Bundle savedInstanceState)
    {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        //蓝牙
        if(bluetoothMgr.getBluetoothAdapter()==null){
            Toast.makeText(this,"不支持蓝牙",Toast.LENGTH_LONG).show();
            finish();
        }else if(!bluetoothMgr.getBluetoothAdapter().isEnabled()){
            //如果支持蓝牙却没有打开蓝牙，自动打开并连接
            Log.d("true","开始连接");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,bluetoothMgr.getENABLE_BLUETOOTH());
        }


        /**
         * unity 视图
         */
        getWindow().setFormat(PixelFormat.RGBX_8888); // <--- This makes xperia play happy
        mUnityPlayer = new UnityPlayer(this);
        setContentView(R.layout.content_music);
        LinearLayout unityView=(LinearLayout) this.findViewById(R.id.unityView);
        unityView.addView(mUnityPlayer.getView());
        mUnityPlayer.requestFocus();

        Log.i("MusicPlayerService", "MusicActivity...onCreate........." + Thread.currentThread().hashCode());
        /**
         * 播放器初始化
         */
        initMusicPlayer();
        bluetoothMgr.mtext = (TextView) findViewById(R.id.textView);

    }

    private void initMusicPlayer(){
        musicPlayerMgr.setPlayMode((ImageView)findViewById(R.id.play_mode));
        musicPlayerMgr.setTextView((TextView)findViewById(R.id.musicinfo));         //音乐信息，歌名 播放到第几秒
        musicPlayerMgr.setMusicListView((ListView)findViewById(R.id.musicListView));
        musicPlayerMgr.setBtn_play_pause((ImageView)findViewById(R.id.play_pause));
        musicPlayerMgr.setMusictop((RelativeLayout)findViewById(R.id.music_top));
        musicPlayerMgr.setMusicbotom((RelativeLayout)findViewById(R.id.music_bottom));
        musicPlayerMgr.setAudioSeekBar((SeekBar) findViewById(R.id.seekBar));       //进度条

        musicPlayerMgr.initMusicPlayer();
    }


    public void toInit(View view){
        UnityPlayer.UnitySendMessage("Main Camera","toInit","");
    }
    public void toForward(View view){
        UnityPlayer.UnitySendMessage("Main Camera","toForward","");
    }
    public void toBack(View view){
        UnityPlayer.UnitySendMessage("Main Camera","toBack","");
    }
    public void toLeft(View view){
        UnityPlayer.UnitySendMessage("Main Camera","toLeft","");
    }
    public void toRight(View view){
        UnityPlayer.UnitySendMessage("Main Camera","toRight","");
    }


    @Override protected void onNewIntent(Intent intent)
    {
        // To support deep linking, we need to make sure that the client can get access to
        // the last sent intent. The clients access this through a JNI api that allows them
        // to get the intent set on launch. To update that after launch we have to manually
        // replace the intent with the one caught here.
        setIntent(intent);
    }

    // Quit Unity
    @Override protected void onDestroy ()
    {
        mUnityPlayer.quit();
        super.onDestroy();
        Log.i("MusicPlayerService", "MusicActivity...onDestroy........." + Thread.currentThread().hashCode());
        //关闭蓝牙
        try{
            bluetoothMgr.getBluetoothSocket().close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    // Pause Unity
    @Override protected void onPause()
    {
        super.onPause();
        mUnityPlayer.pause();
        Log.i("MusicPlayerService", "MusicActivity...onPause........." + Thread.currentThread().hashCode());
        musicPlayerMgr.toPause();
    }


    // Resume Unity
    @Override protected void onResume()
    {
        super.onResume();
        mUnityPlayer.resume();
        Log.i("MusicPlayerService", "MusicActivity...onResume........." + Thread.currentThread().hashCode());
        initMusicPlayer();

        //重新连接蓝牙//可以直接封装到mgr里
        Set<BluetoothDevice> devices = bluetoothMgr.getBluetoothAdapter().getBondedDevices();
        bluetoothMgr.setBluetoothDevice(bluetoothMgr.getBluetoothAdapter().getRemoteDevice(bluetoothMgr.getBluetoothAddress()));
        try{
            bluetoothMgr.setBluetoothSocket( bluetoothMgr.getBluetoothDevice().createRfcommSocketToServiceRecord(bluetoothMgr.getMyUuidSecure()));
            Log.d("true","开始连接");
            bluetoothMgr.getBluetoothSocket().connect();
            Log.d("true","完成连接");
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    // Low Memory Unity
    @Override public void onLowMemory()
    {
        super.onLowMemory();
        mUnityPlayer.lowMemory();
    }

    // Trim Memory Unity
    @Override public void onTrimMemory(int level)
    {
        super.onTrimMemory(level);
        if (level == TRIM_MEMORY_RUNNING_CRITICAL)
        {
            mUnityPlayer.lowMemory();
        }
    }

    // This ensures the layout will be correct.
    @Override public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
        mUnityPlayer.configurationChanged(newConfig);
    }

    // Notify Unity of the focus change.
    @Override public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        mUnityPlayer.windowFocusChanged(hasFocus);
    }

    // For some reason the multiple keyevent type is not supported by the ndk.
    // Force event injection by overriding dispatchKeyEvent().
    @Override public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
            return mUnityPlayer.injectEvent(event);
        return super.dispatchKeyEvent(event);
    }

    // Pass any events not handled by (unfocused) views straight to UnityPlayer
    @Override public boolean onKeyUp(int keyCode, KeyEvent event)     { return mUnityPlayer.injectEvent(event); }
    //@Override public boolean onKeyDown(int keyCode, KeyEvent event)   { return mUnityPlayer.injectEvent(event); }
    @Override public boolean onTouchEvent(MotionEvent event)          { return mUnityPlayer.injectEvent(event); }
    /*API12*/ public boolean onGenericMotionEvent(MotionEvent event)  { return mUnityPlayer.injectEvent(event); }

    //按两次返回键退出
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0) {
            //音乐服务启动了，隐藏至通知栏
            if(musicPlayerMgr.getMusicPlayerService() != null){
                exit("再按一次隐藏至通知栏");
            }else{
                exit("再按一次退出程序");
            }

        }
        return mUnityPlayer.injectEvent(event);
    }


    /**
     * 从界面传过来的动作
     *
     */
    public void bluetoothClick(View view){
        Log.d("bluetooth","press");
        bluetoothMgr.fresh();
        bluetoothMgr.mtext.setText("lalala");
       // Toast.makeText(this,"戳了一下",Toast.LENGTH_LONG).show();
    }

    public void previous(View view) {
        musicPlayerMgr.previousMusic();
    }

    public void next(View view) {
        musicPlayerMgr.nextMusic();
    }

    public void play_pause(View view) {
        musicPlayerMgr.play_pause();
    }

    public void changeMode(View view){
        musicPlayerMgr.changeMode();
    }

    private void exit(String info) {
        if(!musicPlayerMgr.isExit()) {
            musicPlayerMgr.setExit(true);
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    musicPlayerMgr.setExit(false);
                }
            }, 2000);
        } else {
            finish();
        }
    }
}
