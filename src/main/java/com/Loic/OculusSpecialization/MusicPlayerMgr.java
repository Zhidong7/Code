package com.Loic.OculusSpecialization;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by 胡敏浩 on 2017/10/14.
 */
public class MusicPlayerMgr {
    private Context context;

    public MusicPlayerMgr(Context context){
        this.context=context;
    }

    private ListView musicListView = null;      //音乐列表
    private static TextView textView = null;    //音乐播放信息，歌名 播放到第几秒

    private ImageView imageView = null;         //分享按钮
    private ImageView playMode = null;          //播放模式按钮
    private ImageView btn_play_pause = null;    //播放暂停按钮

    public static SeekBar audioSeekBar = null;  //进度条

    private RelativeLayout musictop = null;
    private RelativeLayout musicbotom = null;

    private ArrayList<Map<String, Object>> listems = null;//需要显示在listview里的信息
    public static ArrayList<MusicMedia> musicList = null; //音乐信息列表

    private static MusicPlayerService musicPlayerService = null;
    private static MediaPlayer mediaPlayer = null;

    private Intent intent = null;
    private static int currentposition = -1;//当前播放列表里哪首音乐
    private boolean isplay = false;//音乐是否在播放

    public static Handler handler = null;//处理界面更新，seekbar ,textview
    private boolean isservicerunning = false;//退出应用再进入时（点击app图标或者在通知栏点击service）使用，判断服务是否在启动
    private SingleMusicInfo singleMusicInfo = null;//音乐的详细信息



    private boolean isExit = false;//返回键

    private float mLastY = -1;// 标记上下滑动时上次滑动位置,滑动隐藏上下标题栏

    public static SharedPreferences sharedPreferences;
    public static SharedPreferences.Editor editor;//保存播放模式

    private int[] modepic = {R.drawable.ic_shuffle_black_24dp,R.drawable.ic_repeat_black_24dp,R.drawable.ic_repeat_one_black_24dp};
    private int clicktime = 0;//accelerometer 切换

