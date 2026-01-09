// frontend/src/services/workflowService.js
import appConfig from '../config/appConfig';

// 工作流服务基类
class BaseWorkflowService {
  constructor() {
    this.mode = appConfig.mode;
  }

  async getWorkflows() {
    throw new Error('Method not implemented');
  }

  async getWorkflowById(id) {
    throw new Error('Method not implemented');
  }

  async createWorkflow(workflow) {
    throw new Error('Method not implemented');
  }

  async updateWorkflow(id, workflow) {
    throw new Error('Method not implemented');
  }

  async deleteWorkflow(id) {
    throw new Error('Method not implemented');
  }

  async executeWorkflow(id, params) {
    throw new Error('Method not implemented');
  }
}

export default BaseWorkflowService;