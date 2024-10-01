#!/bin/bash

# global variables
debug=""

# check debug environment variables
function check_debug()
{
    debug=`echo $DEBUG`
}

# print error log
function fail_log()
{
    print_log "red" "$1"
}

# print success log
function success_log()
{
    print_log "green" "$1"
}

# print debug log
function debug_log()
{
    if [ "$debug" != "" ]; then
        print_log "yellow" "$1"
    fi
}

function print_log()
{
    local Default="\033[0m"
    local Red="\033[31m"
    local Green="\033[32m"
    local Yellow="\033[33m"

    case $1 in
        "red")
            echo -e "$Red"$2"$Default"
            ;;
        "green")
            echo -e "$Green"$2"$Default"
            ;;
        "yellow")
            echo -e "$Yellow"$2"$Default"
            ;;
        *)
            ;;
    esac
}

# find service in directories
function find_service()
{
    local excludes="ModuMessenger README.md docker-compose.yml build.sh infra"
    local list=`ls`
    local result=""

    for service in $list; do
        local build=true

        for exclude in $excludes; do
            if [ "$service" == "$exclude" ]; then
                build=false
                break;
            fi
        done

        if [ "$build" == true ]; then
            result="$result $service"
        fi
    done

    echo "$result"
}

# build service with gradle
function build_service()
{
    debug_log "[DEBUG] $1 service build start"

    if [ "$1" == "" ]; then
        fail_log "[ERROR] Invalid parameter"
        exit
    fi


    debug_log "[DEBUG] cd $service"
    cd "$1"

    debug_log "[DEBUG] build $service with gradle"

    ./gradlew clean build
    if [ "$?" != 0 ]; then
        fail_log "[ERROR] $1 build fail"
        exit
    fi

    success_log "[INFO] $1 build finish"

    cd ..
}

# build all service
function build_all()
{
    debug_log "[DEBUG] run all service"

    local result=$(find_service)
    for service in $result; do
        build_service $service
    done
}

# build single service
function build_single()
{   
    build_service $1
    container_recreate $1
}

# recreate target service
function container_recreate()
{
    docker-compose up -d --force-recreate --build $1
}

# start all container
function container_start()
{
    docker-compose up -d
}

# stop all container
function container_stop()
{
    docker-compose stop
}

# remove all container
function container_remove()
{
    docker-compose rm
}

# remove all container images
function container_remove_images()
{
    docker-compose rmi
}

# main function
function main()
{
    check_debug

    if [ "$1" == "" ]; then
        build_all
    else
        build_single $1
    fi

    if [ "$?" != 0 ]; then
        exit
    fi

    container_stop
    container_start
}

# start script
main $1
