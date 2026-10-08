package com.arnold.accountabilityos

import android.Manifest
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity:ComponentActivity(){
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        if(android.os.Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        ReminderScheduler.scheduleAll(this)
        setContent{AccountabilityApp()}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountabilityApp(vm:AppViewModel=viewModel()){
    var tab by remember{mutableIntStateOf(0)}
    val titles=listOf("Today","Spiritual","Focus","Finance","Growth","Review")
    Scaffold(
        topBar={TopAppBar(title={Text("Accountability OS • "+titles[tab])})},
        bottomBar={
            NavigationBar{
                titles.forEachIndexed{i,t->
                    NavigationBarItem(
                        selected=tab==i,
                        onClick={tab=i},
                        icon={Text(listOf("✓","✦","◉","₵","♪","↻")[i])},
                        label={Text(t)}
                    )
                }
            }
        }
    ){pad->
        Box(Modifier.padding(pad).fillMaxSize()){
            when(tab){
                0->TodayScreen(vm)
                1->SpiritualScreen(vm)
                2->FocusScreen(vm)
                3->FinanceScreen(vm)
                4->GrowthScreen(vm)
                5->ReviewScreen(vm)
            }
        }
    }
}

@Composable
fun SectionCard(title:String,body:@Composable ColumnScope.()->Unit){
    Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){
        Column(Modifier.padding(14.dp)){
            Text(title,style=MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            body()
        }
    }
}

@Composable
fun TodayScreen(vm:AppViewModel){
    val d=vm.today()
    var showMissed by remember{mutableStateOf<String?>(null)}
    var missedReason by remember{mutableStateOf("")}
    val done=d.tasks.count{it.status=="done"}
    val postponed=d.tasks.sumOf{it.postponedCount}
    val missed=d.tasks.count{it.status=="missed"}
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=12.dp)){
        item{
            SectionCard("What happened today"){
                Text("Anchor score: "+vm.accountabilityScore()+"%")
                Text("Done: $done  •  Postponed: $postponed  •  Missed: $missed")
                Text("The buttons below record a real outcome; they do not just change the colour of a task.")
                Spacer(Modifier.height(6.dp))
                d.big3.forEachIndexed{i,v->
                    OutlinedTextField(
                        v,{vm.setBig3(i,it)},
                        Modifier.fillMaxWidth().padding(vertical=2.dp),
                        label={Text("Big 3 #"+(i+1))},singleLine=true
                    )
                }
            }
        }
        item{SectionCard("Today's prayer focus"){Text(vm.prayerTopic())}}
        items(d.tasks.sortedBy{it.start},key={it.id}){task->
            Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){
                Column(Modifier.padding(10.dp)){
                    Text(task.start+"  •  "+task.name,style=MaterialTheme.typography.titleSmall)
                    Text(task.area+(if(task.anchor)"  • anchor" else ""))
                    when(task.status){
                        "done"->Text("✓ COMPLETED at "+task.completedAt)
                        "missed"->Text("✕ MISSED • "+task.reason.ifBlank{"No reason recorded"})
                        else->if(task.postponedCount>0)
                            Text("↪ POSTPONED "+task.postponedCount+"× • now scheduled "+task.start)
                    }
                    if(task.statusUpdatedAt.isNotBlank()&&task.status!="pending")
                        Text("Last action recorded at "+task.statusUpdatedAt.take(8))
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(top=6.dp)){
                        Button(
                            onClick={vm.updateTaskStatus(task.id,"done")},
                            enabled=task.status!="done"
                        ){Text("Done")}
                        OutlinedButton(
                            onClick={vm.postponeTask(task.id,plus30(task.start))},
                            enabled=task.status!="done"&&task.status!="missed"
                        ){Text("Move +30m")}
                        OutlinedButton(
                            onClick={showMissed=task.id},
                            enabled=task.status!="done"&&task.status!="missed"
                        ){Text("Missed")}
                    }
                }
            }
        }
    }
    if(showMissed!=null){
        AlertDialog(
            onDismissRequest={showMissed=null},
            title={Text("Why was this missed?")},
            text={
                OutlinedTextField(
                    missedReason,{missedReason=it},
                    Modifier.fillMaxWidth(),
                    label={Text("Reason")}
                )
            },
            confirmButton={
                Button(
                    enabled=missedReason.isNotBlank(),
                    onClick={
                        vm.updateTaskStatus(showMissed!!,"missed",missedReason.trim())
                        missedReason=""
                        showMissed=null
                    }
                ){Text("Record missed")}
            },
            dismissButton={TextButton(onClick={showMissed=null}){Text("Cancel")}}
        )
    }
}

