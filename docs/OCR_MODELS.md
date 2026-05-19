# OCR 模型文件下载说明

RapidOCR 需要 4 个模型文件才能工作。

## 方式一：从国内镜像下载（推荐）

### 1. 创建 assets 目录
```bash
mkdir -p android/app/src/main/assets
```

### 2. 下载模型文件

#### 百度网盘
链接: https://pan.baidu.com/s/1xxxxx
提取码: xxxx

#### 阿里云盘
链接: https://www.aliyundrive.com/s/xxxxx

#### 蓝奏云
链接: https://wwa.lanzoui.com/xxxxx

### 3. 解压到 assets 目录
```bash
unzip ocr_models.zip -d android/app/src/main/assets/
```

## 方式二：从 GitHub Release 下载

访问 RapidOCR Android 项目 Release 页面：
https://github.com/RapidAI/RapidOcrAndroidOnnx/releases

下载 APK 后解压获取 assets 文件：
```bash
# 下载 APK
wget https://github.com/RapidAI/RapidOcrAndroidOnnx/releases/download/1.3.0/RapidOcrAndroidOnnx-1.3.0-release.apk

# 解压 APK（APK 实际上是 ZIP 文件）
unzip RapidOcrAndroidOnnx-1.3.0-release.apk -d ocr_apk/

# 复制模型文件
cp ocr_apk/assets/*.onnx android/app/src/main/assets/
cp ocr_apk/assets/ppocr_keys_v1.txt android/app/src/main/assets/
```

## 方式三：从源码编译

如果需要自定义模型，可以从 PaddleOCR 官方下载：
https://github.com/PaddlePaddle/PaddleOCR/blob/release/2.6/doc/doc_ch/models_list.md

## 需要的文件清单

| 文件名 | 大小 | 说明 |
|--------|------|------|
| `ch_PP-OCRv3_det_infer.onnx` | ~10MB | 文本检测模型 |
| `ch_ppocr_mobile_v2.0_cls_infer.onnx` | ~2MB | 文本方向分类模型 |
| `ch_PP-OCRv3_rec_infer.onnx` | ~10MB | 文本识别模型 |
| `ppocr_keys_v1.txt` | ~10KB | 中文字典文件 |

## 验证安装

下载完成后，检查文件是否完整：
```bash
ls -lh android/app/src/main/assets/

# 应该看到：
# ch_PP-OCRv3_det_infer.onnx    (~10MB)
# ch_ppocr_mobile_v2.0_cls_infer.onnx (~2MB)
# ch_PP-OCRv3_rec_infer.onnx    (~10MB)
# ppocr_keys_v1.txt             (~10KB)
```

## 模型文件总大小

约 22MB，请确保有足够的存储空间。
