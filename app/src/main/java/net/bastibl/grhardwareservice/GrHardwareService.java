package net.bastibl.grhardwareservice;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class GrHardwareService extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("SdrHwSetup", "OnCreate Entered");

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        } else {
            Intent uhdIntent = new Intent(this, SdrHwSetupService.class);
            SdrHwSetupService.enqueueWork(this, uhdIntent);
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode == 123) {
            Intent uhdIntent = new Intent(this, SdrHwSetupService.class);
            SdrHwSetupService.enqueueWork(this, uhdIntent);
            finish();
        }
    };

}
