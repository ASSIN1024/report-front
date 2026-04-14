<template>
  <el-dialog
    :visible.sync="dialogVisible"
    title="JSON导入列映射"
    width="700px"
    @close="handleClose">
    <el-steps :active="step - 1" finish-status="success" style="margin-bottom: 20px;">
      <el-step title="输入JSON"></el-step>
      <el-step title="格式校验"></el-step>
      <el-step title="确认导入"></el-step>
    </el-steps>

    <div v-if="step === 1">
      <el-input
        type="textarea"
        v-model="jsonText"
        :rows="12"
        placeholder="粘贴JSON配置..."
        style="font-family: monospace;" />
      <div class="tip">
        <i class="el-icon-info"></i> 支持批量导入列映射配置，清洗规则为可选项
      </div>
    </div>

    <div v-if="step === 2">
      <div v-if="validating" style="text-align: center; padding: 40px;">
        <i class="el-icon-loading" style="font-size: 30px;"></i>
        <p>校验中...</p>
      </div>
      <div v-else-if="validationPassed">
        <el-alert type="success" :closable="false" style="margin-bottom: 15px;">
          <p>校验通过！共解析 <strong>{{ previewCount }}</strong> 个列映射配置</p>
        </el-alert>
        <pre class="preview">{{ truncatedPreview }}</pre>
      </div>
      <div v-else>
        <el-alert type="error" :closable="false" style="margin-bottom: 15px;">
          <p>校验失败，请修复以下错误：</p>
        </el-alert>
        <div v-for="(err, idx) in errors" :key="idx" class="error-item">
          <strong>第{{ err.line }}行:</strong> {{ err.message }}
          <span v-if="err.suggestion" class="suggestion">修复建议: {{ err.suggestion }}</span>
        </div>
      </div>
    </div>

    <div v-if="step === 3">
      <el-alert type="warning" :closable="false" style="margin-bottom: 15px;">
        确认导入以下配置到报表: <strong>{{ reportName }}</strong>
      </el-alert>
      <div class="summary">
        <div class="summary-item">
          <strong>{{ previewCount }}</strong>
          <span>列映射</span>
        </div>
        <div class="summary-item">
          <strong>{{ rulesCount }}</strong>
          <span>清洗规则</span>
        </div>
      </div>
      <div class="warning">此操作将替换现有的列映射配置</div>
    </div>

    <span slot="footer">
      <el-button v-if="step > 1" @click="step--">上一步</el-button>
      <el-button @click="handleClose">取消</el-button>
      <el-button v-if="step < 3" type="primary" @click="nextStep">下一步</el-button>
      <el-button v-if="step === 3" type="success" @click="doImport">确认导入</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { validateColumnMapping, importColumnMapping } from '@/api/reportConfig'

export default {
  name: 'JsonImportDialog',
  props: {
    visible: Boolean,
    reportConfigId: [String, Number],
    reportName: String
  },
  data() {
    return {
      step: 1,
      jsonText: '',
      errors: [],
      validationPassed: false,
      previewCount: 0,
      rulesCount: 0,
      validating: false
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    },
    truncatedPreview() {
      try {
        const parsed = JSON.parse(this.jsonText || '[]')
        return JSON.stringify(parsed.slice(0, 3), null, 2) +
          (parsed.length > 3 ? '\n...' : '')
      } catch (e) {
        return ''
      }
    }
  },
  methods: {
    async nextStep() {
      if (this.step === 1) {
        await this.validate()
      } else if (this.step === 2 && this.validationPassed) {
        this.step = 3
      }
    },
    async validate() {
      this.validating = true
      try {
        const res = await validateColumnMapping(this.reportConfigId, { json: this.jsonText })
        const data = res.data
        this.errors = data.errors || []
        this.validationPassed = data.valid
        this.previewCount = data.count || 0

        if (this.validationPassed) {
          try {
            const parsed = JSON.parse(this.jsonText)
            let rules = 0
            for (const item of parsed) {
              if (item.cleanRules && Array.isArray(item.cleanRules)) {
                rules += item.cleanRules.length
              }
            }
            this.rulesCount = rules
          } catch (e) {
            this.rulesCount = 0
          }
          this.step = 2
        }
      } catch (e) {
        this.$message.error('校验请求失败')
      } finally {
        this.validating = false
      }
    },
    async doImport() {
      try {
        const res = await importColumnMapping(this.reportConfigId, { json: this.jsonText })
        if (res.data.success) {
          this.$message.success(`成功导入 ${res.data.imported} 个列映射`)
          this.$emit('success')
          this.handleClose()
        } else {
          this.$message.error('导入失败')
        }
      } catch (e) {
        this.$message.error('导入失败')
      }
    },
    handleClose() {
      this.step = 1
      this.jsonText = ''
      this.errors = []
      this.validationPassed = false
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style scoped>
.tip {
  margin-top: 10px;
  color: #909399;
  font-size: 12px;
}
.preview {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  font-size: 12px;
  max-height: 200px;
  overflow: auto;
}
.error-item {
  padding: 8px 12px;
  background: #fff2f0;
  border-radius: 4px;
  margin-bottom: 8px;
  font-size: 13px;
}
.suggestion {
  display: block;
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
.summary {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}
.summary-item {
  flex: 1;
  background: #fafafa;
  padding: 15px;
  border-radius: 4px;
  text-align: center;
}
.summary-item strong {
  display: block;
  font-size: 28px;
  color: #1890ff;
}
.summary-item span {
  font-size: 12px;
  color: #666;
}
.warning {
  color: #e6a23c;
  font-size: 12px;
}
</style>
