package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.I18n
import com.example.data.local.entities.StudyBookEntity
import com.example.data.local.entities.StudyChapterEntity
import com.example.ui.components.ConfirmDeleteDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun StudyScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.studyCategories.collectAsState()
    val books by viewModel.studyBooks.collectAsState()
    val allChapters by viewModel.studyChapters.collectAsState()

    var selectedBookId by remember { mutableStateOf<Long?>(null) }
    var showAddBookDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var chapterToDelete by remember { mutableStateOf<StudyChapterEntity?>(null) }

    LaunchedEffect(books) {
        if (selectedBookId == null && books.isNotEmpty()) {
            selectedBookId = books.first().id
        }
    }

    val currentBook = books.find { it.id == selectedBookId }
    val currentChapters = allChapters.filter { it.bookId == selectedBookId }
    val completedCount = currentChapters.count { it.isCompleted }
    val totalCount = currentChapters.size
    val progressPercent = if (totalCount > 0) (completedCount.toFloat() / totalCount * 100).toInt() else 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedBookId != null) showAddChapterDialog = true
                    else showAddBookDialog = true
                },
                containerColor = PrimaryViolet,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = I18n.t("add_chapter"))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Book Selector Dropdown / Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = I18n.t("books"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { showAddBookDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(I18n.t("add_book"))
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = books.indexOfFirst { it.id == selectedBookId }.coerceAtLeast(0),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    contentColor = PrimaryViolet
                ) {
                    books.forEach { book ->
                        Tab(
                            selected = book.id == selectedBookId,
                            onClick = { selectedBookId = book.id },
                            text = { Text(book.title, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            // Progress Banner for Selected Book
            if (currentBook != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentBook.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$completedCount/$totalCount ($progressPercent%)",
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { if (totalCount > 0) completedCount.toFloat() / totalCount else 0f },
                                modifier = Modifier.fillMaxWidth(),
                                color = CyberCyan,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }

            // Chapters List Section
            item {
                Text(
                    text = I18n.t("chapters"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (currentChapters.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        message = "এই বিষয়ের জন্য কোনো অধ্যায় যোগ করা হয়নি।",
                        actionButton = {
                            Button(onClick = { showAddChapterDialog = true }) {
                                Text(I18n.t("add_chapter"))
                            }
                        }
                    )
                }
            } else {
                items(currentChapters) { chapter ->
                    ChapterItemCard(
                        chapter = chapter,
                        onToggle = { viewModel.toggleChapterCompletion(chapter) },
                        onDelete = { chapterToDelete = chapter }
                    )
                }
            }
        }
    }

    if (chapterToDelete != null) {
        ConfirmDeleteDialog(
            title = I18n.t("delete"),
            message = "'${chapterToDelete?.title}' অধ্যায়টি মুছে ফেলতে চান?",
            onConfirm = {
                viewModel.deleteChapter(chapterToDelete!!.id)
                chapterToDelete = null
            },
            onDismiss = { chapterToDelete = null }
        )
    }

    if (showAddBookDialog) {
        AddBookDialog(
            categories = categories,
            onDismiss = { showAddBookDialog = false },
            onSave = { catId, title, author ->
                viewModel.addBook(catId, title, author)
                showAddBookDialog = false
            }
        )
    }

    if (showAddChapterDialog && selectedBookId != null) {
        AddChapterDialog(
            bookId = selectedBookId!!,
            onDismiss = { showAddChapterDialog = false },
            onSave = { bId, title, desc, gdocs, yt, pdf, notes ->
                viewModel.addChapter(bId, title, desc, gdocs, yt, pdf, notes)
                showAddChapterDialog = false
            }
        )
    }
}

@Composable
fun ChapterItemCard(
    chapter: StudyChapterEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = chapter.isCompleted, onCheckedChange = { onToggle() })
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = chapter.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (chapter.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    if (chapter.description.isNotEmpty()) {
                        Text(
                            text = chapter.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError)
                }
            }

            if (chapter.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📝 ${chapter.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberCyan
                )
            }

            if (chapter.googleDocsUrl.isNotEmpty() || chapter.youtubeUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chapter.googleDocsUrl.isNotEmpty()) {
                        Surface(
                            color = PrimaryViolet.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "📄 Google Docs",
                                fontSize = 11.sp,
                                color = PrimaryViolet,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (chapter.youtubeUrl.isNotEmpty()) {
                        Surface(
                            color = RoseError.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "▶️ YouTube",
                                fontSize = 11.sp,
                                color = RoseError,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddBookDialog(
    categories: List<com.example.data.local.entities.StudyCategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n.t("add_book"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(I18n.t("book_title")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("লেখক / নির্দেশক") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onSave(selectedCatId, title, author)
                }
            ) {
                Text(I18n.t("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(I18n.t("cancel")) }
        }
    )
}

@Composable
fun AddChapterDialog(
    bookId: Long,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var gdocs by remember { mutableStateOf("") }
    var yt by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n.t("add_chapter"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(I18n.t("chapter_name")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("বিবরণ") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gdocs,
                    onValueChange = { gdocs = it },
                    label = { Text(I18n.t("gdocs_link")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = yt,
                    onValueChange = { yt = it },
                    label = { Text(I18n.t("youtube_link")) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(I18n.t("study_notes")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onSave(bookId, title, desc, gdocs, yt, "", notes)
                }
            ) {
                Text(I18n.t("save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(I18n.t("cancel")) }
        }
    )
}
