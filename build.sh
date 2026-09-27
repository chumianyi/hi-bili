#!/bin/bash
set -e

# Hi！bili 构建脚本
# 纯命令行构建，不依赖 Gradle/Android Studio

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/build"
ANDROID_JAR="$ANDROID_HOME/platforms/android-34/android.jar"
BUILD_TOOLS="$ANDROID_HOME/build-tools/34.0.0"

echo "=== Hi！bili Build ==="
echo "Project: $PROJECT_DIR"
echo "Android JAR: $ANDROID_JAR"

# 清理
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/dex"

# 1. 编译资源 (aapt2)
echo "[1/6] Compiling resources..."
"$BUILD_TOOLS/aapt2" compile --dir "$PROJECT_DIR/res" -o "$BUILD_DIR/res.zip"

# 2. 链接资源 + 生成 R.java
echo "[2/6] Linking resources..."
"$BUILD_TOOLS/aapt2" link \
    -I "$ANDROID_JAR" \
    --manifest "$PROJECT_DIR/AndroidManifest.xml" \
    -R "$BUILD_DIR/res.zip" \
    --java "$BUILD_DIR/gen" \
    --min-sdk-version 1 \
    --target-sdk-version 17 \
    -o "$BUILD_DIR/resources.apk" \
    --auto-add-overlay

# 3. 编译 Java
echo "[3/6] Compiling Java..."
find "$PROJECT_DIR/src" "$BUILD_DIR/gen" -name "*.java" > "$BUILD_DIR/sources.txt"
javac -source 1.7 -target 1.7 \
    -bootclasspath "$ANDROID_JAR" \
    -classpath "$ANDROID_JAR" \
    -d "$BUILD_DIR/obj" \
    @"$BUILD_DIR/sources.txt"

# 4. 转换为 DEX
echo "[4/6] Converting to DEX..."
"$BUILD_TOOLS/d8" \
    --min-api 1 \
    --output "$BUILD_DIR/dex" \
    --lib "$ANDROID_JAR" \
    $(find "$BUILD_DIR/obj" -name "*.class")

# 5. 打包 APK
echo "[5/6] Packaging APK..."
cp "$BUILD_DIR/resources.apk" "$BUILD_DIR/HiBili_unsigned.apk"
cd "$BUILD_DIR/dex"
zip -u "$BUILD_DIR/HiBili_unsigned.apk" classes.dex
cd "$PROJECT_DIR"

# 6. Zipalign + 签名
echo "[6/6] Zipalign and sign..."
"$BUILD_TOOLS/zipalign" -f -p 4 "$BUILD_DIR/HiBili_unsigned.apk" "$BUILD_DIR/HiBili_aligned.apk"

# 生成 keystore
keytool -genkeypair -v \
    -keystore "$BUILD_DIR/hibili.keystore" \
    -alias hibili \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass hibili -keypass hibili \
    -dname "CN=HiBili, OU=Dev, O=HiBili, L=Shanghai, ST=Shanghai, C=CN" 2>/dev/null

"$BUILD_TOOLS/apksigner" sign \
    --ks "$BUILD_DIR/hibili.keystore" \
    --ks-pass pass:hibili \
    --key-pass pass:hibili \
    --out "$BUILD_DIR/Hi！bili.apk" \
    "$BUILD_DIR/HiBili_aligned.apk"

"$BUILD_TOOLS/apksigner" verify --verbose "$BUILD_DIR/Hi！bili.apk"

echo ""
echo "=== Build Complete ==="
ls -lh "$BUILD_DIR/Hi！bili.apk"
