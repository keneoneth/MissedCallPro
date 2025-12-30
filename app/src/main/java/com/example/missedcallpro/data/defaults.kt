package com.example.missedcallpro.data

object Defaults {

    private const val PH_COMPANY = "{{COMPANY}}"
    private const val PH_LINK = "{{FORM_LINK}}"
    const val SMS_TEMPLATE =
        "$PH_COMPANY: Thanks for calling — we missed you. We'll get back to you soon. $PH_LINK Reply STOP to opt out."

    const val EMAIL_TEMPLATE =
        "Subject: We missed your call\n\nHi,\n\nThanks for calling. We missed you and will respond shortly.\n\nBest regards,"

}