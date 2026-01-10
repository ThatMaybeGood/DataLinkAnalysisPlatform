import React, { useState, useEffect } from 'react';
import {
    Card,
    Row,
    Col,
    Table,
    Tag,
    Typography,
    Space,
    Descriptions,
    Divider,
    Badge
} from 'antd';
import { workflowVersionService } from '../../services/workflowVersionService';
import './VersionComparison.css';

const { Title, Text, Paragraph } = Typography;

/**
 * 版本比较组件
 */
const VersionComparison = ({ versionId1, versionId2 }) => {
    const [comparisonResult, setComparisonResult] = useState(null);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (versionId1 && versionId2) {
            loadComparison();
        }
    }, [versionId1, versionId2]);

    const loadComparison = async () => {
        setLoading(true);
        try {
            const result = await workflowVersionService.compareVersions(versionId1, versionId2);
            setComparisonResult(result);
        } catch (error) {
            console.error('加载版本比较结果失败:', error);
        } finally {
            setLoading(false);
        }
    };

    if (!comparisonResult) {
        return <div>加载中...</div>;
    }

    const { version1, version2, differences } = comparisonResult;

    const columns = [
        {
            title: '字段',
            dataIndex: 'field',
            key: 'field',
            width: 150,
            render: (text) => (
                <Text strong>{text}</Text>
            )
        },
        {
            title: '版本1',
            dataIndex: 'oldValue',
            key: 'oldValue',
            render: (value) => (
                <Text code>{value || '(空)'}</Text>
            )
        },
        {
            title: '版本2',
            dataIndex: 'newValue',
            key: 'newValue',
            render: (value) => (
                <Text code>{value || '(空)'}</Text>
            )
        },
        {
            title: '变化',
            key: 'changeType',
            width: 100,
            render: (_, record) => {
                const changeType = getChangeType(record.oldValue, record.newValue);
                return (
                    <Badge
                        status={getChangeStatus(changeType)}
                        text={changeType}
                    />
                );
            }
        }
    ];

    const getChangeType = (oldValue, newValue) => {
        if (!oldValue && newValue) return '新增';
        if (oldValue && !newValue) return '删除';
        if (oldValue !== newValue) return '修改';
        return '相同';
    };

    const getChangeStatus = (changeType) => {
        const statusMap = {
            '新增': 'success',
            '删除': 'error',
            '修改': 'warning',
            '相同': 'default'
        };
        return statusMap[changeType] || 'default';
    };

    return (
        <div className="version-comparison">
            <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={12}>
                    <Card title="版本1">
                        <Descriptions column={1} size="small">
                            <Descriptions.Item label="版本号">
                                <Tag color="blue">v{version1.versionNumber}</Tag>
                            </Descriptions.Item>
                            <Descriptions.Item label="版本名称">
                                {version1.versionName}
                            </Descriptions.Item>
                            <Descriptions.Item label="创建时间">
                                {new Date(version1.createTime).toLocaleString()}
                            </Descriptions.Item>
                            <Descriptions.Item label="创建人">
                                {version1.createdBy}
                            </Descriptions.Item>
                            {version1.isCurrent && (
                                <Descriptions.Item label="状态">
                                    <Tag color="green">当前版本</Tag>
                                </Descriptions.Item>
                            )}
                        </Descriptions>
                    </Card>
                </Col>

                <Col span={12}>
                    <Card title="版本2">
                        <Descriptions column={1} size="small">
                            <Descriptions.Item label="版本号">
                                <Tag color="blue">v{version2.versionNumber}</Tag>
                            </Descriptions.Item>
                            <Descriptions.Item label="版本名称">
                                {version2.versionName}
                            </Descriptions.Item>
                            <Descriptions.Item label="创建时间">
                                {new Date(version2.createTime).toLocaleString()}
                            </Descriptions.Item>
                            <Descriptions.Item label="创建人">
                                {version2.createdBy}
                            </Descriptions.Item>
                            {version2.isCurrent && (
                                <Descriptions.Item label="状态">
                                    <Tag color="green">当前版本</Tag>
                                </Descriptions.Item>
                            )}
                        </Descriptions>
                    </Card>
                </Col>
            </Row>

            <Divider>差异对比</Divider>

            {differences && differences.length > 0 ? (
                <Table
                    columns={columns}
                    dataSource={differences}
                    rowKey="field"
                    pagination={false}
                    size="small"
                />
            ) : (
                <Paragraph type="secondary" style={{ textAlign: 'center' }}>
                    两个版本内容相同，没有差异
                </Paragraph>
            )}

            <Divider>统计信息</Divider>

            <Row gutter={16}>
                <Col span={8}>
                    <Card size="small">
                        <StatisticItem
                            label="总差异数"
                            value={differences?.length || 0}
                        />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card size="small">
                        <StatisticItem
                            label="新增字段"
                            value={differences?.filter(d => !d.oldValue && d.newValue).length || 0}
                            color="green"
                        />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card size="small">
                        <StatisticItem
                            label="删除字段"
                            value={differences?.filter(d => d.oldValue && !d.newValue).length || 0}
                            color="red"
                        />
                    </Card>
                </Col>
            </Row>
        </div>
    );
};

const StatisticItem = ({ label, value, color }) => (
    <Space direction="vertical" align="center" style={{ width: '100%' }}>
        <Text type="secondary">{label}</Text>
        <Title level={3} style={{ color: color || 'inherit', margin: 0 }}>
            {value}
        </Title>
    </Space>
);

export default VersionComparison;