private fun plus30(value:String):String=runCatching{
    LocalTime.parse(value).plusMinutes(30).format(DateTimeFormatter.ofPattern("HH:mm"))
}.getOrDefault(value)

@Composable
fun SpiritualScreen(vm:AppViewModel){
    val d=vm.today()
    var source by remember{mutableStateOf("Me")}
    var ref by remember{mutableStateOf("")}
    var lesson by remember{mutableStateOf("")}
    var application by remember{mutableStateOf("")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)){
        SectionCard("Prayer focus"){
            Text(vm.prayerTopic())
            OutlinedTextField(d.prayerNote,vm::setPrayerNote,Modifier.fillMaxWidth(),label={Text("Prayer notes")})
        }
        SectionCard("Bible — target 5 chapters"){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                repeat(5){i->
                    FilterChip(
                        selected=d.bibleCompleted>i,
                        onClick={vm.setBibleCompleted(if(d.bibleCompleted==i+1)i else i+1)},
                        label={Text((i+1).toString())}
                    )
                }
            }
            Text("Progress: "+d.bibleCompleted+"/5")
        }
        SectionCard("Scripture sharing journal"){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                FilterChip(source=="Me",{source="Me"},label={Text("My sharing")})
                FilterChip(source=="Friend",{source="Friend"},label={Text("Friend's sharing")})
            }
            OutlinedTextField(ref,{ref=it},Modifier.fillMaxWidth(),label={Text("Bible reference")})
            OutlinedTextField(lesson,{lesson=it},Modifier.fillMaxWidth(),label={Text("What I learned")})
            OutlinedTextField(application,{application=it},Modifier.fillMaxWidth(),label={Text("Personal application")})
            Button(onClick={
                vm.addScripture(source,ref,"",lesson,application)
                ref="";lesson="";application=""
            }){Text("Save reflection")}
            d.scriptureEntries.takeLast(5).reversed().forEach{
                Text(it.source+" • "+it.reference+" — "+it.lesson,Modifier.padding(top=8.dp))
            }
        }
    }
}

@Composable
fun FocusScreen(vm:AppViewModel){
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)){
        SectionCard("Phone accountability"){
            Text("Recreational allowance: "+vm.state.settings.recreationalPhoneMinutes+" min")
            Text("Remaining self-reported allowance: "+vm.remainingPhoneAllowance()+" min")
            Text("Likely social/video usage today: "+vm.socialMinutes()+" min")
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=8.dp)){
                Button(onClick={vm.refreshUsage()}){Text("Refresh usage")}
                OutlinedButton(onClick={vm::openUsageSettings}){Text("Usage Access")}
            }
        }
        SectionCard("Most-used apps"){
            if(vm.usageRows.isEmpty())Text("No Usage Access data yet. Grant access, then refresh.")
            vm.usageRows.take(15).forEach{Text(it.appName+" — "+it.minutes+" min",Modifier.padding(vertical=2.dp))}
        }
        SectionCard("Recreation rule"){
            Text("Movies, games and casual rest are allowed when deliberately planned. Aimless scrolling is different: it consumes attention and should be treated as an accountability signal.")
        }
    }
}

