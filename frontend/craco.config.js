const CracoAntDesignPlugin = require('craco-antd');
const path = require('path');

module.exports = {
  plugins: [
    {
      plugin: CracoAntDesignPlugin,
      options: {
        customizeTheme: {
          '@primary-color': '#1890ff',
          '@border-radius-base': '4px',
        },
      },
    },
  ],
  webpack: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
      '@components': path.resolve(__dirname, 'src/components'),
      '@services': path.resolve(__dirname, 'src/services'),
      '@utils': path.resolve(__dirname, 'src/utils'),
      '@hooks': path.resolve(__dirname, 'src/hooks'),
    },
    configure: (webpackConfig, { env, paths }) => {
      // 修改输出配置
      if (env === 'production') {
        webpackConfig.output = {
          ...webpackConfig.output,
          filename: 'static/js/[name].[contenthash:8].js',
          chunkFilename: 'static/js/[name].[contenthash:8].chunk.js',
          path: path.resolve(__dirname, 'build'),
          publicPath: '/',
        };

        // 添加环境变量
        webpackConfig.plugins.forEach(plugin => {
          if (plugin.constructor.name === 'DefinePlugin') {
            plugin.definitions['process.env'].REACT_APP_MODE = JSON.stringify(process.env.REACT_APP_MODE || 'online');
          }
        });
      }

      return webpackConfig;
    },
  },
  devServer: {
    port: 3000,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
    historyApiFallback: true,
    hot: true,
  },
  style: {
    postcss: {
      plugins: [
        require('tailwindcss'),
        require('autoprefixer'),
      ],
    },
  },
};