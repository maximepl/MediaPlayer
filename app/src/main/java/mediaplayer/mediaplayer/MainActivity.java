package mediaplayer.mediaplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button buttonRewind;
    private Button buttonPause;
    private Button buttonStart;
    private Button buttonFoward;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.buttonStart = (Button) this.findViewById(R.id.btn_Start);
    }
}
