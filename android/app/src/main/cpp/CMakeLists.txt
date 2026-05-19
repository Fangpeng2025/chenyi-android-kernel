cmake_minimum_required(VERSION 3.18.1)

project("RapidOcr")

# 设置 C++ 标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# OpenMP 支持
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fopenmp")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fopenmp")

# 优化选项
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fomit-frame-pointer -fstrict-aliasing -ffast-math")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fomit-frame-pointer -fstrict-aliasing -ffast-math")

# 隐藏符号
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fvisibility=hidden")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fvisibility=hidden -fvisibility-inlines-hidden")

# 包含目录
include_directories(include)

# 源文件
file(GLOB OCR_SRC src/*.cpp)

# 创建库
add_library(RapidOcr SHARED ${OCR_SRC})

# 查找 log 库
find_library(log-lib log)

# 链接库
target_link_libraries(RapidOcr
    android
    log
    jnigraphics
    ${log-lib}
)