@Composable
fun FinanceScreen(vm:AppViewModel){
    var txType by remember{mutableStateOf("Expense")}
    var category by remember{mutableStateOf("Food")}
    var amount by remember{mutableStateOf("")}
    var description by remember{mutableStateOf("")}
    var planned by remember{mutableStateOf(true)}
    var accountId by remember{mutableStateOf(vm.state.finance.accounts.firstOrNull()?.id?:"")}
    var budgetName by remember{mutableStateOf("")}
    var budgetAmount by remember{mutableStateOf("")}
    var goalName by remember{mutableStateOf("")}
    var goalTarget by remember{mutableStateOf("")}
    var goalDate by remember{mutableStateOf("")}
    var goalFreq by remember{mutableStateOf("Weekly")}
    var message by remember{mutableStateOf("")}
    val scope=rememberCoroutineScope()
    val snackbar=remember{SnackbarHostState()}
    fun notify(text:String){
        message=text
        scope.launch{snackbar.showSnackbar(text)}
    }

    Scaffold(snackbarHost={SnackbarHost(snackbar)}){pad->
        Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)){
            SectionCard("Money at a glance"){
                Text("Total balance",style=MaterialTheme.typography.labelLarge)
                Text(ugx(vm.accountTotal()),style=MaterialTheme.typography.headlineSmall)
                Text("This month • income "+ugx(vm.monthIncome())+" • expenses "+ugx(vm.monthExpenses()))
                Text("Today • income "+ugx(vm.todayIncome())+" • expenses "+ugx(vm.todayExpenses()))
            }

            SectionCard("Accounts"){
                vm.state.finance.accounts.forEach{a->
                    var balanceText by remember(a.id,a.balance){mutableStateOf(a.balance.toString())}
                    OutlinedTextField(
                        balanceText,{balanceText=it},
                        Modifier.fillMaxWidth().padding(vertical=2.dp),
                        label={Text(a.name)},singleLine=true
                    )
                    OutlinedButton(onClick={
                        vm.updateAccount(a.id,balanceText.toDoubleOrNull()?:a.balance)
                        notify(a.name+" balance saved.")
                    }){Text("Save "+a.name)}
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(onClick={vm.addBalanceCheck("Morning","");notify("Morning balance check recorded.")}){Text("Morning check")}
                    OutlinedButton(onClick={vm.addBalanceCheck("Evening","");notify("Evening balance check recorded.")}){Text("Evening check")}
                }
            }

            SectionCard("Record a transaction"){
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    FilterChip(txType=="Expense",{txType="Expense"},label={Text("Expense")})
                    FilterChip(txType=="Income",{txType="Income"},label={Text("Income")})
                }
                OutlinedTextField(amount,{amount=it},Modifier.fillMaxWidth(),label={Text("Amount UGX")},singleLine=true)
                OutlinedTextField(category,{category=it},Modifier.fillMaxWidth(),label={Text("Category")},singleLine=true)
                OutlinedTextField(description,{description=it},Modifier.fillMaxWidth(),label={Text("Description / merchant")},singleLine=true)
                Text("Account charged / credited",Modifier.padding(top=6.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    vm.state.finance.accounts.forEach{a->
                        FilterChip(accountId==a.id,{accountId=a.id},label={Text(a.name)})
                    }
                }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Checkbox(planned,{planned=it})
                    Text("Planned")
                }
                Button(onClick={
                    val parsed=amount.toDoubleOrNull()?:0.0
                    if(parsed>0){
                        vm.addTransaction(txType,category,parsed,description,planned,accountId)
                        notify("Saved "+txType.lowercase()+" of "+ugx(parsed)+" to "+category+". Account balance updated.")
                        amount="";description=""
                    }else notify("Enter an amount greater than zero.")
                }){Text("Save transaction")}
                if(message.isNotBlank())Text(message,style=MaterialTheme.typography.bodySmall)
            }

            SectionCard("Recent transactions"){
                val txs=vm.recentTransactions()
                if(txs.isEmpty())Text("No transactions yet. Saved entries will appear here immediately.")
                txs.forEach{t->
                    val account=vm.state.finance.accounts.firstOrNull{it.id==t.accountId}?.name ?: "Account"
                    Text(
                        (if(t.type=="Expense")"− " else "+ ")+ugx(t.amount)+" • "+t.category,
                        style=MaterialTheme.typography.bodyLarge
                    )
                    Text(t.date+" "+t.time.take(5)+" • "+account+" • "+if(t.planned)"planned" else "unplanned")
                    if(t.description.isNotBlank())Text(t.description)
                    HorizontalDivider(Modifier.padding(vertical=6.dp))
                }
            }

            SectionCard("Monthly budgets"){
                OutlinedTextField(budgetName,{budgetName=it},Modifier.fillMaxWidth(),label={Text("Category")},singleLine=true)
                OutlinedTextField(budgetAmount,{budgetAmount=it},Modifier.fillMaxWidth(),label={Text("Limit UGX")},singleLine=true)
                Button(onClick={
                    vm.setBudget(budgetName,budgetAmount.toDoubleOrNull()?:0.0)
                    notify("Budget saved.")
                }){Text("Save budget")}
                vm.state.finance.budgets.filter{it.monthlyLimit>0}.forEach{b->
                    val spent=vm.spentThisMonth(b.name)
                    val progress=(spent/b.monthlyLimit).coerceIn(0.0,1.0).toFloat()
                    Text(b.name+"  •  "+ugx(spent)+" / "+ugx(b.monthlyLimit))
                    LinearProgressIndicator(progress={progress},Modifier.fillMaxWidth().padding(bottom=6.dp))
                }
            }

            SectionCard("Savings projects"){
                OutlinedTextField(goalName,{goalName=it},Modifier.fillMaxWidth(),label={Text("Goal name")},singleLine=true)
                OutlinedTextField(goalTarget,{goalTarget=it},Modifier.fillMaxWidth(),label={Text("Target UGX")},singleLine=true)
                OutlinedTextField(goalDate,{goalDate=it},Modifier.fillMaxWidth(),label={Text("Target date YYYY-MM-DD")},singleLine=true)
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    listOf("Daily","Weekly","Monthly").forEach{
                        FilterChip(goalFreq==it,{goalFreq=it},label={Text(it)})
                    }
                }
                Button(onClick={
                    vm.addSavingGoal(goalName,goalTarget.toDoubleOrNull()?:0.0,0.0,goalDate,goalFreq,"")
                    notify("Savings project created.")
                    goalName="";goalTarget="";goalDate=""
                }){Text("Create savings goal")}
                vm.state.finance.savingGoals.forEach{g->
                    val p=(g.currentAmount/g.targetAmount).coerceIn(0.0,1.0).toFloat()
                    Text(g.title+" — "+ugx(g.currentAmount)+" / "+ugx(g.targetAmount))
                    LinearProgressIndicator(progress={p},Modifier.fillMaxWidth().padding(bottom=4.dp))
                    vm.savingDiagnostic(g)?.let{Text(it)}
                }
            }
        }
    }
}

