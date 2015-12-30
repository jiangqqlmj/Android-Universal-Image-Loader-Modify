package com.chinaztt.testuil;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.chinaztt.utils.ImageDataUtils;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageSize;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView test_img_one=(ImageView)this.findViewById(R.id.test_img_one);
        DisplayImageOptions options=new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.ic_data_loading)
                .showImageOnFail(R.drawable.ic_data_error)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .build();
        ImageSize size=new ImageSize(100,50);
        ImageLoader.getInstance().displayImage(ImageDataUtils.ImagesUtils[1],test_img_one,size);

        Button btn_one=(Button)this.findViewById(R.id.btn_one);
        btn_one.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                  MainActivity.this.startActivity(new Intent(MainActivity.this,TwoActivity.class));
            }
        });

    }
}
