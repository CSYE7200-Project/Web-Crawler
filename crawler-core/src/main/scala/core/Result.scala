package core

trait Result[P, D] extends SerializableMessage {
    def taskId: String
    def payload: P
    def success: Boolean
    def data: Option[D]
}