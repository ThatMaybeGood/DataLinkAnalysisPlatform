import axios from 'axios';
import { EventEmitter } from 'events';

// 类型定义
export enum ModeType {
    ONLINE = 'online',
    OFFLINE = 'offline',
    MIXED = 'mixed'
}

export interface HeartbeatRequest {
    clientId: string;
    mode: ModeType;
    clientVersion: string;
    platform: string;
    sessionId: string;
    timestamp: number;
    metadata?: Record<string, any>;
}

export interface HeartbeatResponse {
    timestamp: number;
    serverTime: number;
    serverMode: ModeType;
    nextHeartbeatInterval: number;
    needsSync: boolean;
    syncData?: SystemStatus;
    modeConsistent: boolean;
    suggestedMode?: ModeType;
}

export interface SystemStatus {
    serverTime: number;
    serverMode: ModeType;
    serverVersion: string;
    systemStatus: string;
    statistics: Record<string, any>;
    clientId?: string;
    syncTime?: number;
}

export interface ClientStatus {
    clientId: string;
    heartbeatStatus: string;
    online: boolean;
    lastHeartbeatTime: number;
    modeConsistent: boolean;
    currentMode?: string;
    suggestedMode?: string;
    additionalInfo?: Record<string, any>;
}

export interface CoordinationStats {
    totalClients: number;
    activeClients: number;
    healthyClients: number;
    heartbeatHealthRate: number;
    modeConsistencyRate: number;
    inconsistencyCount: number;
    syncQueueSize: number;
    pendingSyncTasks: number;
    successSyncTasks: number;
    failedSyncTasks: number;
    webSocketConnections: number;
    lastUpdateTime: number;
}

// WebSocket消息类型
export interface WebSocketMessage {
    type: string;
    timestamp: number;
    data: any;
    messageId?: string;
    correlationId?: string;
}

// 协调服务类
export class CoordinationService extends EventEmitter {
    private static instance: CoordinationService;
    private baseURL: string;
    private clientId: string;
    private sessionId: string;
    private heartbeatInterval: number = 30000;
    private heartbeatTimer?: NodeJS.Timeout;
    private ws?: WebSocket;
    private wsReconnectTimer?: NodeJS.Timeout;
    private isWsConnected: boolean = false;
    private mode: ModeType = ModeType.ONLINE;
    private lastHeartbeatTime: number = 0;
    private isModeConsistent: boolean = true;

    private constructor() {
        super();
        this.baseURL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';
        this.clientId = this.generateClientId();
        this.sessionId = this.generateSessionId();
    }

    public static getInstance(): CoordinationService {
        if (!CoordinationService.instance) {
            CoordinationService.instance = new CoordinationService();
        }
        return CoordinationService.instance;
    }

    // 初始化协调服务
    public async initialize(mode: ModeType): Promise<void> {
        this.mode = mode;

        // 初始化心跳
        this.startHeartbeat();

        // 初始化WebSocket连接
        this.connectWebSocket();

        // 发送初始状态
        await this.sendInitialStatus();

        console.log('协调服务初始化完成', {
            clientId: this.clientId,
            mode: this.mode,
            sessionId: this.sessionId
        });
    }

    // 发送心跳
    private async sendHeartbeat(): Promise<void> {
        try {
            const heartbeat: HeartbeatRequest = {
                clientId: this.clientId,
                mode: this.mode,
                clientVersion: '1.0.0',
                platform: navigator.userAgent,
                sessionId: this.sessionId,
                timestamp: Date.now(),
                metadata: {
                    screenResolution: `${window.screen.width}x${window.screen.height}`,
                    language: navigator.language,
                    online: navigator.onLine
                }
            };

            const response = await axios.post<HeartbeatResponse>(
                `${this.baseURL}/coordination/heartbeat`,
                heartbeat
            );

            const data = response.data;
            this.lastHeartbeatTime = Date.now();
            this.isModeConsistent = data.modeConsistent;
            this.heartbeatInterval = data.nextHeartbeatInterval || 30000;

            // 处理服务器响应
            this.handleHeartbeatResponse(data);

            // 触发事件
            this.emit('heartbeat', data);

        } catch (error) {
            console.error('发送心跳失败:', error);
            this.emit('heartbeat-error', error);

            // 如果心跳失败，检查网络状态
            this.checkNetworkStatus();
        }
    }

    // 启动心跳定时器
    private startHeartbeat(): void {
        // 立即发送第一次心跳
        this.sendHeartbeat();

        // 设置定时器
        this.heartbeatTimer = setInterval(() => {
            this.sendHeartbeat();
        }, this.heartbeatInterval);
    }

    // 停止心跳
    private stopHeartbeat(): void {
        if (this.heartbeatTimer) {
            clearInterval(this.heartbeatTimer);
            this.heartbeatTimer = undefined;
        }
    }

