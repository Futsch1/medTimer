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


object TakenDataSerializer : Serializer<TakenData> {
    val gson = Gson()
    override val defaultValue: TakenData
        get() = TakenData("", 0, 0, false)

    override suspend fun readFrom(input: InputStream): TakenData {
        try {
            val ois = ObjectInputStream(input)
            val stringData = ois.readObject() as String
            return gson.fromJson(stringData, TakenData::class.java)
        } catch (exception: IOException) {
            throw CorruptionException("Cannot read proto.", exception)
        } catch (exception: ClassNotFoundException) {
            throw CorruptionException("Cannot read class.", exception)
        } catch (exception: ClassCastException) {
            return TakenData("", 0, 0, false)
        } catch (exception: JsonSyntaxException) {
            return TakenData("", 0, 0, false)
        }
    }

    override suspend fun writeTo(t: TakenData, output: OutputStream) {
        val oos = ObjectOutputStream(output)
        val json = gson.toJson(t)
        oos.writeObject(json)
    }
}

