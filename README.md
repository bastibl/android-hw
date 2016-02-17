# Hardware Support for GNU Radio Apps

This is a service-only application used to program and set up access
to USRP hardware on Android devices. Specifically, the USRP B2xx
series over USB needs USB permissions and to have the firmware and
FPGA images programmed when first powered on. This applications is
designed to listen for a new USRP hardware device connected over USB
and make sure it is programmed and enabled for other GNU Radio
applications to use.

Programming happens in two stages. First, the device's firmware image
must be installed. When first run, the program will ask the user for
permission to use the USB device with this application;

"SDR HW Setup
Open SDR HW Setup when this USB device is connected?"

Check the "Use by default of this USB device" and click ok. It will
then load the USRP's firmware image, which actually changes the device
as far as the system is concerned. It _should_ then ask again for
permission to use the device. Again, click to use by default and
OK. It will then go to the second stage and install the FPGA image
onto the device.

This is an application that really only runs a service in the
background. Once the service is launched, the application kills itself
and lets the service continue. It produces notifications in the
notification bar, which you will see as the GNU Radio logo. Looking at
this, it will provide messages about the current behavior. If
everything is working well, it will say that it is:

* loading the firmware
* that loading the firmware is done
* that the firmware is loaded and it is now programming the FPGA

If something goes wrong, it may either get stuck in one of these
states or lose connection to the USB device, at which point it will
provide a USB error message in the notification area. This last is
easily triggered when trying to run the app by hand when a UHD device
is not plugged in.

Another intermittent issue observed is that the firmware is loaded
correctly but the program never moves on to load the FPGA image. The
notification bar will just keep saying it is loading the firmware,
which only takes a few seconds. If this happens, just unplug and
replug the USRP and watch to make sure it continues on to the FPGA
programming part this time.


# Building the Application Yourself

This application only interacts with the UHD devices over USB, but it
still requires a few libraries to be built through the Android NDK. We
have instructions for building the necessary dependencies for GNU
Radio applications. For this program, we only need a subset of them,
including:

* Boost
* libusb
* UHD

For Boost, see
http://gnuradio.org/redmine/projects/gnuradio/wiki/GRAndDeps. And for
libusb and the UHD library, see
http://gnuradio.org/redmine/projects/gnuradio/wiki/GRAndRFE.

When following the instructions, the JNI setup in this application,
controlled by GrAndroid.mk, assumes that the installation PREFIX is
/opt/grandroid. If you use a different prefix, amend then line in
GrAndroid.mk:

```
GRLIBPATH := /opt/grandroid/lib
```
