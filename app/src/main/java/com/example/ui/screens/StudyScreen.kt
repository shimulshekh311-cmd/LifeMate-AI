package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.i18n.I18n
import com.example.core.util.DocumentOpener
import com.example.data.local.entities.StudyBookEntity
import com.example.data.local.entities.StudyCategoryEntity
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

    // 0: Job Preparation, 1: Academic
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val currentCategoryType = if (selectedCategoryIndex == 0) "JOB_PREPARATION" else "ACADEMIC"

    val currentSectionBooks = remember(books, currentCategoryType) {
        books.filter { it.categoryType == currentCategoryType }
    }

    var selectedBookId by remember { mutableStateOf<Long?>(null) }
    var showAddBookDialog by remember { mutableStateOf(false) }
    var showAddChapterDialog by remember { mutableStateOf(false) }
    var chapterToDelete by remember { mutableStateOf<StudyChapterEntity?>(null) }
    var bookToDelete by remember { mutableStateOf<StudyBookEntity?>(null) }

    // Safe selection: update selectedBookId when section books change or tab switches
    LaunchedEffect(currentSectionBooks) {
        if (currentSectionBooks.isNotEmpty()) {
            if (selectedBookId == null || currentSectionBooks.none { it.id == selectedBookId }) {
                selectedBookId = currentSectionBooks.first().id
            }
        } else {
            selectedBookId = null
        }
    }

    val currentBook = currentSectionBooks.find { it.id == selectedBookId }
    val currentChapters = allChapters.filter { it.bookId == selectedBookId }
    val completedCount = currentChapters.count { it.isCompleted }
    val totalCount = currentChapters.size
    val progressPercent = if (totalCount > 0) (completedCount.toFloat() / totalCount * 100).toInt() else 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (currentSectionBooks.isEmpty()) showAddBookDialog = true
                    else showAddChapterDialog = true
                },
                containerColor = PrimaryViolet,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (currentSectionBooks.isEmpty()) I18n.t("add_book") else I18n.t("add_chapter")
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Section Tabs: Job Preparation vs Academic Books
            TabRow(
                selectedTabIndex = selectedCategoryIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryViolet
            ) {
                Tab(
                    selected = selectedCategoryIndex == 0,
                    onClick = { selectedCategoryIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (I18n.currentLanguage == "bn") "চাকরির প্রস্তুতি" else "Job Preparation",
                                fontWeight = if (selectedCategoryIndex == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedCategoryIndex == 1,
                    onClick = { selectedCategoryIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (I18n.currentLanguage == "bn") "একাডেমিক পড়াশোনা" else "Academic Books",
                                fontWeight = if (selectedCategoryIndex == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Book Selector Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCategoryIndex == 0) {
                                if (I18n.currentLanguage == "bn") "চাকরির বই ও বিষয়সমূহ" else "Job Prep Subjects"
                            } else {
                                if (I18n.currentLanguage == "bn") "একাডেমিক পাঠ্যবইসমূহ" else "Academic Subjects"
                            },
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

                // Independent Empty States per Category
                if (currentSectionBooks.isEmpty()) {
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
                                    imageVector = if (selectedCategoryIndex == 0) Icons.Default.WorkOutline else Icons.Default.School,
                                    contentDescription = null,
                                    tint = PrimaryViolet,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (selectedCategoryIndex == 0) {
                                        if (I18n.currentLanguage == "bn") "চাকরির প্রস্তুতির কোনো বই যুক্ত নেই" else "No Job Preparation Books Yet"
                                    } else {
                                        if (I18n.currentLanguage == "bn") "একাডেমিক পড়াশোনার কোনো বই যুক্ত নেই" else "No Academic Books Yet"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (selectedCategoryIndex == 0) {
                                        if (I18n.currentLanguage == "bn")
                                            "বিসিএস, ব্যাংক বা অন্যান্য সরকারি-বেসরকারি চাকরির পরীক্ষার বই ও অধ্যায় যুক্ত করুন।"
                                        else
                                            "Add BCS, Bank, or job exam preparation guides and track topics."
                                    } else {
                                        if (I18n.currentLanguage == "bn")
                                            "স্কুল, কলেজ বা বিশ্ববিদ্যালয় কোর্সের পাঠ্যবই ও নোট যুক্ত করুন।"
                                        else
                                            "Add course textbooks, chapters, and resources to study systematically."
                                    },
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
                                    Text(
                                        if (selectedCategoryIndex == 0)
                                            (if (I18n.currentLanguage == "bn") "+ চাকরির বই যোগ করুন" else "+ Add Job Book")
                                        else
                                            (if (I18n.currentLanguage == "bn") "+ একাডেমিক বই যোগ করুন" else "+ Add Academic Book")
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        val safeIndex = currentSectionBooks.indexOfFirst { it.id == selectedBookId }.let { if (it >= 0) it else 0 }
                        ScrollableTabRow(
                            selectedTabIndex = safeIndex,
                            edgePadding = 0.dp,
                            containerColor = Color.Transparent,
                            contentColor = PrimaryViolet
                        ) {
                            currentSectionBooks.forEach { book ->
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
                                    Surface(
                                        color = CyberCyan.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "$progressPercent% সম্পন্ন",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyberCyan,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    IconButton(onClick = { bookToDelete = currentBook }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Book", tint = RoseError)
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$totalCount টির মধ্যে $completedCount টি অধ্যায় সম্পন্ন",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$completedCount/$totalCount",
                                        style = MaterialTheme.typography.bodySmall,
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
                        items(currentChapters, key = { it.id }) { chapter ->
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
            initialCategoryType = currentCategoryType,
            onDismiss = { showAddBookDialog = false },
            onSave = { catId, title, author, catType ->
                viewModel.addBook(catId, title, author, catType)
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
    val context = LocalContext.current

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
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = RoseError, modifier = Modifier.size(18.dp))
                }
            }

            if (chapter.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "📝 ${chapter.notes}",
                    fontSize = 11.sp,
                    color = CyberCyan
                )
            }

            // Google Docs, YouTube, or Local Docs Links
            if (chapter.googleDocsUrl.isNotEmpty() || chapter.youtubeUrl.isNotEmpty() || chapter.pdfFilePath.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Google Docs Link using DocumentOpener (official app with browser fallback)
                    if (chapter.googleDocsUrl.isNotEmpty()) {
                        Surface(
                            onClick = {
                                DocumentOpener.openGoogleDocsLink(context, chapter.googleDocsUrl)
                            },
                            color = PrimaryViolet.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryViolet, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Google Docs",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryViolet
                                )
                            }
                        }
                    }

                    // Local Document / PDF
                    if (chapter.pdfFilePath.isNotEmpty()) {
                        Surface(
                            onClick = {
                                DocumentOpener.openLocalDocument(context, Uri.parse(chapter.pdfFilePath), chapter.pdfFilePath)
                            },
                            color = AmberWarning.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "ডকুমেন্ট / PDF",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberWarning
                                )
                            }
                        }
                    }

                    // YouTube Video Link
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
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayCircle, contentDescription = null, tint = RoseError, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "YouTube",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoseError
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddBookDialog(
    categories: List<StudyCategoryEntity>,
    initialCategoryType: String = "JOB_PREPARATION",
    onDismiss: () -> Unit,
    onSave: (Long, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var categoryType by remember { mutableStateOf(initialCategoryType) }
    var selectedCatId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 1L) }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n.t("add_book"), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Category Type Selection
                Text(
                    text = if (I18n.currentLanguage == "bn") "বইয়ের বিভাগ নির্বাচন করুন:" else "Select Book Section:",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = categoryType == "JOB_PREPARATION",
                        onClick = { categoryType = "JOB_PREPARATION" },
                        label = { Text(if (I18n.currentLanguage == "bn") "চাকরির প্রস্তুতি" else "Job Prep", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.WorkOutline, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = categoryType == "ACADEMIC",
                        onClick = { categoryType = "ACADEMIC" },
                        label = { Text(if (I18n.currentLanguage == "bn") "একাডেমিক" else "Academic", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (it.isNotBlank()) titleError = false
                    },
                    label = { Text(I18n.t("book_title") + " *") },
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text(if (I18n.currentLanguage == "bn") "বইয়ের নাম আবশ্যক" else "Book title is required", color = RoseError) }
                    } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(if (I18n.currentLanguage == "bn") "লেখক / নির্দেশক" else "Author / Guide") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSave(selectedCatId, title.trim(), author.trim(), categoryType)
                    }
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
    var pdf by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(I18n.t("add_chapter"), fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (it.isNotBlank()) titleError = false
                        },
                        label = { Text(I18n.t("chapter_name") + " *") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text(if (I18n.currentLanguage == "bn") "অধ্যায়ের নাম আবশ্যক" else "Chapter name is required", color = RoseError) }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(I18n.t("item_desc")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = gdocs,
                        onValueChange = { gdocs = it },
                        label = { Text(I18n.t("gdocs_link")) },
                        placeholder = { Text("https://docs.google.com/...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = yt,
                        onValueChange = { yt = it },
                        label = { Text(I18n.t("youtube_link")) },
                        placeholder = { Text("https://youtube.com/...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text(I18n.t("study_notes")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSave(bookId, title.trim(), desc.trim(), gdocs.trim(), yt.trim(), pdf.trim(), notes.trim())
                    }
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
