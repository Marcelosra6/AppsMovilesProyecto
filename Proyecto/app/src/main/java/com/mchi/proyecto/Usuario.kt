package com.mchi.proyecto

data class Usuario(
    var tipoDoc: String = "",
    var numDoc: String = "",
    var fechaNac: String = "",
    var aceptaTerminos: Boolean = false,
    var aceptaPromociones: Boolean = false,

    var nombres: String = "",
    var apellidos: String = "",
    var celular: String = "",
    var genero: String = "",
    var peso: Int = 0,
    var talla: Int = 0,

    var correo: String = "",
    var contrasena: String = ""
)