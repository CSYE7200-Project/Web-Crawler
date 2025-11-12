package core

trait Protocol[M <: Message] {
    def encode(msg: M): Array[Byte]
    def decode(bytes: Array[Byte]): M
}