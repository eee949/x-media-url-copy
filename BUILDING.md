# Build and test

Windows / PowerShell向けです。開発用ツールはリポジトリに同梱していません。

## 必要なもの

検証した構成:

- JDK 25（Javaソースの出力はJava 17）
- Kotlin compiler embeddable 2.4.21とJetBrains annotationsのJAR
- Android API 36の `android.jar`
- R8/D8 9.6.0-alpha08
- Morphe Patcher 2.0.1とKotlin・コルーチン等の依存ライブラリを含むJVMランタイムJAR
  - 検証では `morphe-desktop-1.19.1-all.jar` を利用。
  - `app.morphe.patcher.*` と `com.android.tools.smali.dexlib2.*` をクラスパスに提供します。

各ファイルは正規の配布元から用意し、引数にそのパスを指定してください。JAR本体をこのリポジトリへコミットする必要はありません。

## ビルド

リポジトリのルートで実行します。下記の `tools` は自分のツール配置に合わせて変更してください。

```powershell
./build.ps1 `
  -Runtime ./tools/morphe-desktop-1.19.1-all.jar `
  -AndroidJar ./tools/android.jar `
  -R8 ./tools/r8.jar `
  -KotlinCompiler ./tools/kotlin-compiler-embeddable-2.4.21.jar `
  -Annotations ./tools/annotations.jar
```

`JAVA_HOME` が設定されていない場合はPATH上の `javac.exe` からJDKを検出します。別のJDKを使う場合は `-Jdk` を指定します。

出力: `dist/media-url-copy-0.1.0.mpp`

Java拡張とKotlinパッチのJVMクラス・Android DEXを作成し、パッチを1個だけ含むMPPにまとめます。Pikoのバンドルを取り込む工程はありません。

## ユニットテスト

```powershell
./test.ps1 -AndroidJar ./tools/android.jar
```

媒体取得処理はテスト用のネイティブ構造で検証します。Androidの画面やクリップボードを実行するテストではありません。

## 元APKMへの適用確認

元のAPK/APKMは各自で用意します。検証用の未署名APKは配布対象ではありません。

```powershell
java -Xmx4g -jar ./tools/morphe-desktop-1.19.1-all.jar patch `
  -p ./dist/media-url-copy-0.1.0.mpp --unsigned --striplibs arm64-v8a `
  -o ./build/test-unsigned.apk -t ./build/patching `
  -r ./build/result.json ./input/X-original.apkm
```

CLIの終了コードだけでなく、`result.json` の `success` と `failedPatches` を確認してください。単独適用なら `appliedPatches` は `Copy media URL` の1個です。

出力DEXを検査する場合:

```powershell
javac -cp ./tools/morphe-desktop-1.19.1-all.jar -d ./build/check ./tests/CheckPatchedApk.java
java -cp "./tools/morphe-desktop-1.19.1-all.jar;./build/check" CheckPatchedApk ./build/test-unsigned.apk
```

`CheckPatchedApk` は単独適用の出力専用です。他のパッチ集と併用した出力を合格にするためのものではありません。
