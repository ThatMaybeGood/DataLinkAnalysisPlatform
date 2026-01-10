import React, { useState, useEffect } from 'react';
import {
    Modal,
    Button,
    Alert,
    Row,
    Col,
    Card,
    Tabs,
    Radio,
    Space,
    Typography,
    Divider,
    Tooltip,
    Badge
} from 'antd';
import {
    ExclamationCircleOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    EyeOutlined,
    CodeOutlined,
    MergeCellsOutlined
} from '@ant-design/icons';
import { conflictService } from '../../services/conflictService';
import './ConflictResolver.css';

const { Title, Text, Paragraph } = Typography;
const { TabPane } = Tabs;

/**
 * 冲突解决组件
 */
const ConflictResolver = ({ conflictId, visible, onResolve, onCancel }) => {
    const [conflict, setConflict] = useState(null);
    const [loading, setLoading] = useState(false);
    const [resolutionType, setResolutionType] = useState('client');
    const [selectedChanges, setSelectedChanges] = useState({});
    const [previewData, setPreviewData] = useState(null);

    useEffect(() => {
        if (conflictId && visible) {
            loadConflict();
        }
    }, [conflictId, visible]);

    const loadConflict = async () => {
        setLoading(true);
        try {
            const data = await conflictService.getConflictDetail(conflictId);
            setConflict(data);
            initializeSelectedChanges(data);
        } catch (error) {
            console.error('加载冲突详情失败:', error);
        } finally {
            setLoading(false);
        }
    };

    const initializeSelectedChanges = (conflictData) => {
        const changes = {};
        // 默认选择本地版本
        if (conflictData.localData && conflictData.localData.changes) {
            conflictData.localData.changes.forEach(change => {
                changes[change.field] = 'local';
            });
        }
        setSelectedChanges(changes);
    };

    const handleResolve = async () => {
        setLoading(true);
        try {
            const resolution = {
                conflictId,
                resolutionType,
                selectedChanges,
                notes: '手动解决冲突'
            };

            const result = await conflictService.resolveConflict(resolution);

            if (result.success) {
                onResolve(result.data);
            } else {
                throw new Error(result.message);
            }
        } catch (error) {
            console.error('解决冲突失败:', error);
        } finally {
            setLoading(false);
        }
    };

    const handlePreview = (version) => {
        if (version === 'local') {
            setPreviewData(conflict.localData);
        } else {
            setPreviewData(conflict.remoteData);
        }
    };

    const renderConflictDetails = () => {
        if (!conflict) return null;

        return (
            <div className="conflict-details">
                <Alert
                    message="检测到数据冲突"
                    description={`发现 ${conflict.conflictCount} 处冲突，请选择解决方案`}
                    type="warning"
                    showIcon
                    style={{ marginBottom: 16 }}
                />

                <Row gutter={16}>
                    <Col span={12}>
                        <Card
                            title={
                                <Space>
                                    <Badge status="processing" text="本地版本" />
                                    <Text type="secondary">
                                        更新时间: {formatDate(conflict.localData.updateTime)}
                                    </Text>
                                </Space>
                            }
                            extra={
                                <Button
                                    type="link"
                                    icon={<EyeOutlined />}
                                    onClick={() => handlePreview('local')}
                                >
                                    预览
                                </Button>
                            }
                        >
                            {renderChanges(conflict.localData.changes, 'local')}
                        </Card>
                    </Col>

                    <Col span={12}>
                        <Card
                            title={
                                <Space>
                                    <Badge status="success" text="服务器版本" />
                                    <Text type="secondary">
                                        更新时间: {formatDate(conflict.remoteData.updateTime)}
                                    </Text>
                                </Space>
                            }
                            extra={
                                <Button
                                    type="link"
                                    icon={<EyeOutlined />}
                                    onClick={() => handlePreview('remote')}
                                >
                                    预览
                                </Button>
                            }
                        >
                            {renderChanges(conflict.remoteData.changes, 'remote')}
                        </Card>
                    </Col>
                </Row>

                <Divider />

                <Title level={4}>选择解决方案</Title>
                <Radio.Group
                    value={resolutionType}
                    onChange={e => setResolutionType(e.target.value)}
                    style={{ marginBottom: 16 }}
                >
                    <Space direction="vertical">
                        <Radio value="client">
                            <Tooltip title="使用本地版本覆盖服务器版本">
                                <Space>
                                    <CheckCircleOutlined />
                                    <Text strong>使用本地版本</Text>
                                </Space>
                            </Tooltip>
                        </Radio>
                        <Radio value="server">
                            <Tooltip title="使用服务器版本覆盖本地版本">
                                <Space>
                                    <CheckCircleOutlined />
                                    <Text strong>使用服务器版本</Text>
                                </Space>
                            </Tooltip>
                        </Radio>
                        <Radio value="timestamp">
                            <Tooltip title="使用最新的修改">
                                <Space>
                                    <ClockCircleOutlined />
                                    <Text strong>使用最新版本</Text>
                                </Space>
                            </Tooltip>
                        </Radio>
                        <Radio value="merge">
                            <Tooltip title="手动合并两个版本的修改">
                                <Space>
                                    <MergeCellsOutlined />
                                    <Text strong>手动合并</Text>
                                </Space>
                            </Tooltip>
                        </Radio>
                    </Space>
                </Radio.Group>

                {resolutionType === 'merge' && renderMergeOptions()}
            </div>
        );
    };

    const renderChanges = (changes, version) => {
        if (!changes || changes.length === 0) {
            return <Paragraph type="secondary">无修改</Paragraph>;
        }

        return (
            <div className="changes-list">
                {changes.map((change, index) => (
                    <div key={index} className="change-item">
                        <Space direction="vertical" size="small">
                            <Space>
                                <Text strong>{change.field}</Text>
                                <Badge
                                    count={change.type}
                                    style={{ backgroundColor: getChangeTypeColor(change.type) }}
                                />
                            </Space>
                            <Text code>{change.value}</Text>
                            {resolutionType === 'merge' && (
                                <Radio.Group
                                    value={selectedChanges[change.field]}
                                    onChange={e => setSelectedChanges({
                                        ...selectedChanges,
                                        [change.field]: e.target.value
                                    })}
                                    size="small"
                                >
                                    <Radio value="local">本地</Radio>
                                    <Radio value="remote">服务器</Radio>
                                </Radio.Group>
                            )}
                        </Space>
                    </div>
                ))}
            </div>
        );
    };

    const renderMergeOptions = () => {
        const fields = Object.keys(selectedChanges);
        const localSelected = fields.filter(f => selectedChanges[f] === 'local').length;
        const remoteSelected = fields.filter(f => selectedChanges[f] === 'remote').length;

        return (
            <Card
                title="手动合并选项"
                style={{ marginTop: 16 }}
            >
                <Space direction="vertical" style={{ width: '100%' }}>
                    <Alert
                        message={`已选择 ${localSelected} 个本地修改，${remoteSelected} 个服务器修改`}
                        type="info"
                        showIcon
                    />

                    <div className="merge-controls">
                        <Button
                            onClick={() => selectAll('local')}
                            size="small"
                        >
                            全部使用本地
                        </Button>
                        <Button
                            onClick={() => selectAll('remote')}
                            size="small"
                        >
                            全部使用服务器
                        </Button>
                        <Button
                            onClick={toggleSelections}
                            size="small"
                        >
                            切换选择
                        </Button>
                    </div>
                </Space>
            </Card>
        );
    };

    const selectAll = (version) => {
        const newSelections = {};
        Object.keys(selectedChanges).forEach(field => {
            newSelections[field] = version;
        });
        setSelectedChanges(newSelections);
    };

    const toggleSelections = () => {
        const newSelections = {};
        Object.keys(selectedChanges).forEach(field => {
            newSelections[field] = selectedChanges[field] === 'local' ? 'remote' : 'local';
        });
        setSelectedChanges(newSelections);
    };

    const getChangeTypeColor = (type) => {
        const colors = {
            'ADD': '#52c41a',
            'MODIFY': '#1890ff',
            'DELETE': '#f5222d',
            'MOVE': '#722ed1'
        };
        return colors[type] || '#d9d9d9';
    };

    const formatDate = (dateString) => {
        return new Date(dateString).toLocaleString();
    };

    return (
        <Modal
            title={
                <Space>
                    <ExclamationCircleOutlined />
                    <span>冲突解决</span>
                </Space>
            }
            visible={visible}
            width={1200}
            onCancel={onCancel}
            footer={[
                <Button key="cancel" onClick={onCancel}>
                    取消
                </Button>,
                <Button
                    key="resolve"
                    type="primary"
                    loading={loading}
                    onClick={handleResolve}
                >
                    解决冲突
                </Button>
            ]}
        >
            {loading && !conflict ? (
                <div className="loading-container">加载中...</div>
            ) : (
                <Tabs defaultActiveKey="1">
                    <TabPane tab="冲突详情" key="1">
                        {renderConflictDetails()}
                    </TabPane>
                    <TabPane tab="数据预览" key="2">
                        {previewData && (
                            <div className="data-preview">
                                <Card>
                  <pre>
                    <code>
                      {JSON.stringify(previewData.content, null, 2)}
                    </code>
                  </pre>
                                </Card>
                            </div>
                        )}
                    </TabPane>
                    <TabPane tab="解决历史" key="3">
                        <div className="resolution-history">
                            {/* 这里可以显示历史解决记录 */}
                            <Paragraph type="secondary">
                                暂无解决历史记录
                            </Paragraph>
                        </div>
                    </TabPane>
                </Tabs>
            )}
        </Modal>
    );
};

export default ConflictResolver;