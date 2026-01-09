// frontend/src/services/offlineWorkflowService.js
import BaseWorkflowService from './workflowService';
import { OfflineStorage } from '../utils/offlineStorage';

class OfflineWorkflowService extends BaseWorkflowService {
  constructor() {
    super();
    this.storage = new OfflineStorage('workflow_offline_db', 1);
    this.init();
  }

  async init() {
    await this.storage.init();
    // 创建对象存储
    await this.storage.createStore('workflows', { keyPath: 'id' });
    await this.storage.createStore('nodes', { keyPath: 'id' });
    await this.storage.createStore('validationRules', { keyPath: 'id' });
  }

  async getWorkflows() {
    return this.storage.getAll('workflows');
  }

  async getWorkflowById(id) {
    return this.storage.get('workflows', id);
  }

  async createWorkflow(workflow) {
    const id = `offline_${Date.now()}`;
    workflow.id = id;
    workflow.createdAt = new Date().toISOString();
    await this.storage.save('workflows', workflow);
    return workflow;
  }

  async updateWorkflow(id, workflow) {
    workflow.updatedAt = new Date().toISOString();
    await this.storage.save('workflows', workflow);
    return workflow;
  }

  async deleteWorkflow(id) {
    await this.storage.delete('workflows', id);
    return true;
  }

  async executeWorkflow(id, params) {
    // 离线模式下模拟执行
    const workflow = await this.getWorkflowById(id);
    if (!workflow) {
      throw new Error('Workflow not found');
    }

    // 模拟执行逻辑
    const result = {
      success: true,
      steps: [],
      output: params
    };

    return result;
  }
}

export default OfflineWorkflowService;