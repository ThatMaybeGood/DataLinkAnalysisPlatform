import React, { useState, useEffect } from 'react';
import { Table, Button, Card, Tag, Space, Input, Modal, message } from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  PlayCircleOutlined,
  DownloadOutlined,
  UploadOutlined,
  CloudSyncOutlined
} from '@ant-design/icons';
import ServiceFactory from '../../services/ServiceFactory';
import appConfig from '../../config/appConfig';
import './WorkflowManager.css';

const { Search } = Input;

const WorkflowManager = () => {
  const [workflows, setWorkflows] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedWorkflow, setSelectedWorkflow] = useState(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [searchText, setSearchText] = useState('');

  // 获取服务实例
  const workflowService = ServiceFactory.createWorkflowService();
  const fileService = ServiceFactory.createFileService();

  // 当前模式
  const isOfflineMode = appConfig.mode === 'offline';

  // 加载工作流列表
  useEffect(() => {
    loadWorkflows();
  }, []);

  const loadWorkflows = async () => {
    setLoading(true);
    try {
      const data = await workflowService.getWorkflows();
      setWorkflows(data);
    } catch (error) {
      message.error('加载工作流失败: ' + error.message);
      console.error('Failed to load workflows:', error);
    } finally {
      setLoading(false);
    }
  };

  // 创建工作流
  const handleCreateWorkflow = async (workflowData) => {
    try {
      const newWorkflow = await workflowService.createWorkflow(workflowData);
      setWorkflows([...workflows, newWorkflow]);
      message.success('工作流创建成功');
    } catch (error) {
      message.error('创建工作流失败: ' + error.message);
    }
  };

  // 删除工作流
  const handleDeleteWorkflow = async (id) => {
    Modal.confirm({
      title: '确认删除',
      content: '确定要删除这个工作流吗？删除后无法恢复。',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await workflowService.deleteWorkflow(id);
          setWorkflows(workflows.filter(w => w.id !== id));
          message.success('工作流删除成功');
        } catch (error) {
          message.error('删除失败: ' + error.message);
        }
      }
    });
  };

  // 执行工作流
  const handleExecuteWorkflow = async (id) => {
    try {
      const result = await workflowService.executeWorkflow(id, {});
      message.success('工作流执行成功');
      console.log('Execution result:', result);
    } catch (error) {
      message.error('执行失败: ' + error.message);
    }
  };

  // 导出工作流
  const handleExportWorkflow = async (id) => {
    try {
      await fileService.exportWorkflow(id);
      message.success('导出成功');
    } catch (error) {
      message.error('导出失败: ' + error.message);
    }
  };

  // 导入工作流
  const handleImportWorkflow = async (file) => {
    try {
      const workflow = await fileService.importWorkflow(file);
      setWorkflows([...workflows, workflow]);
      message.success('导入成功');
    } catch (error) {
      message.error('导入失败: ' + error.message);
    }
  };

  // 同步工作流（离线模式特有）
  const handleSyncWorkflow = async (id) => {
    if (!isOfflineMode) {
      message.warning('当前在线模式，无需同步');
      return;
    }

    try {
      // 这里调用同步逻辑
      message.info('同步功能开发中...');
    } catch (error) {
      message.error('同步失败: ' + error.message);
    }
  };

  // 过滤工作流
  const filteredWorkflows = workflows.filter(workflow =>
    workflow.name.toLowerCase().includes(searchText.toLowerCase()) ||
    workflow.alias?.toLowerCase().includes(searchText.toLowerCase()) ||
    workflow.description?.toLowerCase().includes(searchText.toLowerCase())
  );

  const columns = [
    {
      title: '名称',
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <div>
          <div className="workflow-name">{text}</div>
          <div className="workflow-alias">
            {record.alias && <Tag color="blue">#{record.alias}</Tag>}
          </div>
        </div>
      ),
    },
    {
      title: '分类',
      dataIndex: 'category',
      key: 'category',
      render: (category) => (
        <Tag color="purple">{category || '未分类'}</Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status) => {
        const color = status === 'active' ? 'green' : 'orange';
        const text = status === 'active' ? '活跃' : '已停用';
        return <Tag color={color}>{text}</Tag>;
      },
    },
    {
      title: '节点数',
      dataIndex: 'nodeCount',
      key: 'nodeCount',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date) => new Date(date).toLocaleString(),
    },
    {
      title: '操作',
      key: 'actions',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => setSelectedWorkflow(record)}
          >
            编辑
          </Button>

          <Button
            type="link"
            icon={<PlayCircleOutlined />}
            onClick={() => handleExecuteWorkflow(record.id)}
          >
            执行
          </Button>

          {isOfflineMode && (
            <>
              <Button
                type="link"
                icon={<DownloadOutlined />}
                onClick={() => handleExportWorkflow(record.id)}
              >
                导出
              </Button>

              <Button
                type="link"
                icon={<CloudSyncOutlined />}
                onClick={() => handleSyncWorkflow(record.id)}
              >
                同步
              </Button>
            </>
          )}

          <Button
            type="link"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDeleteWorkflow(record.id)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="workflow-manager">
      {/* 头部工具栏 */}
      <Card className="toolbar-card">
        <div className="toolbar-content">
          <div className="toolbar-left">
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setModalVisible(true)}
            >
              新建工作流
            </Button>

            {isOfflineMode && (
              <Button
                icon={<UploadOutlined />}
                onClick={() => document.getElementById('import-file').click()}
              >
                导入工作流
              </Button>
            )}
          </div>

          <div className="toolbar-right">
            <Search
              placeholder="搜索工作流名称、别名或描述"
              allowClear
              onSearch={value => setSearchText(value)}
              onChange={e => setSearchText(e.target.value)}
              style={{ width: 300 }}
            />

            <div className="mode-indicator">
              <Tag color={isOfflineMode ? 'orange' : 'green'}>
                {isOfflineMode ? '离线模式' : '在线模式'}
              </Tag>
            </div>
          </div>
        </div>
      </Card>

      {/* 工作流表格 */}
      <Card>
        <Table
          columns={columns}
          dataSource={filteredWorkflows}
          rowKey="id"
          loading={loading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: total => `共 ${total} 条`
          }}
        />
      </Card>

      {/* 隐藏的文件输入（用于导入） */}
      <input
        id="import-file"
        type="file"
        accept=".json,.zip"
        style={{ display: 'none' }}
        onChange={async (e) => {
          const file = e.target.files[0];
          if (file) {
            await handleImportWorkflow(file);
            e.target.value = ''; // 清空输入
          }
        }}
      />

      {/* 创建/编辑工作流弹窗 */}
      <WorkflowModal
        visible={modalVisible}
        workflow={selectedWorkflow}
        onCancel={() => {
          setModalVisible(false);
          setSelectedWorkflow(null);
        }}
        onSuccess={(workflow) => {
          if (selectedWorkflow) {
            // 更新现有工作流
            setWorkflows(workflows.map(w =>
              w.id === workflow.id ? workflow : w
            ));
          } else {
            // 添加新工作流
            setWorkflows([...workflows, workflow]);
          }
          setModalVisible(false);
          setSelectedWorkflow(null);
        }}
      />
    </div>
  );
};

