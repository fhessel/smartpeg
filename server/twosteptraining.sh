#!/bin/bash
python net.py --lr 0.001 --epochs 500 --data "data/${1}.csv.train.npy" --save "models/${1}intermediate.ckpt"
python net.py --lr 0.0001 --epochs 500 --data "data/${1}.csv.train.npy" --refine "models/${1}intermediate.ckpt" --save "models/${1}final.ckpt"

