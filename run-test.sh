#!/bin/bash

set -a
source .env.test
set +a
export BOOTSTRAP_SUPER_ADMIN_PASSWORD='admin1234'
./gradlew bootRun --args='--spring.profiles.active=test'
