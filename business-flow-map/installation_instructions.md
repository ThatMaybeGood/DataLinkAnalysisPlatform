### 第一步：环境准备与工具安装

你需要安装 Node.js (推荐 v18 或更高版本)。

打开你的终端（CMD 或 PowerShell），我们使用 **Vite** 来快速创建一个高性能的 React 项目：

Bash

```
# 1. 创建项目 (项目名叫 business-flow-map)
npm create vite@latest business-flow-map -- --template react

# 2. 进入目录
cd business-flow-map

# 3. 安装核心依赖
# antd: UI组件库(侧边栏、弹窗、布局)
# @antv/x6: 图形引擎核心
# @antv/x6-react-shape: 让X6可以使用React组件渲染节点
# @ant-design/icons: 图标库(用于状态显示)
npm install antd @antv/x6 @antv/x6-react-shape @ant-design/icons

# 4. 安装依赖并启动
npm install
npm run dev
```
