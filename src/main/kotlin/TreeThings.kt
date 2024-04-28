class TreeThings {
}

interface node{
    val truth : Boolean?
    val stringRepresentation : String
    val children : List<node>

    fun getStringRepresentation()

    fun onVisit()
}