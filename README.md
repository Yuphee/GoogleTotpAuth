# GoogleTotpAuth
手机宝令 基于 GoogleAuthenticator的totp算法的手机宝令功能，附带翻牌效果
> 关于我，欢迎关注  
  邮箱：437220638@qq.com
 
## Screenshots
![image](/screenshots/video1.gif)

##特性
- 根据GoogleAuthenticator demo中抽离出来的关键部分算法，可以和https://github.com/google/google-authenticator-android 官方demo做下对比
- 模仿了现在主流手机宝令翻牌的效果

##原理说明
- demo中需要一个secret key字母表在ABCDEFGHIJKLMNOPQRSTUVWXYZ234567之间，需要同服务端相同
- 项目中时间步长为30s,算法还需传递一个当前时间，必须保证这个时间的准确性，所以必须和服务器时间相同(可以有一定的误差，不能太大)
- 最后生成一个6位的认证令牌

##使用方法
参考demo使用

##TODO
将一系列的算法进一步分装,方便集成。
