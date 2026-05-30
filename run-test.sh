#!/bin/bash

set -a
source .env.test
set +a

./gradlew bootRun --args='--spring.profiles.active=test'
