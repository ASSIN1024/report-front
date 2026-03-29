<template>
  <div class="page-container">
    <div class="page-header">
      <span class="page-title">FTP配置管理</span>
      <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新增配置</el-button>
    </div>

    <el-form :inline="true" :model="searchForm" class="search-form">
      <el-form-item label="配置名称">
        <el-input v-model="searchForm.configName" placeholder="请输入配置名称" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
        <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tableData" border stripe>
      <el-table-column prop="configName" label="配置名称" />
      <el-table-column prop="host" label="FTP服务器" />
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="scanPath" label="扫描路径" />
      <el-table-column prop="filePattern" label="文件匹配" />
      <el-table-column prop="scanInterval" label="扫描间隔(秒)" width="110" />
      <el-table-column prop="status" label="状态" width="80">
        <template slot-scope="{ row }">
          <status-tag :status="String(row.status)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button type="text" size="small" @click="handleTest(row)">测试</el-button>
          <el-button type="text" size="small" style="color: #F56C6C" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      :current-page="pagination.pageNum"
      :page-size="pagination.pageSize"
      :total="pagination.total"
      @pagination="handlePagination"
    />

    <el-dialog :title="dialogTitle" :visible.sync="dialogVisible" width="600px" @close="handleDialogClose">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="FTP服务器" prop="host">
          <el-input v-model="form.host" placeholder="请输入FTP服务器地址" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>
        <el-form-item label="扫描路径" prop="scanPath">
          <el-input v-model="form.scanPath" placeholder="请输入扫描路径" />
        </el-form-item>
        <el-form-item label="文件匹配" prop="filePattern">
          <el-input v-model="form.filePattern" placeholder="如: *.xlsx" />
        </el-form-item>
        <el-form-item label="扫描间隔(秒)" prop="scanInterval">
          <el-input-number v-model="form.scanInterval" :min="60" :step="60" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getFtpConfigPage, saveFtpConfig, updateFtpConfig, deleteFtpConfig, testFtpConnection } from '@/api/ftpConfig'
import StatusTag from '@/components/StatusTag'
import Pagination from '@/components/Pagination'

export default {
  name: 'FtpConfig',
  components: { StatusTag, Pagination },
  data() {
    return {
      searchForm: {
        configName: '',
        status: null
      },
      tableData: [],
      pagination: {
        pageNum: 1,
        pageSize: 10,
        total: 0
      },
      dialogVisible: false,
      dialogTitle: '',
      form: {
        id: null,
        configName: '',
        host: '',
        port: 21,
        username: '',
        password: '',
        scanPath: '',
        filePattern: '*.xlsx',
        scanInterval: 300,
        status: 1,
        remark: ''
      },
      rules: {
        configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
        host: [{ required: true, message: '请输入FTP服务器地址', trigger: 'blur' }],
        port: [{ required: true, message: '请输入端口', trigger: 'blur' }],
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    async loadData() {
      try {
        const params = {
          ...this.searchForm,
          pageNum: this.pagination.pageNum,
          pageSize: this.pagination.pageSize
        }
        const res = await getFtpConfigPage(params)
        this.tableData = res.data.records
        this.pagination.total = res.data.total
      } catch (error) {
        console.error('加载数据失败', error)
      }
    },
    handleSearch() {
      this.pagination.pageNum = 1
      this.loadData()
    },
    handleReset() {
      this.searchForm = { configName: '', status: null }
      this.handleSearch()
    },
    handlePagination({ page, rows }) {
      this.pagination.pageNum = page
      this.pagination.pageSize = rows
      this.loadData()
    },
    handleAdd() {
      this.dialogTitle = '新增FTP配置'
      this.resetForm()
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.dialogTitle = '编辑FTP配置'
      this.form = { ...row }
      this.dialogVisible = true
    },
    async handleTest(row) {
      try {
        await testFtpConnection(row.id)
        this.$message.success('连接测试成功')
      } catch (error) {
        this.$message.error('连接测试失败')
      }
    },
    handleDelete(row) {
      this.$confirm('确认删除该配置?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteFtpConfig(row.id)
          this.$message.success('删除成功')
          this.loadData()
        } catch (error) {
          this.$message.error('删除失败')
        }
      })
    },
    handleDialogClose() {
      this.resetForm()
    },
    resetForm() {
      this.form = {
        id: null,
        configName: '',
        host: '',
        port: 21,
        username: '',
        password: '',
        scanPath: '',
        filePattern: '*.xlsx',
        scanInterval: 300,
        status: 1,
        remark: ''
      }
      this.$refs.form && this.$refs.form.resetFields()
    },
    async handleSubmit() {
      try {
        await this.$refs.form.validate()
        if (this.form.id) {
          await updateFtpConfig(this.form)
        } else {
          await saveFtpConfig(this.form)
        }
        this.$message.success('保存成功')
        this.dialogVisible = false
        this.loadData()
      } catch (error) {
        console.error('保存失败', error)
      }
    }
  }
}
</script>
