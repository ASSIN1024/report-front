<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">{{ isEdit ? '编辑报表配置' : '新增报表配置' }}</span>
      <div>
        <el-button @click="handleBack">返回</el-button>
        <el-button v-if="!readonly" type="primary" @click="handleSave">保存</el-button>
      </div>
    </div>

    <el-form ref="form" :model="form" :rules="rules" label-width="120px">
      <el-card class="box-card">
        <div slot="header">
          <span>基本信息</span>
        </div>
        <el-form-item label="报表编码" prop="reportCode">
          <el-input v-model="form.reportCode" :disabled="isEdit || readonly" placeholder="如: SALES_REPORT" />
        </el-form-item>
        <el-form-item label="报表名称" prop="reportName">
          <el-input v-model="form.reportName" :disabled="readonly" placeholder="如: 销售报表" />
        </el-form-item>
        <el-form-item label="关联FTP" prop="ftpConfigId">
          <el-select v-model="form.ftpConfigId" :disabled="readonly" placeholder="请选择FTP配置">
            <el-option v-for="item in ftpConfigList" :key="item.id" :label="item.configName" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" :disabled="readonly" type="textarea" :rows="2" />
        </el-form-item>
      </el-card>

      <el-card class="box-card" style="margin-top: 20px;">
        <div slot="header">
          <span>文件扫描配置</span>
        </div>
        <el-form-item label="文件匹配" prop="filePattern">
          <el-input v-model="form.filePattern" :disabled="readonly" placeholder="如: sales_*.xlsx" />
        </el-form-item>
        <el-form-item label="Sheet索引" prop="sheetIndex">
          <el-input-number v-model="form.sheetIndex" :min="0" :disabled="readonly" />
        </el-form-item>
        <el-form-item label="表头行号" prop="headerRow">
          <el-input-number v-model="form.headerRow" :min="0" :disabled="readonly" />
        </el-form-item>
        <el-form-item label="数据起始行" prop="dataStartRow">
          <el-input-number v-model="form.dataStartRow" :min="1" :disabled="readonly" />
        </el-form-item>
      </el-card>

      <el-card class="box-card" style="margin-top: 20px;">
        <div slot="header">
          <span>列映射配置</span>
          <div style="float: right;" v-if="!readonly">
            <el-button type="text" @click="handleAddColumn">+ 添加映射</el-button>
            <el-button type="text" @click="jsonImportVisible = true">JSON导入</el-button>
          </div>
        </div>
        <el-table :data="form.columnMappings" border style="margin-top: 10px;">
          <el-table-column label="Excel列" prop="excelColumn" width="120">
            <template slot-scope="{ row, $index }">
              <el-input v-model="row.excelColumn" :disabled="readonly" placeholder="如: A" />
            </template>
          </el-table-column>
          <el-table-column label="字段名称" prop="fieldName" width="150">
            <template slot-scope="{ row, $index }">
              <el-input v-model="row.fieldName" :disabled="readonly" placeholder="如: order_id" />
            </template>
          </el-table-column>
          <el-table-column label="字段类型" prop="fieldType" width="150">
            <template slot-scope="{ row, $index }">
              <el-select v-model="row.fieldType" :disabled="readonly">
                <el-option label="字符串" value="STRING" />
                <el-option label="整数" value="INTEGER" />
                <el-option label="小数" value="DECIMAL" />
                <el-option label="日期" value="DATE" />
                <el-option label="日期时间" value="DATETIME" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="日期格式" prop="dateFormat" width="150">
            <template slot-scope="{ row, $index }">
              <el-input v-model="row.dateFormat" :disabled="readonly" placeholder="如: yyyy-MM-dd" />
            </template>
          </el-table-column>
          <el-table-column label="小数精度" prop="scale" width="120">
            <template slot-scope="{ row, $index }">
              <el-input-number v-model="row.scale" :min="0" :max="10" :disabled="readonly" />
            </template>
          </el-table-column>
          <el-table-column label="清洗规则" width="120" v-if="!readonly">
            <template slot-scope="{ row, $index }">
              <el-button type="text" @click="openCleanRules(row, $index)">
                {{ row.cleanRules && row.cleanRules.length ? `规则 (${row.cleanRules.length})` : '配置规则' }}
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" v-if="!readonly">
            <template slot-scope="{ row, $index }">
              <el-button type="text" style="color: #F56C6C" @click="handleDeleteColumn($index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <el-card class="box-card" style="margin-top: 20px;">
        <div slot="header">
          <span>输出配置</span>
        </div>
        <el-form-item label="输出表名" prop="outputTable">
          <el-input v-model="form.outputTable" :disabled="readonly" placeholder="如: t_sales_data" />
        </el-form-item>
        <el-form-item label="输出模式" prop="outputMode">
          <el-radio-group v-model="form.outputMode" :disabled="readonly">
            <el-radio label="APPEND">追加</el-radio>
            <el-radio label="OVERWRITE">覆盖</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status" :disabled="readonly">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-card>
    </el-form>

    <CleanRulesDialog
      :visible.sync="cleanRulesVisible"
      :current-field="currentColumn"
      @save="handleCleanRulesSave"
    />

    <JsonImportDialog
      v-if="jsonImportVisible"
      :visible.sync="jsonImportVisible"
      :report-config-id="form.id"
      :report-name="form.reportName"
      @success="handleJsonImportSuccess"
    />
  </div>
