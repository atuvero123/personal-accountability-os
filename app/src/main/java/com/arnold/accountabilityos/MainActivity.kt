package com.arnold.accountabilityos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MainActivity:ComponentActivity(){
    private val notificationPermission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        if(android.os.Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        ReminderScheduler.scheduleAll(this)
        setContent{AccountabilityApp()}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountabilityApp(vm:AppViewModel=viewModel()){
    var tab by remember{mutableIntStateOf(0)}
    val titles=listOf("Today","Spiritual","Focus","Finance","Review")
    Scaffold(topBar={TopAppBar(title={Text("Accountability OS")},subtitle={Text(vm.todayKey()+" • "+titles[tab])})},bottomBar={
        NavigationBar{titles.forEachIndexed{i,t->NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(listOf("✓","✦","◉","₵","↻")[i])},label={Text(t)})}}
    }){pad->Box(Modifier.padding(pad).fillMaxSize()){when(tab){0->TodayScreen(vm);1->SpiritualScreen(vm);2->FocusScreen(vm);3->FinanceScreen(vm);4->ReviewScreen(vm)}}}}
}

@Composable
fun SectionCard(title:String,body:@Composable ColumnScope.()->Unit){
    Card(Modifier.fillMaxWidth().padding(vertical=5.dp)){Column(Modifier.padding(14.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Spacer(Modifier.height(8.dp));body()}}
}

@Composable
fun TodayScreen(vm:AppViewModel){
    val d=vm.today()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal=12.dp)){
        item{
            SectionCard("Daily accountability"){
                Text("Anchor score: "+vm.accountabilityScore()+"%")
                Text("Operating window: 06:30–23:00")
                Text("Principle: do it, schedule it, or deliberately reject it.")
                Spacer(Modifier.height(8.dp))
                d.big3.forEachIndexed{i,v->OutlinedTextField(v,{vm.setBig3(i,it)},Modifier.fillMaxWidth().padding(vertical=2.dp),label={Text("Big 3 #"+(i+1))},singleLine=true)}
            }
        }
        item{SectionCard("Today's prayer focus"){Text(vm.prayerTopic())}}
        items(d.tasks.sortedBy{it.start},key={it.id}){task->
            Card(Modifier.fillMaxWidth().padding(vertical=3.dp)){
                Column(Modifier.padding(10.dp)){
                    Text(task.start+"  •  "+task.name,style=MaterialTheme.typography.titleSmall)
                    Text(task.area+(if(task.anchor)"  • anchor" else "")+(if(task.postponedCount>0)"  • postponed "+task.postponedCount+"x" else ""))
                    if(task.status=="done")Text("Done at "+task.completedAt)
                    if(task.status=="missed")Text("Missed: "+task.reason)
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(top=6.dp)){
                        Button(onClick={vm.updateTaskStatus(task.id,"done")},enabled=task.status!="done"){Text("Done")}
                        OutlinedButton(onClick={vm.postponeTask(task.id,plus30(task.start))}){Text("+30m")}
                        OutlinedButton(onClick={if(task.status=="missed"){}else->{vm.updateTaskStatus(task.id,"missed","Not completed — review tonight.")}}){Text("Missed")}
                    }
                }
            }
        }
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
        SectionCard("Prayer focus"){Text(vm.prayerTopic());OutlinedTextField(d.prayerNote,vm::setPrayerNote,Modifier.fillMaxWidth(),label={Text("Prayer notes")})}
        SectionCard("Bible — target 5 chapters"){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){repeat(5){i->FilterChip(selected=d.bibleCompleted>i,onClick={vm.setBibleCompleted(if(d.bibleCompleted==i+1)i else i+1)},label={Text((i+1).toString())})}}
            Text("Progress: "+d.bibleCompleted+"/5")
        }
        SectionCard("Scripture sharing journal"){
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){FilterChip(source=="Me",{source="Me"},label={Text("My sharing")});FilterChip(source=="Friend",{source="Friend"},label={Text("Friend's sharing")})}
            OutlinedTextField(ref,{ref=it},Modifier.fillMaxWidth(),label={Text("Bible reference")})
            OutlinedTextField(lesson,{lesson=it},Modifier.fillMaxWidth(),label={Text("What I learned")})
            OutlinedTextField(application,{application=it},Modifier.fillMaxWidth(),label={Text("Personal application")})
            Button(onClick={vm.addScripture(source,ref,"",lesson,application);ref="";lesson="";application=""}){Text("Save reflection")}
            d.scriptureEntries.takeLast(5).reversed().forEach{Text(it.source+" • "+it.reference+" — "+it.lesson,Modifier.padding(top=8.dp))}
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
                OutlinedButton(onClick={vm.openUsageSettings}){Text("Usage Access")}
            }
        }
        SectionCard("Most-used apps"){
            if(vm.usageRows.isEmpty())Text("No Usage Access data yet. Grant access, then refresh.")
            vm.usageRows.take(15).forEach{Text(it.appName+" — "+it.minutes+" min",Modifier.padding(vertical=2.dp))}
        }
        SectionCard("Recreation rule"){Text("Movies, games and casual rest are allowed when deliberately planned. Aimless scrolling is different: it consumes attention and should be treated as an accountability signal.")}
    }
}

