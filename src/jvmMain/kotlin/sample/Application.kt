package sample

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.cio.websocket.*
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.sessions.*
import io.ktor.util.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.*
import java.time.*

fun Application.main() {
    ChatApplication().apply { main() }
}

class ChatApplication {
    private val server = ChatServer()

    fun Application.main() {
        install(DefaultHeaders)
        install(CallLogging)
        install(WebSockets) {
            pingPeriod = Duration.ofMinutes(1)
        }

        install(Sessions) {
            cookie<ChatSession>("SESSION")
        }

        intercept(ApplicationCallPipeline.Features) {
            if (call.sessions.get<ChatSession>() == null) {
                call.sessions.set(ChatSession(generateNonce()))
            }
        }

        routing {
            webSocket("/ws") {
                val session = call.sessions.get<ChatSession>()

                if (session == null) {
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                    return@webSocket
                }

                server.memberJoin(session.id, this)

                try {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            receivedMessage(session.id, frame.readText())
                        }
                    }
                } finally {
                    server.memberLeft(session.id, this)
                }
            }

            static {
                defaultResource("index.html", "web")
                resources("web")
            }
        }
    }

    data class ChatSession(val id: String)

    private suspend fun receivedMessage(id: String, command: String) {
        when {
            command.startsWith("/who") -> server.who(id)

            command.startsWith("/user") -> {
                val newName = command.removePrefix("/user").trim()

                when {
                    newName.isEmpty() -> server.sendTo(
                        id,
                        "server::help",
                        "new name is too long: 50 charachters limit"
                    )
                    else -> server.memberRenamed(id, newName)
                }
            }

            command.startsWith("/help") -> server.help(id)

            command.startsWith("/") -> server.sendTo(
                id,
                "server::help",
                "Unknown command ${command.takeWhile { !it.isWhitespace() }}"
            )
            else -> server.message(id, command)
        }
    }
}
