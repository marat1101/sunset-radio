package org.onedroid.radiowave.data.repository

import androidx.sqlite.SQLiteException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.onedroid.radiowave.app.utils.DataError
import org.onedroid.radiowave.app.utils.EmptyResult
import org.onedroid.radiowave.app.utils.Result
import org.onedroid.radiowave.data.database.RadioWaveDao
import org.onedroid.radiowave.data.mappers.toRadio
import org.onedroid.radiowave.data.mappers.toRadioEntity
import org.onedroid.radiowave.domain.Radio
import org.onedroid.radiowave.domain.RadioRepository
import sunsetradio.composeapp.generated.resources.Res

class RadioRepositoryImpl(
    private val radioWaveDao: RadioWaveDao
) : RadioRepository {

    private var cachedStations: List<Radio>? = null

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadStations(): List<Radio> {
        cachedStations?.let { return it }
        val bytes = Res.readBytes("files/stations.json")
        val jsonString = bytes.decodeToString()
        val jsonArray = Json.parseToJsonElement(jsonString).jsonArray
        val stations = jsonArray.map { element ->
            val obj = element.jsonObject
            Radio(
                id = obj["stationuuid"]?.jsonPrimitive?.content ?: "",
                name = obj["name"]?.jsonPrimitive?.content ?: "",
                streamUrl = obj["url"]?.jsonPrimitive?.content ?: "",
                imageUrl = obj["favicon"]?.jsonPrimitive?.content ?: "",
                country = obj["country"]?.jsonPrimitive?.content ?: "",
                language = obj["language"]?.jsonPrimitive?.content ?: "",
                tags = obj["tags"]?.jsonPrimitive?.content ?: "",
                votes = obj["votes"]?.jsonPrimitive?.intOrNull ?: 0,
                clickCount = obj["clickcount"]?.jsonPrimitive?.intOrNull ?: 0,
            )
        }
        cachedStations = stations
        return stations
    }

    override suspend fun searchRadios(query: String): Result<List<Radio>, DataError.Remote> {
        val all = loadStations()
        val filtered = if (query.isBlank()) all
            else all.filter { it.name.contains(query, ignoreCase = true) }
        return Result.Success(filtered)
    }

    override suspend fun getRadios(
        offset: Int,
        limit: Int
    ): Result<List<Radio>, DataError.Remote> {
        val all = loadStations()
        val paged = all.drop(offset).take(limit)
        return Result.Success(paged)
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
        return radioWaveDao.getSavedRadios().map { radioEntities ->
            radioEntities.sortedByDescending { it.timeStamp }.map { it.toRadio() }
        }
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
        return radioWaveDao.getAllRadios().map { radioEntities ->
            radioEntities.map { it.toRadio() }
        }
    }
}