@Composable
fun FinanceScreen(vm:AppViewModel){
    var txType by remember{mutableStateOf("Expense")}
    var category by remember{mutableStateOf("Food")}
    var amount by remember{mutableStateOf("")}
    var description by remember{mutableStateOf("")}
    var planned by remember{mutableStateOf(true)}
    var budgetName by remember{mutableStateOf("")}
    var budgetAmount by remember{mutableStateOf("")}
    var goalName by remember{mutableStateOf("")}
    var goalTarget by remember{mutableStateOf("")}
    var goalDate by remember{mutableStateOf("")}
    var goalFreq by remember{mutableStateOf("Weekly")}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)){
        SectionCard("Balances"){
            vm.state.finance.accounts.forEach{a->OutlinedTextField(a.balance.toString(),{v->vm.updateAccount(a.id,v.toDoubleOrNull()?:0.0)},Modifier.fillMaxWidth().padding(vertical=2.dp),label={Text(a.name)},singleLine=true)}
            Text("Total: "+ugx(vm.accountTotal()))
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button(onClick={vm.addBalanceCheck("Morning","")}){Text("Morning check")};OutlinedButton(onClick={vm.addBalanceCheck("Evening","")}){Text("Evening check")}}
        }
        SectionCard("Transaction"){
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){FilterChip(txType=="Expense",{txType="Expense"},label={Text("Expense")});FilterChip(txType=="Income",{txType="Income"},label={Text("Income")})}
            OutlinedTextField(amount,{amount=it},Modifier.fillMaxWidth(),label={Text("Amount UGX")},singleLine=true)
            OutlinedTextField(category,{category=it},Modifier.fillMaxWidth(),label={Text("Category")},singleLine=true)
            OutlinedTextField(description,{description=it},Modifier.fillMaxWidth(),label={Text("Description")},singleLine=true)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Checkbox(planned,{planned=it});Text("Planned")}
            Button(onClick={vm.addTransaction(txType,category,amount.toDoubleOrNull()?:0.0,description,planned);amount="";description=""}){Text("Save transaction")}
        }
        SectionCard("Monthly budget"){
            OutlinedTextField(budgetName,{budgetName=it},Modifier.fillMaxWidth(),label={Text("Category")},singleLine=true)
            OutlinedTextField(budgetAmount,{budgetAmount=it},Modifier.fillMaxWidth(),label={Text("Limit UGX")},singleLine=true)
            Button(onClick={vm.setBudget(budgetName,budgetAmount.toDoubleOrNull()?:0.0)}){Text("Save budget")}
            vm.state.finance.budgets.filter{it.monthlyLimit>0}.forEach{Text(it.name+": "+ugx(vm.spentThisMonth(it.name))+" / "+ugx(it.monthlyLimit))}
        }
        SectionCard("Savings project"){
            OutlinedTextField(goalName,{goalName=it},Modifier.fillMaxWidth(),label={Text("Goal name")},singleLine=true)
            OutlinedTextField(goalTarget,{goalTarget=it},Modifier.fillMaxWidth(),label={Text("Target UGX")},singleLine=true)
            OutlinedTextField(goalDate,{goalDate=it},Modifier.fillMaxWidth(),label={Text("Target date YYYY-MM-DD")},singleLine=true)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("Daily","Weekly","Monthly").forEach{FilterChip(goalFreq==it,{goalFreq=it},label={Text(it)})}}
            Button(onClick={vm.addSavingGoal(goalName,goalTarget.toDoubleOrNull()?:0.0,0.0,goalDate,goalFreq,"");goalName="";goalTarget="";goalDate=""}){Text("Create savings goal")}
            vm.state.finance.savingGoals.forEach{g->Text(g.title+" — "+ugx(g.currentAmount)+" / "+ugx(g.targetAmount));vm.savingDiagnostic(g)?.let{Text(it)}}
        }
    }
}

@Composable
fun ReviewScreen(vm:AppViewModel){
    val context=LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)){
        SectionCard("Personal diagnostics"){vm.personalDiagnostics().forEach{Text("• "+it,Modifier.padding(vertical=3.dp))}}
        SectionCard("Finance diagnostics"){vm.financeDiagnostics().forEach{Text("• "+it,Modifier.padding(vertical=3.dp))}}
        SectionCard("Daily report"){
            Text(vm.dailyReport())
            Spacer(Modifier.height(8.dp))
            Button(onClick={
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,vm.dailyReport())},"Share daily report"))
            }){Text("Share to ChatGPT / another app")}
        }
        SectionCard("Core rule"){Text("When the schedule becomes difficult, reduce the routine rather than cancel it. Never miss twice deliberately.") }
    }
}