    // 连接WebSocket
    private connectWebSocket(): void {
        const wsURL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws';

        try {
            this.ws = new WebSocket(wsURL);

            this.ws.onopen = () => {
                console.log('WebSocket连接已建立');
                this.isWsConnected = true;
                this.emit('ws-connected');

                // 发送连接消息
                this.sendWebSocketMessage({
                    type: 'connect',
                    timestamp: Date.now(),
                    data: {
                        clientId: this.clientId,
                        mode: this.mode,
                        sessionId: this.sessionId
                    }
                });
            };

            this.ws.onmessage = (event) => {
                try {
                    const message: WebSocketMessage = JSON.parse(event.data);
                    this.handleWebSocketMessage(message);
                } catch (error) {
                    console.error('解析WebSocket消息失败:', error);
                }
            };

            this.ws.onclose = (event) => {
                console.log('WebSocket连接关闭:', event.code, event.reason);
                this.isWsConnected = false;
                this.emit('ws-disconnected', event);

                // 尝试重新连接
                this.scheduleWebSocketReconnect();
            };

            this.ws.onerror = (error) => {
                console.error('WebSocket错误:', error);
                this.emit('ws-error', error);
            };

        } catch (error) {
            console.error('创建WebSocket连接失败:', error);
        }
    }

    // 发送WebSocket消息
    private sendWebSocketMessage(message: WebSocketMessage): void {
        if (this.ws && this.isWsConnected) {
            try {
                this.ws.send(JSON.stringify(message));
            } catch (error) {
                console.error('发送WebSocket消息失败:', error);
            }
        }
    }

    // 处理WebSocket消息
    private handleWebSocketMessage(message: WebSocketMessage): void {
        console.log('收到WebSocket消息:', message.type, message.data);

        switch (message.type) {
            case 'welcome':
                this.handleWelcomeMessage(message);
                break;
            case 'heartbeat_response':
                this.handleHeartbeatResponse(message.data);
                break;
            case 'mode_switch':
                this.handleModeSwitchMessage(message);
                break;
            case 'data_sync':
                this.handleDataSyncMessage(message);
                break;
            case 'notification':
                this.handleNotificationMessage(message);
                break;
            case 'ping':
                this.handlePingMessage(message);
                break;
            case 'system_status':
                this.handleSystemStatusMessage(message);
                break;
            default:
                console.warn('未知的WebSocket消息类型:', message.type);
        }

        // 触发事件
        this.emit('ws-message', message);
    }

    // 处理心跳响应
    private handleHeartbeatResponse(data: HeartbeatResponse): void {
        // 更新本地状态
        this.isModeConsistent = data.modeConsistent;

        if (data.needsSync && data.syncData) {
            // 需要同步数据
            this.emit('sync-required', data.syncData);
        }

        if (!data.modeConsistent && data.suggestedMode) {
            // 模式不一致，需要切换
            this.emit('mode-mismatch', {
                currentMode: this.mode,
                suggestedMode: data.suggestedMode,
                serverMode: data.serverMode
            });
        }
    }

    // 处理模式切换消息
    private handleModeSwitchMessage(message: WebSocketMessage): void {
        const { newMode, reason, effectiveTime } = message.data;

        console.log('收到模式切换通知:', newMode, reason);

        // 触发模式切换事件
        this.emit('mode-switch', {
            newMode,
            reason,
            effectiveTime,
            shouldConfirm: true
        });
    }

    // 处理数据同步消息
    private handleDataSyncMessage(message: WebSocketMessage): void {
        const { syncType, data, clientId } = message.data;

        if (clientId === this.clientId) {
            console.log('收到数据同步请求:', syncType);

            // 触发数据同步事件
            this.emit('data-sync-request', {
                syncType,
                data,
                timestamp: message.timestamp
            });
        }
    }

    // 处理通知消息
    private handleNotificationMessage(message: WebSocketMessage): void {
        const { title, content, level, type, sender } = message.data;

        // 显示通知
        this.showNotification(title, content, level);

        // 触发通知事件
        this.emit('notification', {
            title,
            content,
            level,
            type,
            sender,
            timestamp: message.timestamp
        });
    }

    // 处理ping消息
    private handlePingMessage(message: WebSocketMessage): void {
        // 发送pong响应
        this.sendWebSocketMessage({
            type: 'pong',
            timestamp: Date.now(),
            data: {
                receivedAt: message.timestamp,
                clientTime: Date.now()
            }
        });
    }

    // 处理系统状态消息
    private handleSystemStatusMessage(message: WebSocketMessage): void {
        // 更新本地系统状态缓存
        this.emit('system-status-update', message.data);
    }

