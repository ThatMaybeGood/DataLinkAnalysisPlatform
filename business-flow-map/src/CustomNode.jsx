import React from 'react';
import { DatabaseOutlined } from '@ant-design/icons';

export default function CustomNode({ node }) {
  // 增加安全取值，防止 data 为空
  const data = (node && node.getData) ? node.getData() : {};
  
  return (
    <div style={{
      display: 'flex', 
      flexDirection: 'column', 
      alignItems: 'center', 
      justifyContent: 'center',
      padding: '10px', 
      background: '#141414', 
      border: '1px solid #1890ff',
      borderRadius: '4px', 
      width: '150px', 
      height: '70px', 
      color: '#fff',
      cursor: 'pointer',
      boxShadow: '0 0 10px rgba(24, 144, 255, 0.3)',
      // 保证内部元素不影响点击事件
      pointerEvents: 'none' 
    }}>
      <div style={{ pointerEvents: 'none', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <DatabaseOutlined style={{ color: '#1890ff', fontSize: '20px', marginBottom: '4px' }} />
        <div style={{ fontSize: '12px', fontWeight: 'bold' }}>{data.label || '未命名站点'}</div>
      </div>
    </div>
  );
}