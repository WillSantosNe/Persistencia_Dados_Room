package com.example.persistencia_dados_room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.persistencia_dados_room.ui.theme.Persistencia_Dados_RoomTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.text.SimpleDateFormat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilita o modo edge-to-edge para a UI
        enableEdgeToEdge()

        // Define o conteúdo da interface
        setContent {
            Persistencia_Dados_RoomTheme {
                CadastroLivroScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroLivroScreen() {
    // Campos de entrada do formulário de cadastro
    var titulo by remember { mutableStateOf("") }
    var autor by remember { mutableStateOf("") }
    var dataConclusao by remember { mutableStateOf("") }
    var avaliacao by remember { mutableStateOf("") }

    // Estados para o DatePicker
    var isDatePickerVisible by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Lista para armazenar os livros recuperados do banco
    var livros by remember { mutableStateOf<List<Livro>>(emptyList()) }

    // Obtém o contexto e inicializa o banco e repositório
    val context = LocalContext.current
    val db = BibliotecaDatabase.getDatabase(context)
    val livroRepository = LivroRepository(db.livroDao())

    // Carrega os livros do banco ao montar o Composable
    LaunchedEffect(Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            livros = livroRepository.buscarLivros()
        }
    }

    // Layout principal
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        // Campos de entrada para o formulário
        TextField(
            value = titulo,
            onValueChange = { titulo = it },
            label = { Text(text = "Título") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = autor,
            onValueChange = { autor = it },
            label = { Text(text = "Autor") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para selecionar a data
        Button(
            onClick = { isDatePickerVisible = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = if (dataConclusao.isEmpty()) "Selecione a Data de Conclusão" else dataConclusao)
        }

        // Exibe o DatePickerDialog quando visível
        if (isDatePickerVisible) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { isDatePickerVisible = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = datePickerState.selectedDateMillis ?: 0L
                            dataConclusao = dateFormatter.format(calendar.time)
                            isDatePickerVisible = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isDatePickerVisible = false }) {
                        Text("Cancelar")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextField(
            value = avaliacao,
            onValueChange = { avaliacao = it },
            label = { Text(text = "Avaliação (1 a 5)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Botão para cadastrar o livro
        Button(
            onClick = {
                // Valida se os campos foram preenchidos corretamente
                if (titulo.isNotBlank() && autor.isNotBlank() && dataConclusao.isNotBlank() && avaliacao.isNotBlank()) {
                    val avaliacaoInt = avaliacao.toIntOrNull()
                    if (avaliacaoInt != null && avaliacaoInt in 1..5) {
                        // Insere o livro no banco de dados utilizando o repositório
                        CoroutineScope(Dispatchers.IO).launch {
                            livroRepository.inserirLivro(
                                Livro(
                                    titulo = titulo,
                                    autor = autor,
                                    dataConclusao = dataConclusao,
                                    avaliacao = avaliacaoInt
                                )
                            )
                            // Atualiza a lista de livros após a inserção
                            livros = livroRepository.buscarLivros()
                        }

                        // Limpa os campos após o cadastro
                        titulo = ""
                        autor = ""
                        dataConclusao = ""
                        avaliacao = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Cadastrar Livro")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Exibição da lista de livros cadastrados
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(livros) { livro ->
                LivroItem(livro)
            }
        }
    }
}

// Composable que exibe os detalhes de cada livro na lista
@Composable
fun LivroItem(livro: Livro) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(text = "Título: ${livro.titulo}")
        Text(text = "Autor: ${livro.autor}")
        Text(text = "Data de Conclusão: ${livro.dataConclusao}")
        Text(text = "Avaliação: ${livro.avaliacao}/5")
        Spacer(modifier = Modifier.height(8.dp))
    }
}

// Preview para a tela de cadastro de livro
@Preview(showBackground = true)
@Composable
fun PreviewCadastroLivroScreen() {
    Persistencia_Dados_RoomTheme {
        CadastroLivroScreen()
    }
}
