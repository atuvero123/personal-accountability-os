package com.arnold.accountabilityos

import kotlinx.serialization.Serializable

@Serializable
data class AppState(
    val days: Map<String, DayState> = emptyMap(),
    val finance: FinanceState = FinanceState(),
    val settings: SettingsState = SettingsState(),
    val growth: GrowthState = GrowthState()
)

@Serializable
data class SettingsState(
    val wakeTime:String="06:30",
    val sleepTime:String="23:00",
    val recreationalPhoneMinutes:Int=30
)

@Serializable
data class DayState(
    val date:String,
    val tasks:List<TaskItem> = emptyList(),
    val big3:List<String> = listOf("","",""),
    val bibleCompleted:Int=0,
    val prayerNote:String="",
    val scriptureEntries:List<ScriptureEntry> = emptyList(),
    val phonePenaltyMinutes:Int=0,
    val morningReview:MorningReview=MorningReview(),
    val eveningReview:EveningReview=EveningReview()
)

@Serializable
data class TaskItem(
    val id:String,
    val name:String,
    val start:String,
    val durationMinutes:Int,
    val area:String,
    val anchor:Boolean=false,
    val status:String="pending",
    val postponedCount:Int=0,
    val reason:String="",
    val completedAt:String="",
    val statusUpdatedAt:String=""
)

@Serializable
data class ScriptureEntry(
    val id:String,
    val time:String,
    val source:String,
    val reference:String,
    val verseText:String,
    val lesson:String,
    val application:String
)

@Serializable data class MorningReview(val mustDo:String="",val risk:String="",val adjustment:String="")
@Serializable data class EveningReview(val win:String="",val lostControl:String="",val postponed:String="",val tomorrowChange:String="")

@Serializable
data class DrummingWeek(
    val weekKey:String="",
    val skill:String="5-stroke roll",
    val techniqueNote:String="Learn the sticking, rebound, accents and clean execution.",
    val grooveApplication:String="Explore whether the rudiment can become a groove phrase; record useful groove ideas.",
    val fillApplication:String="Build short fills using the rudiment, then connect them back into time.",
    val skillProgress:Int=0,
    val learningTargetBpm:Int=60,
    val learningBpm:Int=0,
    val secureMinBpm:Int=70,
    val secureTargetBpm:Int=80,
    val secureBpm:Int=0,
    val grooveTarget:Int=3,
    val groovesCompleted:Int=0,
    val fillTarget:Int=5,
    val fillsCompleted:Int=0,
    val skillSong:String="",
    val skillSongApplied:Boolean=false,
    val skillSongResult:String="",
    val worshipSong:String="",
    val worshipSections:List<String> = listOf("Intro","Verse","Chorus","Bridge / transition","Ending","Full play-through"),
    val completedSections:List<String> = emptyList(),
    val skillCompleted:Boolean=false,
    val confidence:Int=0
)

@Serializable
data class CoffeeLessonRecord(
    val id:String,
    val lessonNumber:Int,
    val topic:String,
    val stage:String,
    val status:String,
    val date:String,
    val learnNotes:String,
    val practiceResult:String,
    val applicationResult:String
)

@Serializable
data class CoffeeCourseState(
    val currentStage:String="Foundations",
    val currentLesson:String="Coffee quality foundations — from cherry to cup",
    val lessonStatus:String="Planned",
    val roadmap:List<String> = listOf(
        "1. Foundations — what coffee quality means from cherry to cup",
        "2. Varieties, origins and altitude",
        "3. Cherry development, harvesting and selection",
        "4. Processing — washed, natural and honey",
        "5. Fermentation and drying",
        "6. Storage, transport and green coffee",
        "7. Green grading and physical defects",
        "8. Cupping and sensory foundations",
        "9. Roasting and roast development",
        "10. Brewing and extraction",
        "11. Quality control in coffee operations",
        "12. Advanced sensory practice and Q Grader preparation"
    ),
    val lessonNumber:Int=1,
    val learnNotes:String="",
    val practiceResult:String="",
    val applicationResult:String="",
    val history:List<CoffeeLessonRecord> = emptyList()
)

