package com.example.missedcallpro.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.platform.LocalContext
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
            plan = com.example.missedcallpro.data.PlanState("",0,0,false),
            quotas = com.example.missedcallpro.data.Quotas(0, 0),
            smsTemplate = com.example.missedcallpro.data.Defaults.SMS_TEMPLATE,
            companyName = "",
            includeFormLinkInSms = true
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
                store = store,
                onOpenSmsTemplate = { nav.navigate(Routes.TEMPLATE_SMS) },
                onUpgrade = { nav.navigate(Routes.PAYMENT) },
                onViewMyAccount = { nav.navigate(Routes.MY_ACCOUNT) },
                onSignOut = { PlanActions.signOut(nav, store) },
                onOpenFilterList = { nav.navigate(Routes.FILTER_LIST) }
            )
        }

        composable(Routes.TEMPLATE_SMS) {
            TemplateScreen(
                title = "SMS Template",
                state = state,
                onBack = { nav.popBackStack() },
                onUpgrade = { nav.navigate(Routes.PAYMENT) },
                saveSmsSettings = {
                    company,template,includeLink -> TemplateActions.saveSmsSettings(
                    store,company, template,includeLink)
                }
            )
        }

        composable(Routes.PAYMENT) {

            val context = LocalContext.current

            PaymentScreen(
                currentPlan = state.plan,
                onBack = { nav.popBackStack() },
                onManageSubscription = {
                    PaymentActions.openManageSubscriptions(context)
                }
            )
        }

        composable(Routes.MY_ACCOUNT) {
            AccountScreen(
                username = state.username,
                email = state.email,
                onBack = { nav.popBackStack() },
                onConfirmDelete = {AccountActions.deleteAccount(nav)},
                onOpenFeedback = { nav.navigate(Routes.FEEDBACK) }
            )
        }

        composable(Routes.FILTER_LIST) {
            FilterListRoute(
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.FEEDBACK) {
            FeedbackScreen (
                onBack = { nav.popBackStack() }
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
    fun saveSmsSettings(store: AppStateStore, company: String, template: String, includeLink: Boolean) {
        kotlinx.coroutines.GlobalScope.launch {
            store.saveSmsSettings( company, template,includeLink)
        }
    }
}

private object PaymentActions {
    fun openManageSubscriptions(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/store/account/subscriptions")
            setPackage("com.android.vending")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                // Fallback: open in browser if Play Store app isn't available
                val web = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/account/subscriptions")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(web)
            }
    }
}

private object AccountActions{
    fun deleteAccount(nav: NavHostController) {
        nav.navigate(Routes.LANDING) { popUpTo(Routes.PLAN) { inclusive = true } }
    }
}