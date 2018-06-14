package gti785.spotify;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.MediaMetadataRetriever;
import android.media.audiofx.Visualizer;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private ImageButton btn_Previous,btnPause, btnStart, btnNext;
    private Button btnShuffle, btnRepeat;
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

    private boolean isLooping = false;

    VisualizerView mVisualizerView;
    private Visualizer mVisualizer;

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
        btnShuffle = (Button)findViewById(R.id.btn_shuffle);
        btnRepeat = (Button)findViewById(R.id.btn_repeat);
        btnNext = (ImageButton)findViewById(R.id.btn_Next);
        songPoster = (ImageView)findViewById(R.id.songPoster);

        txtTimeFormStart = (TextView)findViewById(R.id.timeFormStart);
        txtTimeToFinish = (TextView)findViewById(R.id.timeToFinish);
        txtSongName = (TextView)findViewById(R.id.songTitle);
        txtArtistName = (TextView)findViewById(R.id.artistName);

        mVisualizerView = (VisualizerView) findViewById(R.id.audioVisualizer);

        path = Uri.parse("android.resource://" + getPackageName() + "/" + "raw/" + songNames[position].toString());
        //String url = "http://vprbbc.streamguys.net/vprbbc24.mp3";
        try {
            mediaPlayer.setDataSource(this, path);
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //setupVisualizerFxAndUI();
        //mVisualizer.setEnabled(true);
        setSongInfo(path);

        seekBar = (SeekBar)findViewById(R.id.seekBar);
        btnPause.setEnabled(false);
        btnPause.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);

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


        btnRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isLooping){
                    isLooping = false;
                    mediaPlayer.setLooping(false);
                    btnRepeat.getBackground().clearColorFilter();
                } else {
                    isLooping = true;
                    mediaPlayer.setLooping(true);
                    btnRepeat.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                }
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
                btnStart.getBackground().clearColorFilter();
                btnPause.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
                //mVisualizer.release();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_spotify, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.preferences) {
            Intent i = new Intent(this, PreferencesActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void preferences(View view)
    {
        Intent i = new Intent(this, PreferencesActivity.class);
        startActivity(i);
    }

    private String[] getAllSongName() {
        //Field fields[] = R.raw.class.getDeclaredFields() ;
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

    /**
     * Function that play the next song from the song list
     */
    private void playNextSong(){
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
    }

    /**
     * Function that play the previous song from the song list
     */
    private void playPreviousSong(){
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
    }

    /**
     * Function that take the path of the song and use it's MetaData to set the song artist, the
     * song title and the album art
     *
     * @param path
     */
    private void setSongInfo(Uri path){
        MediaMetadataRetriever metaRetriever = new MediaMetadataRetriever();
        metaRetriever.setDataSource(this, path);
        String artist =  metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String title = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        byte[] songPosterPicture = metaRetriever.getEmbeddedPicture();
        Bitmap songImage = BitmapFactory.decodeByteArray(songPosterPicture, 0, songPosterPicture.length);

        songPoster.setImageBitmap(songImage);
        txtSongName.setText(title);
        txtArtistName.setText(artist);
    }

    /**
     * Function that is used to play the current song on the client
     */
    private void playSong(){
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
        btnPause.getBackground().clearColorFilter();
        btnStart.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
    }

    private void setupVisualizerFxAndUI() {
        int audioSessionId = mediaPlayer.getAudioSessionId();
        Log.d("Test", "audioSessionId: " + audioSessionId);

        mVisualizer = new Visualizer(mediaPlayer.getAudioSessionId());
        /*mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        mVisualizer.setDataCaptureListener(
                new Visualizer.OnDataCaptureListener() {
                    public void onWaveFormDataCapture(Visualizer visualizer,
                                                      byte[] bytes, int samplingRate) {
                        mVisualizerView.updateVisualizer(bytes);
                    }

                    public void onFftDataCapture(Visualizer visualizer,
                                                 byte[] bytes, int samplingRate) {
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, false);*/
    }
}
