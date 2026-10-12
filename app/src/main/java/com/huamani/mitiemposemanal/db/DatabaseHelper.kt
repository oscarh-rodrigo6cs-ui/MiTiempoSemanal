package com.huamani.mitiemposemanal.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.huamani.mitiemposemanal.model.Categoria
import com.huamani.mitiemposemanal.model.Tarea

class DatabaseHelper(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val DATABASE_NAME = "mitiemposemanal.db"
        private const val DATABASE_VERSION = 1

        // Tabla Categorías
        const val TABLE_CATEGORIAS = "categorias"
        const val COLUMN_CAT_ID = "id"
        const val COLUMN_CAT_NOMBRE = "nombre"

        // Tabla Tareas
        const val TABLE_TAREAS = "tareas"
        const val COLUMN_TAREA_ID = "id"
        const val COLUMN_TAREA_NOMBRE = "nombre"
        const val COLUMN_TAREA_CAT_ID = "categoria_id"
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

        db.execSQL(createTableCategorias)
        db.execSQL(createTableTareas)

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
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TAREAS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIAS")
        onCreate(db)
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
}
