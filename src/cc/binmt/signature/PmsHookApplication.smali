.class public Lcc/binmt/signature/PmsHookApplication;
.super Landroid/app/Application;
.source "PmsHookApplication.java"

# interfaces
.implements Ljava/lang/reflect/InvocationHandler;


# static fields
.field private static final GET_SIGNATURES:I = 0x40


# instance fields
.field private appPkgName:Ljava/lang/String;

.field private base:Ljava/lang/Object;

.field private sign:[[B


# direct methods
.method public constructor <init>()V
    .registers 2

    .prologue
    .line 20
    invoke-direct {p0}, Landroid/app/Application;-><init>()V

    .line 25
    const-string/jumbo v0, ""

    iput-object v0, p0, Lcc/binmt/signature/PmsHookApplication;->appPkgName:Ljava/lang/String;

    return-void
.end method

.method private hook(Landroid/content/Context;)V
    .registers 22
    .param p1, "context"    # Landroid/content/Context;

    .prologue
    .line 52
    :try_start_0
    const-string/jumbo v6, "### Signatures Data ###"

    .line 53
    .local v6, "data":Ljava/lang/String;
    new-instance v10, Ljava/io/DataInputStream;

    new-instance v17, Ljava/io/ByteArrayInputStream;

    const/16 v18, 0x0

    move/from16 v0, v18

    invoke-static {v6, v0}, Landroid/util/Base64;->decode(Ljava/lang/String;I)[B

    move-result-object v18

    invoke-direct/range {v17 .. v18}, Ljava/io/ByteArrayInputStream;-><init>([B)V

    move-object/from16 v0, v17

    invoke-direct {v10, v0}, Ljava/io/DataInputStream;-><init>(Ljava/io/InputStream;)V

    .line 54
    .local v10, "is":Ljava/io/DataInputStream;
    invoke-virtual {v10}, Ljava/io/DataInputStream;->read()I

    move-result v17

    move/from16 v0, v17

    and-int/lit16 v0, v0, 0xff

    move/from16 v17, v0

    move/from16 v0, v17

    new-array v0, v0, [[B

    move-object/from16 v16, v0

    .line 55
    .local v16, "sign":[[B
    const/4 v8, 0x0

    .local v8, "i":I
    :goto_28
    move-object/from16 v0, v16

    array-length v0, v0

    move/from16 v17, v0

    move/from16 v0, v17

    if-ge v8, v0, :cond_47

    .line 56
    invoke-virtual {v10}, Ljava/io/DataInputStream;->readInt()I

    move-result v17

    move/from16 v0, v17

    new-array v0, v0, [B

    move-object/from16 v17, v0

    aput-object v17, v16, v8

    .line 57
    aget-object v17, v16, v8

    move-object/from16 v0, v17

    invoke-virtual {v10, v0}, Ljava/io/DataInputStream;->readFully([B)V

    .line 55
    add-int/lit8 v8, v8, 0x1

    goto :goto_28

    .line 61
    :cond_47
    const-string/jumbo v17, "android.app.ActivityThread"

    invoke-static/range {v17 .. v17}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v3

    .line 62
    .local v3, "activityThreadClass":Ljava/lang/Class;, "Ljava/lang/Class<*>;"
    const-string/jumbo v17, "currentActivityThread"

    const/16 v18, 0x0

    move/from16 v0, v18

    new-array v0, v0, [Ljava/lang/Class;

    move-object/from16 v18, v0

    .line 63
    move-object/from16 v0, v17

    move-object/from16 v1, v18

    invoke-virtual {v3, v0, v1}, Ljava/lang/Class;->getDeclaredMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;

    move-result-object v5

    .line 64
    .local v5, "currentActivityThreadMethod":Ljava/lang/reflect/Method;
    const/16 v17, 0x0

    const/16 v18, 0x0

    move/from16 v0, v18

    new-array v0, v0, [Ljava/lang/Object;

    move-object/from16 v18, v0

    move-object/from16 v0, v17

    move-object/from16 v1, v18

    invoke-virtual {v5, v0, v1}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v4

    .line 67
    .local v4, "currentActivityThread":Ljava/lang/Object;
    const-string/jumbo v17, "sPackageManager"

    move-object/from16 v0, v17

    invoke-virtual {v3, v0}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;

    move-result-object v15

    .line 68
    .local v15, "sPackageManagerField":Ljava/lang/reflect/Field;
    const/16 v17, 0x1

    move/from16 v0, v17

    invoke-virtual {v15, v0}, Ljava/lang/reflect/Field;->setAccessible(Z)V

    .line 69
    invoke-virtual {v15, v4}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v14

    .line 72
    .local v14, "sPackageManager":Ljava/lang/Object;
    const-string/jumbo v17, "android.content.pm.IPackageManager"

    invoke-static/range {v17 .. v17}, Ljava/lang/Class;->forName(Ljava/lang/String;)Ljava/lang/Class;

    move-result-object v9

    .line 73
    .local v9, "iPackageManagerInterface":Ljava/lang/Class;, "Ljava/lang/Class<*>;"
    move-object/from16 v0, p0

    iput-object v14, v0, Lcc/binmt/signature/PmsHookApplication;->base:Ljava/lang/Object;

    .line 74
    move-object/from16 v0, v16

    move-object/from16 v1, p0

    iput-object v0, v1, Lcc/binmt/signature/PmsHookApplication;->sign:[[B

    .line 75
    invoke-virtual/range {p1 .. p1}, Landroid/content/Context;->getPackageName()Ljava/lang/String;

    move-result-object v17

    move-object/from16 v0, v17

    move-object/from16 v1, p0

    iput-object v0, v1, Lcc/binmt/signature/PmsHookApplication;->appPkgName:Ljava/lang/String;

    .line 78
    invoke-virtual {v9}, Ljava/lang/Class;->getClassLoader()Ljava/lang/ClassLoader;

    move-result-object v17

    const/16 v18, 0x1

    move/from16 v0, v18

    new-array v0, v0, [Ljava/lang/Class;

    move-object/from16 v18, v0

    const/16 v19, 0x0

    aput-object v9, v18, v19

    .line 77
    move-object/from16 v0, v17

    move-object/from16 v1, v18

    move-object/from16 v2, p0

    invoke-static {v0, v1, v2}, Ljava/lang/reflect/Proxy;->newProxyInstance(Ljava/lang/ClassLoader;[Ljava/lang/Class;Ljava/lang/reflect/InvocationHandler;)Ljava/lang/Object;

    move-result-object v13

    .line 83
    .local v13, "proxy":Ljava/lang/Object;
    invoke-virtual {v15, v4, v13}, Ljava/lang/reflect/Field;->set(Ljava/lang/Object;Ljava/lang/Object;)V

    .line 86
    invoke-virtual/range {p1 .. p1}, Landroid/content/Context;->getPackageManager()Landroid/content/pm/PackageManager;

    move-result-object v12

    .line 87
    .local v12, "pm":Landroid/content/pm/PackageManager;
    invoke-virtual {v12}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v17

    const-string/jumbo v18, "mPM"

    invoke-virtual/range {v17 .. v18}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;

    move-result-object v11

    .line 88
    .local v11, "mPmField":Ljava/lang/reflect/Field;
    const/16 v17, 0x1

    move/from16 v0, v17

    invoke-virtual {v11, v0}, Ljava/lang/reflect/Field;->setAccessible(Z)V

    .line 89
    invoke-virtual {v11, v12, v13}, Ljava/lang/reflect/Field;->set(Ljava/lang/Object;Ljava/lang/Object;)V

    .line 90
    sget-object v17, Ljava/lang/System;->out:Ljava/io/PrintStream;

    const-string/jumbo v18, "PmsHook success."

    invoke-virtual/range {v17 .. v18}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V
    :try_end_e0
    .catch Ljava/lang/Exception; {:try_start_0 .. :try_end_e0} :catch_e1

    .line 95
    .end local v3    # "activityThreadClass":Ljava/lang/Class;, "Ljava/lang/Class<*>;"
    .end local v4    # "currentActivityThread":Ljava/lang/Object;
    .end local v5    # "currentActivityThreadMethod":Ljava/lang/reflect/Method;
    .end local v6    # "data":Ljava/lang/String;
    .end local v8    # "i":I
    .end local v9    # "iPackageManagerInterface":Ljava/lang/Class;, "Ljava/lang/Class<*>;"
    .end local v10    # "is":Ljava/io/DataInputStream;
    .end local v11    # "mPmField":Ljava/lang/reflect/Field;
    .end local v12    # "pm":Landroid/content/pm/PackageManager;
    .end local v13    # "proxy":Ljava/lang/Object;
    .end local v14    # "sPackageManager":Ljava/lang/Object;
    .end local v15    # "sPackageManagerField":Ljava/lang/reflect/Field;
    .end local v16    # "sign":[[B
    :goto_e0
    return-void

    .line 91
    :catch_e1
    move-exception v7

    .line 92
    .local v7, "e":Ljava/lang/Exception;
    sget-object v17, Ljava/lang/System;->err:Ljava/io/PrintStream;

    const-string/jumbo v18, "PmsHook failed."

    invoke-virtual/range {v17 .. v18}, Ljava/io/PrintStream;->println(Ljava/lang/String;)V

    .line 93
    invoke-virtual {v7}, Ljava/lang/Exception;->printStackTrace()V

    goto :goto_e0
.end method


# virtual methods
.method protected attachBaseContext(Landroid/content/Context;)V
    .registers 2
    .param p1, "base"    # Landroid/content/Context;

    .prologue
    .line 29
    invoke-direct {p0, p1}, Lcc/binmt/signature/PmsHookApplication;->hook(Landroid/content/Context;)V

    .line 30
    invoke-super {p0, p1}, Landroid/app/Application;->attachBaseContext(Landroid/content/Context;)V

    .line 31
    return-void
.end method

.method public invoke(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;
    .registers 11
    .param p1, "proxy"    # Ljava/lang/Object;
    .param p2, "method"    # Ljava/lang/reflect/Method;
    .param p3, "args"    # [Ljava/lang/Object;
    .annotation system Ldalvik/annotation/Throws;
        value = {
            Ljava/lang/Throwable;
        }
    .end annotation

    .prologue
    .line 35
    const-string/jumbo v4, "getPackageInfo"

    invoke-virtual {p2}, Ljava/lang/reflect/Method;->getName()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v4, v5}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_4c

    .line 36
    const/4 v4, 0x0

    aget-object v3, p3, v4

    check-cast v3, Ljava/lang/String;

    .line 37
    .local v3, "pkgName":Ljava/lang/String;
    const/4 v4, 0x1

    aget-object v0, p3, v4

    check-cast v0, Ljava/lang/Integer;

    .line 38
    .local v0, "flag":Ljava/lang/Integer;
    invoke-virtual {v0}, Ljava/lang/Integer;->intValue()I

    move-result v4

    and-int/lit8 v4, v4, 0x40

    if-eqz v4, :cond_4c

    iget-object v4, p0, Lcc/binmt/signature/PmsHookApplication;->appPkgName:Ljava/lang/String;

    invoke-virtual {v4, v3}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v4

    if-eqz v4, :cond_4c

    .line 39
    iget-object v4, p0, Lcc/binmt/signature/PmsHookApplication;->base:Ljava/lang/Object;

    invoke-virtual {p2, v4, p3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    check-cast v2, Landroid/content/pm/PackageInfo;

    .line 40
    .local v2, "info":Landroid/content/pm/PackageInfo;
    iget-object v4, p0, Lcc/binmt/signature/PmsHookApplication;->sign:[[B

    array-length v4, v4

    new-array v4, v4, [Landroid/content/pm/Signature;

    iput-object v4, v2, Landroid/content/pm/PackageInfo;->signatures:[Landroid/content/pm/Signature;

    .line 41
    const/4 v1, 0x0

    .local v1, "i":I
    :goto_37
    iget-object v4, v2, Landroid/content/pm/PackageInfo;->signatures:[Landroid/content/pm/Signature;

    array-length v4, v4

    if-ge v1, v4, :cond_52

    .line 42
    iget-object v4, v2, Landroid/content/pm/PackageInfo;->signatures:[Landroid/content/pm/Signature;

    new-instance v5, Landroid/content/pm/Signature;

    iget-object v6, p0, Lcc/binmt/signature/PmsHookApplication;->sign:[[B

    aget-object v6, v6, v1

    invoke-direct {v5, v6}, Landroid/content/pm/Signature;-><init>([B)V

    aput-object v5, v4, v1

    .line 41
    add-int/lit8 v1, v1, 0x1

    goto :goto_37

    .line 47
    .end local v0    # "flag":Ljava/lang/Integer;
    .end local v1    # "i":I
    .end local v2    # "info":Landroid/content/pm/PackageInfo;
    .end local v3    # "pkgName":Ljava/lang/String;
    :cond_4c
    iget-object v4, p0, Lcc/binmt/signature/PmsHookApplication;->base:Ljava/lang/Object;

    invoke-virtual {p2, v4, p3}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v2

    :cond_52
    return-object v2
.end method
