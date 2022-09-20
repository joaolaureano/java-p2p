#!/bin/bash

result=( $(cat "./server_config.json" | jq -r 'keys[]') )

array_len=${#result[@]}

for type in "${result[@]}"; do
    port=( $(cat "./server_config.json" | jq -r ''.$type' | .port' ))
    next_port=( $(cat "./server_config.json" | jq -r ''.$type' | .next_port' ))
    ring_position=( $(cat "./server_config.json" | jq -r ''.$type' | .ring_position' ))
    
    gnome-terminal -e 'bash -c "./setup_java.sh '$port' '$next_port' '$ring_position' '$array_len'; exec bash"'
done