package org.onedroid.radiowave.data.repository

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.onedroid.radiowave.app.utils.DataError
import org.onedroid.radiowave.app.utils.EmptyResult
import org.onedroid.radiowave.app.utils.Result
import org.onedroid.radiowave.data.database.RadioWaveDao
import org.onedroid.radiowave.data.mappers.toRadio
import org.onedroid.radiowave.data.mappers.toRadioEntity
import org.onedroid.radiowave.domain.Radio
import org.onedroid.radiowave.domain.RadioRepository

class RadioRepositoryImpl(
    private val radioWaveDao: RadioWaveDao
) : RadioRepository {

    private val localStations = listOf(
        Radio(id="1", name="Sunset FM", streamUrl="http://152.53.82.216:8080/listen/sunset.fm/radio.mp3", imageUrl="https://radio.sunset-media.org/Sunsetfm.png", country="Russia", language="russian", tags="sunset", votes=100, clickCount=100),
        Radio(id="2", name="Radio .Sueta", streamUrl="https://kino-stream.online/hls/sueta-radio.m3u8", imageUrl="https://kino-stream.online/sueta.png", country="Russia", language="russian", tags="sueta", votes=90, clickCount=90),
        Radio(id="3", name="Диско-радио SOVA", streamUrl="https://evcast.mediacp.eu:1965/stream", imageUrl="https://radio.sunset-media.org/SOVARadio.png", country="Russia", language="russian", tags="disco", votes=80, clickCount=80),
        Radio(id="4", name="Советская эстрада", streamUrl="https://evcast.mediacp.eu:2075/stream", imageUrl="https://radio.sunset-media.org/Sovestr.jpg", country="Russia", language="russian", tags="soviet", votes=80, clickCount=80),
        Radio(id="5", name="Limit FM", streamUrl="http://zvukradio.com/live-app/8889", imageUrl="https://radio.sunset-media.org/limit.jpg", country="Russia", language="russian", tags="hits", votes=70, clickCount=70)
    )

    override suspend fun searchRadios(query: String): Result<List<Radio>, DataError.Remote> {
        val filtered = if (query.isBlank()) localStations
            else localStations.filter { it.name.contains(query, ignoreCase = true) }
        return Result.Success(filtered)
    }

    override suspend fun getRadios(offset: Int, limit: Int): Result<List<Radio>, DataError.Remote> {
        return Result.Success(localStations.drop(offset).take(limit))
    }

    override suspend fun saveRadio(radio: Radio): EmptyResult<DataError.Local> {
        return try {
            radioWaveDao.upsert(radio.toRadioEntity(isSaved = true))
            Result.Success(Unit)
        } catch (e: SQLiteException) {
            Result.Error(DataError.Local.DISK_FULL)
        }
    }

    override suspend fun deleteFromSaved(id: String) {
        radioWaveDao.deleteSavedRadio(id)
    }

    override fun getSavedRadios(): Flow<List<Radio>> {
        return radioWaveDao.getSavedRadios().map { it.sortedByDescending { e -> e.timeStamp }.map { e -> e.toRadio() } }
    }

    override suspend fun isSaved(id: String): Flow<Boolean> {
        return radioWaveDao.isSaved(id)
    }

    override suspend fun insertRecentlyUpdatedRadios(radios: List<Radio>): EmptyResult<DataError.Local> {
        return try {
            radioWaveDao.upsertRecentlyUpdatedRadios(radios.map { it.toRadioEntity() })
            Result.Success(Unit)
        } catch (e: SQLiteException) {
            Result.Error(DataError.Local.DISK_FULL)
        }
    }

    override fun getRecentlyUpdatedRadios(): Flow<List<Radio>> {
        return radioWaveDao.getAllRadios().map { it.map { e -> e.toRadio() } }
    }
}
