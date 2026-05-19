#!/bin/bash
# 下载 RapidOCR 模型文件

MODELS_DIR="android/app/src/main/assets"
mkdir -p $MODELS_DIR

echo "正在下载 OCR 模型文件..."

# 检测模型
curl -L -o $MODELS_DIR/ch_PP-OCRv3_det_infer.onnx \
  "https://huggingface.co/RapidAI/RapidOcrOnnxLibrary/resolve/main/models/ch_PP-OCRv3_det_infer.onnx"

# 分类模型
curl -L -o $MODELS_DIR/ch_ppocr_mobile_v2.0_cls_infer.onnx \
  "https://huggingface.co/RapidAI/RapidOcrOnnxLibrary/resolve/main/models/ch_ppocr_mobile_v2.0_cls_infer.onnx"

# 识别模型
curl -L -o $MODELS_DIR/ch_PP-OCRv3_rec_infer.onnx \
  "https://huggingface.co/RapidAI/RapidOcrOnnxLibrary/resolve/main/models/ch_PP-OCRv3_rec_infer.onnx"

# 字典文件
curl -L -o $MODELS_DIR/ppocr_keys_v1.txt \
  "https://huggingface.co/RapidAI/RapidOcrOnnxLibrary/resolve/main/models/ppocr_keys_v1.txt"

echo "模型下载完成！"
ls -lh $MODELS_DIR/
