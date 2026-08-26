package tg.goddivor.jobcalender.di

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import tg.goddivor.jobcalender.data.local.ApplicationDao
import tg.goddivor.jobcalender.data.local.EventDao
import tg.goddivor.jobcalender.data.local.PendingWriteDao
import tg.goddivor.jobcalender.data.local.JobCalenderDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): JobCalenderDatabase =
        Room.databaseBuilder<JobCalenderDatabase>(context, JobCalenderDatabase.NAME)
            .setDriver(AndroidSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

    @Provides
    fun provideApplicationDao(database: JobCalenderDatabase): ApplicationDao =
        database.applicationDao()

    @Provides
    fun provideEventDao(database: JobCalenderDatabase): EventDao = database.eventDao()

    @Provides
    fun providePendingWriteDao(database: JobCalenderDatabase): PendingWriteDao =
        database.pendingWriteDao()
}
