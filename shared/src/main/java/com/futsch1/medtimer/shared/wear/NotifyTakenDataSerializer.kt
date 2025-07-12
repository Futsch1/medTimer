package com.futsch1.medtimer.shared.wear
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okio.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream


object NotifyTakenDataSerializer : Serializer<NotifyTakenData> {
    val gson = Gson()
    override val defaultValue: NotifyTakenData
        get() = NotifyTakenData("", 0, 0)

    override suspend fun readFrom(input: InputStream): NotifyTakenData {
        try {
            val ois = ObjectInputStream(input)
            val stringData = ois.readObject() as String
            return gson.fromJson(stringData, NotifyTakenData::class.java)
        } catch (exception: IOException) {
            throw CorruptionException("Cannot read proto.", exception)
        } catch (exception: ClassNotFoundException) {
            throw CorruptionException("Cannot read class.", exception)
        } catch (exception: ClassCastException) {
            return NotifyTakenData("", 0, 0)
        } catch (exception: JsonSyntaxException) {
            return NotifyTakenData("", 0, 0)
        }
    }

    override suspend fun writeTo(t: NotifyTakenData, output: OutputStream) {
        val oos = ObjectOutputStream(output)
        val json = gson.toJson(t)
        oos.writeObject(json)
    }
}

