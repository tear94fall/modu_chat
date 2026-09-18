package com.example.modumessenger.core.di

import com.example.modumessenger.core.lock.DataStoreLockStorage
import com.example.modumessenger.core.lock.LockStorage
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LockModule {

    @Binds
    @Singleton
    abstract fun bindLockStorage(impl: DataStoreLockStorage): LockStorage

    companion object {
        @Provides
        @Singleton
        fun provideClock(): Clock = Clock.systemUTC()
    }
}
