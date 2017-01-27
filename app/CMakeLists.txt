# This was made by the guidance of -
# https://developer.android.com/studio/projects/add-native-code.html#create-cmake-script

# Set the CMake minimum required version.
# Remember to install the below CMake version or above.
cmake_minimum_required(VERSION 3.4.1)

# Specified a path to native header files.
include_directories(src/main/cpp/ )
include_directories(src/main/cpp/x264/)

# Adding the prebuilt x264 library.
# In case you're wondering what it is, it is the base x264 library built by VideoLAN.
# I built it with Linux, but you are free to use anything to build it.
# To build it, search something like "android x264 library" and find a modern tutorial.
add_library( x264-lib
             STATIC
             IMPORTED )
set_target_properties( # Specifies the target library.
                       x264-lib

                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION

                       # Provides the path to the library you want to import.
                       D:/SoftwareDevelopment/GitHub-Projects/realtime-camera/app/src/main/cpp/x264/android-static/${ANDROID_ABI}/lib/libx264.a )

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add.library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.
add_library( # Specifies the name of the library.
             video-encoder
             # Sets the library as a shared library.
             SHARED
             # Provides a relative path to your source file(s).
             src/main/cpp/main_jni.cpp
             src/main/cpp/h264encoder.cpp )

target_include_directories( video-encoder PRIVATE
                            src/main/cpp/ )

# Links your native library against one or more other native libraries.
target_link_libraries( video-encoder x264-lib )