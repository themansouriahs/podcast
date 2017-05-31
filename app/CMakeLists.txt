# Sets the minimum version of CMake required to build your native library.
# This ensures that a certain set of CMake features is available to
# your build.

cmake_minimum_required(VERSION 3.4.1)


# Sets lib_src_DIR to the path of the target CMake project.
set( lib_src_DIR . )

# Specifies a library name, specifies whether the library is STATIC or
# SHARED, and provides relative paths to the source code. You can
# define multiple libraries by adding multiple add.library() commands,
# and CMake builds them for you. When you build your app, Gradle
# automatically packages shared libraries with your APK.

add_library( # Specifies the name of the library.
             sonic2

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             ${lib_build_DIR}/sonicjni.c
             ${lib_build_DIR}/sonic.c )

# Specifies a path to native header files.
include_directories(${lib_build_DIR}/include)