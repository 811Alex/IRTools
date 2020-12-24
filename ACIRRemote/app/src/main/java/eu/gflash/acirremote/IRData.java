package eu.gflash.acirremote;

import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

class IRData {
    private int startPulseLen, startPulseSepLen, oneLen, zeroLen, separatorLen, freq;
    private ConsumerIrManager consumerIrManager;

    IRData(Context ctx, int startPulseLen, int startPulseSepLen, int oneLen, int zeroLen, int separatorLen, int freq) throws RuntimeException{
        this.consumerIrManager = (ConsumerIrManager) ctx.getSystemService(Context.CONSUMER_IR_SERVICE);
        this.startPulseLen = startPulseLen;
        this.startPulseSepLen = startPulseSepLen;
        this.oneLen = oneLen;
        this.zeroLen = zeroLen;
        this.separatorLen = separatorLen;
        this.freq = freq;
        if(this.consumerIrManager == null) throw new RuntimeException("Couldn't get consumer IR manager!");
    }

    void setFreq(int freq){
        this.freq = freq;
    }

    void send(String data) throws NullPointerException, UnsupportedOperationException{
        if(consumerIrManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (consumerIrManager.hasIrEmitter()) {
                    data = data.replace(" ", "");
                    int[] pattern = new int[data.length() * 2 + 3];
                    pattern[0] = startPulseLen;
                    pattern[1] = startPulseSepLen;
                    pattern[2] = separatorLen;
                    for (int i = 3, j = 0; j < data.length(); i += 2, j++) {
                        pattern[i] = (data.charAt(j) == '1' ? oneLen : zeroLen);
                        pattern[i + 1] = separatorLen;
                    }

                    consumerIrManager.transmit(freq, pattern);
                } else throw new UnsupportedOperationException("This Phone Does't have any IR Hardware Component");
            } else throw new UnsupportedOperationException("Your Android version doesn't support this");
        } else throw new UnsupportedOperationException("No consumer IR manager found!");
    }
}
