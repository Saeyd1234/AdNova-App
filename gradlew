#!/bin/sh

DIR=$(cd "$(dirname "$0")" && pwd)
exec java -Xmx2048m -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
