package com.chinaztt.testuil;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.chinaztt.utils.ImageDataUtils;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

/**
 * 当前类注释:
 * 项目名：Android-Universal-Image-Loader-Modify
 * 包名：com.chinaztt.testuil
 * 作者：江清清 on 15/12/30 21:30
 * 邮箱：jiangqqlmj@163.com
 * QQ： 781931404
 * 公司：江苏中天科技软件技术有限公司
 */
public class TwoActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.two_activity);
        final ImageView test_img_two=(ImageView)this.findViewById(R.id.test_img_two);
//        ImageLoader.getInstance().loadImage(ImageDataUtils.ImagesUtils[3], new ImageLoadingListener() {
//            @Override
//            public void onLoadingStarted(String imageUri, View view) {
//                //图片开始加载的时候调用
//                Log.d("zttjiangqq","onLoadingStarted...");
//            }
//
//            @Override
//            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
//                //图片加载失败调用
//                Log.d("zttjiangqq","onLoadingFailed...");
//            }
//
//            @Override
//            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                //图片加载完成调用
//                Log.d("zttjiangqq","onLoadingComplete...");
//                test_img_two.setImageBitmap(loadedImage);
//            }
//
//            @Override
//            public void onLoadingCancelled(String imageUri, View view) {
//                //图片加载取消调用
//                Log.d("zttjiangqq","onLoadingCancelled...");
//            }
//        });
//        ImageLoader.getInstance().loadImage(ImageDataUtils.ImagesUtils[4],new SimpleImageLoadingListener(){
//            @Override
//            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                super.onLoadingComplete(imageUri, view, loadedImage);
//                //图片加载完成时候调用
//                Log.d("zttjiangqq","onLoadingComplete...");
//                test_img_two.setImageBitmap(loadedImage);
//            }
//
//            @Override
//            public void onLoadingStarted(String imageUri, View view) {
//                super.onLoadingStarted(imageUri, view);
//                //图片开始加载的时候调用
//                Log.d("zttjiangqq", "onLoadingStarted...");
//            }
//        });
//        DisplayImageOptions options=new DisplayImageOptions.Builder()
//                .showImageOnLoading(R.drawable.ic_data_loading)
//                .showImageOnFail(R.drawable.ic_data_error)
//                .cacheInMemory(true)
//                .cacheOnDisk(true)
//                .build();
//        ImageLoader.getInstance().loadImage(ImageDataUtils.ImagesUtils[5],options,new SimpleImageLoadingListener(){
//            @Override
//            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                super.onLoadingComplete(imageUri, view, loadedImage);
//                //图片加载完成时候调用
//                Log.d("zttjiangqq","onLoadingComplete...");
//                test_img_two.setImageBitmap(loadedImage);
//            }
//        });
        ImageSize size=new ImageSize(100,50);
        ImageLoader.getInstance().loadImage(ImageDataUtils.ImagesUtils[6],size,new SimpleImageLoadingListener(){
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                super.onLoadingComplete(imageUri, view, loadedImage);
                  //图片加载完成时候调用
                  Log.d("zttjiangqq","onLoadingComplete...");
                  test_img_two.setImageBitmap(loadedImage);
            }
        });
    }
}
