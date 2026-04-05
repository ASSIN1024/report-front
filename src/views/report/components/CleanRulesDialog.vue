<template>
  <el-dialog
    :visible.sync="dialogVisible"
    title="清洗规则配置"
    width="600px"
    @close="handleClose">
    <div class="field-info">
      <span><strong>字段:</strong> {{ currentField.fieldName }}</span>
      <span><strong>类型:</strong> {{ fieldTypeLabel }}</span>
      <span><strong>来源:</strong> 列 {{ currentField.excelColumn }}</span>
    </div>

    <h4>替换规则</h4>
    <p class="subtitle">当检测到以下值时，替换为指定值</p>

    <el-table :data="rules" border size="small" style="margin-bottom: 10px;">
      <el-table-column label="原始值">
        <template slot-scope="{ row, $index }">
          <el-input v-model="row.pattern" size="small" placeholder="输入值" />
        </template>
      </el-table-column>
      <el-table-column label="替换为">
        <template slot-scope="{ row, $index }">
          <el-input v-model="row.replace" size="small" placeholder="替换为" />
        </template>
      </el-table-column>
      <el-table-column width="60">
        <template slot-scope="{ $index }">
          <el-button type="text" style="color: #F56C6C" @click="removeRule($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-button size="small" plain @click="addRule" style="margin-bottom: 15px;">+ 添加规则</el-button>

    <div slot="footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'CleanRulesDialog',
  props: {
    visible: Boolean,
    currentField: Object
  },
  data() {
    return {
      rules: []
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
    fieldTypeLabel() {
      const typeMap = {
        'STRING': '字符串',
        'INTEGER': '整数',
        'DECIMAL': '小数',
        'DATE': '日期',
        'DATETIME': '日期时间'
      }
      return typeMap[this.currentField.fieldType] || this.currentField.fieldType
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.rules = this.currentField.cleanRules && this.currentField.cleanRules.length > 0
          ? JSON.parse(JSON.stringify(this.currentField.cleanRules))
          : []
      }
    }
  },
  methods: {
    addRule() {
      this.rules.push({ pattern: '', replace: '' })
    },
    removeRule(index) {
      this.rules.splice(index, 1)
    },
    handleSave() {
      const validRules = this.rules.filter(r => r.pattern !== '')
      this.$emit('save', validRules)
      this.$emit('update:visible', false)
    },
    handleClose() {
      this.$emit('update:visible', false)
    }
  }
}
</script>

<style scoped>
.field-info {
  background: #fafafa;
  padding: 12px;
  border-radius: 4px;
  margin-bottom: 15px;
  display: flex;
  gap: 20px;
}
.field-info span {
  font-size: 13px;
}
h4 {
  margin: 0 0 5px 0;
}
.subtitle {
  color: #909399;
  font-size: 12px;
  margin: 0 0 10px 0;
}
</style>
