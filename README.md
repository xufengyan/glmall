# glmall
商城项目源码

项目模块：介绍

glmall-user端口为8010
glmall-parent 模块统一管理项目中的公用的依赖以及版本号，当需要使用时候直接通过依赖名引入就行。
glmall-api模块主要存放entity和通用mapper的service接口
glmall-commonUtil 存放的是通用的第三方工具包比如:springboot,common等依赖的
glmall-serviceUtil 主要管理连接数据库层操作的依赖包
glmall-webUtil 主要管理的是前端需要的依赖包

使用dubbo的soa原理，将一个项目拆分为web和service两个项目，然后通过dubbo的rpc远程调用服务
进行通讯
下面是例子：
glmall-webUser 端口为8050
glmall-serviceUSer 端口为8060

glmall-itemservice 商品详情后台 端口为 8280
glmall-itemweb 商品详情前台  端口为 8180

glmall-searchweb 搜索服务前台 端口为8181和glmall-itemweb （同用glmall-itemservice连接数据库）

glmall-cartweb 购物车前台功能 端口为8182