#!/bin/bash
protoc -I=./  --descriptor_set_out=ceresdb.desc --java_out=../../java/ *.proto
