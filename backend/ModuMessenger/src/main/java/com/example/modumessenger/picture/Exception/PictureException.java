package com.example.modumessenger.picture.Exception;

public class PictureException extends RuntimeException{
    public PictureException(String message){
        super(message);
    }
    public PictureException(String message, Throwable cause){
        super(message, cause);
    }
}