    public void initMusicPlayer() {
        intent = new Intent();
        intent.setAction("player");
        intent.setPackage(context.getPackageName());
        handler = new Handler();

        //默认随机播放
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
        int playmode = sharedPreferences.getInt("play_mode", -1);
        if(playmode == -1){//没有设置模式，默认随机
            editor.putInt("play_mode",0).commit();
        }else{
            changeMode(playmode);
        }

        /**
         * 对音乐列表操作
         */
        //点击
        musicListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //点击播放音乐，不过需要判断一下当前是否有音乐在播放，需要关闭正在播放的
                //position 可以获取到点击的是哪一个，去 musicList 里寻找播放
                currentposition = position;
                player(currentposition);
            }
        });
        //上下滚动
        musicListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (mLastY == -1) {
                    mLastY = event.getRawY();
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_MOVE:
                        //判断上滑还是下滑
                        if (event.getRawY() > mLastY) {
                            //下滑显示bottom，隐藏top

                        } else if (event.getRawY() < mLastY) {
                            //上滑，显示top，隐藏bottom

                        } else {
                            // deltaY = 0.0 时
                            mLastY = event.getRawY();
                            return false;//返回false即可响应click事件
                        }
                        mLastY = event.getRawY();
                        break;
                    default:
                        // reset
                        mLastY = -1;
                        musictop.setVisibility(View.VISIBLE);
                        musicbotom.setVisibility(View.VISIBLE);
                        break;
                }
                return false;
            }
        });

        /**
         * 音乐列表内容
         */
        musicList  = scanAllAudioFiles();
        listems = new ArrayList<Map<String, Object>>();
        for (Iterator iterator = musicList.iterator(); iterator.hasNext();) {
            Map<String, Object> map = new HashMap<String, Object>();
            MusicMedia mp3Info = (MusicMedia) iterator.next();
            map.put("title", mp3Info.getTitle());
            map.put("artist", mp3Info.getArtist());
            map.put("album", mp3Info.getAlbum());
            map.put("duration", mp3Info.getTime());
            map.put("size", mp3Info.getSize());
            map.put("url", mp3Info.getUrl());
            map.put("bitmap", R.drawable.musicfile);
            listems.add(map);
        }


        /*SimpleAdapter的参数说明
         * 第一个参数 表示访问整个android应用程序接口，基本上所有的组件都需要
         * 第二个参数表示生成一个Map(String ,Object)列表选项
         * 第三个参数表示界面布局的id  表示该文件作为列表项的组件
         * 第四个参数表示该Map对象的哪些key对应value来生成列表项
         * 第五个参数表示来填充的组件 Map对象key对应的资源一依次填充组件 顺序有对应关系
         * 注意的是map对象可以key可以找不到 但组件的必须要有资源填充  因为 找不到key也会返回null 其实就相当于给了一个null资源
         * 下面的程序中如果 new String[] { "name", "head", "desc","name" } new int[] {R.id.name,R.id.head,R.id.desc,R.id.head}
         * 这个head的组件会被name资源覆盖
         *
         * 其实就是一一对应的一个适配器，设置好适配器后，就可以加载到musicListView中
         * */
        SimpleAdapter mSimpleAdapter = new SimpleAdapter(
                context,
                listems,
                R.layout.music_item,
                new String[] {"bitmap","title","artist", "size","duration"},
                new int[] {R.id.video_imageView,R.id.video_title,R.id.video_singer,R.id.video_size,R.id.video_duration}
        );

        //加载数据
        musicListView.setAdapter(mSimpleAdapter);


        /**
         * 进度条控制
         */
        //退出后再次进去程序时，进度条保持持续更新
        if(MusicPlayerService.mediaPlayer!=null){
            reinit();//更新页面布局以及变量相关
        }

        //播放进度监控 ，使用静态变量时别忘了Service里面还有个进度条刷新
        audioSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (currentposition == -1) {
                    Log.i("MusicPlayerService", "MusicActivity...showInfo(请选择要播放的音乐);.........");
                    //还没有选择要播放的音乐
                    showInfo("请选择要播放的音乐");
                } else {
                    //假设改变源于用户拖动
                    if (fromUser) {
                        //这里有个问题，如果播放时用户拖进度条还好说，但是如果是暂停时，拖完会自动播放，所以还需要把图标设置一下
                        btn_play_pause.setBackgroundResource(R.drawable.pause);
                        MusicPlayerService.mediaPlayer.seekTo(progress);// 当进度条的值改变时，音乐播放器从新的位置开始播放
                    }

                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.pause();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                }
            }
        });
    }


    private void reinit() {
        Log.i("MusicPlayerService","reinit.........");
        isservicerunning = true;
        //如果是正在播放
        if(MusicPlayerService.mediaPlayer.isPlaying()){
            isplay = true;
            btn_play_pause.setBackgroundResource(R.drawable.pause);
        }
        //重新绑定service
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE);
    }


    /*加载媒体库里的音频*/
    public ArrayList<MusicMedia> scanAllAudioFiles(){
        //生成动态数组，并且转载数据
        ArrayList<MusicMedia> mylist = new ArrayList<MusicMedia>();

        /*查询媒体数据库
        参数分别为（路径，要查询的列名，条件语句，条件参数，排序）
        视频：MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        图片：MediaStore.Images.Media.EXTERNAL_CONTENT_URI
         */
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        //遍历媒体数据库
        if(cursor.moveToFirst()){
            while (!cursor.isAfterLast()) {
                //歌曲编号
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                //歌曲标题
                String tilte = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                //歌曲的专辑名：MediaStore.Audio.Media.ALBUM
                String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                int albumId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                //歌曲的歌手名： MediaStore.Audio.Media.ARTIST
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
                //歌曲文件的路径 ：MediaStore.Audio.Media.DATA
                String url = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                //歌曲的总播放时长 ：MediaStore.Audio.Media.DURATION
                int duration = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
                //歌曲文件的大小 ：MediaStore.Audio.Media.SIZE
                Long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE));
                if (size >1024*800){//大于800K
                    MusicMedia musicMedia = new MusicMedia();
                    musicMedia.setId(id);
                    musicMedia.setArtist(artist);
                    musicMedia.setSize(size);
                    musicMedia.setTitle(tilte);
                    musicMedia.setTime(duration);
                    musicMedia.setUrl(url);
                    musicMedia.setAlbum(album);
                    musicMedia.setAlbumId(albumId);
                    mylist.add(musicMedia);
                }
                cursor.moveToNext();
            }
        }
        return mylist;
    }

    public void play_pause() {
        Log.i("MusicPlayerService", "MusicActivity...play_pause........." +isplay);
        //当前是pause的图标,（使用图标来判断是否播放，就不需要再新定义变量为状态了,表示没能找到得到当前背景的图片的）实际上播放着的，暂停
        if(isservicerunning){//服务启动着，这里点击播放暂停按钮时只需要当前音乐暂停或者播放就好
            if (isplay) {
                pause();
            } else {
                //暂停--->继续播放
                player("2");
            }
        }else {
            if (isplay) {
                pause();
            } else {
                Log.i("MusicPlayerService", "MusicActivity...not play.........");
                //当前是play的图标,是 暂停 着的
                //初始化时，没有点击列表，直接点击了播放按钮
                if (currentposition == -1) {
                    showInfo("请选择要播放的音乐");
                } else {
                    //暂停--->继续播放
                    player("2");
                }
            }
        }
    }



    private void player() {
        player(currentposition);
    }

    private void player(int position){

        textView.setText(musicList.get(position).getTitle()+"   playing...");

        intent.putExtra("curposition", position);//把位置传回去，方便再启动时调用
        intent.putExtra("url", musicList.get(position).getUrl());
        intent.putExtra("MSG","0");
        isplay = true;
        //播放时就改变btn_play_pause图标，下面这个过期了
        btn_play_pause.setBackgroundResource(R.drawable.pause);
        context.startService(intent);
        context.bindService(intent, conn, Context.BIND_AUTO_CREATE);
        Log.i("MusicPlayerService","MusicActivity...bindService.......");
    }
    private ServiceConnection conn = new ServiceConnection() {
        /** 获取服务对象时的操作 */
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            musicPlayerService = ((MusicPlayerService.musicBinder)service).getPlayInfo();
            mediaPlayer = musicPlayerService.getMediaPlayer();
            Log.i("MusicPlayerService", "MusicActivity...onServiceConnected.......");
            currentposition = musicPlayerService.getCurposition();
            //设置进度条最大值
            audioSeekBar.setMax(mediaPlayer.getDuration());
            //这里开了一个线程处理进度条,这个方式官方貌似不推荐，说违背什么单线程什么鬼
            //使用runnable + handler
            handler.post(seekBarHandler);
        }
        /** 无法获取到服务对象时的操作 */
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            musicPlayerService = null;
        }
    };

    //1s更新一次进度条
    Runnable seekBarThread = new Runnable() {
        @Override
        public void run() {
            while (musicPlayerService != null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                audioSeekBar.setProgress(musicPlayerService.getCurrentPosition());
            }
        }
    };

    static Runnable seekBarHandler = new Runnable() {
        @Override
        public void run() {
            Log.i("MusicPlayerService", "seekBarHandler run......."+musicPlayerService.getDuration()+" "+musicPlayerService.getCurrentPosition());
            audioSeekBar.setMax(musicPlayerService.getDuration());
            audioSeekBar.setProgress(musicPlayerService.getCurrentPosition());
            textView.setText( "(Click Me)  "+ musicPlayerService.getMusicMedia().getTitle() +"       " +
                    musicPlayerService.toTime(musicPlayerService.getCurrentPosition()) +
                    "  / " + musicPlayerService.toTime(musicPlayerService.getDuration() ));
            handler.postDelayed(seekBarHandler, 1000);
        }
    };


    private  void player(String info){
        intent.putExtra("MSG",info);
        isplay = true;
        btn_play_pause.setBackgroundResource(R.drawable.pause);
        context.startService(intent);
    }

    /*
    * MSG :
    *  0    未播放--->播放
    *  1    播放--->暂停
    *  2    暂停--->继续播放
    *
    * */
    private void pause() {
        intent.putExtra("MSG","1");
        isplay = false;
        btn_play_pause.setBackgroundResource(R.drawable.play);
        context.startService(intent);
    }

    public  void previousMusic() {
        if(currentposition > 0){
            currentposition -= 1;
            player();
        }else{
            showInfo("已经是第一首音乐了");
        }
    }

    public void nextMusic() {
        if(currentposition < musicList.size()-2){
            currentposition += 1;
            player();
        }else{
            showInfo("已经是最后一首音乐了");
        }
    }

    public void toPause(){
        //绑定服务了
        if(musicPlayerService != null){
            context.unbindService(conn);
        }
        handler.removeCallbacks(seekBarHandler);
    }

    private void showInfo(String info) {
        Toast.makeText(context,info,Toast.LENGTH_SHORT).show();
    }

    //修改播放模式  单曲循环 随机播放 顺序播放
    int clicktimes = 0;
    public void changeMode() {
        switch (clicktimes){
            case 0://随机 --> 顺序
                clicktimes++;
                changeMode(clicktimes);
                break;
            case 1://顺序 --> 单曲
                clicktimes++;
                changeMode(clicktimes);
                break;
            case 2://单曲 --> 随机
                clicktimes = 0;
                changeMode(clicktimes);
                break;
            default:
                break;
        }

    }
    private void changeMode(int playmode) {
        editor.putInt("play_mode",playmode).commit();
        playMode.setBackgroundResource(modepic[playmode]);
    }

    public ImageView getPlayMode() {
        return playMode;
    }

    public void setPlayMode(ImageView playMode) {
        this.playMode = playMode;
    }

    public ImageView getImageView() {
        return imageView;
    }

    public void setImageView(ImageView imageView) {
        this.imageView = imageView;
    }

    public static TextView getTextView() {
        return textView;
    }

    public static void setTextView(TextView textView) {
        MusicPlayerMgr.textView = textView;
    }

    public ListView getMusicListView() {
        return musicListView;
    }

    public void setMusicListView(ListView musicListView) {
        this.musicListView = musicListView;
    }

    public ImageView getBtn_play_pause() {
        return btn_play_pause;
    }

    public void setBtn_play_pause(ImageView btn_play_pause) {
        this.btn_play_pause = btn_play_pause;
    }

    public RelativeLayout getMusictop() {
        return musictop;
    }

    public void setMusictop(RelativeLayout musictop) {
        this.musictop = musictop;
    }

    public RelativeLayout getMusicbotom() {
        return musicbotom;
    }

    public void setMusicbotom(RelativeLayout musicbotom) {
        this.musicbotom = musicbotom;
    }

    public static SeekBar getAudioSeekBar() {
        return audioSeekBar;
    }

    public static void setAudioSeekBar(SeekBar audioSeekBar) {
        MusicPlayerMgr.audioSeekBar = audioSeekBar;
    }

    public static MusicPlayerService getMusicPlayerService() {
        return musicPlayerService;
    }

    public static void setMusicPlayerService(MusicPlayerService musicPlayerService) {
        MusicPlayerMgr.musicPlayerService = musicPlayerService;
    }

    public static MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public static void setMediaPlayer(MediaPlayer mediaPlayer) {
        MusicPlayerMgr.mediaPlayer = mediaPlayer;
    }

    public boolean isExit() {
        return isExit;
    }

    public void setExit (boolean exit) {
        isExit = exit;
    }

    /**音乐信息
     * Created by chenling on 2016/3/15.
     */
    public static class MusicMedia {
        private int id;
        private String title;
        private String artist;
        private String url;
        private String time;
        private String size;
        private int albumId;
        private String album;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getArtist() {
            return artist;
        }

        public void setArtist(String artist) {
            this.artist = artist;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTime() {
            return time;
        }

        //格式化时间
        public void setTime(int time) {
            time /= 1000;
            int minute = time / 60;
            int hour = minute / 60;
            int second = time % 60;
            minute %= 60;
            this.time = String.format("%02d:%02d", minute, second);
        }

        public String getSize() {
            return size;
        }

        public void setSize(Long size) {
            long kb = 1024;
            long mb = kb * 1024;
            long gb = mb * 1024;
            if (size >= gb) {
                this.size = String.format("%.1f GB", (float) size / gb);
            } else if (size >= mb) {
                float f = (float) size / mb;
                this.size = String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
            } else if (size >= kb) {
                float f = (float) size / kb;
                this.size = String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
            } else
                this.size = String.format("%d B", size);
        }

        public int getAlbumId() {
            return albumId;
        }

        public void setAlbumId(int albumId) {
            this.albumId = albumId;
        }

        public String getAlbum() {
            return album;
        }

        public void setAlbum(String album) {
            this.album = album;
        }
    }
}
