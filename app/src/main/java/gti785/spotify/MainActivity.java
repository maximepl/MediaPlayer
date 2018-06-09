package gti785.spotify;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import android.os.Bundle;

//import org.jaudiotagger.audio.AudioFile;
//import org.jaudiotagger.audio.AudioFileIO;

public class MainActivity extends AppCompatActivity {
    private ImageButton btn_Previous,btnPause, btnStart, btnNext;
    private ImageView songPoster;
    private MediaPlayer mediaPlayer = new MediaPlayer();

    private double startTime = 0;
    private double finalTime = 0;

    private Handler myHandler = new Handler();
    private int forwardTime = 5000;
    private int backwardTime = 5000;
    private SeekBar seekBar;
    private TextView txtTimeFormStart,txtTimeToFinish,txtSongName, txtArtistName;
    Server server;

    public static int oneTimeOnly = 0;

    Field fields[] = R.raw.class.getDeclaredFields();
    private String[] songNames = new String[fields.length];
    private ArrayList<Integer> musicList = new ArrayList<>();

    private int position = 0;
    private int fieldLength = 0;

    private Uri path;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getAllSongName(); //Return all the names of the songs in the raw folder

        //get the server instance
        try {
            server = Server.getServer();
        } catch (IOException e) {
            e.printStackTrace();
        }

        btn_Previous = (ImageButton) findViewById(R.id.btn_Previous);
        btnPause = (ImageButton) findViewById(R.id.btn_Pause);
        btnStart = (ImageButton)findViewById(R.id.btn_Start);
        btnNext = (ImageButton)findViewById(R.id.btn_Next);
        songPoster = (ImageView)findViewById(R.id.songPoster);

        txtTimeFormStart = (TextView)findViewById(R.id.timeFormStart);
        txtTimeToFinish = (TextView)findViewById(R.id.timeToFinish);
        txtSongName = (TextView)findViewById(R.id.songTitle);
        txtArtistName = (TextView)findViewById(R.id.artistName);

        path = Uri.parse("android.resource://" + getPackageName() + "/" + "raw/" + songNames[position].toString());
        try {
            mediaPlayer.setDataSource(this, path);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        setSongInfo(path);

        seekBar = (SeekBar)findViewById(R.id.seekBar);
        btnPause.setEnabled(false);
        btnPause.setColorFilter(Color.GRAY);

        songPoster.setOnTouchListener(new OnSwipeTouchListener(this) {
            public void onSwipeTop() {
            }
            public void onSwipeRight() {
                playNextSong();
            }
            public void onSwipeLeft() {
                playPreviousSong();
            }
            public void onSwipeBottom() {
            }

        });

        btn_Previous.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPreviousSong();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNextSong();
            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Pausing sound",Toast.LENGTH_SHORT).show();
                mediaPlayer.pause();
                btnPause.setEnabled(false);
                btnStart.setEnabled(true);
                btnStart.setColorFilter(null);
                btnPause.setColorFilter(Color.GRAY);
            }
        });
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playSong();
            }
        });

        //Seek bar listener
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean user) {
                if (mediaPlayer != null && user)
                {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }


    private String[] getAllSongName() {
        Field fields[] = R.raw.class.getDeclaredFields() ;
        fieldLength = fields.length;

        try {
            for( int i=0; i< fields.length; i++ ) {
                Field f = fields[i] ;
                songNames[i] = f.getName();
                String songName = f.getName();

                if (!songName.equals("$change") && !songName.equals("serialVersionUID")) {
                    int songId = getApplicationContext().getResources().getIdentifier(songName, "raw", getApplicationContext().getPackageName());
                    musicList.add(songId);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return songNames ;
    }

    private Runnable UpdateSongTime = new Runnable() {
        public void run() {
            startTime = mediaPlayer.getCurrentPosition();
            txtTimeFormStart.setText(String.format(Locale.CANADA,"%d min, %d sec",
                    TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                    TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.
                                    toMinutes((long) startTime)))
            );
            seekBar.setProgress((int)startTime);
            myHandler.postDelayed(this, 100);
        }
    };

    private int playNextSong(){
        position++;

        if(position >= fieldLength){
            Toast.makeText(getApplicationContext(), "You are at your last song",Toast.LENGTH_LONG).show();
            position--;
        } else {
            mediaPlayer.stop();
            mediaPlayer.reset();

            Uri path = Uri.parse("android.resource://" + getPackageName() + "/" + "raw/" + songNames[1].toString());
            try {
                mediaPlayer.setDataSource(getApplicationContext(), path);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setSongInfo(path);
            playSong();
        }

        return position;
    }

    private int playPreviousSong(){
        position--;

        if(position <= fieldLength){
            Toast.makeText(getApplicationContext(), "You are at your first song",Toast.LENGTH_LONG).show();
            position++;

            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        } else {
            mediaPlayer.stop();
            mediaPlayer.reset();

            Uri path = Uri.parse("android.resource://" + getPackageName() + "/" + "raw/" + songNames[1].toString());
            try {
                mediaPlayer.setDataSource(getApplicationContext(), path);
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            setSongInfo(path);
            playSong();
        }

        return position;
    }

    private String setSongInfo(Uri path){
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(this, path);
        String artist =  metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);

        txtSongName.setText(title);
        txtArtistName.setText(artist);
        return artist;
    }

    private String playSong(){
        String vache = "asd";

        mediaPlayer.start();

        finalTime = mediaPlayer.getDuration();
        startTime = mediaPlayer.getCurrentPosition();

        if (oneTimeOnly == 0) {
            seekBar.setMax((int) finalTime);
            oneTimeOnly = 1;
        }

        txtTimeToFinish.setText(String.format(Locale.CANADA, "%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes((long) finalTime),
                TimeUnit.MILLISECONDS.toSeconds((long) finalTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                finalTime)))
        );

        txtTimeFormStart.setText(String.format(Locale.CANADA, "%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes((long) startTime),
                TimeUnit.MILLISECONDS.toSeconds((long) startTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes((long)
                                startTime)))
        );

        seekBar.setProgress((int)startTime);
        myHandler.postDelayed(UpdateSongTime,100);
        btnPause.setEnabled(true);
        btnStart.setEnabled(false);
        btnStart.setColorFilter(Color.GRAY);
        btnPause.setColorFilter(null);

        return vache;
    }
}
