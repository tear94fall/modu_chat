package com.example.modumessenger.Global;

import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;

public class GlideUtil {

    public static void setProfileImage(ImageView imageView, String fileName) {
        if(fileName == null || fileName.equals("")) {
            setBasicProfileImage(imageView);
            return ;
        }

        String accessToken = PreferenceManager.getString("access-token");
        String url = RetrofitClient.getBaseUrl() + "storage-service/view/"+ fileName;

        GlideUrl glideUrl = new GlideUrl(url,
                new LazyHeaders.Builder()
                        .addHeader("Authorization", accessToken)
                        .build());

        Glide.with(imageView)
                .load(glideUrl)
                .error(Glide.with(imageView)
                        .load(R.drawable.basic_profile_image)
                        .into(imageView))
                .into(imageView);
    }

    public static void setBasicProfileImage(ImageView imageView) {
        Glide.with(imageView)
                .load(R.drawable.basic_profile_image)
                .into(imageView);
    }
}
