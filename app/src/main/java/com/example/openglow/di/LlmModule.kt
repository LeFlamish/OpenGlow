package com.example.openglow.di

import com.example.openglow.data.localai.ConfigurableLocalLlmClient
import com.example.openglow.data.localai.LocalLlmClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {
    @Binds
    @Singleton
    abstract fun bindLocalLlmClient(
        client: ConfigurableLocalLlmClient,
    ): LocalLlmClient
}
