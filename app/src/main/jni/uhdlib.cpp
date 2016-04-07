#include <jni.h>
#include <string>
#include <cstdlib>
#include <errno.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <gnuradio/logger.h>
#include <uhd/device.hpp>
#include <uhd/transport/usb_control.hpp>
#include <uhd/transport/usb_device_handle.hpp>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

extern "C" {

JNIEXPORT void JNICALL
Java_org_gnuradio_grhardwareservice_SdrHwSetupService_UnPackUhdImages(JNIEnv * env, jobject thiz,
                                                                      jobject assetManager)
{
  int BUFSIZE=1024;
  AAssetManager* amanager = AAssetManager_fromJava(env, assetManager);
  AAssetDir* assetdir = AAssetManager_openDir(amanager, "");
  const char* filename = (const char*)NULL;
  while((filename = AAssetDir_getNextFileName(assetdir)) != NULL) {
    AAsset* asset = AAssetManager_open(amanager, filename, AASSET_MODE_STREAMING);
    char buf[BUFSIZE];
    int nread = 0;

#if 0
    int r = mkdir("/data/data/org.gnuradio.grhardwareservice/files", 0770);
    if(r == -1) {
      GR_INFO("UnPackUhdImages",
              boost::str(boost::format
                      ("Failed to create files directory with errno = 1%") % (int)errno));
      throw std::runtime_error("Failed to create files directory.");
    }
#endif

    std::stringstream outdir;
    //outdir << "/data/data/org.gnuradio.grhardwareservice/files/" << filename;
    outdir << "/sdcard/" << filename;

    GR_INFO("UnPackUhdImages",
            boost::str(boost::format
                       ("Asset file %1% being written to --> %2%")
                       % filename % outdir.str()));

    FILE* fout = fopen(outdir.str().c_str(), "wb");
    if(fout == 0) {
      GR_INFO("UnPackUhdImages",
              boost::str(boost::format
                        ("Unable to open file %1% (fout = %2%; errno = %3%)")
                        % outdir.str() % fout % (int)errno));
      throw std::runtime_error("Failed to open file.");
    }

    while((nread = AAsset_read(asset, buf, BUFSIZE)) > 0) {
      fwrite(buf, nread, 1, fout);
    }
    fclose(fout);
    AAsset_close(asset);
  }
  AAssetDir_close(assetdir);
}

JNIEXPORT int JNICALL
Java_org_gnuradio_grhardwareservice_SdrHwSetupService_IsUHDFirmwareLoaded(JNIEnv * env, jobject thiz,
                                                                          int vid, int pid, int fd,
                                                                          jstring devname)
{
  const char *usbfs_path = env->GetStringUTFChars(devname, NULL);
  std::stringstream args;
  args << "uhd,fd=" << fd << ",usbfs_path=" << usbfs_path;
  GR_INFO( "IsUHDFirmwareLoaded" , boost::str(boost::format(
          "Checking if firmware is loaded; Using UHD args=%1%") % args . str())) ;

  uhd::transport::usb_device_handle::sptr handle;
  std::vector <uhd::transport::usb_device_handle::sptr> handles;

  handles = uhd::transport::usb_device_handle::get_device_list(vid, pid, fd, usbfs_path);

  if(handles.size() > 0)
    handle = handles[0];
  else
    return -1;

  if(!handle)
    return -1;

  GR_INFO( "IsUHDFirmwareLoaded" , "B2xx detected..." ) ;
  if(handle -> firmware_loaded()) {
    return 1 ;
  }
  else {
    return 0;
  }
}

JNIEXPORT int JNICALL
Java_org_gnuradio_grhardwareservice_SdrHwSetupService_IsUHDFPGAReady(JNIEnv * env, jobject thiz,
                                                                     int vid, int pid, int fd,
                                                                     jstring devname)
{
  const char *usbfs_path = env->GetStringUTFChars(devname, NULL);
  std::stringstream args;
  args << "uhd,fd=" << fd << ",usbfs_path=" << usbfs_path;
  GR_INFO( "IsUHDFPGAReady" , boost::str(boost::format(
        "Checking if FPGA is ready; Using UHD args=%1%") % args . str())) ;

  uhd::transport::usb_device_handle::sptr handle;
  std::vector <uhd::transport::usb_device_handle::sptr> handles;

  handles = uhd::transport::usb_device_handle::get_device_list(vid, pid, fd, usbfs_path);

  if(handles.size() > 0)
    handle = handles[0];
  else
    return -1;

  if(!handle)
    return - 1 ;

  std::string prodname = handle->get_product();
  GR_INFO( "IsUHDFPGAReady" , boost::str(boost::format(
          "FPGA Product Name: = %1%") % prodname));

  if(prodname == "USRP B200xxx") {
    return 1 ;
  }
  else {
    return 0;
  }
}


JNIEXPORT void JNICALL
Java_org_gnuradio_grhardwareservice_SdrHwSetupService_UHDLoadFirmware(JNIEnv* env, jobject thiz,
                                                                      int fd, jstring devname)
{
  const char *c_devname = env->GetStringUTFChars(devname, NULL);
  std::stringstream args;
  args << "uhd,fd=" << fd << ",usbfs_path=" << c_devname;
  GR_INFO("UHDLoadFirmware",
    boost::str(boost::format("Loading Firmware; Using UHD args=%1%")
      % args.str()));

  uhd::device_addrs_t device_addrs = uhd::device::find(args.str());

  for(size_t i = 0; i<device_addrs.size(); i++) {
    GR_INFO("UHDLoadFirmware",
      boost::str(boost::format("Device Found: %1%")
        % (device_addrs[i].to_pp_string())));
  }
}

JNIEXPORT void JNICALL
Java_org_gnuradio_grhardwareservice_SdrHwSetupService_UHDMakeDevice(JNIEnv* env, jobject thiz,
                                                       int fd, jstring devname)
{
  const char *c_devname = env->GetStringUTFChars(devname, NULL);
  std::stringstream args;
  args << "uhd,fd=" << fd << ",usbfs_path=" << c_devname;
  GR_INFO("UHDLoadFirmware",
    boost::str(boost::format("Loading FPGA; Using UHD args=%1%")
      % args.str()));

  uhd::device::sptr dev = uhd::device::make(args.str(), uhd::device::USRP);

  GR_INFO("UHDMakeDevice",
    boost::str(boost::format("Device Built: %1%") % dev));
}

JNIEXPORT void JNICALL
Java_org_gnuradio_grhardwareservice_SdrHwSetupService_SetTMP(JNIEnv* env, jobject thiz,
		                                                         jstring tmpname)
{
  const char *tmp_c;
  tmp_c = env->GetStringUTFChars(tmpname, NULL);
  setenv("TMP", tmp_c, 1);
}

}
