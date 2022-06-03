package com.example.modumessenger.Activity;

import static android.app.Activity.RESULT_OK;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.modumessenger.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;

public class ChatSendOthersActivity extends BottomSheetDialogFragment {

    ActivityResultLauncher<Intent> resultLauncher;
    Button imageSend, fileSend, audioSend;
    String roomId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chat_send_others, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getData();
        bindingView(view);
        setLauncher();
        setButtonClickEvent();
    }

    private void getData() {
        roomId = requireActivity().getIntent().getStringExtra("roomId");
    }

    private void bindingView(View view) {
        imageSend = view.findViewById(R.id.send_image);
        fileSend = view.findViewById(R.id.send_file);
        audioSend = view.findViewById(R.id.send_audio);
    }

    private void setButtonClickEvent() {
        imageSend.setOnClickListener(view -> {
            Toast.makeText(getContext(), "사진을 전송합니다.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            resultLauncher.launch(intent);
        });

        fileSend.setOnClickListener(view -> {
            Toast.makeText(getContext(), "파일을 전송합니다.", Toast.LENGTH_SHORT).show();
        });

        audioSend.setOnClickListener(view -> {
            Toast.makeText(getContext(), "오디오를 전송합니다.", Toast.LENGTH_SHORT).show();
        });
    }

    private void setLauncher() {
        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(result.getResultCode() == RESULT_OK){
                        Intent intent = result.getData();
                        Uri uri = intent != null ? intent.getData() : null;

                        if(uri!=null) {
                            // send image to server
                            sendPicture(uri);
                        } else {
                            Toast.makeText(getContext(), "선택된 이미지가 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }

    private void sendPicture(Uri imgUri) {
        String imagePath = getRealPathFromURI(imgUri);
        File file = new File(imagePath);

        try {
            FileInputStream fileStream = new FileInputStream(imagePath);
            byte[] imageFile = Files.readAllBytes(file.toPath());
            // send image file to server

            fileStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = requireActivity().getContentResolver().query(contentUri, proj, null, null, null);
        cursor.moveToFirst();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        return cursor.getString(column_index);
    }
}