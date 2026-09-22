package com.example.modumessenger.core.di

import com.example.modumessenger.data.repository.AttachmentRepository
import com.example.modumessenger.data.repository.AttachmentRepositoryImpl
import com.example.modumessenger.feature.chat.audio.AudioPlayer
import com.example.modumessenger.feature.chat.audio.MediaAudioPlayer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** 채팅 첨부(파일 내려받기·음성 재생). */
@Module
@InstallIn(SingletonComponent::class)
abstract class AttachmentModule {

    @Binds
    @Singleton
    abstract fun bindAttachmentRepository(impl: AttachmentRepositoryImpl): AttachmentRepository

    @Binds
    @Singleton
    abstract fun bindAudioPlayer(impl: MediaAudioPlayer): AudioPlayer
}
