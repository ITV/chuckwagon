package com.itv.chuckwagon.lambda

case class CronTriggeredEvent(
    version: String,
    id: String,
    `detail-type`: String,
    source: String,
    account: String,
    time: String,
    region: String,
    resources: List[String]
)
