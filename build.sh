#!/bin/bash
set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
OUTPUT_DIR="$PROJECT_DIR/output"
APP_NAME="Hi！bili"

# Android SDK paths - detect available platform
ANDROID_PLATFORM=""
for p in 34 33 32 31 30 29 28 27 26 25 24 23 22 21 17; do
    if [ -f "$ANDROID_HOME/platforms/android-$p/android.jar" ]; then
        ANDROID_PLATFORM="$p"
        break
    fi
done
if [ -z "$ANDROID_PLATFORM" ]; then
    echo "ERROR: No Android platform found"
    exit 1
fi
echo "Using platform android-$ANDROID_PLATFORM"
ANDROID_JAR="$ANDROID_HOME/platforms/android-$ANDROID_PLATFORM/android.jar"
BUILD_TOOLS="$ANDROID_HOME/build-tools/34.0.0"

mkdir -p "$BUILD_DIR" "$OUTPUT_DIR"

echo "=== 1. 编译资源 (aapt2) ==="
mkdir -p "$BUILD_DIR/compiled_res"
find "$PROJECT_DIR/res" -type f \( -name "*.xml" -o -name "*.png" -o -name "*.jpg" \) | while read f; do
    "$BUILD_TOOLS/aapt2" compile "$f" -o "$BUILD_DIR/compiled_res/" 2>/dev/null || true
done

"$BUILD_TOOLS/aapt2" link \
    -I "$ANDROID_JAR" \
    --manifest "$PROJECT_DIR/AndroidManifest.xml" \
    -R "$BUILD_DIR/compiled_res/"*.flat \
    -o "$BUILD_DIR/app.apk" \
    --java "$BUILD_DIR/gen" \
    --auto-add-overlay 2>&1 | tail -5

echo "=== 2. 编译 Java ==="
mkdir -p "$BUILD_DIR/classes"
find "$PROJECT_DIR/src" "$BUILD_DIR/gen" -name "*.java" > "$BUILD_DIR/sources.txt"

# Include zxing jar in classpath
CLASSPATH="$ANDROID_JAR"
if [ -f "$PROJECT_DIR/libs/core.jar" ]; then
    CLASSPATH="$CLASSPATH:$PROJECT_DIR/libs/core.jar"
fi

javac -source 1.7 -target 1.7 \
    -cp "$CLASSPATH" \
    -d "$BUILD_DIR/classes" \
    @"$BUILD_DIR/sources.txt" 2>&1 | tail -10

echo "=== 3. DEX 转换 (d8/dx) ==="
DEX_INPUTS=$(find "$BUILD_DIR/classes" -name "*.class")
if [ -f "$BUILD_TOOLS/d8" ]; then
    echo "Using d8"
    if [ -f "$PROJECT_DIR/libs/core.jar" ]; then
        "$BUILD_TOOLS/d8" --min-api 1 --output "$BUILD_DIR" $DEX_INPUTS "$PROJECT_DIR/libs/core.jar" 2>&1 | tail -5
    else
        "$BUILD_TOOLS/d8" --min-api 1 --output "$BUILD_DIR" $DEX_INPUTS 2>&1 | tail -5
    fi
else
    echo "d8 not found, using dx fallback"
    # dx needs a jar or directory
    if [ -f "$PROJECT_DIR/libs/core.jar" ]; then
        "$BUILD_TOOLS/dx" --dex --output="$BUILD_DIR/classes.dex" "$BUILD_DIR/classes" "$PROJECT_DIR/libs/core.jar" 2>&1 | tail -5
    else
        "$BUILD_TOOLS/dx" --dex --output="$BUILD_DIR/classes.dex" "$BUILD_DIR/classes" 2>&1 | tail -5
    fi
fi

echo "=== 4. 打包 APK ==="
cd "$BUILD_DIR"
cp app.apk app_unsigned.apk
# Add classes.dex
zip -j app_unsigned.apk classes.dex
# Add resources (mipmap icons etc already in apk from aapt2 link)

echo "=== 5. zipalign ==="
"$BUILD_TOOLS/zipalign" -f 4 app_unsigned.apk app_aligned.apk

echo "=== 6. 签名 ==="
# Generate debug keystore if not exists
if [ ! -f "$BUILD_DIR/debug.keystore" ]; then
    keytool -genkeypair -v -keystore "$BUILD_DIR/debug.keystore" \
        -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
        -storepass android -keypass android \
        -dname "CN=Debug,O=Android,C=US" 2>/dev/null
fi

"$BUILD_TOOLS/apksigner" sign \
    --v1-signing-enabled true \
    --v2-signing-enabled true \
    --ks "$BUILD_DIR/debug.keystore" \
    --ks-key-alias androiddebugkey \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out "$OUTPUT_DIR/${APP_NAME}.apk" \
    app_aligned.apk 2>&1 | tail -3

echo "=== 完成 ==="
ls -la "$OUTPUT_DIR/"
