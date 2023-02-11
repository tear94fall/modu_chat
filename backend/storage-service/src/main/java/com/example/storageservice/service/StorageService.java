package com.example.storageservice.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucketImageName}")
    String bucket;

    public void upload(MultipartFile file) {
        Path path = Path.of(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            InputStream inputStream = file.getInputStream();

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path.toString())
                    .stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void upload(String filePath) {
        Path path = Path.of(Objects.requireNonNull(filePath));

        try {
            InputStream inputStream = new FileInputStream(filePath);
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(path.toString())
                    .stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(args);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @SneakyThrows
    public byte[] view(String name) {
        InputStream inputStream = minioClient
                .getObject(GetObjectArgs
                        .builder()
                        .bucket(bucket)
                        .object(name)
                        .build());

        return inputStream.readAllBytes();
    }

    public void delete(String name) {
        try {
            Path path = Path.of(name);
            RemoveObjectArgs args = RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(path.toString())
                    .build();
            minioClient.removeObject(args);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public String downloadFromUrl(String imgURL) {
        String fullPath = "";
        String[] fileName = imgURL.substring(imgURL.lastIndexOf("/")).split("/");

        try{
            URL url = new URL(imgURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream is = connection.getInputStream();

            File file = new File(fileName[1]);
            FileOutputStream out = new FileOutputStream(file);
            int i = 0;

            while((i=is.read()) != -1){
                out.write(i);
            }

            is.close();
            out.close();

            fullPath = file.getPath();
        } catch(Exception e){
            e.printStackTrace();
        }

        return fullPath;
    }

    public void deleteFile(String filePath) {
        File file = new File(filePath);

        if(file.exists()){
            file.delete();
        }
    }
}