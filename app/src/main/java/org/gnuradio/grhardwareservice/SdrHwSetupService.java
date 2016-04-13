package org.gnuradio.grhardwareservice;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SdrHwSetupService extends IntentService {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final String DEFAULT_USBFS_PATH = "/dev/bus/usb";

    private PendingIntent mPermissionIntent;
    private UsbManager mUsbManager;

    private static String usbfs_path = null;
    private static int fd = -1;
    private static boolean ready = false;
    int notifyID = 1;

    private static final String UHD_USB_INFO_FILE = "uhd_usb_info.txt";

    public SdrHwSetupService() {
        super("SdrHwSetupService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("SdrHwSetupService", "onHandleIntent Called");

        DeleteUSBInfoFile();

        AssetManager amanager = getResources().getAssets();
        UnPackUhdImages(amanager);
        MoveConfigFiles();

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        Notification.Builder mBuilder = new Notification.Builder(this)
                .setSmallIcon(R.drawable.gr_icon)
                .setContentTitle("GNU Radio");

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifyID, mBuilder.build());

        UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (device == null) {
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
        }
        Log.d("UhdSetupService", "Selected Device: " + device);

        if (device == null) {
            mBuilder.setContentText("Unable to find USB device");
            mNotificationManager.notify(notifyID, mBuilder.build());
            Log.d("UhdSetupService", "Unable to find USB device");
            Toast.makeText(getApplicationContext(), "Unable to find USB device", Toast.LENGTH_LONG).show();
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

        fd = connection.getFileDescriptor();
        usbfs_path = properDeviceName(device.getDeviceName());

        int vid = device.getVendorId();
        int pid = device.getProductId();
        int fw_loaded = IsUHDFirmwareLoaded(vid, pid, fd, usbfs_path);
        if (fw_loaded == -1) {
            mBuilder.setContentText("Can not connect to B200");
            mNotificationManager.notify(notifyID, mBuilder.build());
            Log.d("UhdSetupService", "Can not connect to B200");
            Toast.makeText(getApplicationContext(), "Can not connect to B200", Toast.LENGTH_LONG).show();
        } else if (fw_loaded == 0) {
            mBuilder.setContentText("B200 firmware not loaded; loading.");
            mNotificationManager.notify(notifyID, mBuilder.build());
            Log.d("UhdSetupService", "B200 firmware not loaded; loading.");
            UHDLoadFirmware(fd, usbfs_path);
        } else if (fw_loaded == 1) {
            int fpga_ready = IsUHDFPGAReady(vid, pid, fd, usbfs_path);
            if(fpga_ready == 1) {
                ready = true;
                WriteUSBInfoFile();
            }
            else {
                mBuilder.setContentText("B200 firmware loaded; now loading FPGA image.");
                mNotificationManager.notify(notifyID, mBuilder.build());
                Log.d("UhdSetupService", "B200 firmware loaded; now loading FPGA image.");
                UHDMakeDevice(fd, usbfs_path);
                mNotificationManager.cancel(notifyID);
                Log.d("UhdSetupService", "B200 FPGA image loaded. B200 is ready.");
                Toast.makeText(getApplicationContext(), "B200 Ready", Toast.LENGTH_LONG).show();
                ready = true;
                WriteUSBInfoFile();
            }
        } else {
            mBuilder.setContentText("B200 Firmware check returned unknown code.");
            mNotificationManager.notify(notifyID, mBuilder.build());
            Log.d("UhdSetupService", "B200 Firmware check returned unknown code.");
            Toast.makeText(getApplicationContext(), "B200 Firmware check returned unknown code.", Toast.LENGTH_LONG).show();
        }
    }

    private void MoveConfigFiles() {
        Log.d("UhdSetupService", "Moving Config Files");
        File sdcard = new File("/sdcard");

        File grconf = new File(sdcard + "/config.conf");
        Log.d("UhdSetupService", "GR Conf: " + grconf);
        File thconf = new File(sdcard + "/thrift.conf");
        Log.d("UhdSetupService", "Thrift Conf: " + thconf);
        File grdir = new File(sdcard + "/.gnuradio");
        Log.d("UhdSetupService", "GR DIR " + grdir);
        if(!grdir.exists()) {
            Log.d("UhdSetupService", "Making " + grdir);
            grdir.mkdirs();
        }
        Log.d("UhdSetupService", "moving " + grconf);
        grconf.renameTo(new File(grdir + "/config.conf"));
        Log.d("UhdSetupService", "moving " + thconf);
        thconf.renameTo(new File(grdir + "/thrift.conf"));

        File vlkconf = new File(sdcard + "/volk_config");
        File vlkdir = new File(sdcard + "/.volk/");
        if(!vlkdir.exists()) {
            Log.d("UhdSetupService", "Making " + vlkdir);
            vlkdir.mkdirs();
        }
        Log.d("UhdSetupService", "moving " + vlkconf);
        vlkconf.renameTo(new File(vlkdir + "/volk_config"));
    }

    private void DeleteUSBInfoFile() {
        File sdcard = Environment.getExternalStorageDirectory();
        File fname = new File(sdcard, UHD_USB_INFO_FILE);
        fname.delete();
    }

    private void WriteUSBInfoFile()
    {
        File sdCard = Environment.getExternalStorageDirectory();
        File fname = new File(sdCard, UHD_USB_INFO_FILE);

        String strfd = Integer.toString(fd) + "\n";
        String strusb = usbfs_path + "\n";

        FileOutputStream f = null;
        try {
            f = new FileOutputStream(fname);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            f.write(strfd.getBytes());
            f.write(strusb.getBytes());
            f.close();
        } catch (IOException e) {
            e.printStackTrace();
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

                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (xml.getName().equals("usb-device")) {
                            final AttributeSet as = Xml.asAttributeSet(xml);
                            final Integer vendorId = Integer.valueOf(as.getAttributeValue(null, "vendor-id"), 10);
                            final Integer productId = Integer.valueOf(as.getAttributeValue(null, "product-id"), 10);
                            ans.add("v"+vendorId+"p"+productId);
                        }
                        break;
                }
                xml.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ans;
    }

    public final static String properDeviceName(String deviceName) {
        if (deviceName == null) return DEFAULT_USBFS_PATH;
        deviceName = deviceName.trim();
        if (deviceName.isEmpty()) return DEFAULT_USBFS_PATH;

        final String[] paths = deviceName.split("/");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < paths.length-2; i++)
            if (i == 0)
                sb.append(paths[i]);
            else
                sb.append("/").append(paths[i]);
        final String stripped_name = sb.toString().trim();
        if (stripped_name.isEmpty())
            return DEFAULT_USBFS_PATH;
        else
            return stripped_name;
    }

    public static boolean isUhdReady() { return ready; };
    public static int getFd() { return fd; };
    public static String getUSBFSPath() { return usbfs_path; };

    public native static void UnPackUhdImages(AssetManager amanager);
    public native static int IsUHDFirmwareLoaded(int vid, int pid, int fd, String devname);
    public native static int IsUHDFPGAReady(int vid, int pid, int fd, String devname);
    public native static void UHDLoadFirmware(int fd, String devname);
    public native static void UHDMakeDevice(int fd, String devname);

    static {
        System.loadLibrary("uhdlib");
    }
}
