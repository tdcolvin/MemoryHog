cmake_minimum_required(VERSION 3.22.1)

project("memoryhog")

add_library(
    memoryhog
    SHARED
        memoryhog.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    memoryhog
    ${log-lib}
)
