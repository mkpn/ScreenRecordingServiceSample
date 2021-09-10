# ScreenRecordingServiceSample
スマホ画面とマイク音声が入った動画の録画ができる

## 処理手順
### 録画前に以下を済ませる
1. contentResolver.insertでuri取得
2. contentResolver.openFileDescriptor(newUri, "w")　でfileDescriptorを取得
3. mediaRecorder.setOutputFile(file.fileDescriptor)　で録画ファイル保存先決定
4. 録画開始する

### 録画後
Android 10以上のみaddRecordingToMediaLibrary()を読んでファイルのアップデート必要

### 注意
まだAndroid 10でしか試せていない
