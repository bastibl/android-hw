# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.4.1)

add_library(uhdlib
            SHARED
            uhdlib.cpp )

find_library(log-lib log)
find_library(android-lib android)

set(TOOLCHAIN  $ENV{HOME}/src/gnuradio-android/toolchain)

target_include_directories(uhdlib PUBLIC
        ${TOOLCHAIN}/${ANDROID_ABI}/include
        ${TOOLCHAIN}/${ANDROID_ABI}/include/boost-1_69)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries(uhdlib
        ${TOOLCHAIN}/${ANDROID_ABI}/lib/libuhd.so
        ${TOOLCHAIN}/${ANDROID_ABI}/lib/libusb1.0.so
        ${android-lib}
        ${log-lib})