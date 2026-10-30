# GitHubへの公開手順

1. GitHubで公開リポジトリを作成します。例: `x-media-url-copy`。
2. このフォルダのソース一式を登録します。`build/` と `dist/` は登録しません。
3. `LICENSE` と `NOTICE` を保持します。
4. Releases → Draft a new release でタグ `v0.1.0` を作成します。
5. `RELEASE_NOTES.md` の本文を説明に使い、`media-url-copy-0.1.0.mpp` を添付します。
6. 実機未確認の初回版なのでpre-releaseを選択して公開します。

XのAPK/APKM、署名鍵、個人のログ、ツールのJARは公開するソースに含めません。GitHubで公開したソースから対応するバージョンをビルドできる状態を維持してください。
