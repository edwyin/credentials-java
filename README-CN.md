[English](./README.md) | 简体中文

![](https://aliyunsdk-pages.alicdn.com/icons/AlibabaCloud.svg)

# Alibaba Cloud Credentials for Java
[![Travis Build Status](https://travis-ci.org/aliyun/credentials-java.svg?branch=master)](https://travis-ci.org/aliyun/credentials-php)
[![Appveyor Build Status](https://ci.appveyor.com/api/projects/status/6jxpwmhyfipagtge/branch/master?svg=true)](https://ci.appveyor.com/project/aliyun/credentials-java)
[![codecov](https://codecov.io/gh/aliyun/credentials-java/branch/master/graph/badge.svg)](https://codecov.io/gh/aliyun/credentials-java)
[![Latest Stable Version](https://img.shields.io/maven-central/v/com.aliyun/credentials-java.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.aliyun%22%20AND%20a:%22credentials-java%22)

Alibaba Cloud Credentials for Java 是帮助 Java 开发者管理凭据的工具。

本文将介绍如何获取和使用 Credentials for Java。

## 环境要求
1.  Alibaba Cloud Credentials for Java 需要1.8以上的JDK。

## 安装
```xml
<dependency>
    <groupId>com.aliyun</groupId>
    <artifactId>credentials-java</artifactId>
    <version>使用maven标签显示的版本</version>
</dependency>
```

##快速使用
在您开始之前，您需要注册阿里云帐户并获取您的[凭证](https://usercenter.console.aliyun.com/#/manage/ak)。

### 凭证类型

#### AccessKey
通过[用户信息管理][ak]设置 access_key，它们具有该账户完全的权限，请妥善保管。有时出于安全考虑，您不能把具有完全访问权限的主账户 AccessKey 交于一个项目的开发者使用，您可以[创建RAM子账户][ram]并为子账户[授权][permissions]，使用RAM子用户的 AccessKey 来进行API调用。

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception{
        Config config = new Config();
        // 凭证类型
        config.type = "access_key";
        // AccessKeyId
        config.accessKeyId = "AccessKeyId";
        // AccessKeySecret
        config.accessKeySecret = "AccessKeySecret";
        Client client = new Client(config);
    }
}
```

#### STS
通过安全令牌服务（Security Token Service，简称 STS），申请临时安全凭证（Temporary Security Credentials，简称 TSC），创建临时安全凭证。

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception{
        Config config = new Config();
        // 凭证类型
        config.type = "sts";
        // AccessKeyId
        config.accessKeyId = "AccessKeyId";
        // AccessKeySecret
        config.accessKeySecret = "AccessKeySecret";
        // STS Token
        config.securityToken = "SecurityToken";
        Client client = new Client(config);
    }
}
```

#### RamRoleArn
通过指定[RAM角色][RAM Role]，让凭证自动申请维护 STS Token。你可以通过为 `Policy` 赋值来限制获取到的 STS Token 的权限。

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception{
        Config config = new Config();
        // 凭证类型
        config.type = "ram_role_arn";
        // AccessKeyId
        config.accessKeyId = "AccessKeyId";
        // AccessKeySecret
        config.accessKeySecret = "AccessKeySecret";
        // 格式: acs:ram::用户Id:role/角色名
        config.roleArn = "RoleArn";
        // 角色会话名称
        config.roleSessionName = "RoleSessionName";
        // 可选, 限制 STS Token 的权限
        config.policy = "policy";
        // 可选, 限制 STS Token 的有效时间
        config.roleSessionExpiration = 3600;
        Client client = new Client(config);
    }
}
```

#### OIDCRoleArn
通过指定[OIDC 角色][OIDC Role]，让凭证自动申请维护 STS Token。你可以通过为 `Policy` 赋值来限制获取到的 STS Token 的权限。

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception{
        Config config = new Config();
        // 凭证类型
        config.type = "oidc_role_arn";
        // AccessKeyId，可选参数
        config.accessKeyId = "AccessKeyId";
        // AccessKeySecret，可选参数
        config.accessKeySecret = "AccessKeySecret";
        // 格式: acs:ram::用户Id:role/角色名
        config.roleArn = "RoleArn";
        // 格式: acs:ram::用户Id:oidc-provider/OIDC身份提供商名称
        config.oidcProviderArn = "OIDCProviderArn";
        // 格式: path
        // OIDCTokenFilePath 可不设，但需要通过设置 ALIBABA_CLOUD_OIDC_TOKEN_FILE 来代替
        config.oidcTokenFilePath = "/Users/xxx/xxx";
        // 角色会话名称
        config.roleSessionName = "RoleSessionName";
        // 可选, 限制 STS Token 的权限
        config.policy = "policy";
        // 可选, 限制 STS Token 的有效时间
        config.roleSessionExpiration = 3600;
        Client client = new Client(config);
    }
}
```

#### EcsRamRole
通过指定角色名称，让凭证自动申请维护 STS Token

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        // 凭证类型
        config.type = "ecs_ram_role";
        // 账户RoleName，非必填，不填则自动获取，建议设置，可以减少请求
        config.roleName = "RoleName";
        Client client = new Client(config);
    }
}
```

#### RsaKeyPair
通过指定公钥Id和私钥文件，让凭证自动申请维护 AccessKey。仅支持日本站。

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        // 凭证类型
        config.type = "rsa_key_pair";
        // PrivateKey文件路径
        config.privateKeyFile = "PrivateKeyFile";
        // 账户PublicKeyId
        config.publicKeyId = "PublicKeyId";
        Client client = new Client(config);
    }
}
```

#### URLCredential
通过指定提供凭证的自定义网络服务地址，让凭证自动申请维护 STS Token

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        // 凭证类型
        config.type = "credentials_uri";
        // 提供凭证的 URL，可不设，但需要通过设置 ALIBABA_CLOUD_CREDENTIALS_URI 来代替
        config.credentialsURI = "http://xxx";
        Client client = new Client(config);
    }
}
```

#### Bearer Token
如呼叫中心(CCC)需用此凭证，请自行申请维护 Bearer Token。

```java
import com.aliyun.credentials.Client;
import com.aliyun.credentials.models.Config;

public class DemoTest {
    public static void main(String[] args) throws Exception {
        Config config = new Config();
        // 凭证类型
        config.type = "bearer";
        // BearerToken
        config.bearerToken = "BearerToken";
        Client client = new Client(config);
    }
}
```

### 使用默认凭证提供链
如果你调用 `Client client = new Client()` 时， 将通过凭证提供链来为你获取凭证。

默认凭证提供程序链查找可用的凭证，寻找顺序如下：

1.系统属性

在系统属性里寻找环境凭证，如果定义了 `alibabacloud.accessKeyId` 和 `alibabacloud.accessKeyIdSecret` 系统属性且不为空，程序将使用它们创建默认凭证。

2.环境凭证

在环境变量里寻找环境凭证，如果定义了 `ALIBABA_CLOUD_ACCESS_KEY_ID` 和 `ALIBABA_CLOUD_ACCESS_KEY_SECRET` 环境变量且不为空，程序将使用它们创建默认凭证。

3.配置文件

如果用户主目录存在默认文件 `~/.alibabacloud/credentials （Windows 为 C:\Users\USER_NAME\.alibabacloud\credentials）`，程序会自动创建指定类型和名称的凭证。默认文件可以不存在，但解析错误会抛出异常。配置名小写。不同的项目、工具之间可以共用这个配置文件，因为不在项目之内，也不会被意外提交到版本控制。
可以通过定义 `ALIBABA_CLOUD_CREDENTIALS_FILE` 环境变量修改默认文件的路径。不配置则使用默认配置 `default`，也可以设置环境变量 `ALIBABA_CLOUD_PROFILE` 使用配置。 

```ini
[default]                          # 默认配置
enable = true                      # 启用，没有该选项默认不启用
type = access_key                  # 认证方式为 access_key
access_key_id = foo                # Key
access_key_secret = bar            # Secret

[client1]                          # 命名为 `client1` 的配置
type = ecs_ram_role                # 认证方式为 ecs_ram_role
role_name = EcsRamRoleTest         # Role Name

[client2]                          # 命名为 `client2` 的配置
enable = false                     # 不启用
type = ram_role_arn                # 认证方式为 ram_role_arn
region_id = cn-test                # 获取session用的region
policy = test                      # 选填 指定权限
access_key_id = foo
access_key_secret = bar
role_arn = role_arn
role_session_name = session_name   # 选填

[client3]                          # 命名为 `client3` 的配置
type = rsa_key_pair                # 认证方式为 rsa_key_pair
public_key_id = publicKeyId        # Public Key ID
private_key_file = /your/pk.pem    # Private Key 文件

[client4]                          # 命名为 `client4` 的配置
enable = false                     # 不启用
type = oidc_role_arn               # 认证方式为 oidc_role_arn
region_id = cn-test                # 获取session用的region
policy = test                      # 选填 指定权限
access_key_id = foo                # 选填
access_key_secret = bar            # 选填
role_arn = role_arn
oidc_provider_arn = oidc_provider_arn
oidc_token_file_path = /xxx/xxx    # 可通过设置环境变量 ALIBABA_CLOUD_OIDC_TOKEN_FILE 来代替
role_session_name = session_name   # 选填
```

## 问题
[提交 Issue](https://github.com/aliyun/credentials-java/issues/new)，不符合指南的问题可能会立即关闭。

## 发行说明
每个版本的详细更改记录在[发行说明](./ChangeLog.txt)中。

## 贡献
提交 Pull Request 之前请阅读[贡献指南](./.github/PULL_REQUEST_TEMPLATE.md)。

## 相关
* [阿里云服务 Regions & Endpoints](https://developer.aliyun.com/endpoints)
* [OpenAPI 开发者门户](https://next.api.aliyun.com/)
* [最新源码](https://github.com/aliyun/aliyun-openapi-java-sdk)

## 许可证
[Apache-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Copyright 2009-present Alibaba Cloud All rights reserved.
