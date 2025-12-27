package com.example.missedcallpro.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.missedcallpro.data.AppStateStore
import com.example.missedcallpro.screens.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(DelicateCoroutinesApi::class)
@Composable
fun AppNavGraph(
    nav: NavHostController,
    store: AppStateStore
) {
    val state by store.state.collectAsState(
        initial = com.example.missedcallpro.data.AppState(
            username = "",
            email = "",
            signedIn = false,
            plan = com.example.missedcallpro.data.PlanTier("",0,0,false),
            quotas = com.example.missedcallpro.data.Quotas(0, 0),
            smsTemplate = com.example.missedcallpro.data.Defaults.SMS_TEMPLATE,
            emailTemplate = com.example.missedcallpro.data.Defaults.EMAIL_TEMPLATE
        )
    )

    val start = if (state.signedIn) Routes.PLAN else Routes.LANDING

    NavHost(navController = nav, startDestination = start) {

        composable(Routes.LANDING) {
            LandingScreen(
                onGoogleLogin =  {
                    username, email-> LandingActions.signIn(username, email, nav, store)
                }
            )
        }

        composable(Routes.PLAN) {
            PlanQuotaRoute(
                state = state,
                store = store,
                onOpenSmsTemplate = { nav.navigate(Routes.TEMPLATE_SMS) },
                onOpenEmailTemplate = { nav.navigate(Routes.TEMPLATE_EMAIL) },
                onUpgrade = { nav.navigate(Routes.PAYMENT) },
                onViewMyAccount = { nav.navigate(Routes.MY_ACCOUNT) },
                onSignOut = { PlanActions.signOut(nav, store) }
            )
        }

        composable(Routes.TEMPLATE_SMS) {
            TemplateScreen(
                title = "SMS Template",
                state = state,
                type = com.example.missedcallpro.data.TemplateType.SMS,
                onBack = { nav.popBackStack() },
                onEditBlocked = { nav.navigate(Routes.PAYMENT) },
                onSave = { newText -> TemplateActions.saveTemplate(store, com.example.missedcallpro.data.TemplateType.SMS, newText) }
            )
        }

        composable(Routes.TEMPLATE_EMAIL) {
            TemplateScreen(
                title = "Email Template",
                state = state,
                type = com.example.missedcallpro.data.TemplateType.EMAIL,
                onBack = { nav.popBackStack() },
                onEditBlocked = { nav.navigate(Routes.PAYMENT) },
                onSave = { newText -> TemplateActions.saveTemplate(store, com.example.missedcallpro.data.TemplateType.EMAIL, newText) }
            )
        }

        composable(Routes.PAYMENT) {
            PaymentScreen(
                state=state,
                currentPlan = state.plan,
                onBack = { nav.popBackStack() },
                onSelectPlan = { selected ->
                    PaymentActions.setPlanThenBack(nav, store, selected)
                }
            )
        }

        composable(Routes.MY_ACCOUNT) {
            AccountScreen(
                state = state,
                username = state.username,
                email = state.email,
                onBack = { nav.popBackStack() },
                onConfirmDelete = {AccountActions.deleteAccount(nav)}
            )
        }
    }
}

/** Keep actions separate so screens stay dumb/simple */
private object LandingActions {
    @OptIn(DelicateCoroutinesApi::class)
    fun signIn(username: String, email: String, nav: NavHostController, store: AppStateStore) {

        kotlinx.coroutines.GlobalScope.launch {
            store.setUsername(username)
            store.setEmail(email)
            store.signIn()
        }
        nav.navigate(Routes.PLAN) { popUpTo(Routes.LANDING) { inclusive = true } }
    }
}

private object PlanActions {
    @OptIn(DelicateCoroutinesApi::class)
    fun signOut(nav: NavHostController, store: AppStateStore) {
        kotlinx.coroutines.GlobalScope.launch { store.signOut() }
        nav.navigate(Routes.LANDING) { popUpTo(Routes.PLAN) { inclusive = true } }
    }
}

private object TemplateActions {
    @OptIn(DelicateCoroutinesApi::class)
    fun saveTemplate(store: AppStateStore, type: com.example.missedcallpro.data.TemplateType, text: String) {
        kotlinx.coroutines.GlobalScope.launch { store.setTemplate(type, text) }
    }
}

private object PaymentActions {
    @OptIn(DelicateCoroutinesApi::class)
    fun setPlanThenBack(nav: NavHostController, store: AppStateStore, plan: com.example.missedcallpro.data.PlanTier) {
        kotlinx.coroutines.GlobalScope.launch { store.setPlan(plan) }
        nav.popBackStack()
    }
}

private object AccountActions{
    @OptIn(DelicateCoroutinesApi::class)
    fun deleteAccount(nav: NavHostController) {
        nav.navigate(Routes.LANDING) { popUpTo(Routes.PLAN) { inclusive = true } }
    }
}