一键破解APK签名校验
=================

参考自[fourbrother/kstools](https://github.com/fourbrother/kstools)，在其基础上进行了部分改进，并用纯 Java 实现。

原理
-----

通过插入代码到 Application 入口，hook 了程序中 PackageManager 的 getPackageInfo 方法，改变了其获取到的签名信息。

处理步骤
-------

1. 读取原APK的签名信息。
2. 替换或添加 AndroidManifest.xml 中 application 的 name 属性。
3. 读取 PmsHookApplication.smali 并替换签名信息，如果原 APK 自定义了 Application，还需要修改 PmsHookApplication 的父类，最后编译 smali 并添加到 classes.dex 中。
4. 对输出的APK进行签名。

使用方法
-------

1. 修改 config.txt 中的配置信息。
2. 打开终端，cd到项目根目录，输入 java -jar nkstool.jar 并回车。
