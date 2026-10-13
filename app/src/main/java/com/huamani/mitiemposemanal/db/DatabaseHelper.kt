package com.huamani.mitiemposemanal.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.huamani.mitiemposemanal.model.Categoria
import com.huamani.mitiemposemanal.model.Evento
import com.huamani.mitiemposemanal.model.ResumenCategoria
import com.huamani.mitiemposemanal.model.Tarea

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "mitiemposemanal.db"
        private const val DATABASE_VERSION = 3

        // Tabla Categorías
        const val TABLE_CATEGORIAS = "categorias"
        const val COLUMN_CAT_ID = "id"
        const val COLUMN_CAT_NOMBRE = "nombre"

        // Tabla Tareas
        const val TABLE_TAREAS = "tareas"
        const val COLUMN_TAREA_ID = "id"
        const val COLUMN_TAREA_NOMBRE = "nombre"
        const val COLUMN_TAREA_CAT_ID = "categoria_id"

        // Tabla Eventos (Sprint 2 & 3)
        const val TABLE_EVENTOS = "eventos"
        const val COLUMN_EV_ID = "id"
        const val COLUMN_EV_TAREA_ID = "tarea_id"
        const val COLUMN_EV_INICIO = "inicio"
        const val COLUMN_EV_FIN = "fin"
        const val COLUMN_EV_DURACION = "duracion"
        const val COLUMN_EV_DURACION_MINUTOS = "duracion_minutos"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableCategorias = """
            CREATE TABLE $TABLE_CATEGORIAS (
                $COLUMN_CAT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_CAT_NOMBRE TEXT UNIQUE NOT NULL
            );
        """.trimIndent()

        val createTableTareas = """
            CREATE TABLE $TABLE_TAREAS (
                $COLUMN_TAREA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TAREA_NOMBRE TEXT UNIQUE NOT NULL,
                $COLUMN_TAREA_CAT_ID INTEGER NOT NULL,
                FOREIGN KEY($COLUMN_TAREA_CAT_ID) REFERENCES $TABLE_CATEGORIAS($COLUMN_CAT_ID)
            );
        """.trimIndent()

        val createTableEventos = """
            CREATE TABLE $TABLE_EVENTOS (
                $COLUMN_EV_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EV_TAREA_ID INTEGER NOT NULL,
                $COLUMN_EV_INICIO TEXT NOT NULL,
                $COLUMN_EV_FIN TEXT,
                $COLUMN_EV_DURACION TEXT,
                $COLUMN_EV_DURACION_MINUTOS REAL DEFAULT 0.0,
                FOREIGN KEY($COLUMN_EV_TAREA_ID) REFERENCES $TABLE_TAREAS($COLUMN_TAREA_ID) ON DELETE CASCADE
            );
        """.trimIndent()

        db.execSQL(createTableCategorias)
        db.execSQL(createTableTareas)
        db.execSQL(createTableEventos)

        // Precarga obligatoria de las 4 categorías por defecto
        val categoriasIniciales = arrayOf("Estudio", "Trabajo", "Descanso", "Ocio")
        for (cat in categoriasIniciales) {
            val cv = ContentValues().apply {
                put(COLUMN_CAT_NOMBRE, cat)
            }
            db.insert(TABLE_CATEGORIAS, null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createTableEventos = """
                CREATE TABLE IF NOT EXISTS $TABLE_EVENTOS (
                    $COLUMN_EV_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_EV_TAREA_ID INTEGER NOT NULL,
                    $COLUMN_EV_INICIO TEXT NOT NULL,
                    $COLUMN_EV_FIN TEXT,
                    $COLUMN_EV_DURACION TEXT,
                    $COLUMN_EV_DURACION_MINUTOS REAL DEFAULT 0.0,
                    FOREIGN KEY($COLUMN_EV_TAREA_ID) REFERENCES $TABLE_TAREAS($COLUMN_TAREA_ID) ON DELETE CASCADE
                );
            """.trimIndent()
            db.execSQL(createTableEventos)
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE $TABLE_EVENTOS ADD COLUMN $COLUMN_EV_DURACION_MINUTOS REAL DEFAULT 0.0;")
            } catch (_: Exception) {}
        }
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    // --- OPERACIONES DE CATEGORÍAS ---

    fun getAllCategorias(): List<Categoria> {
        val list = mutableListOf<Categoria>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT $COLUMN_CAT_ID, $COLUMN_CAT_NOMBRE FROM $TABLE_CATEGORIAS ORDER BY $COLUMN_CAT_ID ASC",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_CAT_ID))
                    val nombre = it.getString(it.getColumnIndexOrThrow(COLUMN_CAT_NOMBRE))
                    list.add(Categoria(id, nombre))
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun insertCategoria(nombre: String): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_CAT_NOMBRE, nombre.trim())
        }
        return db.insert(TABLE_CATEGORIAS, null, cv)
    }

    fun updateCategoria(id: Int, nuevoNombre: String): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_CAT_NOMBRE, nuevoNombre.trim())
        }
        return db.update(
            TABLE_CATEGORIAS,
            cv,
            "$COLUMN_CAT_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun deleteCategoria(id: Int): Boolean {
        val db = writableDatabase
        val rows = db.delete(
            TABLE_CATEGORIAS,
            "$COLUMN_CAT_ID = ?",
            arrayOf(id.toString())
        )
        return rows > 0
    }

    fun existsCategoria(nombre: String, excludeId: Int = -1): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_CATEGORIAS WHERE LOWER($COLUMN_CAT_NOMBRE) = LOWER(?) AND $COLUMN_CAT_ID != ?",
            arrayOf(nombre.trim(), excludeId.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0) > 0
            }
        }
        return false
    }

    fun hasTareasAsociadas(categoriaId: Int): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TAREAS WHERE $COLUMN_TAREA_CAT_ID = ?",
            arrayOf(categoriaId.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0) > 0
            }
        }
        return false
    }

    // --- OPERACIONES DE TAREAS ---

    fun getAllTareas(): List<Tarea> {
        val list = mutableListOf<Tarea>()
        val db = readableDatabase
        val query = """
            SELECT t.$COLUMN_TAREA_ID, t.$COLUMN_TAREA_NOMBRE, t.$COLUMN_TAREA_CAT_ID, c.$COLUMN_CAT_NOMBRE AS cat_nombre
            FROM $TABLE_TAREAS t
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CAT_ID = c.$COLUMN_CAT_ID
            ORDER BY t.$COLUMN_TAREA_ID DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_TAREA_ID))
                    val nombre = it.getString(it.getColumnIndexOrThrow(COLUMN_TAREA_NOMBRE))
                    val catId = it.getInt(it.getColumnIndexOrThrow(COLUMN_TAREA_CAT_ID))
                    val catNombre = it.getString(it.getColumnIndexOrThrow("cat_nombre"))
                    list.add(Tarea(id, nombre, catId, catNombre))
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun searchTareas(query: String): List<Tarea> {
        val list = mutableListOf<Tarea>()
        val db = readableDatabase
        val sql = """
            SELECT t.$COLUMN_TAREA_ID, t.$COLUMN_TAREA_NOMBRE, t.$COLUMN_TAREA_CAT_ID, c.$COLUMN_CAT_NOMBRE AS cat_nombre
            FROM $TABLE_TAREAS t
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CAT_ID = c.$COLUMN_CAT_ID
            WHERE LOWER(t.$COLUMN_TAREA_NOMBRE) LIKE LOWER(?)
            ORDER BY t.$COLUMN_TAREA_NOMBRE ASC
        """.trimIndent()

        val cursor = db.rawQuery(sql, arrayOf("%${query.trim()}%"))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_TAREA_ID))
                    val nombre = it.getString(it.getColumnIndexOrThrow(COLUMN_TAREA_NOMBRE))
                    val catId = it.getInt(it.getColumnIndexOrThrow(COLUMN_TAREA_CAT_ID))
                    val catNombre = it.getString(it.getColumnIndexOrThrow("cat_nombre"))
                    list.add(Tarea(id, nombre, catId, catNombre))
                } while (it.moveToNext())
            }
        }
        return list
    }

    fun getTareaByNombre(nombre: String): Tarea? {
        val db = readableDatabase
        val query = """
            SELECT t.$COLUMN_TAREA_ID, t.$COLUMN_TAREA_NOMBRE, t.$COLUMN_TAREA_CAT_ID, c.$COLUMN_CAT_NOMBRE AS cat_nombre
            FROM $TABLE_TAREAS t
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CAT_ID = c.$COLUMN_CAT_ID
            WHERE LOWER(t.$COLUMN_TAREA_NOMBRE) = LOWER(?)
        """.trimIndent()
        val cursor = db.rawQuery(query, arrayOf(nombre.trim()))
        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_TAREA_ID))
                val nom = it.getString(it.getColumnIndexOrThrow(COLUMN_TAREA_NOMBRE))
                val catId = it.getInt(it.getColumnIndexOrThrow(COLUMN_TAREA_CAT_ID))
                val catNombre = it.getString(it.getColumnIndexOrThrow("cat_nombre"))
                return Tarea(id, nom, catId, catNombre)
            }
        }
        return null
    }

    fun insertTarea(nombre: String, categoriaId: Int): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_TAREA_NOMBRE, nombre.trim())
            put(COLUMN_TAREA_CAT_ID, categoriaId)
        }
        return db.insert(TABLE_TAREAS, null, cv)
    }

    fun updateTarea(id: Int, nuevoNombre: String, categoriaId: Int): Int {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_TAREA_NOMBRE, nuevoNombre.trim())
            put(COLUMN_TAREA_CAT_ID, categoriaId)
        }
        return db.update(
            TABLE_TAREAS,
            cv,
            "$COLUMN_TAREA_ID = ?",
            arrayOf(id.toString())
        )
    }

    fun deleteTarea(id: Int): Boolean {
        val db = writableDatabase
        val rows = db.delete(
            TABLE_TAREAS,
            "$COLUMN_TAREA_ID = ?",
            arrayOf(id.toString())
        )
        return rows > 0
    }

    fun existsTarea(nombre: String, excludeId: Int = -1): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_TAREAS WHERE LOWER($COLUMN_TAREA_NOMBRE) = LOWER(?) AND $COLUMN_TAREA_ID != ?",
            arrayOf(nombre.trim(), excludeId.toString())
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getInt(0) > 0
            }
        }
        return false
    }

    // --- OPERACIONES DE EVENTOS (Sprint 2 & 3) ---

    fun getEventoEnCurso(): Evento? {
        val db = readableDatabase
        val query = """
            SELECT e.$COLUMN_EV_ID, e.$COLUMN_EV_TAREA_ID, e.$COLUMN_EV_INICIO, e.$COLUMN_EV_FIN, e.$COLUMN_EV_DURACION,
                   t.$COLUMN_TAREA_NOMBRE AS tarea_nombre, c.$COLUMN_CAT_NOMBRE AS cat_nombre
            FROM $TABLE_EVENTOS e
            INNER JOIN $TABLE_TAREAS t ON e.$COLUMN_EV_TAREA_ID = t.$COLUMN_TAREA_ID
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CAT_ID = c.$COLUMN_CAT_ID
            WHERE e.$COLUMN_EV_FIN IS NULL
            LIMIT 1
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_EV_ID))
                val tareaId = it.getInt(it.getColumnIndexOrThrow(COLUMN_EV_TAREA_ID))
                val inicio = it.getString(it.getColumnIndexOrThrow(COLUMN_EV_INICIO))
                val fin = it.getString(it.getColumnIndexOrThrow(COLUMN_EV_FIN))
                val duracion = it.getString(it.getColumnIndexOrThrow(COLUMN_EV_DURACION))
                val tareaNombre = it.getString(it.getColumnIndexOrThrow("tarea_nombre"))
                val catNombre = it.getString(it.getColumnIndexOrThrow("cat_nombre"))
                return Evento(id, tareaId, tareaNombre, catNombre, inicio, fin, duracion)
            }
        }
        return null
    }

    fun insertEvento(tareaId: Int, inicio: String): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_EV_TAREA_ID, tareaId)
            put(COLUMN_EV_INICIO, inicio)
            putNull(COLUMN_EV_FIN)
            putNull(COLUMN_EV_DURACION)
            put(COLUMN_EV_DURACION_MINUTOS, 0.0)
        }
        return db.insert(TABLE_EVENTOS, null, cv)
    }

    fun finalizarEvento(eventoId: Int, fin: String, duracion: String, duracionMinutos: Double): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put(COLUMN_EV_FIN, fin)
            put(COLUMN_EV_DURACION, duracion)
            put(COLUMN_EV_DURACION_MINUTOS, duracionMinutos)
        }
        val rows = db.update(
            TABLE_EVENTOS,
            cv,
            "$COLUMN_EV_ID = ?",
            arrayOf(eventoId.toString())
        )
        return rows > 0
    }

    fun getAllEventos(): List<Evento> {
        val list = mutableListOf<Evento>()
        val db = readableDatabase
        val query = """
            SELECT e.$COLUMN_EV_ID, e.$COLUMN_EV_TAREA_ID, e.$COLUMN_EV_INICIO, e.$COLUMN_EV_FIN, e.$COLUMN_EV_DURACION,
                   t.$COLUMN_TAREA_NOMBRE AS tarea_nombre, c.$COLUMN_CAT_NOMBRE AS cat_nombre
            FROM $TABLE_EVENTOS e
            INNER JOIN $TABLE_TAREAS t ON e.$COLUMN_EV_TAREA_ID = t.$COLUMN_TAREA_ID
            INNER JOIN $TABLE_CATEGORIAS c ON t.$COLUMN_TAREA_CAT_ID = c.$COLUMN_CAT_ID
            ORDER BY e.$COLUMN_EV_ID DESC
        """.trimIndent()

        val cursor = db.rawQuery(query, null)
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow(COLUMN_EV_ID))
                    val tareaId = it.getInt(it.getColumnIndexOrThrow(COLUMN_EV_TAREA_ID))
                    val inicio = it.getString(it.getColumnIndexOrThrow(COLUMN_EV_INICIO))
                    val fin = it.getString(it.getColumnIndexOrThrow(COLUMN_EV_FIN))
                    val duracion = it.getString(it.getColumnIndexOrThrow(COLUMN_EV_DURACION))
                    val tareaNombre = it.getString(it.getColumnIndexOrThrow("tarea_nombre"))
                    val catNombre = it.getString(it.getColumnIndexOrThrow("cat_nombre"))
                    list.add(Evento(id, tareaId, tareaNombre, catNombre, inicio, fin, duracion))
                } while (it.moveToNext())
            }
        }
        return list
    }

    // --- CÁLCULO RESUMEN SEMANAL (Sprint 3: SUM y GROUP BY con precisión Double/Minutos fraccionales) ---

    fun getResumenSemanal(inicioSemana: String, finSemana: String): List<ResumenCategoria> {
        val list = mutableListOf<ResumenCategoria>()
        val db = readableDatabase
        val query = """
            SELECT c.$COLUMN_CAT_ID AS cat_id, c.$COLUMN_CAT_NOMBRE AS cat_nombre,
                   COALESCE(SUM(e.$COLUMN_EV_DURACION_MINUTOS), 0.0) AS total_minutos
            FROM $TABLE_CATEGORIAS c
            LEFT JOIN $TABLE_TAREAS t ON t.$COLUMN_TAREA_CAT_ID = c.$COLUMN_CAT_ID
            LEFT JOIN $TABLE_EVENTOS e ON e.$COLUMN_EV_TAREA_ID = t.$COLUMN_TAREA_ID 
                 AND e.$COLUMN_EV_FIN IS NOT NULL 
                 AND e.$COLUMN_EV_INICIO >= ? 
                 AND e.$COLUMN_EV_INICIO <= ?
            GROUP BY c.$COLUMN_CAT_ID, c.$COLUMN_CAT_NOMBRE
            ORDER BY c.$COLUMN_CAT_ID ASC
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(inicioSemana, finSemana))
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val id = it.getInt(it.getColumnIndexOrThrow("cat_id"))
                    val nombre = it.getString(it.getColumnIndexOrThrow("cat_nombre"))
                    val minutos = it.getDouble(it.getColumnIndexOrThrow("total_minutos"))
                    list.add(ResumenCategoria(id, nombre, minutos))
                } while (it.moveToNext())
            }
        }
        return list
    }
}
