### TVRemote
基于google的anymote协议，通过手机远程控制智能电视TV的一个客户端

#### 功能模块

+ 手势模式
    * 通过上下左右滑动来控制方向
    * 中间的OK为确定键
    * 单点触控滑动效果
+ 鼠标模式
    * 类似笔记本的触摸板，可通过滑动控制电视上的鼠标
+ 多屏互动
    * 目前客户端需要手机的硬件支持，因此只调用系统的设置。
    一般手机都自带有无线显示功能，TV端的需要打开Miracast界面，并进行连接，即可将手机屏幕投影到TV端
+ 扫一扫
    * 当搜索到附近的设备时，点击连接，TV端会弹出PIN码和二维码两种方式进行连接，通过扫码连接更加方便和快捷
+ 体感手柄
    * 空中鼠标模式，获取手机传感器数据发送到TV端进行操作。通过挥动手机来玩体感游戏
    * 触摸模式，通过触摸移动游戏中的鼠标指针
+ 文件共享
    * 待续
+ 设置
    * 自动连接设备，当第一次连接成功后，每次打开应用都自动匹配并连接已连接过的设备
    * 振动反馈，进行远程操作时智能振动提示
    * 重置应用，清除缓存及已连接过的设备信息

#### 界面
主界面
![main.png](screenshots/main.png)

体感手柄
![handle.png](screenshots/handle.png)
