// 版本管理配置
const versionConfig = {
    // 版本命名规则
    naming: {
        autoGenerate: true,
        prefix: 'v',
        pattern: '{prefix}{number}',
        customAllowed: true
    },

    // 版本创建规则
    creation: {
        autoOnSave: true,
        manualAllowed: true,
        requireDescription: false,
        maxVersions: 50,
        retentionDays: 365
    },

    // 版本比较配置
    comparison: {
        enableSideBySide: true,
        highlightChanges: true,
        showMetadata: true,
        exportFormats: ['json', 'html', 'pdf']
    },

    // 标签配置
    tags: {
        predefined: ['stable', 'draft', 'archived', 'backup', 'release', 'test'],
        customAllowed: true,
        maxTags: 5,
        colors: {
            stable: 'green',
            draft: 'blue',
            archived: 'gray',
            backup: 'orange',
            release: 'red',
            test: 'purple'
        }
    },

    // 权限配置
    permissions: {
        create: ['admin', 'editor'],
        delete: ['admin'],
        rollback: ['admin', 'editor'],
        tag: ['admin', 'editor'],
        export: ['admin', 'editor', 'viewer']
    },

    // 存储配置
    storage: {
        compress: true,
        encrypt: false,
        maxSize: 10485760, // 10MB
        backupEnabled: true
    },

    // 同步配置
    sync: {
        enabled: true,
        autoSync: false,
        conflictStrategy: 'timestamp', // client, server, timestamp, merge
        retryAttempts: 3
    }
};

export default versionConfig;