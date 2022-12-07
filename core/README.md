# adblib

https://github.com/alipay/SoloPi/tree/master/src/AdbLib

## 用法

### 1. 必须命令行执行指令: adb tcpip 5555

### 2. 初始化adb链接功能

``` java
// 需要context对象，并且初始化链接功能
Mys.context(MainActivity.this).generateConnection();
```

### 3. 指令高权限指令

``` java
Mys.execHighPrivilegeCmd("dumpsys window | grep mCurrentFocus");
```