# This was made by the guidance of -
# https://developer.android.com/studio/projects/add-native-code.html#create-cmake-script

# Set the CMake minimum required version.
# Remember to install the below CMake version or above.
cmake_minimum_required(VERSION 3.4.1)

# Added the image-processor library
add_library( # Specifies the name of the library.
             image-processor
             # Sets the library as a shared library.
             SHARED
             # Provides a relative path to your source file(s).
             processor.cpp)

# Add the jni-graphics library
find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.
              jni-graphics-lib
              # Specifies the name of the NDK library that
              # CMake needs to locate.
              jnigraphics )

# Add the log library
find_library( # Defines the name of the path variable that stores the
              # location of the NDK library.
              log-lib
              # Specifies the name of the NDK library that
              # CMake needs to locate.
              log )

# Add the OpenCV Java library
add_library( lib-opencv-java
             SHARED
             IMPORTED )
set_target_properties( # Specifies the target library.
                       lib-opencv-java
                       # Specifies the parameter you want to define.
                       PROPERTIES IMPORTED_LOCATION
                       # Provides the path to the library you want to import.
                       /root/Documents/SoftwareDevelopment/Android-Workspace/realtime-camera/app/src/main/jniLibs/${ANDROID_ABI}/libopencv_java3.so )

# Specifies a path to OpenCV and OpenCL header files.
include_directories(include/)

# Link the OpenCV library with the image-processor library
target_link_libraries( image-processor ${log-lib} ${jni-graphics-lib} lib-opencv-java)