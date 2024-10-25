package com.example.persistencia_dados_room

class LivroRepository(private val livroDao: LivroDao) {

    suspend fun inserirLivro(livro: Livro) {
        livroDao.inserir(livro)
    }

    suspend fun buscarLivros(): List<Livro> {
        return livroDao.listarTodos()
    }

    suspend fun deletarLivro(livro: Livro) {
        livroDao.deletar(livro)
    }
}
