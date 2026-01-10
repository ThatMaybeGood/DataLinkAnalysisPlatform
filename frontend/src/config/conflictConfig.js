// 冲突解决配置
const conflictConfig = {
    // 检测配置
    detection: {
        enabled: true,
        checkOnSave: true,
        checkOnSync: true,
        autoDetect: true,
        threshold: 1000 // 检测阈值（毫秒）
    },

    // 解决策略
    strategies: {
        clientPriority: {
            name: '客户端优先',
            description: '使用本地版本覆盖服务器版本',
            icon: 'user',
            color: 'blue',
            default: true
        },
        serverPriority: {
            name: '服务器优先',
            description: '使用服务器版本覆盖本地版本',
            icon: 'cloud',
            color: 'green'
        },
        timestampPriority: {
            name: '时间戳优先',
            description: '使用最新的修改',
            icon: 'clock-circle',
            color: 'orange'
        },
        merge: {
            name: '手动合并',
            description: '手动选择每个字段的版本',
            icon: 'merge',
            color: 'purple'
        }
    },

    // 通知配置
    notification: {
        enabled: true,
        sound: false,
        autoShow: true,
        duration: 5000
    },

    // 界面配置
    ui: {
        sideBySide: true,
        highlightChanges: true,
        showPreview: true,
        autoResolve: false,
        maxItems: 50
    },

    // 历史记录
    history: {
        enabled: true,
        maxRecords: 100,
        retentionDays: 30
    },

    // 权限配置
    permissions: {
        autoResolve: ['admin'],
        manualResolve: ['admin', 'editor'],
        viewHistory: ['admin', 'editor', 'viewer']
    }
};

export default conflictConfig;