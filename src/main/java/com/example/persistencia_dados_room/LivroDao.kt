package com.example.persistencia_dados_room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface LivroDao {
    @Insert
    suspend fun inserir(livro: Livro)

    @Query("SELECT * FROM livros ORDER BY dataConclusao DESC")
    suspend fun listarTodos(): List<Livro>

    @Query("SELECT * FROM livros WHERE id = :id")
    suspend fun obterPorId(id: Int): Livro?

    @Update
    suspend fun atualizar(livro: Livro)

    @Delete
    suspend fun deletar(livro: Livro)
}
