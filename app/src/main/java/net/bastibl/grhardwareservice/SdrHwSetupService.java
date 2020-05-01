package net.bastibl.grhardwareservice;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import org.xmlpull.v1.XmlPullParser;

import java.util.HashMap;
import java.util.HashSet;

public class SdrHwSetupService extends JobIntentService {
    public static final int JOB_ID = 1;

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String CHANNEL_ID = "foo";

    int notifyID = 1;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, SdrHwSetupService.class, JOB_ID, work);
    }

    private void createNotificationChannel() {
        CharSequence name = "foo";
        String description = "bar";
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {

        Log.d("SdrHwSetupService", "onHandleIntent Called");

        if(Looper.myLooper() == null) {
            Looper.prepare();
        }

        createNotificationChannel();

        NotificationManager mNotificationManager = getSystemService(NotificationManager.class);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.gr_icon)
                .setContentTitle("GNU Radio")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        UsbDevice device = null;

        Log.d("UhdSetupService", "Didn't get a device; finding it now.");

        final HashSet<String> allowed_devices = getAllowedDevices(this);
        final HashMap<String, UsbDevice> usb_device_list = mUsbManager.getDeviceList();

        for (UsbDevice candidate : usb_device_list.values()) {
            String candstr = "v" + candidate.getVendorId() + "p" + candidate.getProductId();
            if (allowed_devices.contains(candstr)) {
                // Need to handle case where we have more than one device connected
                device = candidate;
            }
        }

        if (device == null) {
            mBuilder.setContentText("Unable to find USB device");
            mNotificationManager.notify(notifyID, mBuilder.build());
            Log.d("UhdSetupService", "Unable to find USB device");
            Toast.makeText(getApplicationContext(), "Unable to find USB device", Toast.LENGTH_LONG).show();
            return;
        }

        mUsbManager.requestPermission(device, mPermissionIntent);

        final UsbDeviceConnection connection = mUsbManager.openDevice(device);
        if (connection == null) {
            mBuilder.setContentText("Unable to connect to USB device!");
            mNotificationManager.notify(notifyID, mBuilder.build());
            Log.d("UhdSetupService", "Could not create a connection to USB device");
            Toast.makeText(getApplicationContext(), "Could not create a connection to USB device!", Toast.LENGTH_LONG).show();
            return;
        }

        int fd = connection.getFileDescriptor();
        String usbfs_path = device.getDeviceName();
        String manufacturer = device.getManufacturerName();

        if (manufacturer.equals("Cypress")) {
            mBuilder.setContentText("B200 firmware not loaded; loading.");
            mNotificationManager.notify(notifyID, mBuilder.build());
            Log.d("UhdSetupService", "B200 firmware not loaded; loading.");
            UHDLoadFirmware(fd, usbfs_path);
        } else if (manufacturer.equals("Ettus Research LLC")) {
                    mBuilder.setContentText("B200 firmware loaded.");
                    mNotificationManager.notify(notifyID, mBuilder.build());
                    Log.d("UhdSetupService", "B200 firmware loaded.");
        }
    }

    /*
     * Reads from the device_filter.xml file to get a list of the allowable devices.
     */
    private static HashSet<String> getAllowedDevices(final Context ctx) {
        final HashSet<String> ans = new HashSet<String>();
        try {
            final XmlResourceParser xml = ctx.getResources().getXml(R.xml.device_filter);

            xml.next();
            int eventType;
            while ((eventType = xml.getEventType()) != XmlPullParser.END_DOCUMENT) {

                if (eventType == XmlPullParser.START_TAG) {
                    if (xml.getName().equals("usb-device")) {
                        final AttributeSet as = Xml.asAttributeSet(xml);
                        final Integer vendorId = Integer.valueOf(as.getAttributeValue(null, "vendor-id"), 10);
                        final Integer productId = Integer.valueOf(as.getAttributeValue(null, "product-id"), 10);
                        ans.add("v" + vendorId + "p" + productId);
                    }
                }
                xml.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ans;
    }

    public native static void UHDLoadFirmware(int fd, String devname);

    static {
        System.loadLibrary("uhdlib");
    }
}
