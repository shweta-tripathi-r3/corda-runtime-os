package net.corda.sample.hello.impl

import net.corda.sample.hello.impl.avro.AccessList
import net.corda.sample.hello.impl.avro.Colour
import net.corda.sample.hello.impl.avro.User
import org.apache.avro.file.DataFileReader
import org.apache.avro.file.DataFileWriter
import org.apache.avro.file.SeekableByteArrayInput
import org.apache.avro.specific.SpecificDatumReader
import org.apache.avro.specific.SpecificDatumWriter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

internal class HelloWorldImplTest {
    @Test
    fun `prints HelloWorld`() {
        assertThat(HelloWorldImpl().sayHello()).isEqualTo("Hello world!")
    }

    @Test
    fun useAvroPlugin() {
        val out = ByteArrayOutputStream()
        val datumWriter = SpecificDatumWriter(User::class.java)
        DataFileWriter(datumWriter).use {
            val barry = User.newBuilder()
                .setName("Barry")
                .setFavoriteColor(Colour.BLUE)
                .setFavoriteNumber(42) // duh
                .build()

            val bill = User("Bill", null, null)

            it.create(barry.schema, out)
            it.append(barry)
            it.append(bill)
            it.close()
        }
        println(out)

        val input = SeekableByteArrayInput(out.toByteArray())
        val datumReader = SpecificDatumReader(User::class.java)
        DataFileReader(input, datumReader).use {
            val barry = it.next()
            assertThat(barry.name).isEqualTo("Barry")
            assertThat(barry.favoriteColor).isEqualTo(Colour.BLUE)
            assertThat(barry.favoriteNumber).isEqualTo(42)
            val bill = it.next()
            assertThat(bill.name).isEqualTo("Bill")
            assertThat(bill.favoriteColor).isNull()
            assertThat(bill.favoriteNumber as Int?).isNull()
        }
    }

    @Test
    fun `use gradle-avro-plugin with a map`() {
        val out = ByteArrayOutputStream()
        val datumWriter = SpecificDatumWriter(AccessList::class.java)
        DataFileWriter(datumWriter).use {
            val barry = User("Barry", 42, Colour.BLUE)
            val bill = User("Bill", null, Colour.BLACK)
            val list = AccessList("users", mapOf("1" to barry, "2" to bill))

            it.create(AccessList.`SCHEMA$`, out)
            it.append(list)
            it.close()
        }

        val input = SeekableByteArrayInput(out.toByteArray())
        val datumReader = SpecificDatumReader(AccessList::class.java)
        lateinit var bill: User
        DataFileReader(input, datumReader).use {
            val list = it.next()
            println(list)
            assertThat(list.users.size).isEqualTo(2)
            val barry = list.users["1"]!!
            bill = list.users["2"]!!
            assertThat(barry.name).isEqualTo("Barry")
            assertThat(barry.favoriteColor).isEqualTo(Colour.BLUE)
            assertThat(barry.favoriteNumber).isEqualTo(42)
            assertThat(bill.name).isEqualTo("Bill")
            assertThat(bill.favoriteColor).isEqualTo(Colour.BLACK)
            assertThat(bill.favoriteNumber as Int?).isNull()
        }
    }

    @Test
    fun `use gradle-avro-plugin with GenericRecord`() {
//        val out = ByteArrayOutputStream()
//        val datumWriter = GenericDatumWriter(User::class.java)
    }
}