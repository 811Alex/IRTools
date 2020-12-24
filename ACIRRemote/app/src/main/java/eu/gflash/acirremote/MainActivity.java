package eu.gflash.acirremote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.prefs.Preferences;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {
    IRData ird;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        try{
            ird = new IRData(getApplicationContext(), 8930, 4497, 3100, 1000, 555, 33000);
        }catch(RuntimeException ex){
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
        ((SeekBar) findViewById(R.id.speed)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ((RadioButton)findViewById(R.id.normal)).setChecked(true);
                sendIRData();
            }
        });
        loadSettings();
    }

    public void loadSettings(){
        SharedPreferences p = getPreferences(Activity.MODE_PRIVATE);
        ((RadioButton)findViewById(R.id.cold)).setChecked(p.getBoolean("cold", false));
        ((RadioButton)findViewById(R.id.heat)).setChecked(p.getBoolean("heat", true));
        ((RadioButton)findViewById(R.id.hipow)).setChecked(p.getBoolean("hipow", false));
        ((RadioButton)findViewById(R.id.normal)).setChecked(p.getBoolean("normal", true));
        ((RadioButton)findViewById(R.id.sleep)).setChecked(p.getBoolean("sleep", false));
        ((RadioButton)findViewById(R.id.timeroff)).setChecked(p.getBoolean("timeroff", true));
        ((RadioButton)findViewById(R.id.timerminutes)).setChecked(p.getBoolean("timerminutes", false));
        ((RadioButton)findViewById(R.id.timerhour)).setChecked(p.getBoolean("timerhour", false));
        ((Switch)findViewById(R.id.swing)).setChecked(p.getBoolean("swing", false));
        ((Switch)findViewById(R.id.fresh)).setChecked(p.getBoolean("fresh", false));
        //((Switch)findViewById(R.id.power)).setChecked(p.getBoolean("power", false));
        ((SeekBar)findViewById(R.id.speed)).setProgress(p.getInt("speed", 3));
        ((TextView)findViewById(R.id.temp)).setText(String.valueOf(p.getInt("temp", 30)));
    }

    public void saveSettings(){
        SharedPreferences.Editor e = getPreferences(Activity.MODE_PRIVATE).edit();
        e.putBoolean("cold", ((RadioButton)findViewById(R.id.cold)).isChecked());
        e.putBoolean("heat", ((RadioButton)findViewById(R.id.heat)).isChecked());
        e.putBoolean("hipow", ((RadioButton)findViewById(R.id.hipow)).isChecked());
        e.putBoolean("normal", ((RadioButton)findViewById(R.id.normal)).isChecked());
        e.putBoolean("sleep", ((RadioButton)findViewById(R.id.sleep)).isChecked());
        e.putBoolean("timeroff", ((RadioButton)findViewById(R.id.timeroff)).isChecked());
        e.putBoolean("timerminutes", ((RadioButton)findViewById(R.id.timerminutes)).isChecked());
        e.putBoolean("timerhour", ((RadioButton)findViewById(R.id.timerhour)).isChecked());
        e.putBoolean("swing", ((Switch)findViewById(R.id.swing)).isChecked());
        e.putBoolean("fresh", ((Switch)findViewById(R.id.fresh)).isChecked());
        e.putBoolean("power", ((Switch)findViewById(R.id.power)).isChecked());
        e.putInt("speed", ((SeekBar)findViewById(R.id.speed)).getProgress());
        e.putInt("temp", Integer.parseInt(((TextView)findViewById(R.id.temp)).getText().toString()));
        e.apply();
    }

    public void sendIRData(View v){sendIRData();}

    public void sendIRData(){
        try{
            ird.send(buildIRData());
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if(v != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
                else v.vibrate(25);
            }
            saveSettings();
        }catch (UnsupportedOperationException ex){
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public String buildIRData(){
        String data = "";
        String id = "11011101";
        int tempint = Integer.parseInt(((TextView)findViewById(R.id.temp)).getText().toString());
        tempint -= 15;
        String temp = Integer.toBinaryString(tempint);
        temp = new StringBuilder(temp).reverse().toString();
        data += temp;
        data += "00";
        data += ((RadioButton)findViewById(R.id.timeroff)).isChecked() ? "0" : "1";
        data += ((Switch)findViewById(R.id.power)).isChecked() ? "1" : "0";
        data += " 00000";
        switch(((RadioButton)findViewById(((RadioGroup)findViewById(R.id.mode)).getCheckedRadioButtonId())).getText().toString()){
            case "Cold": data += "100"; break;
            case "Heat": data += "001"; break;
        }
        data += " 000000";
        switch(((SeekBar)findViewById(R.id.speed)).getProgress()){
            case 0: data += "10"; break;
            case 1: data += "01"; break;
            case 2: data += "11"; break;
            case 3: data += "00"; break;
        }
        data += " ";
        data += (((RadioButton)findViewById(R.id.timerhour)).isChecked()) ? "1000" : "0000";
        data += "00";
        data += (((RadioButton)findViewById(R.id.sleep)).isChecked()) ? "1" : "0";
        data += (((Switch)findViewById(R.id.swing)).isChecked()) ? "1" : "0";
        data += " ";
        data += (((RadioButton)findViewById(R.id.timerminutes)).isChecked()) ? "01111" : "00000";
        data += "0";
        data += (((Switch)findViewById(R.id.fresh)).isChecked()) ? "1" : "0";
        data += (((RadioButton)findViewById(R.id.hipow)).isChecked()) ? "1" : "0";
        data = id + " " + data + " " + calcParity(data);
        return data;
    }

    public String calcParity(String data){
        int sum = Stream.of(data.split(" "))
                .map(StringBuilder::new)
                .map(StringBuilder::reverse)
                .map(StringBuilder::toString)
                .map(dataByte -> Integer.parseInt(dataByte, 2))
                .reduce(0, Integer::sum);
        StringBuilder parity = new StringBuilder(Integer.toBinaryString(sum));
        parity.reverse();
        return parity.substring(0, 8);
    }

    public void minus(View view) {
        addTemp(-1);
        sendIRData();
    }

    public void plus(View view) {
        addTemp(1);
        sendIRData();
    }

    protected void addTemp(int t){
        int o = Integer.parseInt(((TextView)findViewById(R.id.temp)).getText().toString());
        o += t;
        if(o <= 30 && o >= 18)
            ((TextView)findViewById(R.id.temp)).setText(String.valueOf(o));
    }

    public void radioMode2Change(View view) {
        if(((RadioButton) findViewById(R.id.sleep)).isChecked()) ((SeekBar) findViewById(R.id.speed)).setProgress(0);
        else if(((RadioButton) findViewById(R.id.hipow)).isChecked()) ((SeekBar) findViewById(R.id.speed)).setProgress(3);
        sendIRData();
    }
}
