trait PageParser[D] {
    def parse(html: String): D
}
