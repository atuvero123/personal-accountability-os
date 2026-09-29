package com.arnold.accountabilityos

import kotlinx.serialization.Serializable

@Serializable
data class AppState(val days: Map<String, DayState> = emptyMap(), val finance: FinanceState = FinanceState(), val settings: SettingsState = SettingsState())
@Serializable
data class SettingsState(val wakeTime:String="06:30", val sleepTime:String="23:00", val recreationalPhoneMinutes:Int=30)
@Serializable
data class DayState(val date:String, val tasks:List<TaskItem> = emptyList(), val big3:List<String> = listOf("","",""), val bibleCompleted:Int=0, val prayerNote:String="", val scriptureEntries:List<ScriptureEntry> = emptyList(), val phonePenaltyMinutes:Int=0, val morningReview:MorningReview=MorningReview(), val eveningReview:EveningReview=EveningReview())
@Serializable
data class TaskItem(val id:String,val name:String,val start:String,val durationMinutes:Int,val area:String,val anchor:Boolean=false,val status:String="pending",val postponedCount:Int=0,val reason:String="",val completedAt:String="")
@Serializable
data class ScriptureEntry(val id:String,val time:String,val source:String,val reference:String,val verseText:String,val lesson:String,val application:String)
@Serializable data class MorningReview(val mustDo:String="",val risk:String="",val adjustment:String="")
@Serializable data class EveningReview(val win:String="",val lostControl:String="",val postponed:String="",val tomorrowChange:String="")
@Serializable
data class FinanceState(
    val accounts:List<AccountBalance> = listOf(AccountBalance("cash","Cash",0.0),AccountBalance("mobile","Mobile Money",0.0),AccountBalance("bank","Bank / Other",0.0)),
    val transactions:List<FinanceTransaction> = emptyList(),
    val budgets:List<Budget> = listOf(Budget("food","Food",0.0),Budget("transport","Transport",0.0),Budget("data","Airtime/Data",0.0),Budget("personal","Personal",0.0),Budget("solm","Springs of Life",0.0),Budget("betting","Betting/Entertainment",0.0)),
    val savingGoals:List<SavingGoal> = emptyList(), val balanceChecks:List<BalanceCheck> = emptyList()
)
@Serializable data class AccountBalance(val id:String,val name:String,val balance:Double)
@Serializable data class FinanceTransaction(val id:String,val date:String,val time:String,val type:String,val category:String,val amount:Double,val description:String,val planned:Boolean)
@Serializable data class Budget(val id:String,val name:String,val monthlyLimit:Double)
@Serializable data class SavingGoal(val id:String,val title:String,val targetAmount:Double,val currentAmount:Double,val startDate:String,val targetDate:String,val frequency:String,val notes:String="")
@Serializable data class BalanceCheck(val id:String,val date:String,val time:String,val period:String,val total:Double,val note:String)
data class UsageRow(val packageName:String,val appName:String,val minutes:Long)
