package com.example.chatstoreservice.message

class ChatPayload {
    var op: String? = null
    var before: ChatModel? = null
    var after: ChatModel? = null

    override fun toString(): String = "ChatPayload(op=$op, before=$before, after=$after)"
}
