package com.arnold.accountabilityos

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.math.ceil
import kotlin.math.max

class AppViewModel(app: Application):AndroidViewModel(app){
    private val store=AppStore(app)
    private val usage=UsageStatsRepository(app)
    var state by mutableStateOf(store.load()); private set
    var usageRows by mutableStateOf<List<UsageRow>>(emptyList()); private set

    init{ensureToday();ensureGrowthWeek();refreshUsage()}

    fun todayKey()=LocalDate.now().toString()
    fun today():DayState{ensureToday();return state.days[todayKey()]!!}
    private fun save(s:AppState){state=s;store.save(s)}
    private fun updateDay(f:(DayState)->DayState){
        val d=f(today())
        save(state.copy(days=state.days+(d.date to d)))
    }

    private fun ensureToday(){
        val k=todayKey()
        if(!state.days.containsKey(k))save(state.copy(days=state.days+(k to DayState(k,defaultTasks()))))
    }

    private fun weekKey():String{
        val d=LocalDate.now()
        val week=d.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear())
        val year=d.get(java.time.temporal.WeekFields.ISO.weekBasedYear())
        return "%04d-W%02d".format(year,week)
    }

    private fun ensureGrowthWeek(){
        val key=weekKey()
        if(state.growth.drumming.weekKey!=key){
            save(state.copy(growth=state.growth.copy(
                drumming=state.growth.drumming.copy(
                    weekKey=key,
                    skill=state.growth.nextDrummingSkill,
                    skillProgress=0,
                    completedSections=emptyList(),
                    skillCompleted=false,
                    confidence=0
                ),
                exercise=state.growth.exercise.copy(
                    weekKey=key,
                    completedSessions=0,
                    currentDay="",
                    feedback="",
                    nextAdjustment=""
                )
            )))
        }
    }

    private fun updateGrowth(f:(GrowthState)->GrowthState){
        ensureGrowthWeek()
        save(state.copy(growth=f(state.growth)))
    }

    fun drummingCriteriaComplete():Boolean{
        val d=state.growth.drumming
        return d.learningBpm>=d.learningTargetBpm &&
            d.secureBpm>=d.secureTargetBpm &&
            d.groovesCompleted>=d.grooveTarget &&
            d.fillsCompleted>=d.fillTarget &&
            d.skillSongApplied
    }

    fun drummingCriteriaProgress():Int{
        val d=state.growth.drumming
        var done=0
        if(d.learningBpm>=d.learningTargetBpm)done++
        if(d.secureBpm>=d.secureTargetBpm)done++
        if(d.groovesCompleted>=d.grooveTarget)done++
        if(d.fillsCompleted>=d.fillTarget)done++
        if(d.skillSongApplied)done++
        return done
    }

    fun drummingProgressLabel():String{
        val d=state.growth.drumming
        if(d.skillCompleted)return "5 • Mastered"
        return when(drummingCriteriaProgress()){
            0->"1 • Learn / technique"
            1->"2 • Technique secure"
            2->"3 • Groove application"
            3->"4 • Fills"
            else->"5 • Song application / ready"
        }
    }

    fun drummingMissingCriteria():List<String>{
        val d=state.growth.drumming
        val missing=mutableListOf<String>()
        if(d.learningBpm<d.learningTargetBpm)missing.add("Learning: reach ${d.learningTargetBpm} BPM cleanly")
        if(d.secureBpm<d.secureTargetBpm)missing.add("Secure: reach ${d.secureTargetBpm} BPM cleanly")
        if(d.groovesCompleted<d.grooveTarget)missing.add("Groove: ${d.groovesCompleted}/${d.grooveTarget} applications")
        if(d.fillsCompleted<d.fillTarget)missing.add("Fills: ${d.fillsCompleted}/${d.fillTarget} variations")
        if(!d.skillSongApplied)missing.add("Song: apply the skill in a selected song")
        return missing
    }

    fun setDrummingCriteria(
        learningBpm:Int,
        secureBpm:Int,
        grooves:Int,
        fills:Int,
        song:String,
        songApplied:Boolean,
        songResult:String
    )=updateGrowth{g->
        g.copy(drumming=g.drumming.copy(
            learningBpm=learningBpm.coerceAtLeast(0),
            secureBpm=secureBpm.coerceAtLeast(0),
            groovesCompleted=grooves.coerceAtLeast(0),
            fillsCompleted=fills.coerceAtLeast(0),
            skillSong=song,
            skillSongApplied=songApplied,
            skillSongResult=songResult
        ))
    }

    fun setDrummingTargets(
        learningTarget:Int,
        secureMin:Int,
        secureTarget:Int,
        grooveTarget:Int,
        fillTarget:Int
    )=updateGrowth{g->
        g.copy(drumming=g.drumming.copy(
            learningTargetBpm=learningTarget.coerceAtLeast(1),
            secureMinBpm=secureMin.coerceAtLeast(1),
            secureTargetBpm=secureTarget.coerceAtLeast(1),
            grooveTarget=grooveTarget.coerceAtLeast(1),
            fillTarget=fillTarget.coerceAtLeast(1)
        ))
    }

    fun setDrummingProgress(v:Int)=updateGrowth{
        it.copy(drumming=it.drumming.copy(skillProgress=v.coerceIn(0,4)))
    }

    fun setDrummingConfidence(v:Int)=updateGrowth{
        it.copy(drumming=it.drumming.copy(confidence=v.coerceIn(0,5)))
    }

    fun startNextDrummingSkill()=updateGrowth{g->
        val next=g.nextDrummingSkill
        g.copy(drumming=DrummingWeek(
            weekKey=g.drumming.weekKey,
            skill=next
        ))
    }

    fun completeDrummingSkill(){
        if(!drummingCriteriaComplete())return
        updateGrowth{
            val current=it.drumming.skill
            val next=when(current){
                "5-stroke roll"->"Single paradiddle"
                "Single paradiddle"->"6-stroke roll"
                "6-stroke roll"->"Double paradiddle"
                "Double paradiddle"->"Flam taps"
                else->"Next skill — review with ChatGPT"
            }
            it.copy(
                drumming=it.drumming.copy(skillCompleted=true,skillProgress=4),
                drummingHistory=(it.drummingHistory+current).distinct(),
                nextDrummingSkill=next
            )
        }
    }

    fun setWorshipSong(v:String)=updateGrowth{it.copy(drumming=it.drumming.copy(worshipSong=v))}

    fun toggleWorshipSection(section:String)=updateGrowth{g->
        val done=g.drumming.completedSections.toMutableList()
        if(done.contains(section))done.remove(section)else done.add(section)
        g.copy(drumming=g.drumming.copy(completedSections=done))
    }

    fun setDrummingNotes(technique:String,groove:String,fill:String)=updateGrowth{g->
        g.copy(drumming=g.drumming.copy(
            techniqueNote=technique,
            grooveApplication=groove,
            fillApplication=fill
        ))
    }

    fun setCoffeeStatus(status:String)=updateGrowth{g->g.copy(coffee=g.coffee.copy(lessonStatus=status))}

    fun setCoffeeNotes(learn:String,practice:String,application:String)=updateGrowth{g->
        g.copy(coffee=g.coffee.copy(
            learnNotes=learn,
            practiceResult=practice,
            applicationResult=application
        ))
    }

    fun saveCoffeeLesson(
        learn:String,
        practice:String,
        application:String
    ):Boolean{
        if(learn.isBlank() && practice.isBlank() && application.isBlank())return false
        val current=state.growth.coffee
        val record=CoffeeLessonRecord(
            id=UUID.randomUUID().toString(),
            lessonNumber=current.lessonNumber,
            topic=current.currentLesson,
            stage=current.currentStage,
            status=current.lessonStatus,
            date=todayKey(),
            learnNotes=learn.trim(),
            practiceResult=practice.trim(),
            applicationResult=application.trim()
        )
        updateGrowth{g->
            val c=g.coffee
            val nextIndex=c.lessonNumber
            if(nextIndex<c.roadmap.size){
                val nextTopic=c.roadmap[nextIndex].substringAfter(". ")
                g.copy(coffee=c.copy(
                    currentLesson=nextTopic,
                    lessonNumber=c.lessonNumber+1,
                    lessonStatus="Planned",
                    learnNotes="",
                    practiceResult="",
                    applicationResult="",
                    history=c.history+record
                ))
            }else{
                g.copy(coffee=c.copy(
                    lessonStatus="Reviewed",
                    learnNotes="",
                    practiceResult="",
                    applicationResult="",
                    history=c.history+record
                ))
            }
        }
        return true
    }

    fun coffeeHistory(): List<CoffeeLessonRecord> = state.growth.coffee.history

    fun coffeeStudyMethod()="Learn → Practice → Apply → Review. Save each completed topic to its own lesson record; the form then resets for the next topic."

    fun exerciseToday():String{
        val day=LocalDate.now().dayOfWeek
        val index=day.value-1
        return state.growth.exercise.weeklyPlan.getOrElse(index){state.growth.exercise.weeklyPlan.first()}
    }

    fun completeExerciseSession(feedback:String)=updateGrowth{g->
        g.copy(exercise=g.exercise.copy(
            currentDay=LocalDate.now().toString(),
            completedSessions=g.exercise.completedSessions+1,
            feedback=feedback,
            nextAdjustment=exerciseAdjustment(g.exercise.completedSessions+1,feedback)
        ))
    }

    fun saveExerciseFeedback(feedback:String)=updateGrowth{g->
        g.copy(exercise=g.exercise.copy(
            currentDay=LocalDate.now().toString(),
            feedback=feedback,
            nextAdjustment=exerciseAdjustment(g.exercise.completedSessions,feedback)
        ))
    }

    private fun exerciseAdjustment(completed:Int,feedback:String):String{
        val f=feedback.lowercase()
        return when{
            f.contains("too hard")||f.contains("pain")||f.contains("difficult")->
                "Reduce the next session. Keep the movement controlled; do not force painful exercise."
            completed>=3&&(!f.contains("hard")&&!f.contains("difficult"))->
                "Progress gradually next week: add 1–2 reps to strength movements or a small amount of time."
            else->"Stay with the beginner plan until the movements feel controlled and repeatable."
        }
    }

    fun setChatgptGrowthRecommendation(v:String)=updateGrowth{it.copy(chatgptGrowthRecommendation=v)}

    fun addSkill(name:String,domain:String,goal:String,nextSkill:String)=updateGrowth{g->
        if(name.isBlank())g else g.copy(otherSkills=g.otherSkills+SkillTrack(
            UUID.randomUUID().toString(),name.trim(),domain.trim(),goal=goal.trim(),nextSkill=nextSkill.trim()
        ))
    }

    fun updateSkillProgress(id:String,progress:Int,stage:String,notes:String)=updateGrowth{g->
        g.copy(otherSkills=g.otherSkills.map{
            if(it.id==id)it.copy(progress=progress.coerceIn(0,100),stage=stage,notes=notes)else it
        })
    }

    fun completeSkill(id:String)=updateGrowth{g->
        g.copy(otherSkills=g.otherSkills.map{
            if(it.id==id)it.copy(completed=true,progress=100,stage="Completed")else it
        })
    }

    fun prayerTopic()=when(LocalDate.now().dayOfWeek){
        DayOfWeek.MONDAY->"Personal alignment — relationship with God, discipline, habits, character and self-control."
        DayOfWeek.TUESDAY->"Family & relationships — family, friendships and people close to you."
        DayOfWeek.WEDNESDAY->"Work & finances — coffee operations, stewardship, provision and wise decisions."
        DayOfWeek.THURSDAY->"Springs of Life Ministry — members, leaders, choir, spiritual growth and direction."
        DayOfWeek.FRIDAY->"Purpose & future — education, career, projects, decisions and opportunities."
        DayOfWeek.SATURDAY->"Intercession — friends, church members, people struggling, community and country."
        DayOfWeek.SUNDAY->"Thanksgiving & reflection — answered prayer, lessons, failures, victories and the coming week."
    }

    fun setBig3(i:Int,v:String)=updateDay{d->
        d.copy(big3=d.big3.toMutableList().also{it[i]=v})
    }

    fun setPrayerNote(v:String)=updateDay{it.copy(prayerNote=v)}
    fun setBibleCompleted(v:Int)=updateDay{it.copy(bibleCompleted=v.coerceIn(0,5))}

    fun addScripture(source:String,reference:String,verse:String,lesson:String,application:String){
        val e=ScriptureEntry(UUID.randomUUID().toString(),LocalTime.now().toString(),source,reference,verse,lesson,application)
        updateDay{it.copy(scriptureEntries=it.scriptureEntries+e)}
    }

    fun updateTaskStatus(id:String,status:String,reason:String=""){
        updateDay{d->d.copy(tasks=d.tasks.map{
            if(it.id==id)it.copy(
                status=status,
                reason=reason,
                completedAt=if(status=="done")LocalTime.now().toString() else it.completedAt,
                statusUpdatedAt=LocalTime.now().toString()
            )else it
        })}
    }

    fun postponeTask(id:String,time:String){
        updateDay{d->d.copy(tasks=d.tasks.map{
            if(it.id==id)it.copy(
                start=time,
                status="pending",
                reason="",
                statusUpdatedAt=LocalTime.now().toString(),
                postponedCount=it.postponedCount+1
            )else it
        })}
    }

    fun addTask(name:String,start:String,duration:Int,area:String,anchor:Boolean){
        if(name.isNotBlank())updateDay{it.copy(tasks=it.tasks+TaskItem(UUID.randomUUID().toString(),name,start,duration,area,anchor))}
    }

    fun addPhonePenalty(min:Int)=updateDay{it.copy(phonePenaltyMinutes=it.phonePenaltyMinutes+max(0,min))}
    fun remainingPhoneAllowance()=max(0,state.settings.recreationalPhoneMinutes-today().phonePenaltyMinutes)
    fun setPhoneAllowance(v:Int)=save(state.copy(settings=state.settings.copy(recreationalPhoneMinutes=max(0,v))))

    fun hasUsageAccess()=usage.hasUsageAccess()
    fun openUsageSettings()=usage.openUsageAccessSettings()
    fun refreshUsage(){usageRows=usage.todayUsage()}
    fun socialMinutes()=usage.likelySocialMinutes(usageRows)

    fun updateMorningReview(v:MorningReview)=updateDay{it.copy(morningReview=v)}
    fun updateEveningReview(v:EveningReview)=updateDay{it.copy(eveningReview=v)}

    fun updateAccount(id:String,balance:Double){
        save(state.copy(finance=state.finance.copy(
            accounts=state.finance.accounts.map{if(it.id==id)it.copy(balance=balance)else it}
        )))
    }

    fun accountTotal()=state.finance.accounts.sumOf{it.balance}

    fun addBalanceCheck(period:String,note:String){
        val c=BalanceCheck(UUID.randomUUID().toString(),todayKey(),LocalTime.now().toString(),period,accountTotal(),note)
        save(state.copy(finance=state.finance.copy(balanceChecks=state.finance.balanceChecks+c)))
    }

    fun addTransaction(type:String,category:String,amount:Double,description:String,planned:Boolean,accountId:String){
        if(amount<=0)return
        val account=state.finance.accounts.firstOrNull{it.id==accountId} ?: state.finance.accounts.firstOrNull()
        val selectedId=account?.id ?: ""
        val t=FinanceTransaction(
            UUID.randomUUID().toString(),todayKey(),LocalTime.now().toString(),
            type,category.ifBlank{"Uncategorised"},amount,description,planned,selectedId
        )
        val newAccounts=state.finance.accounts.map{
            if(it.id==selectedId){
                val delta=if(type=="Expense")-amount else amount
                it.copy(balance=it.balance+delta)
            }else it
        }
        save(state.copy(finance=state.finance.copy(
            accounts=newAccounts,
            transactions=state.finance.transactions+t
        )))
        if(type=="Expense"&&!planned)NotificationHelper.notify(
            getApplication(),"Unplanned spending",ugx(amount)+" in "+t.category,7101
        )
        val b=state.finance.budgets.firstOrNull{it.name.equals(t.category,true)&&it.monthlyLimit>0}
        if(type=="Expense"&&b!=null&&spentThisMonth(b.name)>=b.monthlyLimit*.8)NotificationHelper.notify(
            getApplication(),"Budget warning",
            b.name+" is at "+(spentThisMonth(b.name)/b.monthlyLimit*100).toInt()+"%.",7102
        )
    }

    fun monthIncome()=state.finance.transactions.filter{
        it.type=="Income"&&it.date.startsWith(todayKey().substring(0,7))
    }.sumOf{it.amount}

    fun monthExpenses()=state.finance.transactions.filter{
        it.type=="Expense"&&it.date.startsWith(todayKey().substring(0,7))
    }.sumOf{it.amount}

    fun todayIncome()=state.finance.transactions.filter{
        it.type=="Income"&&it.date==todayKey()
    }.sumOf{it.amount}

    fun todayExpenses()=state.finance.transactions.filter{
        it.type=="Expense"&&it.date==todayKey()
    }.sumOf{it.amount}

    fun recentTransactions(limit:Int=8)=state.finance.transactions.asReversed().take(limit)

    fun setBudget(name:String,limit:Double){
        if(name.isBlank())return
        val id=name.trim().lowercase().replace(" ","-")
        val bs=if(state.finance.budgets.any{it.id==id})
            state.finance.budgets.map{if(it.id==id)it.copy(name=name,monthlyLimit=limit)else it}
        else state.finance.budgets+Budget(id,name,limit)
        save(state.copy(finance=state.finance.copy(budgets=bs)))
    }

    fun addSavingGoal(title:String,target:Double,current:Double,date:String,frequency:String,notes:String){
        if(title.isBlank()||target<=0)return
        val g=SavingGoal(UUID.randomUUID().toString(),title,target,max(0.0,current),todayKey(),date,frequency,notes)
        save(state.copy(finance=state.finance.copy(savingGoals=state.finance.savingGoals+g)))
    }

    fun contributeToGoal(id:String,amount:Double){
        val g=state.finance.savingGoals.firstOrNull{it.id==id}?:return
        val a=amount.coerceIn(0.0,g.targetAmount-g.currentAmount)
        if(a<=0)return
        val gs=state.finance.savingGoals.map{if(it.id==id)it.copy(currentAmount=it.currentAmount+a)else it}
        val t=FinanceTransaction(
            UUID.randomUUID().toString(),todayKey(),LocalTime.now().toString(),
            "Savings",g.title,a,"Contribution to "+g.title,true,""
        )
        save(state.copy(finance=state.finance.copy(
            savingGoals=gs,
            transactions=state.finance.transactions+t
        )))
    }

    fun spentThisMonth(category:String?=null)=state.finance.transactions.filter{
        it.type=="Expense"&&it.date.startsWith(todayKey().substring(0,7))&&
        (category==null||it.category.equals(category,true))
    }.sumOf{it.amount}

    fun financeDiagnostics():List<String>{
        val out=mutableListOf<String>()
        val month=todayKey().substring(0,7)
        val day=LocalDate.now()
        val progress=day.dayOfMonth.toDouble()/day.lengthOfMonth()
        state.finance.budgets.filter{it.monthlyLimit>0}.forEach{b->
            val s=spentThisMonth(b.name)
            val r=s/b.monthlyLimit
            if(r>=1)out+="${b.name}: budget exceeded by "+ugx(s-b.monthlyLimit)
            else if(r>=.8)out+="${b.name}: "+(r*100).toInt()+"% of monthly budget used."
            else if(r>progress+.15)out+="${b.name}: spending is ahead of the month."
        }
        val week=day.minusDays(6)
        val unplanned=state.finance.transactions.filter{it.type=="Expense"&&!it.planned}
            .filter{runCatching{LocalDate.parse(it.date)}.getOrNull()?.let{d->!d.isBefore(week)&&!d.isAfter(day)}==true}
            .sumOf{it.amount}
        if(unplanned>0)out+="Last 7 days: "+ugx(unplanned)+" was unplanned spending."
        val checks=state.finance.balanceChecks.filter{it.date==todayKey()}
        if(checks.none{it.period=="Morning"})out+="Morning financial check-in has not been recorded today."
        if(LocalTime.now().hour>=18&&checks.none{it.period=="Evening"})out+="Evening financial check-in is outstanding."
        state.finance.savingGoals.forEach{savingDiagnostic(it)?.let(out::add)}
        return if(out.isEmpty())listOf("No finance warning is active. Keep recording consistently.")else out
    }

    fun savingRequiredPerPeriod(g:SavingGoal):Double{
        val target=runCatching{LocalDate.parse(g.targetDate)}.getOrNull()?:return 0.0
        val days=max(1,ChronoUnit.DAYS.between(LocalDate.now(),target).toInt())
        val periods=when(g.frequency){
            "Daily"->days
            "Weekly"->max(1,ceil(days/7.0).toInt())
            else->max(1,ceil(days/30.44).toInt())
        }
        return max(0.0,g.targetAmount-g.currentAmount)/periods
    }

    fun savingDiagnostic(g:SavingGoal):String?{
        val start=runCatching{LocalDate.parse(g.startDate)}.getOrNull()?:return null
        val target=runCatching{LocalDate.parse(g.targetDate)}.getOrNull()?:return null
        if(!target.isAfter(start))return g.title+": choose a future target date."
        if(g.currentAmount>=g.targetAmount)return g.title+": goal reached."
        val total=ChronoUnit.DAYS.between(start,target).coerceAtLeast(1)
        val elapsed=ChronoUnit.DAYS.between(start,LocalDate.now()).coerceIn(0,total)
        val expected=g.targetAmount*(elapsed.toDouble()/total)
        val diff=g.currentAmount-expected
        return when{
            diff< -g.targetAmount*.05->g.title+": behind plan by "+ugx(-diff)+". Suggested "+ugx(savingRequiredPerPeriod(g))+" "+g.frequency.lowercase()+"."
            diff>g.targetAmount*.05->g.title+": ahead of plan."
            else->g.title+": on track. Suggested "+ugx(savingRequiredPerPeriod(g))+" "+g.frequency.lowercase()+"."
        }
    }

    fun accountabilityScore():Int{
        val a=today().tasks.filter{it.anchor}
        return if(a.isEmpty())0 else (a.count{it.status=="done"}*100/a.size)
    }

    fun personalDiagnostics():List<String>{
        val d=today()
        val out=mutableListOf<String>()
        val p=d.tasks.sumOf{it.postponedCount}
        val missed=d.tasks.count{it.anchor&&it.status=="missed"}
        if(p>=3)out+="Postponement signal: "+p+" reschedules today."
        if(missed>0)out+=missed.toString()+" daily anchor(s) are missed. Record why."
        if(socialMinutes()>state.settings.recreationalPhoneMinutes)
            out+="Likely social/video use is "+socialMinutes()+" min against "+state.settings.recreationalPhoneMinutes+" min allowance."
        if(d.bibleCompleted<3&&LocalTime.now().hour>=12)out+="Bible progress is "+d.bibleCompleted+"/5 chapters."
        return if(out.isEmpty())listOf("No major rehabilitation warning is active right now.")else out
    }

    fun dailyReport():String{
        val d=today()
        val done=d.tasks.count{it.status=="done"}
        val missed=d.tasks.count{it.status=="missed"}
        val post=d.tasks.sumOf{it.postponedCount}
        return listOf(
            "Accountability OS — "+todayKey(),
            "Score: "+accountabilityScore()+"% | Done: "+done+" | Missed: "+missed+" | Postponements: "+post,
            "Prayer: "+prayerTopic(),
            "Bible: "+d.bibleCompleted+"/5",
            "Likely social/video: "+socialMinutes()+" min | Allowance remaining: "+remainingPhoneAllowance()+" min",
            "Big 3: "+d.big3.filter{it.isNotBlank()}.joinToString(" | ").ifBlank{"Not set"},
            "Unplanned spending today: "+ugx(state.finance.transactions.filter{
                it.date==todayKey()&&it.type=="Expense"&&!it.planned
            }.sumOf{it.amount}),
            "Exercise: "+state.growth.exercise.completedSessions+" sessions logged this week.",
            "Drumming: "+state.growth.drumming.skill+" • "+drummingProgressLabel()+" • confidence "+state.growth.drumming.confidence+"/5"
        ).joinToString("\n")
    }

    fun growthBrief():String{
        val g=state.growth
        val d=g.drumming
        return listOf(
            "Personal Growth Brief — "+todayKey(),
            "Life goals: spiritual maturity, physical fitness, drumming mastery, coffee quality, work/career growth and financial stability.",
            "Exercise: week "+g.exercise.weekNumber+" • "+g.exercise.completedSessions+" sessions completed. Feedback: "+g.exercise.feedback.ifBlank{"none"},
            "Drumming: "+d.skill+" • "+drummingProgressLabel()+" • evidence "+drummingCriteriaProgress()+"/5 • confidence "+d.confidence+"/5",
            "Drumming evidence: Learn "+d.learningBpm+"/"+d.learningTargetBpm+" BPM; Secure "+d.secureBpm+"/"+d.secureTargetBpm+" BPM; Groove "+d.groovesCompleted+"/"+d.grooveTarget+"; Fills "+d.fillsCompleted+"/"+d.fillTarget+"; Song applied="+d.skillSongApplied,
            "Mastered drumming history: "+g.drummingHistory.joinToString(" → ").ifBlank{"none"},
            "Coffee: current lesson "+g.coffee.lessonNumber+" — "+g.coffee.currentLesson+" • status "+g.coffee.lessonStatus+" • saved lessons "+g.coffee.history.size,
            "Other skills: "+g.otherSkills.joinToString("; "){it.name+" ("+it.progress+"%, "+it.stage+")"}.ifBlank{"none"},
            "Previous ChatGPT growth recommendation: "+g.chatgptGrowthRecommendation.ifBlank{"none"}
        ).joinToString("\n")
    }

    private fun defaultTasks()=listOf(
        TaskItem("wake","Wake up — no scrolling","06:30",5,"Personal",true),
        TaskItem("exercise","Exercise / movement","06:35",20,"Health",true),
        TaskItem("laundry","Wash one pair of clothes + quick tidy","06:55",15,"Personal",true),
        TaskItem("shower-am","Shower + personal hygiene","07:10",20,"Personal",true),
        TaskItem("prayer","Prayer + Bible (3 chapters)","07:30",30,"Spiritual",true),
        TaskItem("plan","Morning review + Big 3","08:00",10,"Personal",true),
        TaskItem("breakfast","Breakfast / prepare for work","08:10",35,"Personal"),
        TaskItem("betslips","Daily betslip making — morning hour","08:45",60,"Betslips",true),
        TaskItem("work-open","Coffee station opening scan / work","09:45",15,"Work",true),
        TaskItem("finance-am","Finance morning check-in","10:00",5,"Finance",true),
        TaskItem("lunch","Lunch / selected sermon or quiet","13:00",30,"Spiritual"),
        TaskItem("work-close","Work closing review","17:15",10,"Work",true),
        TaskItem("rest","Rest / meal / decompress","17:30",30,"Rest"),
        TaskItem("drumming","Drumming — weekly skill + worship song","18:00",45,"Drumming"),
        TaskItem("focus","Main focus — app development or SOLM","18:50",60,"Projects"),
        TaskItem("oracle","Project Oracle development","19:55",40,"Project Oracle"),
        TaskItem("entertainment","Planned entertainment / casual time","20:35",30,"Rest"),
        TaskItem("bible-pm","Bible reading (2 chapters)","21:05",25,"Spiritual",true),
        TaskItem("finance-pm","Finance evening check-in","21:30",5,"Finance",true),
        TaskItem("review","Personal evening review + tomorrow","21:35",20,"Personal",true),
        TaskItem("shower-pm","Shower + prepare clothes / room","21:55",20,"Personal",true),
        TaskItem("quiet","Quiet prayer / no feeds","22:15",45,"Spiritual",true),
        TaskItem("sleep","Sleep","23:00",0,"Health",true)
    )
}

fun ugx(v:Double)="UGX "+"%,.0f".format(v)