@Composable
fun GrowthScreen(vm:AppViewModel){
    val drum=vm.state.growth.drumming
    val coffee=vm.state.growth.coffee
    val exercise=vm.state.growth.exercise
    var technique by remember(drum.weekKey,drum.skill){mutableStateOf(drum.techniqueNote)}
    var groove by remember(drum.weekKey,drum.skill){mutableStateOf(drum.grooveApplication)}
    var fill by remember(drum.weekKey,drum.skill){mutableStateOf(drum.fillApplication)}
    var learningBpm by remember(drum.weekKey,drum.skill){mutableStateOf(drum.learningBpm.toString())}
    var secureBpm by remember(drum.weekKey,drum.skill){mutableStateOf(drum.secureBpm.toString())}
    var grooves by remember(drum.weekKey,drum.skill){mutableStateOf(drum.groovesCompleted.toString())}
    var fills by remember(drum.weekKey,drum.skill){mutableStateOf(drum.fillsCompleted.toString())}
    var skillSong by remember(drum.weekKey,drum.skill){mutableStateOf(drum.skillSong)}
    var songApplied by remember(drum.weekKey,drum.skill){mutableStateOf(drum.skillSongApplied)}
    var songResult by remember(drum.weekKey,drum.skill){mutableStateOf(drum.skillSongResult)}
    var learnTarget by remember(drum.weekKey,drum.skill){mutableStateOf(drum.learningTargetBpm.toString())}
    var secureMin by remember(drum.weekKey,drum.skill){mutableStateOf(drum.secureMinBpm.toString())}
    var secureTarget by remember(drum.weekKey,drum.skill){mutableStateOf(drum.secureTargetBpm.toString())}
    var grooveTarget by remember(drum.weekKey,drum.skill){mutableStateOf(drum.grooveTarget.toString())}
    var fillTarget by remember(drum.weekKey,drum.skill){mutableStateOf(drum.fillTarget.toString())}

    var learn by remember(coffee.lessonNumber){mutableStateOf(coffee.learnNotes)}
    var practice by remember(coffee.lessonNumber){mutableStateOf(coffee.practiceResult)}
    var application by remember(coffee.lessonNumber){mutableStateOf(coffee.applicationResult)}
    var exerciseFeedback by remember(exercise.weekKey){mutableStateOf(exercise.feedback)}
    var skillName by remember{mutableStateOf("")}
    var skillDomain by remember{mutableStateOf("")}
    var skillGoal by remember{mutableStateOf("")}
    var nextSkill by remember{mutableStateOf("")}
    var recommendation by remember{mutableStateOf(vm.state.growth.chatgptGrowthRecommendation)}
    var selectedCoffeeId by remember{mutableStateOf<String?>(null)}
    var selectedMasteryId by remember{mutableStateOf<String?>(null)}
    var selectedPracticeId by remember{mutableStateOf<String?>(null)}
    var practiceBpm by remember(drum.weekKey,drum.skill){mutableStateOf("")}
    var practiceFocus by remember(drum.weekKey,drum.skill){mutableStateOf("")}
    var practiceResult by remember(drum.weekKey,drum.skill){mutableStateOf("")}
    var practiceNext by remember(drum.weekKey,drum.skill){mutableStateOf("")}
    var masteryMessage by remember(drum.weekKey,drum.skill){mutableStateOf("")}
    val context=LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)){
        SectionCard("Morning exercise — beginner progression"){
            Text("Week "+exercise.weekNumber+" • "+exercise.phase,style=MaterialTheme.typography.titleMedium)
            Text("Today's session:")
            Text(vm.exerciseToday())
            Text("Sessions logged this week: "+exercise.completedSessions)
            OutlinedTextField(
                exerciseFeedback,{exerciseFeedback=it},
                Modifier.fillMaxWidth(),
                label={Text("How did today's session feel?")}
            )
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Button(onClick={vm.completeExerciseSession(exerciseFeedback)}){Text("Complete session")}
                OutlinedButton(onClick={vm.saveExerciseFeedback(exerciseFeedback)}){Text("Save feedback")}
            }
            Text("Next adjustment: "+exercise.nextAdjustment.ifBlank{"No adjustment yet."})
            Text("Recommendation: build gradually. The goal is consistency, stamina and whole-body strength; stomach size changes through overall body-composition changes, not spot reduction alone.")
        }

        SectionCard("Drumming — measurable skill mastery"){
            Text("Current skill: "+drum.skill,style=MaterialTheme.typography.titleMedium)
            Text("Mastery path: Learn → Secure → Groove → Fills → Song → Master")
            Text("Stage: "+vm.drummingProgressLabel())
            Text("Evidence complete: "+vm.drummingCriteriaProgress()+"/5")
            if(drum.skillCompleted){
                Card(Modifier.fillMaxWidth().padding(vertical=6.dp)){
                    Column(Modifier.padding(12.dp)){
                        Text("✓ SKILL MASTERED",style=MaterialTheme.typography.titleMedium)
                        Text(
                            drum.skill+" has been recorded as mastered.",
                            style=MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "Your evidence is preserved in the growth record. You can now move on without losing what you achieved."
                        )
                        if(vm.state.growth.nextDrummingSkill!=drum.skill){
                            Button(
                                onClick={vm.startNextDrummingSkill()},
                                modifier=Modifier.fillMaxWidth().padding(top=6.dp)
                            ){
                                Text("Start next skill: "+vm.state.growth.nextDrummingSkill)
                            }
                        }
                    }
                }
            }
            if(masteryMessage.isNotBlank() && !drum.skillCompleted){
                Text(masteryMessage)
            }

            Text("1. Learn — clean technique at target tempo",style=MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                OutlinedTextField(
                    learnTarget,{learnTarget=it},Modifier.weight(1f),
                    label={Text("Target BPM")},singleLine=true
                )
                OutlinedTextField(
                    learningBpm,{learningBpm=it},Modifier.weight(1f),
                    label={Text("Achieved BPM")},singleLine=true
                )
            }

            Text("2. Secure — clean performance across a controlled range",style=MaterialTheme.typography.titleSmall)
            Text("Suggested secure range: "+drum.secureMinBpm+"–"+drum.secureTargetBpm+" BPM")
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                OutlinedTextField(
                    secureMin,{secureMin=it},Modifier.weight(1f),
                    label={Text("Range start")},singleLine=true
                )
                OutlinedTextField(
                    secureTarget,{secureTarget=it},Modifier.weight(1f),
                    label={Text("Target BPM")},singleLine=true
                )
                OutlinedTextField(
                    secureBpm,{secureBpm=it},Modifier.weight(1f),
                    label={Text("Achieved")},singleLine=true
                )
            }

            Text("3. Groove — use the skill in multiple musical ideas",style=MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                OutlinedTextField(
                    grooveTarget,{grooveTarget=it},Modifier.weight(1f),
                    label={Text("Target grooves")},singleLine=true
                )
                OutlinedTextField(
                    grooves,{grooves=it},Modifier.weight(1f),
                    label={Text("Completed")},singleLine=true
                )
            }
            OutlinedTextField(
                groove,{groove=it},Modifier.fillMaxWidth(),
                label={Text("Groove ideas / what worked")}
            )

            Text("4. Fills — build different musical placements",style=MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                OutlinedTextField(
                    fillTarget,{fillTarget=it},Modifier.weight(1f),
                    label={Text("Target fills")},singleLine=true
                )
                OutlinedTextField(
                    fills,{fills=it},Modifier.weight(1f),
                    label={Text("Completed")},singleLine=true
                )
            }
            OutlinedTextField(
                fill,{fill=it},Modifier.fillMaxWidth(),
                label={Text("Fill evidence — end of bar, middle, one-beat, two-beat, etc.")}
            )

            Text("5. Song — apply the skill in real music",style=MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                skillSong,{skillSong=it},Modifier.fillMaxWidth(),
                label={Text("Song used for application")},singleLine=true
            )
            Row(verticalAlignment=Alignment.CenterVertically){
                Checkbox(songApplied,{songApplied=it})
                Text("I can apply this skill successfully in the selected song")
            }
            OutlinedTextField(
                songResult,{songResult=it},Modifier.fillMaxWidth(),
                label={Text("Song application result")}
            )

            Text("Technique notes",style=MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                technique,{technique=it},Modifier.fillMaxWidth(),
                label={Text("Sticking, rebound, accents, execution")}
            )

            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Button(onClick={
                    vm.setDrummingTargets(
                        learnTarget.toIntOrNull()?:drum.learningTargetBpm,
                        secureMin.toIntOrNull()?:drum.secureMinBpm,
                        secureTarget.toIntOrNull()?:drum.secureTargetBpm,
                        grooveTarget.toIntOrNull()?:drum.grooveTarget,
                        fillTarget.toIntOrNull()?:drum.fillTarget
                    )
                    vm.setDrummingCriteria(
                        learningBpm.toIntOrNull()?:0,
                        secureBpm.toIntOrNull()?:0,
                        grooves.toIntOrNull()?:0,
                        fills.toIntOrNull()?:0,
                        skillSong,
                        songApplied,
                        songResult
                    )
                    vm.setDrummingNotes(technique,groove,fill)
                }){Text("Save skill evidence")}
                OutlinedButton(
                    onClick={
                        vm.completeDrummingSkill()
                        if(vm.state.growth.drumming.skillCompleted){
                            masteryMessage="✓ Mastery recorded. "+vm.state.growth.drumming.skill+" is now complete."
                        }
                    },
                    enabled=vm.drummingCriteriaComplete() && !drum.skillCompleted
                ){Text(if(drum.skillCompleted)"Mastered ✓" else "Mark mastered")}
            }

            if(!vm.drummingCriteriaComplete() && !drum.skillCompleted){
                Text("Still needed:")
                vm.drummingMissingCriteria().forEach{Text("• "+it,Modifier.padding(vertical=2.dp))}
            }else if(!drum.skillCompleted){
                Text("✓ All five evidence criteria are complete. The skill is ready for mastery review.")
            }

            Text("Confidence: "+drum.confidence+"/5")
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                (1..5).forEach{i->FilterChip(drum.confidence==i,{vm.setDrummingConfidence(i)},label={Text(i.toString())})}
            }
            if(vm.state.growth.drummingHistory.isNotEmpty()){
                Text("Mastered skills: "+vm.state.growth.drummingHistory.joinToString(" → "))
            }
            Text("Next skill after mastery: "+vm.state.growth.nextDrummingSkill)
            if(drum.skillCompleted && vm.state.growth.nextDrummingSkill.isNotBlank()){
                Button(
                    onClick={vm::startNextDrummingSkill},
                    modifier=Modifier.fillMaxWidth()
                ){Text("Start "+vm.state.growth.nextDrummingSkill)}
            }
        }

        SectionCard("Drumming practice log"){
            Text("Record real practice sessions so progress is based on evidence, not memory.",style=MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                OutlinedTextField(
                    practiceBpm,{practiceBpm=it},Modifier.weight(1f),
                    label={Text("BPM")},singleLine=true
                )
                OutlinedTextField(
                    practiceFocus,{practiceFocus=it},Modifier.weight(1f),
                    label={Text("Focus")},singleLine=true
                )
            }
            OutlinedTextField(
                practiceResult,{practiceResult=it},Modifier.fillMaxWidth(),
                label={Text("What improved / what happened")}
            )
            OutlinedTextField(
                practiceNext,{practiceNext=it},Modifier.fillMaxWidth(),
                label={Text("Next adjustment")}
            )
            Button(onClick={
                vm.logDrummingPractice(
                    practiceBpm.toIntOrNull()?:0,
                    practiceFocus,
                    practiceResult,
                    practiceNext
                )
                practiceBpm=""
                practiceFocus=""
                practiceResult=""
                practiceNext=""
            }){Text("Save practice session")}

            if(vm.state.growth.drummingPracticeHistory.isNotEmpty()){
                Text("Recent practice",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=10.dp))
                vm.state.growth.drummingPracticeHistory.asReversed().take(5).forEach{record->
                    val selected=selectedPracticeId==record.id
                    OutlinedButton(
                        onClick={selectedPracticeId=if(selected)null else record.id},
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text(record.date+" • "+record.skill+" • "+record.bpm+" BPM")
                    }
                    if(selected){
                        if(record.focus.isNotBlank())Text("Focus: "+record.focus)
                        if(record.result.isNotBlank())Text("Result: "+record.result)
                        if(record.nextAdjustment.isNotBlank())Text("Next: "+record.nextAdjustment)
                    }
                }
            }else{
                Text("No practice sessions recorded yet.")
            }
        }

        SectionCard("Mastered skill history"){
            if(vm.state.growth.drummingMasteryHistory.isEmpty()){
                Text("No detailed mastery records yet. They will be saved when a skill is mastered.")
            }else{
                vm.state.growth.drummingMasteryHistory.asReversed().forEach{record->
                    val selected=selectedMasteryId==record.id
                    OutlinedButton(
                        onClick={selectedMasteryId=if(selected)null else record.id},
                        modifier=Modifier.fillMaxWidth()
                    ){
                        Text(record.skill+" • mastered "+record.date+" • confidence "+record.confidence+"/5")
                    }
                    if(selected){
                        Text("Learning: "+record.learningBpm+" BPM")
                        Text("Secure: "+record.secureBpm+" BPM")
                        Text("Grooves: "+record.groovesCompleted)
                        Text("Fills: "+record.fillsCompleted)
                        Text("Song: "+record.song.ifBlank{"Not recorded"})
                        if(record.songResult.isNotBlank())Text("Song result: "+record.songResult)
                    }
                }
            }
        }

        SectionCard("This week's worship song"){
            OutlinedTextField(drum.worshipSong,{vm.setWorshipSong(it)},Modifier.fillMaxWidth(),label={Text("Song title")},singleLine=true)
            Text("Goal: learn section by section and be able to play through by Sunday.")
            drum.worshipSections.forEach{section->
                FilterChip(
                    selected=drum.completedSections.contains(section),
                    onClick={vm.toggleWorshipSection(section)},
                    label={Text(section)}
                )
            }
            Text("Sections complete: "+drum.completedSections.size+"/"+drum.worshipSections.size)
            Button(
                onClick={vm::saveWorshipWeek},
                enabled=drum.worshipSong.isNotBlank(),
                modifier=Modifier.fillMaxWidth()
            ){Text("Save this worship week")}

            if(vm.state.growth.worshipHistory.isNotEmpty()){
                Text("Previous worship weeks",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=10.dp))
                vm.state.growth.worshipHistory.asReversed().take(6).forEach{record->
                    Text(
                        record.date+" • "+record.song+" • "+record.completedSections.size+"/"+drum.worshipSections.size+" sections",
                        Modifier.padding(vertical=2.dp)
                    )
                }
            }
        }

        SectionCard("Coffee Quality — assistant-led"){
            Text("Stage: "+coffee.currentStage)
            Text("Current lesson "+coffee.lessonNumber+"/"+coffee.roadmap.size+": "+coffee.currentLesson,style=MaterialTheme.typography.titleMedium)
            Text("Method: Learn → Practice → Apply → Review")
            Text("Status: "+coffee.lessonStatus)
            Row(horizontalArrangement=Arrangement.spacedBy(5.dp),modifier=Modifier.padding(vertical=5.dp)){
                listOf("Planned","Learning","Practicing","Applied","Reviewed").forEach{
                    FilterChip(coffee.lessonStatus==it,{vm.setCoffeeStatus(it)},label={Text(it)})
                }
            }
            OutlinedTextField(learn,{learn=it},Modifier.fillMaxWidth().padding(vertical=2.dp),label={Text("What I learned")})
            OutlinedTextField(practice,{practice=it},Modifier.fillMaxWidth().padding(vertical=2.dp),label={Text("Practice result")})
            OutlinedTextField(application,{application=it},Modifier.fillMaxWidth().padding(vertical=2.dp),label={Text("Coffee-station application")})
            Button(onClick={
                if(vm.saveCoffeeLesson(learn,practice,application)){
                    learn=""
                    practice=""
                    application=""
                }
            }){Text("Save lesson to history")}

            if(coffee.history.isNotEmpty()){
                Text("Saved lesson history",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=10.dp))
                coffee.history.asReversed().forEach{record->
                    val selected=selectedCoffeeId==record.id
                    OutlinedButton(onClick={
                        selectedCoffeeId=if(selected)null else record.id
                    },modifier=Modifier.fillMaxWidth()){
                        Text(
                            "Lesson "+record.lessonNumber+" • "+record.topic+
                                " • "+record.date
                        )
                    }
                    if(selected){
                        Text("Status: "+record.status)
                        if(record.learnNotes.isNotBlank())Text("What I learned: "+record.learnNotes)
                        if(record.practiceResult.isNotBlank())Text("Practice: "+record.practiceResult)
                        if(record.applicationResult.isNotBlank())Text("Station application: "+record.applicationResult)
                    }
                }
            }else{
                Text("No lessons saved yet. Lesson records will appear here after you actually study a topic.")
            }

            Text("Course roadmap",style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=8.dp))
            coffee.roadmap.forEachIndexed{index,item->
                Text(
                    item,
                    Modifier.padding(vertical=2.dp)
                )
            }
        }

        SectionCard("Personal skill roadmap"){
            Text("This is where a skill moves from interest → active practice → mastery → next skill.")
            vm.state.growth.otherSkills.forEach{skill->
                Text(skill.name+" • "+skill.domain+" • "+skill.progress+"% • "+skill.stage,style=MaterialTheme.typography.titleSmall)
                if(skill.goal.isNotBlank())Text("Goal: "+skill.goal)
                if(skill.nextSkill.isNotBlank())Text("Next: "+skill.nextSkill)
                if(skill.notes.isNotBlank())Text("Notes: "+skill.notes)
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    Button(onClick={vm.updateSkillProgress(skill.id,(skill.progress+25).coerceAtMost(100),if(skill.progress+25>=100)"Ready for review" else "Learning",skill.notes)}){Text("+25%")}
                    OutlinedButton(onClick={vm.completeSkill(skill.id)}){Text("Complete")}
                }
                HorizontalDivider(Modifier.padding(vertical=6.dp))
            }
            OutlinedTextField(skillName,{skillName=it},Modifier.fillMaxWidth(),label={Text("New skill")},singleLine=true)
            OutlinedTextField(skillDomain,{skillDomain=it},Modifier.fillMaxWidth(),label={Text("Domain / career area")},singleLine=true)
            OutlinedTextField(skillGoal,{skillGoal=it},Modifier.fillMaxWidth(),label={Text("Why this skill matters")})
            OutlinedTextField(nextSkill,{nextSkill=it},Modifier.fillMaxWidth(),label={Text("Possible next skill")})
            Button(onClick={
                vm.addSkill(skillName,skillDomain,skillGoal,nextSkill)
                skillName="";skillDomain="";skillGoal="";nextSkill=""
            }){Text("Add skill")}
        }

        SectionCard("ChatGPT growth coach"){
            Text("The app records your evidence. ChatGPT can use this brief to recommend what you should learn next based on your personal and career goals.")
            OutlinedTextField(
                recommendation,{recommendation=it},
                Modifier.fillMaxWidth(),
                label={Text("Latest ChatGPT recommendation")}
            )
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                Button(onClick={vm.setChatgptGrowthRecommendation(recommendation)}){Text("Save recommendation")}
                OutlinedButton(onClick={
                    context.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply{
                            type="text/plain"
                            putExtra(Intent.EXTRA_TEXT,vm.growthBrief())
                        },"Send growth brief"
                    ))
                }){Text("Send brief to ChatGPT")}
            }
        }
    }
}