    // 处理欢迎消息
    private handleWelcomeMessage(message: WebSocketMessage): void {
        console.log('收到欢迎消息:', message.data);
        this.emit('welcome', message.data);
    }

    // 显示通知
    private showNotification(title: string, content: string, level: string = 'info'): void {
        // 在实际应用中，这里应该使用通知组件
        console.log(`[${level.toUpperCase()}] ${title}: ${content}`);

        // 浏览器通知
        if ('Notification' in window && Notification.permission === 'granted') {
            new Notification(title, {
                body: content,
                icon: '/notification-icon.png'
            });
        }
    }

    // 发送初始状态
    private async sendInitialStatus(): Promise<void> {
        try {
            await axios.post(`${this.baseURL}/coordination/status`, {
                clientId: this.clientId,
                mode: this.mode,
                status: 'initialized',
                timestamp: Date.now()
            });
        } catch (error) {
            console.error('发送初始状态失败:', error);
        }
    }

    // 检查网络状态
    private checkNetworkStatus(): void {
        const isOnline = navigator.onLine;

        if (!isOnline) {
            console.warn('网络连接已断开');
            this.emit('network-offline');

            // 切换到离线模式
            if (this.mode !== ModeType.OFFLINE) {
                this.switchMode(ModeType.OFFLINE, '网络连接断开');
            }
        } else {
            console.log('网络连接正常');
            this.emit('network-online');
        }
    }

    // 安排WebSocket重连
    private scheduleWebSocketReconnect(): void {
        if (this.wsReconnectTimer) {
            clearTimeout(this.wsReconnectTimer);
        }

        // 5秒后尝试重连
        this.wsReconnectTimer = setTimeout(() => {
            console.log('尝试重新连接WebSocket...');
            this.connectWebSocket();
        }, 5000);
    }

    // 切换模式
    public async switchMode(newMode: ModeType, reason: string): Promise<boolean> {
        try {
            const oldMode = this.mode;
            this.mode = newMode;

            // 发送模式切换请求
            this.sendWebSocketMessage({
                type: 'mode_update',
                timestamp: Date.now(),
                data: {
                    clientId: this.clientId,
                    newMode,
                    reason,
                    oldMode
                }
            });

            // 更新本地配置
            localStorage.setItem('client-mode', newMode);

            // 触发模式切换事件
            this.emit('mode-changed', {
                oldMode,
                newMode,
                reason,
                timestamp: Date.now()
            });

            console.log(`模式切换: ${oldMode} -> ${newMode} (${reason})`);
            return true;

        } catch (error) {
            console.error('切换模式失败:', error);
            return false;
        }
    }

    // 获取客户端状态
    public async getClientStatus(): Promise<ClientStatus> {
        try {
            const response = await axios.get<ClientStatus>(
                `${this.baseURL}/coordination/status/${this.clientId}`
            );
            return response.data;
        } catch (error) {
            console.error('获取客户端状态失败:', error);
            throw error;
        }
    }

    // 获取协调统计
    public async getCoordinationStats(): Promise<CoordinationStats> {
        try {
            const response = await axios.get<CoordinationStats>(
                `${this.baseURL}/coordination/stats`
            );
            return response.data;
        } catch (error) {
            console.error('获取协调统计失败:', error);
            throw error;
        }
    }

    // 触发数据同步
    public async triggerDataSync(syncType: string): Promise<boolean> {
        try {
            await axios.post(`${this.baseURL}/coordination/sync/trigger`, null, {
                params: {
                    clientId: this.clientId,
                    syncType
                }
            });
            return true;
        } catch (error) {
            console.error('触发数据同步失败:', error);
            return false;
        }
    }

    // 生成客户端ID
    private generateClientId(): string {
        let clientId = localStorage.getItem('client-id');

        if (!clientId) {
            clientId = `client_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
            localStorage.setItem('client-id', clientId);
        }

        return clientId;
    }

    // 生成会话ID
    private generateSessionId(): string {
        let sessionId = sessionStorage.getItem('session-id');

        if (!sessionId) {
            sessionId = `session_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
            sessionStorage.setItem('session-id', sessionId);
        }

        return sessionId;
    }

    // 获取当前模式
    public getCurrentMode(): ModeType {
        return this.mode;
    }

    // 检查模式一致性
    public isModeConsistent(): boolean {
        return this.isModeConsistent;
    }

    // 清理资源
    public cleanup(): void {
        // 停止心跳
        this.stopHeartbeat();

        // 关闭WebSocket
        if (this.ws) {
            this.ws.close();
            this.ws = undefined;
        }

        // 清理定时器
        if (this.wsReconnectTimer) {
            clearTimeout(this.wsReconnectTimer);
            this.wsReconnectTimer = undefined;
        }

        console.log('协调服务已清理');
    }
}

// 导出单例实例
export const coordinationService = CoordinationService.getInstance();