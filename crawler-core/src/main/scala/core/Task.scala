package core

trait Task[P] extends SerializableMessage {
    def id: String
    def payload: P
}