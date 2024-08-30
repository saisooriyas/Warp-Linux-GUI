import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Keys : Table() {
    val id = integer("id").autoIncrement()
    val key_id = varchar("key_id", 50)

    override val primaryKey = PrimaryKey(id)
}

data class Key(val id: Int, val keyID: String)

class AppConfig private constructor() {
    companion object {
        private val userHome = System.getProperty("user.home")
        val appDir = File(userHome, ".myapp").also { it.mkdirs() }
        val logFile = File(appDir, "app_log.txt")
        val dbFile = File(appDir, "app_database")
    }
}

object Logger {
    private val logFile = AppConfig.logFile

//    init {
//        logFile.createNewFile() // Create the file if it doesn't exist
//    }

    fun log(message: String, e: Exception? = null) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val logMessage = "[$timestamp] $message\n"
        logFile.appendText(logMessage)
        e?.printStackTrace(logFile.printWriter())
    }
}

fun initDatabase(): Boolean {
    return try {
        Logger.log("Initializing database...")
        val dbUrl = "jdbc:h2:${AppConfig.dbFile.absolutePath}"
        Logger.log("Database URL going to start connection: $dbUrl ")
        Database.connect(dbUrl, driver = "org.h2.Driver")


        Logger.log("Connecting to database: $dbUrl")
        Database.connect(dbUrl, driver = "org.h2.Driver")
        Logger.log("Connected to database")

        transaction {
            Logger.log("Creating schema")
            SchemaUtils.create(Keys)
        }
        Logger.log("Database initialized successfully")
        true
    } catch (e: Exception) {
        Logger.log("Failed to initialize database: ${e.message}", e)
        throw DatabaseInitializationException("Failed to initialize database", e)
    }
}

class DatabaseInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause)

suspend fun addKey(keyId: String): Boolean = withContext(Dispatchers.IO) {
    try {
        transaction {
            Keys.insert {
                it[key_id] = keyId
            }
        }
        Logger.log("Added key: $keyId")
        true
    } catch (e: Exception) {
        Logger.log("Failed to add key: $keyId. Error: ${e.message}")
        false
    }
}

suspend fun getAllKeys(): List<Key> = withContext(Dispatchers.IO) {
    try {
        val keys = transaction {
            Keys.selectAll().map {
                Key(it[Keys.id], it[Keys.key_id])
            }
        }
        Logger.log("Retrieved ${keys.size} keys")
        keys
    }  catch (e: Exception) {
        Logger.log("Failed to retrieve keys. Error: ${e.message}")
        emptyList()
    }
}

suspend fun deleteKey(id: Int): Boolean = withContext(Dispatchers.IO) {
    try {
        transaction {
            Keys.deleteWhere { Keys.id eq id }
        }
        Logger.log("Deleted key with id: $id")
        true
    } catch (e: Exception) {
        Logger.log("Failed to delete key with id: $id. Error: ${e.message}")
        false
    }
}