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
    Radio(id="1", name="Sunset FM", url="http://152.53.82.216:8080/listen/sunset.fm/radio.mp3", urlResolved="http://152.53.82.216:8080/listen/sunset.fm/radio.mp3", homepage=null, imgUrl="https://radio.sunset-media.org/Sunsetfm.png", tags=listOf("sunset"), country="Russia", state=null, iso=null, language=listOf("russian"), codec=null, bitrate=null, hls=null, voteCount=100, clickCount=100, sslError=null, geoLat=null, geoLong=null, hasExtendedInfo=null),
    Radio(id="2", name="Radio .Sueta", url="https://kino-stream.online/hls/sueta-radio.m3u8", urlResolved="https://kino-stream.online/hls/sueta-radio.m3u8", homepage=null, imgUrl="https://kino-stream.online/sueta.png", tags=listOf("sueta"), country="Russia", state=null, iso=null, language=listOf("russian"), codec=null, bitrate=null, hls=null, voteCount=90, clickCount=90, sslError=null, geoLat=null, geoLong=null, hasExtendedInfo=null),
    Radio(id="3", name="Диско-радио SOVA", url="https://evcast.mediacp.eu:1965/stream", urlResolved="https://evcast.mediacp.eu:1965/stream", homepage=null, imgUrl="https://radio.sunset-media.org/SOVARadio.png", tags=listOf("disco"), country="Russia", state=null, iso=null, language=listOf("russian"), codec=null, bitrate=null, hls=null, voteCount=80, clickCount=80, sslError=null, geoLat=null, geoLong=null, hasExtendedInfo=null),
    Radio(id="4", name="Советская эстрада", url="https://evcast.mediacp.eu:2075/stream", urlResolved="https://evcast.mediacp.eu:2075/stream", homepage=null, imgUrl="https://radio.sunset-media.org/Sovestr.jpg", tags=listOf("soviet"), country="Russia", state=null, iso=null, language=listOf("russian"), codec=null, bitrate=null, hls=null, voteCount=80, clickCount=80, sslError=null, geoLat=null, geoLong=null, hasExtendedInfo=null),
    Radio(id="5", name="Limit FM", url="http://zvukradio.com/live-app/8889", urlResolved="http://zvukradio.com/live-app/8889", homepage=null, imgUrl="https://radio.sunset-media.org/limit.jpg", tags=listOf("hits"), country="Russia", state=null, iso=null, language=listOf("russian"), codec=null, bitrate=null, hls=null, voteCount=70, clickCount=70, sslError=null, geoLat=null, geoLong=null, hasExtendedInfo=null)
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
