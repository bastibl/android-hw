package org.gnuradio.grhardwareservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class GrHardwareService extends Activity implements SdrHwSetupReceiver.Receiver {

    public SdrHwSetupReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("SdrHwSetup", "OnCreate Entered");

        mReceiver = new SdrHwSetupReceiver(new Handler());
        mReceiver.setReceiver(this);

        Intent uhdIntent = new Intent(this, SdrHwSetupService.class);
        uhdIntent.putExtra("receiver", mReceiver);
        startService(uhdIntent);
        finish();
    }

    public void onReceiveResult(int resultCode, Bundle resultData) {
        switch (resultCode) {
            case 0:
                Log.d("SdrHwSetup", "Service Running");
                break;
            case 1:
                Log.d("SdrHwSetup", "Service Finished");
                finish();
                break;
            case -1:
                Log.d("SdrHwSetup", "Service Error");
                finish();
                break;
        }
    }
}
