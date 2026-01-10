import React, { useState, useEffect } from 'react';
import {
    Table,
    Button,
    Space,
    Modal,
    message,
    Tag,
    Tooltip,
    Dropdown,
    Menu,
    Popconfirm,
    Input,
    Select,
    DatePicker,
    Badge,
    Card,
    Row,
    Col,
    Descriptions
} from 'antd';
import {
    HistoryOutlined,
    RollbackOutlined,
    EyeOutlined,
    DownloadOutlined,
    DeleteOutlined,
    TagOutlined,
    CompareOutlined,
    PlusOutlined,
    SearchOutlined,
    FilterOutlined
} from '@ant-design/icons';
import { workflowVersionService } from '../../services/workflowVersionService';
import VersionComparison from './VersionComparison';
import './VersionManager.css';

const { Search } = Input;
const { Option } = Select;
const { RangePicker } = DatePicker;

/**
 * 版本管理组件
 */
const VersionManager = ({ workflowId, onVersionSelect }) => {
    const [versions, setVersions] = useState([]);
    const [loading, setLoading] = useState(false);
    const [pagination, setPagination] = useState({
        current: 1,
        pageSize: 10,
        total: 0
    });
    const [searchParams, setSearchParams] = useState({});
    const [selectedVersions, setSelectedVersions] = useState([]);
    const [comparisonVisible, setComparisonVisible] = useState(false);
    const [tagModalVisible, setTagModalVisible] = useState(false);
    const [selectedVersionId, setSelectedVersionId] = useState(null);
    const [newTag, setNewTag] = useState('');

    useEffect(() => {
        if (workflowId) {
            loadVersions();
        }
    }, [workflowId, pagination.current, pagination.pageSize, searchParams]);

    const loadVersions = async () => {
        setLoading(true);
        try {
            const params = {
                workflowId,
                page: pagination.current,
                size: pagination.pageSize,
                ...searchParams
            };

            const result = await workflowVersionService.getVersions(params);

            setVersions(result.content || result);
            setPagination({
                ...pagination,
                total: result.totalElements || result.length
            });
        } catch (error) {
            console.error('加载版本列表失败:', error);
            message.error('加载版本列表失败');
        } finally {
            setLoading(false);
        }
    };

    const handleTableChange = (pagination, filters, sorter) => {
        setPagination(pagination);

        // 处理过滤和排序
        const newParams = { ...searchParams };

        if (sorter.field) {
            newParams.sort = `${sorter.field},${sorter.order === 'ascend' ? 'asc' : 'desc'}`;
        }

        setSearchParams(newParams);
    };

    const handleSearch = (value) => {
        setSearchParams({
            ...searchParams,
            search: value
        });
        setPagination({ ...pagination, current: 1 });
    };

    const handleFilterChange = (filters) => {
        setSearchParams({
            ...searchParams,
            ...filters
        });
        setPagination({ ...pagination, current: 1 });
    };

    const handleCreateVersion = async () => {
        try {
            const versionData = {
                workflowId,
                versionName: `v${versions.length + 1}`,
                description: '手动创建版本',
                changeSummary: '手动保存'
            };

            const newVersion = await workflowVersionService.createVersion(versionData);

            message.success('创建版本成功');
            loadVersions();

            // 触发回调
            if (onVersionSelect) {
                onVersionSelect(newVersion);
            }
        } catch (error) {
            console.error('创建版本失败:', error);
            message.error('创建版本失败');
        }
    };

    const handleRollback = async (versionId) => {
        try {
            await workflowVersionService.rollbackToVersion(workflowId, versionId);
            message.success('回滚成功');
            loadVersions();
        } catch (error) {
            console.error('回滚失败:', error);
            message.error('回滚失败');
        }
    };

    const handleDeleteVersion = async (versionId) => {
        try {
            await workflowVersionService.deleteVersion(versionId);
            message.success('删除版本成功');
            loadVersions();
        } catch (error) {
            console.error('删除版本失败:', error);
            message.error('删除版本失败');
        }
    };

    const handleBatchDelete = async () => {
        if (selectedVersions.length === 0) {
            message.warning('请选择要删除的版本');
            return;
        }

        try {
            await workflowVersionService.batchDeleteVersions(selectedVersions);
            message.success('批量删除成功');
            setSelectedVersions([]);
            loadVersions();
        } catch (error) {
            console.error('批量删除失败:', error);
            message.error('批量删除失败');
        }
    };

    const handleTagVersion = async () => {
        if (!selectedVersionId || !newTag) {
            message.warning('请选择版本并输入标签');
            return;
        }

        try {
            await workflowVersionService.tagVersion(selectedVersionId, newTag);
            message.success('标记版本成功');
            setTagModalVisible(false);
            setNewTag('');
            setSelectedVersionId(null);
            loadVersions();
        } catch (error) {
            console.error('标记版本失败:', error);
            message.error('标记版本失败');
        }
    };

    const handleExportVersion = async (versionId) => {
        try {
            const data = await workflowVersionService.exportVersion(versionId);

            // 创建下载链接
            const blob = new Blob([data], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `version_${versionId}_export.json`;
            link.click();

            message.success('导出成功');
        } catch (error) {
            console.error('导出失败:', error);
            message.error('导出失败');
        }
    };

    const handleCompare = () => {
        if (selectedVersions.length !== 2) {
            message.warning('请选择两个版本进行比较');
            return;
        }

        setComparisonVisible(true);
    };

    const renderTags = (tags) => {
        if (!tags) return null;

        const tagArray = typeof tags === 'string' ? tags.split(',') : tags;

        return (
            <Space size="small">
                {tagArray.map((tag, index) => (
                    <Tag key={index} color={getTagColor(tag)}>
                        {tag}
                    </Tag>
                ))}
            </Space>
        );
    };

    const getTagColor = (tag) => {
        const colors = {
            'stable': 'green',
            'draft': 'blue',
            'archived': 'gray',
            'backup': 'orange',
            'release': 'red'
        };

        return colors[tag.toLowerCase()] || 'default';
    };

    const columns = [
        {
            title: '版本号',
            dataIndex: 'versionNumber',
            key: 'versionNumber',
            width: 100,
            sorter: true,
            render: (text, record) => (
                <Space>
                    <Badge
                        status={record.isCurrent ? 'processing' : 'default'}
                        text={`v${text}`}
                    />
                    {record.isCurrent && (
                        <Tag color="blue">当前</Tag>
                    )}
                </Space>
            )
        },
        {
            title: '版本名称',
            dataIndex: 'versionName',
            key: 'versionName',
            width: 150
        },
        {
            title: '描述',
            dataIndex: 'description',
            key: 'description',
            ellipsis: true
        },
        {
            title: '标签',
            dataIndex: 'tags',
            key: 'tags',
            render: renderTags
        },
        {
            title: '创建人',
            dataIndex: 'createdBy',
            key: 'createdBy',
            width: 120
        },
        {
            title: '创建时间',
            dataIndex: 'createTime',
            key: 'createTime',
            width: 180,
            sorter: true,
            render: (text) => new Date(text).toLocaleString()
        },
        {
            title: '数据大小',
            dataIndex: 'dataSize',
            key: 'dataSize',
            width: 100,
            render: (size) => {
                if (size < 1024) return `${size} B`;
                if (size < 1024 * 1024) return `${(size / 1024).toFixed(2)} KB`;
                return `${(size / (1024 * 1024)).toFixed(2)} MB`;
            }
        },
        {
            title: '操作',
            key: 'actions',
            width: 200,
            render: (_, record) => (
                <Space size="small">
                    <Tooltip title="查看详情">
                        <Button
                            type="link"
                            icon={<EyeOutlined />}
                            onClick={() => onVersionSelect && onVersionSelect(record)}
                        />
                    </Tooltip>

                    <Tooltip title="回滚到此版本">
                        <Popconfirm
                            title="确定要回滚到此版本吗？"
                            onConfirm={() => handleRollback(record.id)}
                            okText="确定"
                            cancelText="取消"
                        >
                            <Button
                                type="link"
                                icon={<RollbackOutlined />}
                                disabled={record.isCurrent}
                            />
                        </Popconfirm>
                    </Tooltip>

                    <Tooltip title="导出">
                        <Button
                            type="link"
                            icon={<DownloadOutlined />}
                            onClick={() => handleExportVersion(record.id)}
                        />
                    </Tooltip>

                    <Tooltip title="标记">
                        <Button
                            type="link"
                            icon={<TagOutlined />}
                            onClick={() => {
                                setSelectedVersionId(record.id);
                                setTagModalVisible(true);
                            }}
                        />
                    </Tooltip>

                    <Tooltip title="删除">
                        <Popconfirm
                            title="确定要删除此版本吗？"
                            onConfirm={() => handleDeleteVersion(record.id)}
                            okText="确定"
                            cancelText="取消"
                            disabled={record.isCurrent}
                        >
                            <Button
                                type="link"
                                icon={<DeleteOutlined />}
                                danger
                                disabled={record.isCurrent}
                            />
                        </Popconfirm>
                    </Tooltip>
                </Space>
            )
        }
    ];

    const rowSelection = {
        selectedRowKeys: selectedVersions,
        onChange: (selectedRowKeys) => setSelectedVersions(selectedRowKeys),
        getCheckboxProps: (record) => ({
            disabled: record.isCurrent
        })
    };

    return (
        <div className="version-manager">
            <Card
                title={
                    <Space>
                        <HistoryOutlined />
                        <span>版本管理</span>
                    </Space>
                }
                extra={
                    <Space>
                        <Button
                            type="primary"
                            icon={<PlusOutlined />}
                            onClick={handleCreateVersion}
                        >
                            创建版本
                        </Button>

                        <Button
                            icon={<CompareOutlined />}
                            onClick={handleCompare}
                            disabled={selectedVersions.length !== 2}
                        >
                            比较版本
                        </Button>

                        <Popconfirm
                            title="确定要删除选中的版本吗？"
                            onConfirm={handleBatchDelete}
                            okText="确定"
                            cancelText="取消"
                        >
                            <Button
                                danger
                                icon={<DeleteOutlined />}
                                disabled={selectedVersions.length === 0}
                            >
                                批量删除
                            </Button>
                        </Popconfirm>
                    </Space>
                }
            >
                <Row gutter={16} style={{ marginBottom: 16 }}>
                    <Col span={8}>
                        <Search
                            placeholder="搜索版本..."
                            allowClear
                            enterButton={<SearchOutlined />}
                            onSearch={handleSearch}
                        />
                    </Col>

                    <Col span={8}>
                        <Select
                            placeholder="筛选标签"
                            allowClear
                            style={{ width: '100%' }}
                            onChange={(value) => handleFilterChange({ tag: value })}
                        >
                            <Option value="stable">稳定版</Option>
                            <Option value="draft">草案</Option>
                            <Option value="archived">已归档</Option>
                            <Option value="backup">备份</Option>
                        </Select>
                    </Col>

                    <Col span={8}>
                        <RangePicker
                            style={{ width: '100%' }}
                            onChange={(dates) => handleFilterChange({
                                startTime: dates?.[0]?.toISOString(),
                                endTime: dates?.[1]?.toISOString()
                            })}
                        />
                    </Col>
                </Row>

                <Table
                    rowKey="id"
                    columns={columns}
                    dataSource={versions}
                    loading={loading}
                    pagination={pagination}
                    onChange={handleTableChange}
                    rowSelection={rowSelection}
                    scroll={{ x: 1000 }}
                />
            </Card>

            {/* 版本比较模态框 */}
            <Modal
                title="版本比较"
                visible={comparisonVisible}
                width={1000}
                footer={null}
                onCancel={() => setComparisonVisible(false)}
            >
                <VersionComparison
                    versionId1={selectedVersions[0]}
                    versionId2={selectedVersions[1]}
                />
            </Modal>

            {/* 标记版本模态框 */}
            <Modal
                title="标记版本"
                visible={tagModalVisible}
                onOk={handleTagVersion}
                onCancel={() => {
                    setTagModalVisible(false);
                    setNewTag('');
                    setSelectedVersionId(null);
                }}
            >
                <Space direction="vertical" style={{ width: '100%' }}>
                    <Select
                        placeholder="选择标签"
                        value={newTag}
                        onChange={setNewTag}
                        style={{ width: '100%' }}
                    >
                        <Option value="stable">稳定版</Option>
                        <Option value="draft">草案</Option>
                        <Option value="archived">已归档</Option>
                        <Option value="backup">备份</Option>
                        <Option value="release">发布版</Option>
                    </Select>

                    <Input
                        placeholder="或输入自定义标签"
                        value={newTag}
                        onChange={e => setNewTag(e.target.value)}
                    />
                </Space>
            </Modal>
        </div>
    );
};

export default VersionManager;