// 工作流弹窗组件
const WorkflowModal = ({ visible, workflow, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);

  const workflowService = ServiceFactory.createWorkflowService();

  useEffect(() => {
    if (visible) {
      if (workflow) {
        form.setFieldsValue(workflow);
      } else {
        form.resetFields();
      }
    }
  }, [visible, workflow, form]);

  const handleSubmit = async () => {
    try {
      await form.validateFields();
      const values = form.getFieldsValue();
      setLoading(true);

      let result;
      if (workflow) {
        result = await workflowService.updateWorkflow(workflow.id, values);
      } else {
        result = await workflowService.createWorkflow(values);
      }

      onSuccess(result);
      message.success(workflow ? '更新成功' : '创建成功');
    } catch (error) {
      console.error('Failed to save workflow:', error);
      message.error('保存失败: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal
      title={workflow ? '编辑工作流' : '新建工作流'}
      visible={visible}
      onCancel={onCancel}
      onOk={handleSubmit}
      confirmLoading={loading}
      width={600}
    >
      <Form form={form} layout="vertical">
        <Form.Item
          name="name"
          label="工作流名称"
          rules={[{ required: true, message: '请输入工作流名称' }]}
        >
          <Input placeholder="请输入工作流名称" />
        </Form.Item>

        <Form.Item
          name="alias"
          label="别名"
          rules={[{ pattern: /^[a-zA-Z0-9_-]+$/, message: '只能包含字母、数字、下划线和连字符' }]}
        >
          <Input placeholder="用于快速搜索的别名（可选）" />
        </Form.Item>

        <Form.Item
          name="category"
          label="分类"
        >
          <Input placeholder="输入分类，如：订单处理、用户管理" />
        </Form.Item>

        <Form.Item
          name="description"
          label="描述"
        >
          <Input.TextArea
            placeholder="请输入工作流描述"
            rows={3}
          />
        </Form.Item>

        <Form.Item
          name="tags"
          label="标签"
        >
          <Select
            mode="tags"
            placeholder="输入标签，按回车确认"
            style={{ width: '100%' }}
          />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default WorkflowManager;