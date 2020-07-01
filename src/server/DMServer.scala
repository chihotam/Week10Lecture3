package server

import com.corundumstudio.socketio.listener.{DataListener, DisconnectListener}
import com.corundumstudio.socketio.{AckRequest, Configuration, SocketIOClient, SocketIOServer}
import play.api.libs.json.{JsValue, Json}

class DMServer {
  val config: Configuration = new Configuration {
    setHostname("localhost")
    setPort(8080)
  }

  val server: SocketIOServer = new SocketIOServer(config)
  var usernameToSocket: Map[String, SocketIOClient] = Map()
  var socketToUsername: Map[SocketIOClient, String] = Map()

  server.addEventListener("register", classOf[String], new RegisterListener())
  server.addEventListener("direct_message", classOf[String], new DirectMessageListener())
  server.start()

  class RegisterListener() extends DataListener[String] {
    override def onData(socket: SocketIOClient, username: String, ackRequest: AckRequest): Unit = {
      socketToUsername += (socket -> username)
      usernameToSocket += (username -> socket)
    }
  }

  class DirectMessageListener() extends DataListener[String] {
    override def onData(socket: SocketIOClient, message: String, ackRequest: AckRequest): Unit = {
      val json: JsValue = Json.parse(message)
      val user: String = (json \ "to").as[String]
      val msg: String = (json \ "message").as[String]
      val mapMsg: Map[String, JsValue] = Map("from" -> Json.toJson(socketToUsername(socket)), "message" -> Json.toJson(msg))
      val jsValueMsg: JsValue = Json.toJson(mapMsg)
      usernameToSocket(user).sendEvent("dm", Json.stringify(jsValueMsg))
    }
  }
}