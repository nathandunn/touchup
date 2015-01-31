#!/bin/bash

DIRNAME=`dirname $0`
java -Xmx1600m -jar "$DIRNAME"/bin/touchup-all.jar $@
