# FlashSale Project

## Network / Proxy

当执行网络请求（curl、npm、mvn 等下载操作）遇到连接失败或超时时，自动走代理：

- **代理地址**: `http://127.0.0.1:7890`
- **适用工具**: curl (`-x`), npm (`--registry` 或 proxy config), git (`http.proxy`), maven 等
- curl 示例: `curl -x http://127.0.0.1:7890 ...`
- git 示例: `git config --global http.proxy http://127.0.0.1:7890`
- 环境变量: `export http_proxy=http://127.0.0.1:7890 https_proxy=http://127.0.0.1:7890`