</template>

<script>
import { getReportConfigById, saveReportConfig, updateReportConfig, getReportConfigListEnabled } from '@/api/reportConfig'
import { getFtpConfigListEnabled } from '@/api/ftpConfig'
import CleanRulesDialog from './CleanRulesDialog.vue'
import JsonImportDialog from './JsonImportDialog.vue'

export default {
  name: 'ReportConfig',
  components: { CleanRulesDialog, JsonImportDialog },
  props: {
    id: String
  },
  data() {
    return {
      readonly: false,
      form: {
        id: null,
        reportCode: '',
        reportName: '',
        ftpConfigId: null,
        filePattern: '*.xlsx',
        sheetIndex: 0,
        headerRow: 0,
        dataStartRow: 1,
        columnMappings: [],
        outputTable: '',
        outputMode: 'APPEND',
        status: 1,
        remark: ''
      },
      ftpConfigList: [],
      cleanRulesVisible: false,
      currentColumnIndex: null,
      currentColumn: {},
      jsonImportVisible: false,
      rules: {
        reportCode: [{ required: true, message: '请输入报表编码', trigger: 'blur' }],
        reportName: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
        ftpConfigId: [{ required: true, message: '请选择FTP配置', trigger: 'change' }],
        filePattern: [{ required: true, message: '请输入文件匹配模式', trigger: 'blur' }],
        outputTable: [{ required: true, message: '请输入输出表名', trigger: 'blur' }]
      }
    }
  },
  computed: {
    isEdit() {
      return !!this.form.id
    }
  },
  created() {
    this.readonly = this.$route.query.readonly === 'true'
    this.loadFtpConfigList()
    if (this.id) {
      this.loadData()
    }
  },
  methods: {
    async loadFtpConfigList() {
      try {
        const res = await getFtpConfigListEnabled()
        this.ftpConfigList = res.data
      } catch (error) {
        console.error('加载FTP配置列表失败', error)
      }
    },
    async loadData() {
      try {
        const res = await getReportConfigById(this.id)
        this.form = res.data
      } catch (error) {
        console.error('加载数据失败', error)
      }
    },
    handleAddColumn() {
      this.form.columnMappings.push({
        excelColumn: '',
        fieldName: '',
        fieldType: 'STRING',
        dateFormat: '',
        scale: null
      })
    },
    handleDeleteColumn(index) {
      this.form.columnMappings.splice(index, 1)
    },
    handleBack() {
      this.$router.push('/report')
    },
    async handleSave() {
      try {
        await this.$refs.form.validate()
        if (this.form.id) {
          await updateReportConfig(this.form)
        } else {
          await saveReportConfig(this.form)
        }
        this.$message.success('保存成功')
        this.$router.push('/report')
      } catch (error) {
        console.error('保存失败', error)
      }
    },
    openCleanRules(row, index) {
      this.currentColumnIndex = index
      this.currentColumn = { ...row }
      this.cleanRulesVisible = true
    },
    handleCleanRulesSave(rules) {
      if (this.currentColumnIndex !== null) {
        this.form.columnMappings[this.currentColumnIndex].cleanRules = rules
      }
    },
    handleJsonImportSuccess() {
      this.loadData()
    }
  }
}
</script>

<style scoped>
.box-card {
  margin-bottom: 0;
}
</style>
