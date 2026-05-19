# OCR 模型文件说明

由于网络限制，OCR 模型文件需要手动下载。

## 需要的模型文件

请将以下文件放到 `android/app/src/main/assets/` 目录：

1. **ch_PP-OCRv3_det_infer.onnx** - 文本检测模型 (~10MB)
2. **ch_ppocr_mobile_v2.0_cls_infer.onnx** - 文本分类模型 (~2MB)
3. **ch_PP-OCRv3_rec_infer.onnx** - 文本识别模型 (~10MB)
4. **ppocr_keys_v1.txt** - 字典文件 (~10KB)

## 下载地址

### HuggingFace（推荐）
https://huggingface.co/RapidAI/RapidOcrOnnxLibrary/tree/main/models

### GitHub Release
https://github.com/RapidAI/RapidOcrAndroidOnnx/releases

### 百度网盘
链接: https://pan.baidu.com/s/1xxx
提取码: xxxx

## 下载后目录结构

```
android/app/src/main/assets/
├── ch_PP-OCRv3_det_infer.onnx
├── ch_ppocr_mobile_v2.0_cls_infer.onnx
├── ch_PP-OCRv3_rec_infer.onnx
└── ppocr_keys_v1.txt
```

## 验证

下载完成后，运行：
```bash
ls -lh android/app/src/main/assets/
```

应该看到 4 个文件，总大小约 22MB。
