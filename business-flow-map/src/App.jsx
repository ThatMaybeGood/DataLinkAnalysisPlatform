import React, { useState } from 'react';
import { Layout, Tree, Button, Space, Typography, ConfigProvider, Tag, Badge, Tabs, Form, Input, Modal, message, Divider, Card } from 'antd';
import { 
  PartitionOutlined, SaveOutlined, CloseOutlined, 
  PlayCircleOutlined, CheckCircleOutlined, CodeOutlined,
  BuildOutlined, FolderAddOutlined
} from '@ant-design/icons';
import FlowChart from './FlowChart';
import './style.css';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;

export default function App() {
  const [selectedNode, setSelectedNode] = useState(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newFileName, setNewFileName] = useState('');
  
  // 核心状态数据
  const [chartData, setChartData] = useState({
    nodes: [
      { id: 'n-start', label: '数据源入口', x: 50, y: 200, config: { nodeType: 'start', themeColor: '#52c41a', sql: 'SELECT * FROM source;' } }
    ],
    edges: []
  });

  // 创建新链路文件
  const handleCreateFile = () => {
    if (!newFileName) return message.warning('请输入链路名称');
    setChartData({ nodes: [], edges: [] });
    setIsModalOpen(false);
    setNewFileName('');
    message.success(`新工作区: ${newFileName}`);
  };

  // 添加三种不同类型的站点
  const addNewNode = (type) => {
    const id = `node-${Date.now()}`;
    const nodeConfigs = {
      start: { label: '起始数据源', themeColor: '#52c41a', type: 'start' },
      logic: { label: '过程校验站', themeColor: '#1890ff', type: 'logic' },
      end: { label: '归档输出点', themeColor: '#f5222d', type: 'end' }
    };
    
    const settings = nodeConfigs[type];
    const newNode = {
      id,
      label: settings.label,
      x: 100 + (chartData.nodes.length * 40),
      y: 150 + (chartData.nodes.length * 10),
      config: { 
        nodeType: settings.type, 
        themeColor: settings.themeColor, 
        sql: '-- 请输入SQL逻辑' 
      }
    };

    setChartData(prev => ({
      ...prev,
      nodes: [...prev.nodes, newNode]
    }));
  };

  const tabItems = [
    {
      key: '1',
      label: <span><CodeOutlined /> 站点配置</span>,
      children: (
        <div style={{ padding: '20px' }}>
          <Form layout="vertical">
            <Form.Item label="显示名称">
              <Input value={selectedNode?.label} />
            </Form.Item>
            <Form.Item label="SQL/检测逻辑">
              <Input.TextArea rows={12} key={selectedNode?.id} defaultValue={selectedNode?.config?.sql} style={{ background: '#1e1e1e', color: '#b5cea8', fontFamily: 'monospace' }} />
            </Form.Item>
          </Form>
        </div>
      )
    }
  ];

  return (
    <ConfigProvider theme={{ token: { borderRadius: 4 } }}>
      <Layout className="main-container">
        <Header style={{ background: '#001529', display: 'flex', alignItems: 'center', height: '48px', padding: '0 20px', justifyContent: 'space-between' }}>
          <Space>
            <PartitionOutlined style={{ color: '#1890ff', fontSize: 18 }} />
            <Text style={{ color: '#fff', fontWeight: 500 }}>全链路逻辑设计器</Text>
          </Space>
          <Button type="primary" size="small" icon={<SaveOutlined />}>发布并运行</Button>
        </Header>

        <Layout className="content-wrapper">
          <Sider width={260} theme="light" style={{ display: 'flex', flexDirection: 'column' }}>
            {/* 1. 文件管理 */}
            <div style={{ flex: 1, overflowY: 'auto' }}>
              <div style={{ padding: '12px', background: '#fafafa', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between' }}>
                <Text strong>链路资产</Text>
                <FolderAddOutlined onClick={() => setIsModalOpen(true)} style={{ color: '#1890ff', cursor: 'pointer' }} />
              </div>
              <Tree showLine treeData={[{ title: '支付业务线', key: 'p1', children: [{ title: '订单校验链路', key: 'c1', isLeaf: true }] }]} />
            </div>
            
            <Divider style={{ margin: 0 }} />
            
            {/* 2. 分类组件库 */}
            <div style={{ height: '360px', padding: '15px' }}>
              <Text strong style={{ fontSize: '12px', color: '#888', display: 'block', marginBottom: '12px' }}><BuildOutlined /> 站点类型库</Text>
              <Card size="small" hoverable className="component-card" onClick={() => addNewNode('start')} style={{ borderLeft: '4px solid #52c41a' }}>
                <Space><PlayCircleOutlined style={{ color: '#52c41a' }} /> 起始站点 (Input)</Space>
              </Card>
              <Card size="small" hoverable className="component-card" onClick={() => addNewNode('logic')} style={{ borderLeft: '4px solid #1890ff' }}>
                <Space><CodeOutlined style={{ color: '#1890ff' }} /> 过程校验 (Logic)</Space>
              </Card>
              <Card size="small" hoverable className="component-card" onClick={() => addNewNode('end')} style={{ borderLeft: '4px solid #f5222d' }}>
                <Space><CheckCircleOutlined style={{ color: '#f5222d' }} /> 结束站点 (Output)</Space>
              </Card>
            </div>
          </Sider>

          <Content className="chart-container">
            <FlowChart data={chartData} onNodeClick={(data) => setSelectedNode(data)} activeNodeId={selectedNode?.id} />

            <div className="right-panel" style={{ width: selectedNode ? '500px' : '0', overflow: 'hidden' }}>
              {selectedNode && (
                <div style={{ width: '500px', display: 'flex', flexDirection: 'column', height: '100%' }}>
                  <div style={{ padding: '16px', borderBottom: '1px solid #f0f0f0', display: 'flex', justifyContent: 'space-between' }}>
                    <Text strong><Badge status="processing" color={selectedNode.config?.themeColor} /> 站点: {selectedNode.label}</Text>
                    <Button type="text" icon={<CloseOutlined />} onClick={() => setSelectedNode(null)} />
                  </div>
                  <Tabs items={tabItems} defaultActiveKey="1" style={{ flex: 1 }} />
                  <div style={{ padding: '16px', borderTop: '1px solid #f0f0f0', background: '#fafafa' }}>
                    <Button type="primary" block>保存配置</Button>
                  </div>
                </div>
              )}
            </div>
          </Content>
        </Layout>

        <Modal title="新建业务链路" open={isModalOpen} onOk={handleCreateFile} onCancel={() => setIsModalOpen(false)}>
          <Input placeholder="输入新链路文件名称" value={newFileName} onChange={e => setNewFileName(e.target.value)} style={{ marginTop: 15 }} />
        </Modal>
      </Layout>
    </ConfigProvider>
  );
}