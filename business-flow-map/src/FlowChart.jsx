import React, { useEffect, useRef } from 'react';
import { Graph } from '@antv/x6';

export default function FlowChart({ data, onNodeClick, activeNodeId }) {
  const containerRef = useRef(null);
  const graphRef = useRef(null);

  useEffect(() => {
    if (!containerRef.current) return;

    if (!graphRef.current) {
      graphRef.current = new Graph({
        container: containerRef.current,
        autoResize: true,
        grid: { size: 10, visible: true, type: 'dot', color: '#e0e0e0' },
        background: { color: '#f8f9fa' },
        panning: true,
        mousewheel: { enabled: true, modifiers: ['ctrl', 'meta'] },
        connecting: {
          snap: true,
          allowBlank: false,
          allowLoop: false,
          highlight: true,
          connector: 'rounded',
          router: 'manhattan',
          createEdge() {
            return this.createEdge({
              attrs: { line: { stroke: '#1890ff', strokeWidth: 2, targetMarker: 'block' } },
            });
          },
        },
      });
    }

    const graph = graphRef.current;
    graph.clearCells();

    // 渲染节点
    data.nodes.forEach(node => {
      const isSelected = node.id === activeNodeId;
      const { nodeType, themeColor } = node.config || {};
      
      // 根据类型设置形状：起始/结束用圆角胶囊形，过程用矩形
      const rx = (nodeType === 'start' || nodeType === 'end') ? 25 : 4;

      graph.addNode({
        id: node.id,
        x: node.x,
        y: node.y,
        width: 150,
        height: 50,
        attrs: {
          body: {
            fill: isSelected ? '#e6f7ff' : '#ffffff',
            stroke: isSelected ? '#1890ff' : themeColor,
            strokeWidth: isSelected ? 3 : 2,
            rx: rx,
            ry: rx,
          },
          label: { 
            text: node.label, 
            fill: '#333',
            fontSize: 12,
            fontWeight: nodeType === 'start' ? 'bold' : 'normal'
          }
        },
        // 智能连接点控制
        ports: {
          groups: {
            in: { position: 'left', attrs: { circle: { r: 4, magnet: true, stroke: themeColor, fill: '#fff', strokeWidth: 1 } } },
            out: { position: 'right', attrs: { circle: { r: 4, magnet: true, stroke: themeColor, fill: '#fff', strokeWidth: 1 } } }
          },
          items: [
            // 起始节点只有右出，结束节点只有左进，普通节点两边都有
            ...(nodeType !== 'end' ? [{ group: 'out', id: 'p-out' }] : []),
            ...(nodeType !== 'start' ? [{ group: 'in', id: 'p-in' }] : [])
          ]
        },
        data: node
      });
    });

    // 渲染连线
    data.edges.forEach(edge => {
      graph.addEdge({
        source: edge.source,
        target: edge.target,
        attrs: { line: { stroke: '#adc6ff', strokeWidth: 2 } }
      });
    });

    graph.off('node:click');
    graph.on('node:click', ({ node }) => onNodeClick(node.getData()));

  }, [data, activeNodeId]);

  return <div ref={containerRef} style={{ width: '100%', height: '100%' }} />;
}