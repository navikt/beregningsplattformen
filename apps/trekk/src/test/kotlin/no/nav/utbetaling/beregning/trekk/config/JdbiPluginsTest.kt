package no.nav.utbetaling.beregning.trekk.config

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import javax.sql.DataSource

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jdbi.v3.core.statement.SqlStatements

import no.nav.utbetaling.beregning.trekk.domain.TrekkvedtakId

class JdbiPluginsTest :
    FunSpec({
        lateinit var dataSource: DataSource
        lateinit var statement: PreparedStatement

        beforeEach {
            statement = mockk(relaxed = true)
            every { statement.updateCount } returns 1
            val connection = mockk<Connection>(relaxed = true)
            every { connection.autoCommit } returns false
            every { connection.prepareStatement(any<String>(), any<Int>(), any<Int>()) } returns statement
            dataSource = mockk()
            every { dataSource.connection } returns connection
        }

        test("Kotlin data classes map nullable values and domain value classes") {
            val resultSet = mockk<ResultSet>(relaxed = true)
            val metadata = mockk<ResultSetMetaData>()
            every { metadata.columnCount } returns 2
            every { metadata.getColumnLabel(1) } returns "id"
            every { metadata.getColumnLabel(2) } returns "description"
            every { resultSet.metaData } returns metadata
            every { resultSet.next() } returnsMany listOf(true, false)
            every { resultSet.getLong(1) } returns 42L
            every { resultSet.getString(2) } returns null
            every { statement.resultSet } returns resultSet

            val result =
                createJdbi(dataSource).inTransaction<PluginMappedRow, Exception> { tx ->
                    tx
                        .createQuery("SELECT id, description FROM example")
                        .mapTo(PluginMappedRow::class.java)
                        .one()
                }

            result shouldBe PluginMappedRow(TrekkvedtakId(42L), null)
        }

        test("Kotlin domain value classes can be bound as SQL parameters") {
            createJdbi(dataSource).useTransaction<Exception> { tx ->
                tx
                    .createUpdate("UPDATE example SET active = true WHERE id = :id")
                    .bind("id", TrekkvedtakId(42L))
                    .execute() shouldBe 1
            }

            verify(exactly = 1) { statement.setLong(1, 42L) }
        }

        test("bound values remain excluded from Jdbi JFR telemetry") {
            createJdbi(dataSource).getConfig(SqlStatements::class.java).getIncludeBindingsInTelemetry() shouldBe false
        }
    })

data class PluginMappedRow(
    val id: TrekkvedtakId,
    val description: String?,
)