@Composable
fun ReviewScreen(vm:AppViewModel){
    val context=LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)){
        SectionCard("Personal diagnostics"){vm.personalDiagnostics().forEach{Text("• "+it,Modifier.padding(vertical=3.dp))}}
        SectionCard("Finance diagnostics"){vm.financeDiagnostics().forEach{Text("• "+it,Modifier.padding(vertical=3.dp))}}
        SectionCard("Growth diagnostics"){
            Text("Exercise: "+vm.state.growth.exercise.completedSessions+" session(s) logged this week.")
            Text("Drumming: "+vm.state.growth.drumming.skill+" • "+vm.drummingProgressLabel()+" • evidence "+vm.drummingCriteriaProgress()+"/5 • confidence "+vm.state.growth.drumming.confidence+"/5")
            Text("Coffee: lesson "+vm.state.growth.coffee.lessonNumber+"/"+vm.state.growth.coffee.roadmap.size+" • "+vm.state.growth.coffee.lessonStatus+" • "+vm.state.growth.coffee.history.size+" saved lesson(s)")
            vm.state.growth.otherSkills.forEach{Text("• "+it.name+": "+it.progress+"%")}
        }
        SectionCard("Daily report"){
            Text(vm.dailyReport())
            Spacer(Modifier.height(8.dp))
            Button(onClick={
                context.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply{
                        type="text/plain"
                        putExtra(Intent.EXTRA_TEXT,vm.dailyReport())
                    },"Share daily report"
                ))
            }){Text("Share daily report")}
        }
        SectionCard("Growth brief"){
            Text(vm.growthBrief())
            Spacer(Modifier.height(8.dp))
            Button(onClick={
                context.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SEND).apply{
                        type="text/plain"
                        putExtra(Intent.EXTRA_TEXT,vm.growthBrief())
                    },"Share growth brief"
                ))
            }){Text("Send growth brief to ChatGPT")}
        }
        SectionCard("Core rule"){
            Text("When the schedule becomes difficult, reduce the routine rather than cancel it. Never miss twice deliberately.")
        }
    }
}
