#!/bin/bash

result=( $(cat "./server_config.json" | jq -r 'keys[]') )

array_len=${#result[@]}

for type in "${result[@]}"; do
    port=( $(cat "./server_config.json" | jq -r ''.$type' | .port' ))
    next_port=( $(cat "./server_config.json" | jq -r ''.$type' | .next_port' ))
    ring_position=( $(cat "./server_config.json" | jq -r ''.$type' | .ring_position' ))
    
    gnome-terminal -e 'bash -c "./setup_java_server.sh '$port' '$next_port' '$ring_position' '$array_len'; exec bash"'
done


result=( $(cat "./peer_config.json" | jq -r 'keys[]') )

for type in "${result[@]}"; do
    name=( $(cat "./peer_config.json" | jq -r ''.$type' | .name' ))
    port=( $(cat "./peer_config.json" | jq -r ''.$type' | .port' ))
    server_port=( $(cat "./peer_config.json" | jq -r ''.$type' | .server_port' ))
    
    gnome-terminal -e 'bash -c "./setup_java_peer.sh '$name' '$port' '$server_port'; exec bash"'
done