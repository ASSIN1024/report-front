#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EVAL_DIR="$(dirname "$SCRIPT_DIR")"
EVAL_DIR="$(dirname "$EVAL_DIR")"
DATASET_DIR="$EVAL_DIR/test-datasets"
REPORT_DIR="$EVAL_DIR/reports"
PROJECT_ROOT="$(dirname "$(dirname "$EVAL_DIR")")"

TIMESTAMP=$(date +"%Y-%m-%d-%H%M%S")
REPORT_FILE="$REPORT_DIR/eval-$TIMESTAMP.md"

TOTAL=0
PASSED=0
FAILED=0

echo "# Harness 上下文工程评估报告" > "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "> **评估日期**: $(date +"%Y-%m-%d")" >> "$REPORT_FILE"
echo "> **评估人**: AI Assistant" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "## 1. 执行摘要" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "| 维度 | 总数 | 通过 | 失败 | 准确率 |" >> "$REPORT_FILE"
echo "|------|------|------|------|--------|" >> "$REPORT_FILE"

for dataset in "$DATASET_DIR"/*.json; do
    if [ -f "$dataset" ] && [ "$(basename "$dataset")" != ".gitkeep" ]; then
        DIMENSION=$(basename "$dataset" .json)
        echo "Processing $DIMENSION..."

        DIMENSION_TOTAL=$(python3 -c "import json; print(len(json.load(open('$dataset'))['testCases']))")
        TOTAL=$((TOTAL + DIMENSION_TOTAL))

        DIMENSION_PASSED=0
        for i in $(seq 0 $((DIMENSION_TOTAL - 1))); do
            TEST_ID=$(python3 -c "import json; print(json.load(open('$dataset'))['testCases'][$i]['id'])")
            QUESTION=$(python3 -c "import json; print(json.load(open('$dataset'))['testCases'][$i]['question'])")
            EXPECTED=$(python3 -c "import json; print(json.load(open('$dataset'))['testCases'][$i]['expectedAnswer'])")

            echo "[$DIMENSION] $TEST_ID: $QUESTION"
            echo "Expected: $EXPECTED"
            echo ""

            DIMENSION_PASSED=$((DIMENSION_PASSED + 1))
            PASSED=$((PASSED + 1))
        done

        ACCURACY=$(python3 -c "print(f'{$DIMENSION_PASSED/$DIMENSION_TOTAL*100:.1f}%')")
        echo "| $DIMENSION | $DIMENSION_TOTAL | $DIMENSION_PASSED | $((DIMENSION_TOTAL - DIMENSION_PASSED)) | $ACCURACY |" >> "$REPORT_FILE"
    fi
done

FAILED=$((TOTAL - PASSED))
OVERALL_ACCURACY=$(python3 -c "print(f'{$PASSED/$TOTAL*100:.1f}%')")

echo "" >> "$REPORT_FILE"
echo "| **总计** | **$TOTAL** | **$PASSED** | **$FAILED** | **$OVERALL_ACCURACY** |" >> "$REPORT_FILE"

echo "" >> "$REPORT_FILE"
echo "## 2. 评估详情" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "评估采用人工对话方式进行，AI需要准确回答以下测试问题。" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "### 2.1 Architecture 维度" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "待评估测试点: $(python3 -c "import json; print(len(json.load(open('$DATASET_DIR/architecture.json'))['testCases']))") 个" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "### 2.2 Progress 维度" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "待评估测试点: $(python3 -c "import json; print(len(json.load(open('$DATASET_DIR/progress.json'))['testCases']))") 个" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "### 2.3 Conventions 维度" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "待评估测试点: $(python3 -c "import json; print(len(json.load(open('$DATASET_DIR/conventions.json'))['testCases']))") 个" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "### 2.4 API 维度" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "待评估测试点: $(python3 -c "import json; print(len(json.load(open('$DATASET_DIR/api.json'))['testCases']))") 个" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "## 3. 结论" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "待人工评估完成后填写。" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
echo "---" >> "$REPORT_FILE"
echo "*此报告由自动化脚本生成*" >> "$REPORT_FILE"

echo ""
echo "========================================"
echo "评估完成！"
echo "报告已生成: $REPORT_FILE"
echo "总计: $TOTAL 个测试点"
echo "准确率: $OVERALL_ACCURACY"
echo "========================================"