@Serializable
data class ExercisePlanState(
    val weekKey:String="",
    val weekNumber:Int=1,
    val phase:String="Beginner foundation",
    val currentDay:String="",
    val sessionMinutes:Int=15,
    val completedSessions:Int=0,
    val feedback:String="",
    val nextAdjustment:String="",
    val weeklyPlan:List<String> = listOf(
        "Mon • 2×5 push-ups • 2×10 bodyweight squats • 2×10 glute bridges • 2×15s plank • 5 min easy march/walk",
        "Tue • 10–15 min easy movement + mobility; no hard strength work",
        "Wed • 2×5 push-ups • 2×10 squats • 2×10 glute bridges • 2×15s plank • 5 min easy march/walk",
        "Thu • 10–15 min easy movement + mobility",
        "Fri • 2×5 push-ups • 2×10 squats • 2×10 glute bridges • 2×15s plank • 5 min easy march/walk",
        "Sat • 10–15 min easy walk/march + gentle mobility",
        "Sun • Recovery, stretching and weekly feedback"
    )
)

@Serializable
data class SkillTrack(
    val id:String,
    val name:String,
    val domain:String,
    val stage:String="Learning",
    val progress:Int=0,
    val goal:String="",
    val nextSkill:String="",
    val notes:String="",
    val completed:Boolean=false
)

@Serializable
data class DrummingMasteryRecord(
    val id:String,
    val skill:String,
    val date:String,
    val learningBpm:Int,
    val secureBpm:Int,
    val groovesCompleted:Int,
    val fillsCompleted:Int,
    val song:String,
    val songResult:String,
    val confidence:Int
)

@Serializable
data class DrummingPracticeRecord(
    val id:String,
    val date:String,
    val skill:String,
    val bpm:Int,
    val focus:String,
    val result:String,
    val nextAdjustment:String
)

@Serializable
data class WorshipSongRecord(
    val id:String,
    val weekKey:String,
    val song:String,
    val completedSections:List<String>,
    val date:String
)

@Serializable
data class GrowthState(
    val drumming:DrummingWeek=DrummingWeek(),
    val drummingHistory:List<String> = emptyList(),
    val drummingMasteryHistory:List<DrummingMasteryRecord> = emptyList(),
    val drummingPracticeHistory:List<DrummingPracticeRecord> = emptyList(),
    val worshipHistory:List<WorshipSongRecord> = emptyList(),
    val nextDrummingSkill:String="5-stroke roll",
    val coffee:CoffeeCourseState=CoffeeCourseState(),
    val exercise:ExercisePlanState=ExercisePlanState(),
    val otherSkills:List<SkillTrack> = emptyList(),
    val chatgptGrowthRecommendation:String=""
)

@Serializable
data class FinanceState(
    val accounts:List<AccountBalance> = listOf(
        AccountBalance("cash","Cash",0.0),
        AccountBalance("mobile","Mobile Money",0.0),
        AccountBalance("bank","Bank / Other",0.0)
    ),
    val transactions:List<FinanceTransaction> = emptyList(),
    val budgets:List<Budget> = listOf(
        Budget("food","Food",0.0),
        Budget("transport","Transport",0.0),
        Budget("data","Airtime/Data",0.0),
        Budget("personal","Personal",0.0),
        Budget("solm","Springs of Life",0.0),
        Budget("betting","Betting/Entertainment",0.0)
    ),
    val savingGoals:List<SavingGoal> = emptyList(),
    val balanceChecks:List<BalanceCheck> = emptyList()
)

@Serializable data class AccountBalance(val id:String,val name:String,val balance:Double)

@Serializable
data class FinanceTransaction(
    val id:String,
    val date:String,
    val time:String,
    val type:String,
    val category:String,
    val amount:Double,
    val description:String,
    val planned:Boolean,
    val accountId:String=""
)

@Serializable data class Budget(val id:String,val name:String,val monthlyLimit:Double)
@Serializable data class SavingGoal(val id:String,val title:String,val targetAmount:Double,val currentAmount:Double,val startDate:String,val targetDate:String,val frequency:String,val notes:String="")
@Serializable data class BalanceCheck(val id:String,val date:String,val time:String,val period:String,val total:Double,val note:String)
data class UsageRow(val packageName:String,val appName:String,val minutes:Long)
