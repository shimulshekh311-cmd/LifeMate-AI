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
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
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
    var bookToDelete by remember { mutableStateOf<StudyBookEntity?>(null) }

    LaunchedEffect(books) {
        if (books.isNotEmpty()) {
            if (selectedBookId == null || books.none { it.id == selectedBookId }) {
                selectedBookId = books.first().id
            }
        } else {
            selectedBookId = null
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
                    if (books.isEmpty()) showAddBookDialog = true
                    else showAddChapterDialog = true
                },
                containerColor = PrimaryViolet,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (books.isEmpty()) I18n.t("add_book") else I18n.t("add_chapter")
                )
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
            }

            if (books.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = PrimaryViolet,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (I18n.currentLanguage == "bn") "কোনো বই বা বিষয় নেই" else "No Books Added Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (I18n.currentLanguage == "bn")
                                    "পড়াশোনা ও পরীক্ষার প্রস্তুতি গোছাতে আপনার প্রথম বই বা বিষয় যোগ করুন।"
                                else
                                    "Add your first subject or book to organize topics and track your progress.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { showAddBookDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryViolet)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(I18n.t("add_book"))
                            }
                        }
                    }
                }
            } else {
                item {
                    val safeIndex = books.indexOfFirst { it.id == selectedBookId }.let { if (it >= 0) it else 0 }
                    ScrollableTabRow(
                        selectedTabIndex = safeIndex,
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentBook.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (currentBook.author.isNotEmpty()) {
                                        Text(
                                            text = currentBook.author,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$completedCount/$totalCount ($progressPercent%)",
                                        fontWeight = FontWeight.Bold,
                                        color = CyberCyan
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { bookToDelete = currentBook },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Book",
                                            tint = RoseError,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
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

                // Chapters List Section
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = I18n.t("chapters"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showAddChapterDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(I18n.t("add_chapter"))
                        }
                    }
                }

                if (currentChapters.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            message = if (I18n.currentLanguage == "bn") "এই বিষয়ের জন্য কোনো অধ্যায় যোগ করা হয়নি।" else "No chapters added for this subject yet.",
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
    }

    if (bookToDelete != null) {
        ConfirmDeleteDialog(
            title = I18n.t("delete"),
            message = if (I18n.currentLanguage == "bn")
                "'${bookToDelete?.title}' বইটি ও এর অধ্যায়গুলো মুছে ফেলতে চান?"
            else
                "Are you sure you want to delete '${bookToDelete?.title}' and all its chapters?",
            onConfirm = {
                viewModel.deleteBook(bookToDelete!!.id)
                bookToDelete = null
            },
            onDismiss = { bookToDelete = null }
        )
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
                val context = LocalContext.current
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chapter.googleDocsUrl.isNotEmpty()) {
                        Surface(
                            onClick = {
                                try {
                                    val uri = if (chapter.googleDocsUrl.startsWith("http")) Uri.parse(chapter.googleDocsUrl)
                                    else Uri.parse("https://${chapter.googleDocsUrl}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (_: Exception) {}
                            },
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
                            onClick = {
                                try {
                                    val uri = if (chapter.youtubeUrl.startsWith("http")) Uri.parse(chapter.youtubeUrl)
                                    else Uri.parse("https://${chapter.youtubeUrl}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                } catch (_: Exception) {}
                            },
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
