// frontend/src/config/modeConfig.js
import appConfig from './appConfig';

// 服务工厂映射
const serviceFactories = {
  online: () => import('../services/onlineWorkflowService'),
  offline: () => import('../services/offlineWorkflowService')
};

export const ModeConfig = {
  // 获取当前模式
  getCurrentMode: () => appConfig.mode,

  // 获取服务工厂
  getServiceFactory: async () => {
    const mode = appConfig.mode;
    const factory = serviceFactories[mode];
    return factory().then(module => module.default);
  },

  // 获取工作流服务实例
  getWorkflowService: async () => {
    const factory = await this.getServiceFactory();
    return factory.createWorkflowService();
  },

  // 获取存储服务实例（离线模式下）
  getStorageService: async () => {
    const factory = await this.getServiceFactory();
    return factory.createStorageService();
  }
};

export default ModeConfig;