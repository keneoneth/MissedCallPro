package com.example.missedcallpro.data

object Defaults {

    private const val PH_COMPANY = "{{COMPANY}}"
    private const val PH_LINK = "{{FORM_LINK}}"
    const val SMS_TEMPLATE =
        "$PH_COMPANY: Thanks for calling. Fill in $PH_LINK. We'll get back to you soon. Reply STOP to opt out."

}