package com.example.missedcallpro.ui

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
            signedIn = false,
            plan = com.example.missedcallpro.data.PlanTier.FREE,
            quotas = com.example.missedcallpro.data.Quotas(0, 0),
            smsTemplate = com.example.missedcallpro.data.Defaults.FREE_SMS_TEMPLATE,
            emailTemplate = com.example.missedcallpro.data.Defaults.FREE_EMAIL_TEMPLATE
        )
    )

    val start = if (state.signedIn) Routes.PLAN else Routes.LANDING

    NavHost(navController = nav, startDestination = start) {

        composable(Routes.LANDING) {
            LandingScreen(
                onGoogleLogin = {
                    LandingActions.signIn(nav, store)
                }
            )
        }

        composable(Routes.PLAN) {
            PlanQuotaScreen(
                state = state,
                onOpenSmsTemplate = { nav.navigate(Routes.TEMPLATE_SMS) },
                onOpenEmailTemplate = { nav.navigate(Routes.TEMPLATE_EMAIL) },
                onUpgrade = { nav.navigate(Routes.PAYMENT) },
                onResetMonth = { PlanActions.resetMonth(store) },
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
                currentPlan = state.plan,
                onBack = { nav.popBackStack() },
                onSelectPlan = { selected ->
                    PaymentActions.setPlanThenBack(nav, store, selected)
                }
            )
        }
    }
}

/** Keep actions separate so screens stay dumb/simple */
private object LandingActions {
    @OptIn(DelicateCoroutinesApi::class)
    fun signIn(nav: NavHostController, store: AppStateStore) {
        kotlinx.coroutines.GlobalScope.launch {
            store.signIn()
        }
        nav.navigate(Routes.PLAN) { popUpTo(Routes.LANDING) { inclusive = true } }
    }
}

private object PlanActions {
    @OptIn(DelicateCoroutinesApi::class)
    fun resetMonth(store: AppStateStore) {
        kotlinx.coroutines.GlobalScope.launch { store.resetMonth() }
    }
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
