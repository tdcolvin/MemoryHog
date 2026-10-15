cmake_minimum_required(VERSION 3.22.1)

project("lmktester")

add_library(
    lmktester
    SHARED
    lmktester.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    lmktester
    ${log-lib}
)
