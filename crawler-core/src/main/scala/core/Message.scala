package core

trait Message {
    def messageType: String
}

trait SerializableMessage extends Message {
    def toMap: Map[String, Serializable]
}
