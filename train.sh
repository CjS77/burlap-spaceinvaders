#!/usr/bin/env bash
if [ "$#" -ne 3 ]; then
    echo "Usage:"
    echo "train numEps opponent pNum"
    echo "numEps: Number of training episodes"
    echo "opponent: SittingDuck | Foxhole | Feynman0 etc"
    echo "pNum: playerNumer of agent to train (0 or 1)"
else
    ./gradlew -q trainAgent "-PnEps=$1" "-Popp=$2" "-PpNum=$3"
fi