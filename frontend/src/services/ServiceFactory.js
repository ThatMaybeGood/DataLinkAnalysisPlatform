// 服务工厂 - 根据模式创建服务实例
import appConfig from '../config/appConfig';

class ServiceFactory {
  constructor() {
    this.mode = appConfig.mode;
    this.cache = new Map();
  }

  // 创建工作流服务
  createWorkflowService() {
    const key = `workflow_${this.mode}`;
    if (this.cache.has(key)) {
      return this.cache.get(key);
    }

    let service;
    if (this.mode === 'online') {
      service = new OnlineWorkflowService();
    } else {
      service = new OfflineWorkflowService();
    }

    this.cache.set(key, service);
    return service;
  }

  // 创建节点服务
  createNodeService() {
    const key = `node_${this.mode}`;
    if (this.cache.has(key)) {
      return this.cache.get(key);
    }

    let service;
    if (this.mode === 'online') {
      service = new OnlineNodeService();
    } else {
      service = new OfflineNodeService();
    }

    this.cache.set(key, service);
    return service;
  }

  // 创建验证规则服务
  createValidationService() {
    const key = `validation_${this.mode}`;
    if (this.cache.has(key)) {
      return this.cache.get(key);
    }

    let service;
    if (this.mode === 'online') {
      service = new OnlineValidationService();
    } else {
      service = new OfflineValidationService();
    }

    this.cache.set(key, service);
    return service;
  }

  // 创建文件服务
  createFileService() {
    const key = `file_${this.mode}`;
    if (this.cache.has(key)) {
      return this.cache.get(key);
    }

    let service;
    if (this.mode === 'online') {
      service = new OnlineFileService();
    } else {
      service = new OfflineFileService();
    }

    this.cache.set(key, service);
    return service;
  }

  // 创建连接器服务
  createConnectorService() {
    const key = `connector_${this.mode}`;
    if (this.cache.has(key)) {
      return this.cache.get(key);
    }

    let service;
    if (this.mode === 'online') {
      service = new OnlineConnectorService();
    } else {
      service = new OfflineConnectorService();
    }

    this.cache.set(key, service);
    return service;
  }

  // 创建执行服务
  createExecutionService() {
    const key = `execution_${this.mode}`;
    if (this.cache.has(key)) {
      return this.cache.get(key);
    }

    let service;
    if (this.mode === 'online') {
      service = new OnlineExecutionService();
    } else {
      service = new OfflineExecutionService();
    }

    this.cache.set(key, service);
    return service;
  }

  // 清理缓存
  clearCache() {
    this.cache.clear();
  }

  // 切换模式（需要重新创建所有服务）
  switchMode(newMode) {
    if (newMode !== this.mode) {
      this.mode = newMode;
      this.clearCache();
      console.log(`ServiceFactory switched to ${newMode} mode`);
    }
  }
}

// 导出单例
export default new ServiceFactory();