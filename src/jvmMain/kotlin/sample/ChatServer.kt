package sample

import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class ChatServer {
    val usersCounter = AtomicInteger()

    val memberNames = ConcurrentHashMap<String, String>()

    val members = ConcurrentHashMap<String, MutableList<WebSocketSession>>()

    val lastMessages = LinkedList<String>()

    suspend fun memberJoin(member: String, socket: WebSocketSession) {
        val name = memberNames.computeIfAbsent(member) { "user${usersCounter.incrementAndGet()}" }

        val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocketSession>()}
        list.add(socket)

        if (list.size == 1) {
            broadcast("server", "Member joined: $name.")
        }

        val messages = synchronized(lastMessages) { lastMessages.toList() }
        for (message in messages) {
            socket.send(Frame.Text(message))
        }
    }

    suspend fun memberRenamed(member: String, to: String) {
        val oldName = memberNames.put(member, to) ?: member
        broadcast("server", "Member renamed from $oldName to $to")
    }

    suspend fun memberLeft(member: String, socket: WebSocketSession) {
        val connections = members[member]
        connections?.remove(socket)

        if (connections != null && connections.isEmpty()) {
            val name = memberNames.remove(member) ?: member
            broadcast("server", "Member left: $name.")
        }
    }

    suspend fun who(sender: String) {
        members[sender]?.send(Frame.Text(memberNames.values.joinToString(prefix = "[server::who] ")))
    }

    suspend fun help(sender: String) {
        members[sender]?.send(Frame.Text("[server::help] Possible commands are: /user, /help and /who"))
    }

    suspend fun sendTo(recipient: String, sender: String, message: String) {
        members[recipient]?.send(Frame.Text("[$sender] $message"))
    }

    suspend fun message(sender: String, message: String) {
        val name = memberNames[sender] ?: sender
        val formatted = "[$name] $message"

        broadcast(formatted)

        synchronized(lastMessages) {
            lastMessages.add(formatted)
            if (lastMessages.size > 100) {
                lastMessages.removeFirst()
            }
        }
    }

    private suspend fun broadcast(message: String) {
        members.values.forEach { socket ->
            socket.send(Frame.Text(message))
        }
    }

    private suspend fun broadcast(sender: String, message: String) {
        val name = memberNames[sender] ?: sender
        broadcast("[$name] $message")
    }

    suspend fun List<WebSocketSession>.send(frame: Frame) {
        forEach {
            try {
                it.send(frame.copy())
            } catch (t: Throwable) {
                try {
                    it.close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, ""))
                } catch (ignore: ClosedSendChannelException) {
                    //lalalalalalalala
                }
            }
        }
    }
}
