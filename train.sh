#!/usr/bin/env bash
if [ "$#" -ne 5 ]; then
    echo "Usage:"
    echo "train numEps opponent pNum epsilon alpha"
    echo "numEps: Number of training episodes"
    echo "opponent: SittingDuck | Foxhole | Feynman0 etc"
    echo "pNum: playerNumer of agent to train (0 or 1)"
    echo "epsilion: The ε-greedy constant"
    echo "lrate: the learning rate, α"
else
#    ./gradlew build
    ./gradlew -q trainAgent "-PnEps=$1" "-Popp=$2" "-PpNum=$3" "-Pepsilon=$4" "-Plrate=$5"
fi
