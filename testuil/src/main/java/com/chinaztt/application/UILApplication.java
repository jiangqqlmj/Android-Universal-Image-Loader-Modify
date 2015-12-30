package com.chinaztt.application;

import android.app.Application;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

/**
 * 当前类注释:
 * 项目名：Android-Universal-Image-Loader-Modify
 * 包名：com.chinaztt.application
 * 作者：江清清 on 15/12/30 15:02
 * 邮箱：jiangqqlmj@163.com
 * QQ： 781931404
 * 公司：江苏中天科技软件技术有限公司
 */
public class UILApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initImageLoader();
    }
    private void initImageLoader(){
        ImageLoaderConfiguration configuration=ImageLoaderConfiguration.createDefault(this);
        ImageLoader.getInstance().init(configuration);

    }
}
