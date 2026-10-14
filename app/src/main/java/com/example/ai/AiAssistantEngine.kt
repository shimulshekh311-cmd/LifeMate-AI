package com.example.ai

import com.example.core.i18n.I18n
import com.example.data.local.entities.*
import com.example.data.repository.LifeMateRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class AssistantAction {
    data class CreateTask(val title: String, val time: String = "08:00 AM", val type: String = "TASK") : AssistantAction()
    data class CreateHabit(val title: String, val recurrence: String = "DAILY") : AssistantAction()
    data class StartExerciseSession(val planId: Long? = null) : AssistantAction()
    data class AddJobApp(val orgName: String, val position: String) : AssistantAction()
    data class RecordExpense(val amount: Double, val category: String, val description: String) : AssistantAction()
    data class QueryStats(val metric: String) : AssistantAction()
    data class RememberPreference(val key: String, val label: String, val value: String) : AssistantAction()
    data class AnswerQuestion(val responseText: String) : AssistantAction()
    data class RequestClarification(val question: String) : AssistantAction()
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "USER" or "ASSISTANT"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val pendingAction: AssistantAction? = null,
    val isConfirmed: Boolean = false
)

class AiAssistantEngine(
    private val repository: LifeMateRepository
) {
    suspend fun processUserMessage(
        rawMessage: String,
        assistantName: String = "LifeMate"
    ): ChatMessage {
        val message = rawMessage.trim()
        val lower = message.lowercase(Locale.ROOT)
        val isBn = I18n.currentLanguage == "bn"

        if (lower.startsWith("hi") || lower.startsWith("hello") || lower.contains("কেমন আছো") || lower.contains("হ্যালো") || lower.contains("নমস্কার") || lower.contains("সালাম")) {
            val reply = if (isBn) {
                "আমি $assistantName, আপনার সার্বক্ষণিক ডিজিটাল জীবন সহকারী! আমি খুব ভালো আছি। আপনার আজকের কাজ, পড়াশোনা, ব্যায়াম বা হিসাবের ব্যাপারে আমি কীভাবে সাহায্য করতে পারি?"
            } else {
                "Hello! I am $assistantName, your personal life assistant. How can I assist you today with tasks, study, workouts, or finances?"
            }
            return ChatMessage(sender = "ASSISTANT", text = reply)
        }

        if (lower.contains("রিমাইন্ডার") || lower.contains("কাজ") || lower.contains("টাস্ক") || lower.contains("remind") || lower.contains("alarm") || lower.contains("task")) {
            val title = extractTitleAfterKeywords(message, listOf("রিমাইন্ডার", "কাজ", "টাস্ক", "remind me to", "set alarm for", "task", "set reminder"))
            if (title.isBlank()) {
                val q = if (isBn) "আপনি কী কাজের জন্য রিমাইন্ডার বা অ্যালার্ম সেট করতে চান? অনুগ্রহ করে কাজের নাম এবং সময় বলুন।"
                else "What would you like to be reminded about? Please tell me the task name and time."
                return ChatMessage(sender = "ASSISTANT", text = q, pendingAction = AssistantAction.RequestClarification(q))
            }

            val time = extractTime(lower) ?: "09:00 AM"
            val action = AssistantAction.CreateTask(title = title, time = time, type = "TASK")
            val confirmationMsg = if (isBn) {
                "নিশ্চিত করছি: '$title' কাজটি সময় $time-এ আপনার তালিকায় যুক্ত করব?"
            } else {
                "Confirming: Should I add '$title' scheduled for $time to your task list?"
            }
            return ChatMessage(sender = "ASSISTANT", text = confirmationMsg, pendingAction = action)
        }

        if (lower.contains("অভ্যাস") || lower.contains("habit") || lower.contains("রুটিন") || lower.contains("routine")) {
            val title = extractTitleAfterKeywords(message, listOf("অভ্যাস", "রুটিন", "habit of", "create habit", "routine"))
            if (title.isBlank()) {
                val q = if (isBn) "আপনি কোন অভ্যাস বা রুটিনটি শুরু করতে চান? যেমন: 'প্রতিদিন ২০ মিনিট পড়ার অভ্যাস'"
                else "What habit or routine would you like to build? e.g., 'Daily 20 mins reading habit'"
                return ChatMessage(sender = "ASSISTANT", text = q, pendingAction = AssistantAction.RequestClarification(q))
            }
            val action = AssistantAction.CreateHabit(title = title)
            val msg = if (isBn) "আমি আপনার জন্য '$title' দৈনিক অভ্যাসটি তালিকায় যুক্ত করতে প্রস্তুত।"
            else "I'm ready to add '$title' as a daily habit."
            return ChatMessage(sender = "ASSISTANT", text = msg, pendingAction = action)
        }

        if (lower.contains("ব্যায়াম") || lower.contains("ওয়ার্কআউট") || lower.contains("exercise") || lower.contains("workout")) {
            if (lower.contains("শুরু") || lower.contains("start")) {
                val action = AssistantAction.StartExerciseSession()
                val msg = if (isBn) "ব্যায়াম সেশন প্রস্তুত! আপনি কি এখন গাইডেড ওয়ার্কআউট সেশন শুরু করতে চান?"
                else "Workout session is ready! Would you like to start the guided workout now?"
                return ChatMessage(sender = "ASSISTANT", text = msg, pendingAction = action)
            } else {
                val msg = if (isBn) "আপনি স্বাস্থ্য ও ব্যায়াম মডিউলে গিয়ে নতুন ওয়ার্কআউট প্ল্যান তৈরি করতে পারেন অথবা সরাসরি সেশন শুরু করতে পারেন।"
                else "You can view your exercise plans or start a guided session in the Exercise module."
                return ChatMessage(sender = "ASSISTANT", text = msg)
            }
        }

        if (lower.contains("চাকরি") || lower.contains("আবেদন") || lower.contains("job") || lower.contains("application")) {
            val action = AssistantAction.QueryStats("JOBS")
            val msg = if (isBn) "আমি আপনার চাকরির আবেদনের বর্তমান পরিসংখ্যান বিশ্লেষণ করে জানাতে পারি। দেখতে চান?"
            else "I can summarize your active and upcoming job applications. Would you like to see the report?"
            return ChatMessage(sender = "ASSISTANT", text = msg, pendingAction = action)
        }

        if (lower.contains("টাকা") || lower.contains("খরচ") || lower.contains("হিসাব") || lower.contains("spend") || lower.contains("expense") || lower.contains("money")) {
            val amount = extractAmount(lower)
            if (amount != null && amount > 0) {
                val action = AssistantAction.RecordExpense(amount = amount, category = "Food", description = "ব্যক্তিগত খরচ")
                val msg = if (isBn) "৳$amount খরচ হিসেবে লেনদেন যুক্ত করবেন কি?"
                else "Would you like to record an expense of ৳$amount?"
                return ChatMessage(sender = "ASSISTANT", text = msg, pendingAction = action)
            } else {
                val action = AssistantAction.QueryStats("FINANCE")
                val msg = if (isBn) "আপনার বর্তমান ব্যালেন্স ও খরচের হিসাব দেখাব?"
                else "Would you like to view your current balance and monthly expense breakdown?"
                return ChatMessage(sender = "ASSISTANT", text = msg, pendingAction = action)
            }
        }

        if (lower.contains("মনে রাখো") || lower.contains("পছন্দ") || lower.contains("remember") || lower.contains("my preferred")) {
            val key = "custom_pref_${System.currentTimeMillis() % 1000}"
            val action = AssistantAction.RememberPreference(key, "ব্যবহারকারীর নির্দেশ", message)
            val msg = if (isBn) "আমি আপনার এই পছন্দটি স্থায়ী মেমরিতে সংরক্ষণ করব: '$message'। আপনি সেটিংস থেকে এটি যেকোনো সময় দেখতে বা মুছতে পারবেন।"
            else "I will store this preference in my memory: '$message'. You can review or delete it anytime in Settings."
            return ChatMessage(sender = "ASSISTANT", text = msg, pendingAction = action)
        }

        if (lower.contains("গল্প") || lower.contains("story")) {
            val story = if (isBn) {
                "একদা এক ব্যক্তি প্রতিদিন মাত্র ১৫ মিনিট করে একটি নতুন দক্ষতা অনুশীলনের সিদ্ধান্ত নিয়েছিল। প্রথম কয়েক সপ্তাহ তেমন পরিবর্তন দেখা যায়নি, কিন্তু এক বছর পর সে সেই কাজে অতুলনীয় দক্ষতা অর্জন করল। লাইফমেট-এর নীতিও ঠিক তাই—প্রতিদিনের ছোট ছোট ইতিবাচক অভ্যাসেই জীবনের বড় সাফল্য লুকিয়ে থাকে!"
            } else {
                "Once, someone decided to practice a skill for just 15 minutes each day. After a year of quiet consistency, they had mastered it completely. That is the core philosophy of LifeMate—small, continuous habits compound into great life achievements!"
            }
            return ChatMessage(sender = "ASSISTANT", text = story)
        }

        val response = if (isBn) {
            "আমি আপনার কথা বুঝতে পেরেছি। আপনি আমাকে দিয়ে প্রতিদিনের কাজের তালিকা সাজাতে পারেন, পড়ার লক্ষ্য নির্ধারণ করতে পারেন, ব্যায়াম শুরু করতে পারেন কিংবা চাকরির আবেদন ও খরচের হিসাব রাখতে পারেন। আপনি কী করতে চান বলুন!"
        } else {
            "I hear you! I can help you schedule daily routines, organize study chapters, start guided workouts, track job applications, or manage your personal budget. How can I help right now?"
        }
        return ChatMessage(sender = "ASSISTANT", text = response)
    }

    suspend fun executeAction(action: AssistantAction): String {
        val isBn = I18n.currentLanguage == "bn"
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        return when (action) {
            is AssistantAction.CreateTask -> {
                repository.insertTaskHabit(
                    TaskHabitEntity(
                        title = action.title,
                        type = action.type,
                        scheduledDate = todayStr,
                        scheduledTime = action.time
                    )
                )
                if (isBn) "সফল হয়েছে! '${action.title}' কাজটি সময় ${action.time}-এ যুক্ত করা হয়েছে।"
                else "Success! '${action.title}' has been added for ${action.time}."
            }
            is AssistantAction.CreateHabit -> {
                repository.insertTaskHabit(
                    TaskHabitEntity(
                        title = action.title,
                        type = "HABIT",
                        recurrence = action.recurrence
                    )
                )
                if (isBn) "সফল হয়েছে! '${action.title}' দৈনিক অভ্যাস হিসেবে সংরক্ষিত হয়েছে।"
                else "Success! '${action.title}' has been added as a daily habit."
            }
            is AssistantAction.RecordExpense -> {
                repository.insertTransaction(
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = action.amount,
                        category = action.category,
                        date = todayStr,
                        description = action.description
                    )
                )
                if (isBn) "সফল হয়েছে! ৳${action.amount} খরচ হিসেবে রেকর্ড করা হয়েছে।"
                else "Success! Expense of ৳${action.amount} recorded."
            }
            is AssistantAction.RememberPreference -> {
                repository.saveMemory(action.key, action.label, action.value)
                if (isBn) "সফল হয়েছে! আপনার পছন্দটি মেমরিতে সংরক্ষিত হয়েছে।"
                else "Success! Your preference has been saved in assistant memory."
            }
            is AssistantAction.StartExerciseSession -> {
                if (isBn) "ব্যায়াম সেশন সক্রিয় করা হয়েছে! স্ক্রিনের নিচের স্টার্ট বাটনে ট্যাপ করুন।"
                else "Workout session activated! Tap start in the guided session."
            }
            else -> {
                if (isBn) "অ্যাকশন সম্পন্ন হয়েছে।" else "Action completed."
            }
        }
    }

    private fun extractTitleAfterKeywords(text: String, keywords: List<String>): String {
        for (kw in keywords) {
            val idx = text.indexOf(kw, ignoreCase = true)
            if (idx != -1) {
                val candidate = text.substring(idx + kw.length).trim(' ', ':', '-', '"', '\'')
                if (candidate.isNotBlank()) return candidate
            }
        }
        return text
    }

    private fun extractTime(text: String): String? {
        val regex = Regex("(\\d{1,2})(:(\\d{2}))?\\s*(am|pm|এএম|পিএম|সকাল|সন্ধ্যা|রাত)?")
        val match = regex.find(text)
        return match?.value?.uppercase(Locale.ROOT)
    }

    private fun extractAmount(text: String): Double? {
        val regex = Regex("(\\d+(\\.\\d+)?)")
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }
}
