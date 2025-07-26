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


object WearDataSerializer : Serializer<WearData> {
    val gson = Gson()
    override val defaultValue: WearData
        get() = WearData("")

    override suspend fun readFrom(input: InputStream): WearData {
        try {
            val ois = ObjectInputStream(input)
            val stringData = ois.readObject() as String
            return gson.fromJson(stringData, WearData::class.java)
            //return ois.readObject() as WearData
        } catch (exception: IOException) {
            throw CorruptionException("Cannot read proto.", exception)
        } catch (exception: ClassNotFoundException) {
            throw CorruptionException("Cannot read class.", exception)
        } catch (exception: ClassCastException) {
            return WearData("")
        } catch (exception: JsonSyntaxException) {
            return WearData("")
        }
    }

    override suspend fun writeTo(t: WearData, output: OutputStream) {
        val oos = ObjectOutputStream(output)
        val json = gson.toJson(t)
        oos.writeObject(json)
//        oos.writeObject(t)
    }
}

