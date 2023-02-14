package com.example.storageservice.service;

import com.example.storageservice.util.Sha256;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

import static java.util.Objects.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucketImageName}")
    String bucket;

    public String upload(MultipartFile file) {
        try {
            String fileName = createFileName(requireNonNull(file.getOriginalFilename()));
            InputStream inputStream = file.getInputStream();

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(args);

            return fileName;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public String upload(String filePath) {
        try {
            String fileName = createFileName(filePath);
            InputStream inputStream = new FileInputStream(filePath);
            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(inputStream, inputStream.available(), -1)
                    .build();
            minioClient.putObject(args);

            return fileName;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public InputStreamResource get(String name) {
        Path path = Path.of(requireNonNull(name));

        try {
            GetObjectArgs args = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(path.toString())
                    .build();

            InputStream inputStream = minioClient.getObject(args);
            return new InputStreamResource(inputStream);
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

    public StatObjectResponse getMetadata(String name) {
        try {
            Path path = Path.of(name);
            StatObjectArgs args = StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(path.toString())
                    .build();
            return minioClient.statObject(args);
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

    public String createFileName(String filename) throws NoSuchAlgorithmException {
        Path path = Path.of(filename);

        String name = path.toString();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf(".")) : "";

        name = Sha256.encrypt(name + LocalDateTime.now()) + ext;

        return name;
    }
}