// frontend/src/config/appConfig.js
class AppConfig {
  constructor() {
    this.mode = process.env.REACT_APP_MODE || 'online';
    this.configs = {
      online: this.loadOnlineConfig(),
      offline: this.loadOfflineConfig()
    };
  }

  loadOnlineConfig() {
    return {
      mode: 'online',
      apiBaseUrl: process.env.REACT_APP_API_BASE_URL,
      apiTimeout: parseInt(process.env.REACT_APP_ONLINE_API_TIMEOUT) || 30000,
      realtimeUpdate: process.env.REACT_APP_ONLINE_REALTIME_UPDATE === 'true',
      endpoints: {
        workflows: `${process.env.REACT_APP_API_BASE_URL}/workflows`,
        nodes: `${process.env.REACT_APP_API_BASE_URL}/nodes`,
        executions: `${process.env.REACT_APP_API_BASE_URL}/executions`
      }
    };
  }

  loadOfflineConfig() {
    return {
      mode: 'offline',
      storageType: process.env.REACT_APP_OFFLINE_STORAGE_TYPE || 'indexedDB',
      dbName: process.env.REACT_APP_OFFLINE_DB_NAME || 'workflow_offline_db',
      autoSave: process.env.REACT_APP_OFFLINE_AUTO_SAVE === 'true',
      syncWhenOnline: process.env.REACT_APP_OFFLINE_SYNC_ONLINE === 'true',
      storagePaths: {
        workflows: '/workflows',
        nodes: '/nodes',
        rules: '/validationRules',
        exports: '/exports'
      }
    };
  }

  getCurrentConfig() {
    return this.configs[this.mode];
  }

  isOfflineMode() {
    return this.mode === 'offline';
  }

  isOnlineMode() {
    return this.mode === 'online';
  }

  getApiBaseUrl() {
    if (this.isOfflineMode()) {
      return null;
    }
    return this.configs.online.apiBaseUrl;
  }
}

export default new AppConfig();