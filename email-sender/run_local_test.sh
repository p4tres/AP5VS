#!/bin/bash
./gradlew build
java -jar NATT.jar -nc https://gitlab.com/ap5vs/test-config/email-sender-config/-/raw/main/test-config-1.yaml?ref_type=heads
