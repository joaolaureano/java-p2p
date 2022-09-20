#!/bin/bash

# gnome-terminal  -e 'java app/server/Server' 

iterator=0

result=( $(cat "./test.json" | jq -r 'keys[]') )

for type in "${result[@]}"; do
    echo "--$type--"
    port=( $(cat "./test.json" | jq -r ''.$type' | .port' ))
    next_port=( $(cat "./test.json" | jq -r ''.$type' | .next_port' ))
    ring_position=( $(cat "./test.json" | jq -r ''.$type' | .ring_position' ))
    
    gnome-terminal -e 'bash -c "./teste.sh '$port' '$next_port' '$ring_position'; exec bash"'
done