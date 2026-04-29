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
        <el-form-item label="FTP配置">
          <el-select v-model="form.ftpConfigId" disabled placeholder="内置FTP">
            <el-option label="内置FTP" :value="-1" />
          </el-select>
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            (已统一使用内置FTP)
          </span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" :disabled="readonly" type="textarea" :rows="2" />
        </el-form-item>
      </el-card>

      <el-card class="box-card" style="margin-top: 20px;">
        <div slot="header">
          <span>文件扫描配置</span>
        </div>
        <el-form-item label="扫描路径" prop="scanPath">
          <el-input v-model="form.scanPath" :disabled="readonly" placeholder="如: /upload/SALES_REPORT" style="width: 300px;" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            相对于FTP根目录，默认 /upload
          </span>
        </el-form-item>
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
        <el-form-item label="跳过前N列" prop="skipColumns">
          <el-input-number v-model="form.skipColumns" :min="0" :max="10" :disabled="readonly" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            如需跳过Excel的前几列（如序号列、ID列），请设置此值
          </span>
        </el-form-item>
        <el-form-item label="日期提取规则" prop="dateExtractPattern">
          <el-select
            v-model="form.dateExtractPattern"
            :disabled="readonly"
            clearable
            placeholder="自动识别（推荐）"
            style="width: 300px;"
          >
            <el-option label="自动识别（推荐）" value="" />
            <el-option label="yyyyMMdd 格式" value="yyyyMMdd" />
            <el-option label="yyyy-MM-dd 格式" value="yyyy-MM-dd" />
            <el-option label="yyyyMMdd_HHmm 格式" value="yyyyMMdd_HHmm" />
          </el-select>
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            从文件名自动提取分区日期的格式规则
          </span>
        </el-form-item>
      </el-card>

      <el-card class="box-card" style="margin-top: 20px;">
        <div slot="header">
          <span>解析与映射配置</span>
        </div>
        <el-form-item label="起始行号" prop="startRow">
          <el-input-number v-model="form.startRow" :min="1" :max="100" :disabled="readonly" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            数据从第几行开始读取（默认1）
          </span>
        </el-form-item>
        <el-form-item label="起始列号" prop="startCol">
          <el-input-number v-model="form.startCol" :min="1" :max="50" :disabled="readonly" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            数据从第几列开始读取（默认1）
          </span>
        </el-form-item>
        <el-form-item label="映射模式" prop="mappingMode">
          <el-select v-model="form.mappingMode" :disabled="readonly" style="width: 200px;">
            <el-option label="按列名" value="BY_NAME" />
            <el-option label="按列序号" value="BY_INDEX" />
            <el-option label="双模式（推荐）" value="DUAL" />
          </el-select>
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            列名优先+列序号兜底
          </span>
        </el-form-item>
        <el-form-item label="重复列名策略" prop="duplicateColStrategy">
          <el-select v-model="form.duplicateColStrategy" :disabled="readonly" style="width: 200px;">
            <el-option label="自动加后缀" value="AUTO_SUFFIX" />
            <el-option label="按序号标识" value="BY_INDEX" />
          </el-select>
        </el-form-item>
      </el-card>

      <el-card class="box-card" style="margin-top: 20px;">
        <div slot="header">
          <span>ODS备份配置</span>
        </div>
        <el-form-item label="ODS备份">
          <el-switch v-model="form.odsBackupEnabled" :active-value="1" :inactive-value="0" :disabled="readonly" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            开启后处理结果将备份写入ODS表
          </span>
        </el-form-item>
        <el-form-item label="ODS表名" prop="odsTableName" v-if="form.odsBackupEnabled === 1">
          <el-input v-model="form.odsTableName" :disabled="readonly" placeholder="如: ods_sales_daily" />
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
                <el-option label="数值" value="DECIMAL" />
                <el-option label="日期" value="DATE" />
                <el-option label="日期时间" value="DATETIME" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数值精度" prop="scale" width="120">
            <template slot-scope="{ row, $index }">
              <el-input-number v-model="row.scale" :min="0" :max="10" :disabled="readonly"
                v-if="row.fieldType === 'DECIMAL'" />
              <span v-else class="field-hint">-</span>
            </template>
          </el-table-column>
          <el-table-column label="日期格式" prop="dateFormat" width="150">
            <template slot-scope="{ row, $index }">
              <el-input v-model="row.dateFormat" :disabled="readonly" placeholder="如: yyyy-MM-dd"
                v-if="row.fieldType === 'DATE' || row.fieldType === 'DATETIME'" />
              <span v-else class="field-hint">-</span>
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
        <el-form-item label="目标表类型" prop="targetTableType">
          <el-select v-model="form.targetTableType" :disabled="readonly" placeholder="请选择表类型" style="width: 200px;">
            <el-option label="Hive" value="hive" />
            <el-option label="MPP" value="mpp" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标库名" prop="targetDbName">
          <el-input v-model="form.targetDbName" :disabled="readonly" placeholder="hive时填库名, mpp时填mppSchema" style="width: 300px;" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            Hive填写库名, MPP填写mppSchema
          </span>
        </el-form-item>
        <el-form-item label="输出表名" prop="outputTable">
          <el-input v-model="form.outputTable" :disabled="readonly" placeholder="如: t_sales_data" style="width: 300px;" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            bi_前缀不用填写,会自动拼接上
          </span>
        </el-form-item>
        <el-form-item label="是否境外" prop="isOverseas">
          <el-select v-model="form.isOverseas" :disabled="readonly" placeholder="MPP时必填" style="width: 150px;">
            <el-option label="否" :value="0" />
            <el-option label="是" :value="1" />
          </el-select>
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            MPP时应该填写,不填默认0
          </span>
        </el-form-item>
        <el-form-item label="数据载入模式" prop="loadMode">
          <el-select v-model="form.loadMode" :disabled="readonly" placeholder="请选择载入模式" style="width: 250px;">
            <el-option label="无分区追加 (non-partitioned-append)" value="non-partitioned-append" />
            <el-option label="无分区覆盖 (non-partitioned-overwrite)" value="non-partitioned-overwrite" />
            <el-option label="有分区追加 (partitioned-append)" value="partitioned-append" />
            <el-option label="有分区覆盖 (partitioned-overwrite)" value="partitioned-overwrite" />
          </el-select>
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            Hive支持4种, MPP支持前两种
          </span>
        </el-form-item>
        <el-form-item label="分区信息" prop="partitionInfo">
          <el-input v-model="form.partitionInfo" :disabled="readonly" placeholder="如: pt_dt" style="width: 300px;" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            仅填写分区字段名，系统自动从文件名提取日期值
          </span>
        </el-form-item>
        <el-form-item label="Spark executor数量" prop="sparkExecutorNum">
          <el-input-number v-model="form.sparkExecutorNum" :min="1" :disabled="readonly" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            不填默认4
          </span>
        </el-form-item>
        <el-form-item label="Spark executor核数" prop="sparkExecutorCores">
          <el-input-number v-model="form.sparkExecutorCores" :min="1" :disabled="readonly" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            不填默认4
          </span>
        </el-form-item>
        <el-form-item label="Spark executor内存" prop="sparkExecutorMemory">
          <el-select v-model="form.sparkExecutorMemory" :disabled="readonly" placeholder="不填默认8G" style="width: 150px;">
            <el-option label="4G" value="4G" />
            <el-option label="8G" value="8G" />
            <el-option label="16G" value="16G" />
            <el-option label="32G" value="32G" />
          </el-select>
        </el-form-item>
        <el-form-item label="Spark driver数量" prop="sparkDriverNum">
          <el-input-number v-model="form.sparkDriverNum" :min="1" :disabled="readonly" />
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">
            不填默认2
          </span>
        </el-form-item>
        <el-form-item label="Spark driver内存" prop="sparkDriverMemory">
          <el-select v-model="form.sparkDriverMemory" :disabled="readonly" placeholder="不填默认2G" style="width: 150px;">
            <el-option label="2G" value="2G" />
            <el-option label="4G" value="4G" />
            <el-option label="8G" value="8G" />
          </el-select>
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
        ftpConfigId: -1,
        scanPath: '/upload',
        filePattern: '*.xlsx',
        sheetIndex: 0,
        headerRow: 0,
        dataStartRow: 1,
        skipColumns: 0,
        dateExtractPattern: '',
        columnMappings: [],
        outputTable: '',
        startRow: 1,
        startCol: 1,
        mappingMode: 'DUAL',
        duplicateColStrategy: 'AUTO_SUFFIX',
        odsBackupEnabled: 0,
        odsTableName: '',
        targetTableType: '',
        targetDbName: '',
        isOverseas: 0,
        loadMode: 'partitioned-append',
        partitionInfo: '',
        sparkExecutorNum: 4,
        sparkExecutorCores: 4,
        sparkExecutorMemory: '8G',
        sparkDriverNum: 2,
        sparkDriverMemory: '2G',
        status: 1,
        remark: ''
      },
      cleanRulesVisible: false,
      currentColumnIndex: null,
      currentColumn: {},
      jsonImportVisible: false,
      rules: {
        reportCode: [{ required: true, message: '请输入报表编码', trigger: 'blur' }],
        reportName: [{ required: true, message: '请输入报表名称', trigger: 'blur' }],
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
    if (this.id) {
      this.loadData()
    }
  },
  methods: {
    async loadData() {
      try {
        const res = await getReportConfigById(this.id)
        this.form = res.data
        if (!this.form.scanPath) {
          this.form.scanPath = '/upload'
        }
        if (!this.form.ftpConfigId) {
          this.form.ftpConfigId = -1
        }
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
.field-hint {
  color: #c0c4cc;
  font-size: 12px;
}
</style>
