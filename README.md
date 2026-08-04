# 实习证明与实习记录归档平台

实习证明与实习记录归档平台是一个面向学生、学校、企业和平台管理员的实习过程管理系统。平台围绕实习申请、实习登记、日报记录、考核评价、实习证明、材料归档和链上存证等场景展开，提供前后端分离的 Web 操作界面和 Spring Boot 后端接口。

## 功能概览

- 学生端：账号注册、身份信息维护、岗位申请、实习登记、日报填报、考核查看、毕业归档和证明核验。
- 学校端：学生名册管理、企业信息管理、注册审批、过程抽查、证明核验和通知管理。
- 企业端：企业资料维护、岗位发布、招聘处理、实习考核、争议处理和归档确认。
- 平台端：用户权限管理、账号审批、链上配置、存证核验、系统配置和 AI 辅助。
- 区块链存证：对实习证明、实习记录和归档材料进行哈希计算和链上锚定，支持后续核验。

## 技术栈

- 后端：Spring Boot 3.4.0、Spring Security、Spring Data JPA、MySQL、JWT。
- 前端：Vue 3、Vite、Pinia、Vue Router、Element Plus、Axios。
- 区块链：Solidity 合约、WeBASE/FISCO BCOS 接入配置。
- 部署：Docker、Docker Compose、Nginx。

## 目录说明

```text
.
├── backend/                 # Spring Boot 后端服务
├── frontend/                # Vue/Vite 前端项目
├── InternshipCertificate.sol# 实习证明存证合约
├── InternshipRecord.sol     # 实习记录存证合约
├── docker-compose.yml       # 本地容器编排配置
├── .env.example             # 环境变量示例，不包含真实密钥
└── 操作手册.md              # 平台使用说明
```

## 环境要求

- JDK 24
- Maven 3.8+
- Node.js 18+
- MySQL 8.0+
- 可选：Docker Desktop、WeBASE/FISCO BCOS 环境

## 本地启动

1. 准备数据库。

```bash
mysql -uroot -p
CREATE DATABASE internship_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置环境变量。

复制 `.env.example` 为本地环境文件，并填写数据库、JWT、DeepSeek、WeBASE 和合约地址等配置。真实私钥、API Key、链账户地址只放在本地环境变量中，不要提交到 Git。

3. 启动后端。

```bash
cd backend
mvn spring-boot:run
```

默认后端地址为：

```text
http://localhost:8080/api
```

4. 启动前端。

```bash
cd frontend
npm install
npm run dev
```

默认前端地址为：

```text
http://localhost:5173
```

## 构建命令

后端打包：

```bash
cd backend
mvn clean package
```

前端打包：

```bash
cd frontend
npm run build
```

## Docker 启动

项目提供 `docker-compose.yml`，可用于本地联调。

```bash
docker compose up -d --build
```

启动后可访问：

```text
前端：http://localhost:5173
后端：http://localhost:8080/api
MySQL：localhost:3306
```

## 配置说明

常用环境变量如下：

| 配置项 | 说明 |
| --- | --- |
| `DB_HOST` | MySQL 地址 |
| `DB_PORT` | MySQL 端口 |
| `DB_NAME` | 数据库名称 |
| `DB_USER` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥 |
| `DEEPSEEK_API_KEY` | AI 助手接口密钥 |
| `WEBASE_URL` | WeBASE 服务地址 |
| `CERTIFICATE_CONTRACT_ADDRESS` | 实习证明合约地址 |
| `RECORD_CONTRACT_ADDRESS` | 实习记录合约地址 |
| `SCHOOL_PRIVATE_KEY` | 学校链账户私钥 |
| `ENTERPRISE_PRIVATE_KEY` | 企业链账户私钥 |

## 注意事项

- `.env.local`、真实私钥、API Key、本地 Maven 仓库 `.m2/`、`node_modules/`、`target/`、`dist/` 等不应提交到 GitHub。
- 如果链环境未配置，普通业务页面可以先用于课程演示；涉及链上存证和核验的功能需要补齐 WeBASE 与合约配置。
- 首次运行会根据 JPA 配置自动创建或更新数据库表结构。

