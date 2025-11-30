package core.protocol

import core.Message

trait Protocol[M <: Message] {
    def encode(msg: M): Array[Byte]
    def decode(bytes: Array[Byte]): M
}