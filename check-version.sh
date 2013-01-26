#!/bin/bash

set -eux

mvn versions:display-dependency-updates \
    versions:display-plugin-updates
