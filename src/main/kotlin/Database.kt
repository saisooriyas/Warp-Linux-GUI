import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils


object Keys : Table() {
    val id = integer("id").autoIncrement()
    val key_id = varchar("key_id", 50)

    override val primaryKey = PrimaryKey(id)
}

data class Key(val id: Int, val keyID: String)

// Application configuration class
class AppConfig private constructor() {
    companion object {
        // User home directory (cross-platform)
        private val userHome = System.getProperty("user.home")

        // Application directory within the user's home directory
        val appDir = File(userHome, ".myapp").also { it.mkdirs() }

        // Log file within the application directory
        val logFile = File(appDir, "app_log.txt")

        // Database file within the application directory
        val dbFile = File(appDir, "app_database")
    }
}

// Logger object to log messages and exceptions
object Logger {
    private val logFile = AppConfig.logFile

    fun log(message: String, e: Exception? = null) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val logMessage = "[$timestamp] $message\n"

        // Append the log message to the log file
        logFile.appendText(logMessage)

        // If there's an exception, print its stack trace to the log file
        e?.printStackTrace(logFile.printWriter())
    }
}

// Function to initialize the database
fun initDatabase(): Boolean {
    Logger.log("Initializing database...")

    // Construct the database URL using the file path (cross-platform)
    val dbUrl = "jdbc:h2:${AppConfig.dbFile.absolutePath}"
    Logger.log("Current working directory: " + System.getProperty("user.dir"))
    logFilePermissions()
    Logger.log("Database URL going to start connection: $dbUrl ")

    // Connect to the database
    try {
        Database.connect(dbUrl, driver = "org.h2.Driver")
    }
    catch (e: Exception) {
        Logger.log("Failed to initialize database: ${e.message}", e)
        throw DatabaseInitializationException("Failed to initialize database", e)
    }
    Logger.log("Connected to database")

    // Create the database schema
    transaction {
        Logger.log("Creating schema")
        SchemaUtils.create(Keys)
    }

    Logger.log("Database initialized successfully")
    return true
}

fun logFilePermissions() {
    val appDir = AppConfig.appDir
    val dbFile = AppConfig.dbFile
    Logger.log("App directory exists: ${appDir.exists()}, is writable: ${appDir.canWrite()}")
    Logger.log("DB file exists: ${dbFile.exists()}, is writable: ${dbFile.canWrite()}")
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