cmake_minimum_required(VERSION 3.18.1)

project("rapidocr")

# RapidOCR JNI 库
add_library(rapidocr SHARED
    rapidocr_jni.cpp
)

# 链接库
target_link_libraries(rapidocr
    android
    log
    jnigraphics
)
