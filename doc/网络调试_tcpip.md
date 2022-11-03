# 打开网络调试方式

## adb设置

``` shell
# 端口可调整
$adb tcpip 6666
# 单个设备，测试链接，高版本支持USB和网络链接
$ adb connect 10.0.0.210:6666
connected to 10.0.0.210:6666
$ adb devices
List of devices attached
A032DAAEFE6B60	device 
10.0.0.210:6666	device
```

## root方式设置
> 高版本设备失效
``` shell
# 设置网络方式
setprop service.adb.tcp.port 5555
stop adbd
start adbd
# 取消设置网络方式，进入只用USB模式
setprop service.adb.tcp.port -1
stop adbd
start adbd
```