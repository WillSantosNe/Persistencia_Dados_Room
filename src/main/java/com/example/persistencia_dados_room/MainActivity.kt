package com.example.persistencia_dados_room

// Importações necessárias para o funcionamento do aplicativo
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.persistencia_dados_room.ui.theme.Persistencia_Dados_RoomTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.text.SimpleDateFormat

// Classe principal da atividade
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilita o modo edge-to-edge para melhor aproveitamento da tela
        enableEdgeToEdge()

        // Define o conteúdo da interface utilizando Jetpack Compose
        setContent {
            // Aplica o tema personalizado da aplicação
            Persistencia_Dados_RoomTheme {
                // Chama a função composable que representa a tela principal
                CadastroLivroScreen()
            }
        }
    }
}

// Função composable que representa a tela de cadastro e listagem de livros
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroLivroScreen() {
    // Variáveis de estado para controlar os campos de entrada do usuário
    var titulo by remember { mutableStateOf("") }
    var autor by remember { mutableStateOf("") }
    var dataConclusao by remember { mutableStateOf("") }
    var avaliacao by remember { mutableStateOf("") }
    var idEditar by remember { mutableStateOf(0) } // ID do livro em edição
    var isDatePickerVisible by remember { mutableStateOf(false) } // Controla a visibilidade do DatePicker
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // Formata a data selecionada

    // Variável que indica se estamos no modo de edição
    var modoEditar by remember { mutableStateOf(false) }
    // Lista de livros cadastrados
    var livros by remember { mutableStateOf<List<Livro>>(emptyList()) }

    // Contexto da aplicação, necessário para operações como Toast e acesso a recursos
    val context = LocalContext.current
    // Instância do banco de dados e repositório para acesso aos dados
    val db = BibliotecaDatabase.getDatabase(context)
    val livroRepository = LivroRepository(db.livroDao())

    // Cria um escopo de coroutine associado ao ciclo de vida do Composable
    val coroutineScope = rememberCoroutineScope()

    // Efeito colateral que carrega os livros ao iniciar o Composable
    LaunchedEffect(Unit) {
        // Inicia uma coroutine em IO para buscar os livros do banco de dados
        coroutineScope.launch(Dispatchers.IO) {
            livros = livroRepository.buscarLivros()
        }
    }

    // Layout principal da tela
    Column(
        modifier = Modifier
            .fillMaxSize() // Preenche todo o espaço disponível
            .padding(16.dp), // Espaçamento interno
        verticalArrangement = Arrangement.Center // Centraliza o conteúdo verticalmente
    ) {
        // Campo de texto para o título do livro
        TextField(
            value = titulo, // Valor atual do campo
            onValueChange = { titulo = it }, // Atualiza o valor ao modificar
            label = { Text(text = "Título") }, // Rótulo do campo
            modifier = Modifier.fillMaxWidth() // Preenche a largura disponível
        )
        Spacer(modifier = Modifier.height(16.dp)) // Espaçamento entre os campos

        // Campo de texto para o autor do livro
        TextField(
            value = autor,
            onValueChange = { autor = it },
            label = { Text(text = "Autor") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Botão para abrir o seletor de data
        Button(
            onClick = { isDatePickerVisible = true }, // Ao clicar, torna o DatePicker visível
            modifier = Modifier.fillMaxWidth()
        ) {
            // Exibe o texto padrão ou a data selecionada
            Text(text = if (dataConclusao.isEmpty()) "Selecione a Data de Conclusão" else dataConclusao)
        }

        // Verifica se o DatePicker deve ser exibido
        if (isDatePickerVisible) {
            val datePickerState = rememberDatePickerState()
            // Exibe o diálogo de seleção de data
            DatePickerDialog(
                onDismissRequest = { isDatePickerVisible = false }, // Fecha o diálogo ao clicar fora
                confirmButton = {
                    // Botão de confirmação da data selecionada
                    TextButton(
                        onClick = {
                            // Obtém a data selecionada e formata
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = datePickerState.selectedDateMillis ?: 0L
                            dataConclusao = dateFormatter.format(calendar.time)
                            isDatePickerVisible = false // Fecha o diálogo
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    // Botão para cancelar a seleção de data
                    TextButton(onClick = { isDatePickerVisible = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                // Componente de seleção de data
                DatePicker(state = datePickerState)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Campo de texto para a avaliação do livro
        TextField(
            value = avaliacao,
            onValueChange = { avaliacao = it },
            label = { Text(text = "Avaliação (1 a 5)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Botão para cadastrar ou salvar as alterações do livro
        Button(
            onClick = {
                // Verifica se todos os campos foram preenchidos
                if (titulo.isNotBlank() && autor.isNotBlank() && dataConclusao.isNotBlank() && avaliacao.isNotBlank()) {
                    try {
                        // Converte a avaliação para inteiro e valida o intervalo
                        val avaliacaoInt = avaliacao.toIntOrNull()
                        if (avaliacaoInt == null || avaliacaoInt !in 1..5) {
                            Toast.makeText(context, "A avaliação deve ser um número entre 1 e 5", Toast.LENGTH_LONG).show()
                            return@Button // Interrompe o processamento se a avaliação for inválida
                        }

                        // Inicia uma coroutine para operações de banco de dados
                        coroutineScope.launch(Dispatchers.IO) {
                            if (!modoEditar) {
                                // Inserção de um novo livro
                                livroRepository.inserirLivro(
                                    Livro(
                                        titulo = titulo,
                                        autor = autor,
                                        dataConclusao = dataConclusao,
                                        avaliacao = avaliacaoInt
                                    )
                                )
                            } else {
                                // Atualização de um livro existente
                                livroRepository.atualizarLivro(
                                    Livro(
                                        id = idEditar,
                                        titulo = titulo,
                                        autor = autor,
                                        dataConclusao = dataConclusao,
                                        avaliacao = avaliacaoInt
                                    )
                                )
                                modoEditar = false // Sai do modo de edição
                                idEditar = 0 // Reseta o ID de edição
                            }

                            // Busca a lista atualizada de livros
                            val livrosAtualizados = livroRepository.buscarLivros()

                            // Atualiza a lista de livros na thread principal
                            withContext(Dispatchers.Main) {
                                livros = livrosAtualizados
                                // Limpa os campos após salvar
                                titulo = ""
                                autor = ""
                                dataConclusao = ""
                                avaliacao = ""
                            }
                        }
                    } catch (e: Exception) {
                        // Loga o erro caso ocorra alguma exceção
                        Log.e("Erro ao Salvar", "Erro ao salvar: ${e.message}")
                    }
                } else {
                    // Exibe uma mensagem se algum campo não foi preenchido
                    Toast.makeText(context, "Preencha todos os campos", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            // Altera o texto do botão dependendo do modo (edição ou cadastro)
            Text(text = if (modoEditar) "Salvar Alterações" else "Cadastrar Livro")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Lista que exibe os livros cadastrados
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(livros) { livro ->
                // Exibe o título do livro com tamanho de fonte maior
                Text(text = "Título: ${livro.titulo}", fontSize = 22.sp)
                // Exibe o autor, avaliação e data de conclusão
                Text(text = "Autor: ${livro.autor} | Avaliação: ${livro.avaliacao}/5 | Conclusão: ${livro.dataConclusao}")
                Row {
                    // Botão para editar o livro
                    Button(onClick = {
                        // Preenche os campos com os dados do livro selecionado
                        modoEditar = true
                        idEditar = livro.id
                        titulo = livro.titulo
                        autor = livro.autor
                        dataConclusao = livro.dataConclusao
                        avaliacao = livro.avaliacao.toString()
                    }) {
                        Text(text = "Editar")
                    }

                    // Botão para deletar o livro
                    Button(onClick = {
                        try {
                            // Inicia uma coroutine para deletar o livro
                            coroutineScope.launch(Dispatchers.IO) {
                                livroRepository.deletarLivro(livro) // Deleta o livro do banco de dados
                                delay(500) // Aguarda meio segundo (pode ser removido se não necessário)
                                // Busca a lista atualizada de livros
                                val livrosAtualizados = livroRepository.buscarLivros()
                                // Atualiza a lista de livros na thread principal
                                withContext(Dispatchers.Main) {
                                    livros = livrosAtualizados
                                }
                            }
                        } catch (e: Exception) {
                            // Loga o erro caso ocorra
                            Log.e("ERRO Deletar", "${e.message}")
                        }
                    }) {
                        Text(text = "Deletar")
                    }
                }
            }
        }
    }
}

// Função de pré-visualização da tela no Android Studio
@Preview(showBackground = true)
@Composable
fun PreviewCadastroLivroScreen() {
    // Aplica o tema personalizado
    Persistencia_Dados_RoomTheme {
        // Chama a tela de cadastro de livros
        CadastroLivroScreen()
    }
}
