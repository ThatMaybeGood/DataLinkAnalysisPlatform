// IndexedDB 封装
class IndexedDBStorage {
  constructor(dbName, version) {
    this.dbName = dbName;
    this.version = version;
    this.db = null;
  }

  // 初始化数据库
  async init() {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(this.dbName, this.version);

      request.onupgradeneeded = (event) => {
        const db = event.target.result;

        // 创建工作流存储
        if (!db.objectStoreNames.contains('workflows')) {
          const workflowStore = db.createObjectStore('workflows', { keyPath: 'id' });
          workflowStore.createIndex('alias', 'alias', { unique: true });
          workflowStore.createIndex('category', 'category', { unique: false });
          workflowStore.createIndex('updatedAt', 'updatedAt', { unique: false });
        }

        // 创建节点存储
        if (!db.objectStoreNames.contains('nodes')) {
          const nodeStore = db.createObjectStore('nodes', { keyPath: 'id' });
          nodeStore.createIndex('workflowId', 'workflowId', { unique: false });
          nodeStore.createIndex('type', 'type', { unique: false });
        }

        // 创建验证规则存储
        if (!db.objectStoreNames.contains('validationRules')) {
          const ruleStore = db.createObjectStore('validationRules', { keyPath: 'id' });
          ruleStore.createIndex('nodeId', 'nodeId', { unique: false });
        }

        // 创建执行记录存储
        if (!db.objectStoreNames.contains('executions')) {
          const executionStore = db.createObjectStore('executions', { keyPath: 'id' });
          executionStore.createIndex('workflowId', 'workflowId', { unique: false });
          executionStore.createIndex('status', 'status', { unique: false });
        }

        // 创建连接器存储
        if (!db.objectStoreNames.contains('connectors')) {
          const connectorStore = db.createObjectStore('connectors', { keyPath: 'id' });
          connectorStore.createIndex('type', 'type', { unique: false });
        }

        // 创建同步队列存储
        if (!db.objectStoreNames.contains('syncQueue')) {
          const syncStore = db.createObjectStore('syncQueue', { keyPath: 'id' });
          syncStore.createIndex('status', 'status', { unique: false });
          syncStore.createIndex('entityType', 'entityType', { unique: false });
        }
      };

      request.onsuccess = (event) => {
        this.db = event.target.result;
        resolve();
      };

      request.onerror = (event) => {
        reject(new Error('Failed to open IndexedDB: ' + event.target.error));
      };
    });
  }

  // 保存数据
  async save(storeName, data) {
    await this.ensureConnection();

    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction([storeName], 'readwrite');
      const store = transaction.objectStore(storeName);
      const request = store.put(data);

      request.onsuccess = () => resolve(data);
      request.onerror = () => reject(request.error);
    });
  }

  // 批量保存
  async saveAll(storeName, items) {
    await this.ensureConnection();

    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction([storeName], 'readwrite');
      const store = transaction.objectStore(storeName);

      let completed = 0;
      const results = [];

      items.forEach(item => {
        const request = store.put(item);
        request.onsuccess = () => {
          results.push(item);
          completed++;
          if (completed === items.length) {
            resolve(results);
          }
        };
        request.onerror = () => reject(request.error);
      });
    });
  }

  // 获取单个数据
  async get(storeName, id) {
    await this.ensureConnection();

    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction([storeName], 'readonly');
      const store = transaction.objectStore(storeName);
      const request = store.get(id);

      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }

  // 获取所有数据
  async getAll(storeName) {
    await this.ensureConnection();

    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction([storeName], 'readonly');
      const store = transaction.objectStore(storeName);
      const request = store.getAll();

      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }

  // 查询数据
  async query(storeName, indexName, value) {
    await this.ensureConnection();

    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction([storeName], 'readonly');
      const store = transaction.objectStore(storeName);
      const index = store.index(indexName);
      const request = index.getAll(value);

      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }

  // 删除数据
  async delete(storeName, id) {
    await this.ensureConnection();

    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction([storeName], 'readwrite');
      const store = transaction.objectStore(storeName);
      const request = store.delete(id);

      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
  }

  // 批量删除
  async deleteAll(storeName, ids) {
    await this.ensureConnection();

    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction([storeName], 'readwrite');
      const store = transaction.objectStore(storeName);

      let completed = 0;

      ids.forEach(id => {
        const request = store.delete(id);
        request.onsuccess = () => {
          completed++;
          if (completed === ids.length) {
            resolve();
          }
        };
        request.onerror = () => reject(request.error);
      });
    });
  }

  // 清空存储
  async clear(storeName) {
    await this.ensureConnection();

    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction([storeName], 'readwrite');
      const store = transaction.objectStore(storeName);
      const request = store.clear();

      request.onsuccess = () => resolve();
      request.onerror = () => reject(request.error);
    });
  }

  // 获取计数
  async count(storeName) {
    await this.ensureConnection();

    return new Promise((resolve, reject) => {
      const transaction = this.db.transaction([storeName], 'readonly');
      const store = transaction.objectStore(storeName);
      const request = store.count();

      request.onsuccess = () => resolve(request.result);
      request.onerror = () => reject(request.error);
    });
  }

  // 检查数据库连接
  async ensureConnection() {
    if (!this.db) {
      await this.init();
    }
  }

  // 导出数据库内容
  async exportDatabase() {
    await this.ensureConnection();

    const stores = ['workflows', 'nodes', 'validationRules', 'connectors', 'executions'];
    const exportData = {};

    for (const storeName of stores) {
      exportData[storeName] = await this.getAll(storeName);
    }

    // 添加元数据
    exportData.metadata = {
      exportTime: new Date().toISOString(),
      version: '1.0.0',
      dbName: this.dbName,
      storeCount: stores.length,
      totalRecords: Object.values(exportData).reduce((sum, data) => sum + data.length, 0)
    };

    return exportData;
  }

  // 导入数据库内容
  async importDatabase(data) {
    await this.ensureConnection();

    // 先备份现有数据
    const backup = await this.exportDatabase();

    try {
      // 清空现有数据
      const stores = Object.keys(data).filter(key => key !== 'metadata');
      for (const storeName of stores) {
        if (this.db.objectStoreNames.contains(storeName)) {
          await this.clear(storeName);

          // 导入新数据
          const items = data[storeName];
          if (items && items.length > 0) {
            await this.saveAll(storeName, items);
          }
        }
      }

      return { success: true, backup };
    } catch (error) {
      // 恢复备份
      await this.importDatabase(backup);
      throw new Error('导入失败，已恢复备份: ' + error.message);
    }
  }

  // 关闭数据库连接
  close() {
    if (this.db) {
      this.db.close();
      this.db = null;
    }
  }
}

// 导出单例
const offlineStorage = new IndexedDBStorage('workflow_offline_db', 1);
export default offlineStorage;