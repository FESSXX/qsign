import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import moe.fuqiuluo.api.configEnergy
import moe.fuqiuluo.api.configIndex
import moe.fuqiuluo.api.configSign
import moe.fuqiuluo.comm.QSignConfig
import moe.fuqiuluo.comm.checkIllegal
import moe.fuqiuluo.comm.invoke
import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger(Main::class.java)
lateinit var CONFIG: QSignConfig

fun main(args: Array<String>) {
    args().also {
        val baseDir = File(it["basePath", "Lack of basePath."])
        if (!baseDir.exists() ||
            !baseDir.isDirectory ||
            !baseDir.resolve("libfekit.so").exists() ||
            !baseDir.resolve("libQSec.so").exists() ||
            !baseDir.resolve("config.json").exists()
        ) {
            error("The base path is invalid, perhaps it is not a directory or something is missing inside.")
        } else {
            Json {
                ignoreUnknownKeys = true
            }
            CONFIG = Json.decodeFromString<QSignConfig>(baseDir.resolve("config.json").readText())
                .apply { checkIllegal() }
        }
    }

    CONFIG.server.also {
        embeddedServer(Netty, host = it.host, port = it.port, module = Application::init)
        .start(wait = true)
    }
}

fun Application.init() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    routing {
        configIndex()
        configEnergy()
        configSign()
    }
}
