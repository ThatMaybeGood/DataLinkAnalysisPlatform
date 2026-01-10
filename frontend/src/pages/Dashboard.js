import axios from 'axios';
import appConfig from '../config/appConfig';

// 创建基础API实例
const createApiInstance = () => {
  const baseURL = appConfig.getApiBaseUrl();

  const instance = axios.create({
    baseURL,
    timeout: appConfig.isOnlineMode() ? 30000 : 0, // 离线模式不设超时
    headers: {
      'Content-Type': 'application/json',
      'X-App-Mode': appConfig.isOfflineMode() ? 'offline' : 'online'
    }
  });

  // 请求拦截器
  instance.interceptors.request.use(
    (config) => {
      // 如果是离线模式，标记请求为本地请求
      if (appConfig.isOfflineMode()) {
        config.metadata = { isOffline: true };
      }

      // 添加认证token（在线模式）
      if (appConfig.isOnlineMode()) {
        const token = localStorage.getItem('auth_token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
      }

      return config;
    },
    (error) => {
      return Promise.reject(error);
    }
  );

  // 响应拦截器
  instance.interceptors.response.use(
    (response) => {
      return response.data;
    },
    (error) => {
      // 如果是离线模式的请求，不应该有网络错误
      if (error.config?.metadata?.isOffline) {
        console.error('离线模式下的请求错误:', error);
      }

      // 处理网络错误
      if (!error.response) {
        if (appConfig.isOnlineMode()) {
          console.error('网络连接错误，检查API服务是否运行');
          // 可以在这里触发模式切换
          // ModeConfig.switchMode('offline');
        }
      }

      return Promise.reject(error);
    }
  );

  return instance;
};

// 检查API可用性
export const checkApiAvailability = async () => {
  if (appConfig.isOfflineMode()) {
    return { available: false, mode: 'offline' };
  }

  try {
    const api = createApiInstance();
    await api.get('/health');
    return { available: true, mode: 'online' };
  } catch (error) {
    console.warn('API不可用，切换到离线模式');
    return { available: false, mode: 'offline', error };
  }
};

export default createApiInstance;