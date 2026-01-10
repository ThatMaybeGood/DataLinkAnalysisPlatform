import { useState, useEffect } from 'react';
import appConfig from '../config/appConfig';

/**
 * 模式钩子 - 用于在组件中获取当前模式和切换模式
 */
export const useMode = () => {
  const [mode, setMode] = useState(appConfig.mode);
  const [isOnline, setIsOnline] = useState(mode === 'online');
  const [isOffline, setIsOffline] = useState(mode === 'offline');

  // 监听模式变化
  useEffect(() => {
    const handleModeChange = () => {
      const currentMode = appConfig.mode;
      setMode(currentMode);
      setIsOnline(currentMode === 'online');
      setIsOffline(currentMode === 'offline');
    };

    // 如果是动态配置，可以添加事件监听
    // 这里假设模式只在应用启动时设置
  }, []);

  // 切换模式（需要重启应用）
  const switchMode = (newMode) => {
    if (newMode !== mode && ['online', 'offline'].includes(newMode)) {
      // 在实际应用中，这里可能需要调用API通知后端切换模式
      // 并重新加载应用
      localStorage.setItem('preferred_mode', newMode);
      message.info(`模式已切换为${newMode === 'online' ? '在线' : '离线'}，请刷新页面`);
      return true;
    }
    return false;
  };

  // 检查网络状态
  const [isNetworkOnline, setIsNetworkOnline] = useState(navigator.onLine);

  useEffect(() => {
    const handleOnline = () => setIsNetworkOnline(true);
    const handleOffline = () => setIsNetworkOnline(false);

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  // 获取模式配置信息
  const getModeConfig = () => {
    return {
      mode,
      isOnline,
      isOffline,
      isNetworkOnline,
      config: appConfig.getCurrentConfig()
    };
  };

  // 检查是否可以切换到在线模式
  const canSwitchToOnline = () => {
    return isNetworkOnline && isOffline;
  };

  // 检查是否可以切换到离线模式
  const canSwitchToOffline = () => {
    return isOnline;
  };

  return {
    mode,
    isOnline,
    isOffline,
    isNetworkOnline,
    switchMode,
    getModeConfig,
    canSwitchToOnline,
    canSwitchToOffline
  };
};

export default useMode;