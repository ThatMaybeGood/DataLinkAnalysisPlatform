import axios from 'axios';
import BaseWorkflowService from './BaseWorkflowService';
import appConfig from '../config/appConfig';

class OnlineWorkflowService extends BaseWorkflowService {
  constructor() {
    super();
    this.api = axios.create({
      baseURL: appConfig.getApiBaseUrl(),
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
        'X-App-Mode': 'online'
      }
    });

    // 请求拦截器
    this.api.interceptors.request.use(
      config => {
        const token = localStorage.getItem('auth_token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      error => Promise.reject(error)
    );

    // 响应拦截器
    this.api.interceptors.response.use(
      response => response.data,
      error => {
        console.error('API请求失败:', error);
        if (error.response?.status === 401) {
          // 处理未授权
          localStorage.removeItem('auth_token');
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }
    );
  }

  // 获取工作流列表
  async getWorkflows(params = {}) {
    try {
      const response = await this.api.get('/workflows', { params });
      return response;
    } catch (error) {
      console.error('获取工作流列表失败:', error);
      throw error;
    }
  }

  // 获取单个工作流
  async getWorkflowById(id) {
    try {
      const response = await this.api.get(`/workflows/${id}`);
      return response;
    } catch (error) {
      console.error(`获取工作流 ${id} 失败:`, error);
      throw error;
    }
  }

  // 创建工作流
  async createWorkflow(workflow) {
    try {
      const response = await this.api.post('/workflows', workflow);
      return response;
    } catch (error) {
      console.error('创建工作流失败:', error);
      throw error;
    }
  }

  // 更新工作流
  async updateWorkflow(id, workflow) {
    try {
      const response = await this.api.put(`/workflows/${id}`, workflow);
      return response;
    } catch (error) {
      console.error(`更新工作流 ${id} 失败:`, error);
      throw error;
    }
  }

  // 删除工作流
  async deleteWorkflow(id) {
    try {
      await this.api.delete(`/workflows/${id}`);
      return true;
    } catch (error) {
      console.error(`删除工作流 ${id} 失败:`, error);
      throw error;
    }
  }

  // 执行工作流
  async executeWorkflow(id, params) {
    try {
      const response = await this.api.post(`/workflows/${id}/execute`, params);
      return response;
    } catch (error) {
      console.error(`执行工作流 ${id} 失败:`, error);
      throw error;
    }
  }

  // 获取工作流节点
  async getWorkflowNodes(workflowId) {
    try {
      const response = await this.api.get(`/workflows/${workflowId}/nodes`);
      return response;
    } catch (error) {
      console.error(`获取工作流 ${workflowId} 节点失败:`, error);
      throw error;
    }
  }

  // 获取工作流执行记录
  async getWorkflowExecutions(workflowId, params = {}) {
    try {
      const response = await this.api.get(`/workflows/${workflowId}/executions`, { params });
      return response;
    } catch (error) {
      console.error(`获取工作流 ${workflowId} 执行记录失败:`, error);
      throw error;
    }
  }

  // 搜索工作流
  async searchWorkflows(query) {
    try {
      const response = await this.api.get('/workflows/search', { params: query });
      return response;
    } catch (error) {
      console.error('搜索工作流失败:', error);
      throw error;
    }
  }

  // 批量操作
  async batchOperation(operation, ids, data = {}) {
    try {
      const response = await this.api.post('/workflows/batch', {
        operation,
        ids,
        data
      });
      return response;
    } catch (error) {
      console.error(`批量操作 ${operation} 失败:`, error);
      throw error;
    }
  }

  // 克隆工作流
  async cloneWorkflow(id, newName = null) {
    try {
      const response = await this.api.post(`/workflows/${id}/clone`, { newName });
      return response;
    } catch (error) {
      console.error(`克隆工作流 ${id} 失败:`, error);
      throw error;
    }
  }

  // 导出工作流
  async exportWorkflow(id) {
    try {
      const response = await this.api.get(`/workflows/${id}/export`, {
        responseType: 'blob'
      });

      // 创建下载链接
      const url = window.URL.createObjectURL(new Blob([response]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `workflow_${id}_${Date.now()}.json`);
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);

      return true;
    } catch (error) {
      console.error(`导出工作流 ${id} 失败:`, error);
      throw error;
    }
  }

  // 获取统计信息
  async getStatistics() {
    try {
      const response = await this.api.get('/workflows/statistics');
      return response;
    } catch (error) {
      console.error('获取统计信息失败:', error);
      throw error;
    }
  }
}

export default OnlineWorkflowService;