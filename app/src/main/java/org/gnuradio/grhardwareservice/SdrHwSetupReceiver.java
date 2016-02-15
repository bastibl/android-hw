package org.gnuradio.grhardwareservice;

import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

public class SdrHwSetupReceiver extends ResultReceiver {

    private Receiver mReceiver;

    public SdrHwSetupReceiver(Handler handler) {
        super(handler);
    }

    public void setReceiver(Receiver receiver) {
        mReceiver = receiver;
    }

    public interface Receiver {
        void onReceiveResult(int resultCode, Bundle resultData);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
        if (mReceiver != null) {
            mReceiver.onReceiveResult(resultCode, resultData);
        }
    }
}
