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
Java_net_bastibl_grhardwareservice_SdrHwSetupService_UHDLoadFirmware(JNIEnv* env, jobject thiz,
                                                                      int fd, jstring devname)
{
  const char *c_devname = env->GetStringUTFChars(devname, NULL);
  std::stringstream args;
  args << "fd=" << fd << ",usbfs_path=" << c_devname;
  GR_INFO("UHDLoadFirmware",
    boost::str(boost::format("Loading Firmware; Using UHD args=%1%")
      % args.str()));

  uhd::device_addrs_t device_addrs = uhd::device::find(args.str());
}

}
