## 🚀 xFB-RPC Roadmap

* [x] 定义 RpcRequest
* [x] 定义 RpcResponse
* [ ] 定义 RpcMessage
* [ ] 定义 MessageType（REQUEST/RESPONSE/HEARTBEAT）
* [ ] 定义 SerializationType 枚举
* [ ] 定义 RpcConstants（魔数、版本号等）
* [x] 实现 RequestIdGenerator
* [ ] 定义 ServiceMetaInfo
* [ ] 定义 RpcException 体系
* [ ] 设计协议头结构
* [ ] 实现魔数校验
* [ ] 实现协议版本字段
* [ ] 实现序列化类型字段
* [ ] 实现消息类型字段
* [ ] 实现请求ID字段
* [ ] 实现数据长度字段
* [x] 实现 RpcEncoder
* [x] 实现 RpcDecoder
* [x] 处理 TCP 粘包拆包
* [ ] 实现协议兼容机制
* [ ] 编写协议单元测试
* [x] 实现 NettyServer
* [x] 配置 BossGroup
* [x] 配置 WorkerGroup
* [x] 构建 ChannelPipeline
* [ ] 添加 IdleStateHandler
* [x] 添加 编解码 Handler
* [x] 实现 ServerHandler
* [ ] 实现服务端优雅关闭
* [ ] 实现服务端异常处理
* [ ] 实现服务端线程池隔离
* [x] 实现 NettyClient
* [x] 配置客户端 Bootstrap
* [x] 实现客户端连接建立逻辑
* [ ] 实现客户端连接缓存机制
* [ ] 实现客户端连接池管理
* [ ] 实现客户端自动重连机制
* [x] 实现客户端 Handler
* [ ] 实现客户端异常处理
* [x] 实现 RpcClient
* [ ] 实现 JDK 动态代理
* [ ] 构建 RpcInvocation
* [x] 构建 RpcRequest
* [x] 实现请求发送逻辑
* [ ] 实现 requestId → CompletableFuture 映射
* [ ] 实现响应回调处理
* [ ] 实现请求超时机制
* [ ] 实现异步调用支持
* [ ] 实现同步调用支持
* [ ] 实现 ServiceProvider
* [ ] 实现服务注册本地缓存
* [x] 实现服务反射调用
* [x] 实现方法参数解析
* [ ] 实现异常封装
* [x] 构建 RpcResponse
* [x] 实现响应写回
* [ ] 定义 @SPI 注解
* [ ] 实现 ExtensionLoader
* [ ] 实现扩展类扫描
* [ ] 实现默认扩展机制
* [ ] 实现扩展实例缓存
* [ ] 实现按名称加载扩展
* [ ] 实现扩展加载异常处理
* [ ] 编写 SPI 单元测试
* [ ] 定义 Serializer 接口
* [ ] 实现 JSONSerializer
* [ ] 实现 KryoSerializer
* [ ] 实现 HessianSerializer
* [ ] 实现序列化类型编号映射
* [ ] 编写序列化性能对比测试
* [ ] 实现序列化异常处理
* [ ] 定义 LoadBalancer 接口
* [ ] 实现 RandomLoadBalancer
* [ ] 实现 RoundRobinLoadBalancer
* [ ] 实现 ConsistentHashLoadBalancer
* [ ] 实现权重支持
* [ ] 实现动态节点更新
* [ ] 编写负载均衡单元测试
* [x] 定义 Registry 接口
* [x] 定义 ServiceInstance
* [ ] 实现 LocalRegistry
* [ ] 实现 ZooKeeperRegistry
* [ ] 实现 NacosRegistry
* [ ] 实现服务注册
* [ ] 实现服务下线
* [ ] 实现服务订阅
* [ ] 实现节点监听
* [ ] 实现服务变更回调
* [ ] 实现心跳请求
* [ ] 实现心跳响应
* [ ] 实现 IdleState 触发检测
* [ ] 实现断线关闭机制
* [ ] 实现客户端自动重连
* [ ] 实现服务剔除机制
* [ ] 实现超时清理任务
* [ ] 实现自动重试机制
* [ ] 实现最大重试次数控制
* [ ] 实现指数退避策略
* [ ] 实现同步调用模式
* [ ] 实现异步 Future 调用模式
* [ ] 实现 Callback 调用模式
* [ ] 实现泛化调用
* [ ] 实现版本控制（version 字段）
* [ ] 实现分组控制（group 字段）
* [ ] 实现多版本共存支持
* [ ] 实现本地限流机制
* [ ] 实现令牌桶算法
* [ ] 实现服务级限流
* [ ] 实现失败次数统计
* [ ] 实现熔断开启机制
* [ ] 实现半开状态机制
* [ ] 实现熔断恢复机制
* [ ] 实现 ByteBuf 零拷贝优化
* [ ] 优化对象创建减少 GC
* [ ] 实现业务线程池隔离
* [ ] 优化序列化缓存
* [ ] 实现批量发送优化
* [x] 集成 SLF4J + Logback
* [x] 实现请求日志记录
* [ ] 实现错误日志记录
* [ ] 实现调用耗时统计
* [ ] 实现 QPS 统计
* [ ] 实现成功率统计
* [ ] 实现 YAML 配置支持
* [ ] 实现注解方式配置
* [ ] 实现 XML 配置支持
* [ ] 实现 Spring Boot Starter
* [ ] 实现 @XfbRpcService 注解
* [ ] 实现 @XfbRpcReference 注解
* [ ] 实现自动注册机制
* [ ] 实现自动注入机制
* [ ] 编写示例项目
* [ ] 编写 README
* [x] 规范代码格式（Spotless）
