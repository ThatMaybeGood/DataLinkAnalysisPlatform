// frontend/src/services/onlineWorkflowService.js
import BaseWorkflowService from './workflowService';
import api from './api';

class OnlineWorkflowService extends BaseWorkflowService {
  constructor() {
    super();
    this.api = api;
  }

  async getWorkflows() {
    const response = await this.api.get('/workflows');
    return response.data;
  }

  async getWorkflowById(id) {
    const response = await this.api.get(`/workflows/${id}`);
    return response.data;
  }

  async createWorkflow(workflow) {
    const response = await this.api.post('/workflows', workflow);
    return response.data;
  }

  async updateWorkflow(id, workflow) {
    const response = await this.api.put(`/workflows/${id}`, workflow);
    return response.data;
  }

  async deleteWorkflow(id) {
    await this.api.delete(`/workflows/${id}`);
    return true;
  }

  async executeWorkflow(id, params) {
    const response = await this.api.post(`/workflows/${id}/execute`, params);
    return response.data;
  }
}

export default OnlineWorkflowService;