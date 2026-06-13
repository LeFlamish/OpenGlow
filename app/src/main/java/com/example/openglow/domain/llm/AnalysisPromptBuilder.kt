package com.example.openglow.domain.llm

object AnalysisPromptBuilder {
    fun buildGeminiPrompt(input: NoteUpdateInput): String = """
        You are an AI that turns a user's mobile notifications into concise notes.
        Merge the existing final summary with the new notification text and return a new final summary.
        Do not append raw source text. Compress or remove older low-value information.
        Preserve deadlines, meetings, projects, assignments, requests, places, times, and names.
        Even if the new notification is short, summarize it and judge importance and work relevance.

        Input:
        Platform: ${input.platform}
        Package: ${input.packageName}
        Sender scope candidate: ${input.senderScope}
        Display name: ${input.senderDisplayName}
        Actual sender: ${input.senderName ?: "unknown"}
        Existing final summary:
        ${input.previousFinalSummary.orEmpty().ifBlank { "none" }}

        Existing retained facts:
        ${input.previousRetainedFacts.joinToString(separator = "\n").ifBlank { "none" }}

        New notification text:
        ${input.newNotificationText}

        Extraction confidence:
        ${input.completenessConfidence}

        Classifier hint:
        ${input.classificationHint?.let { hint ->
            """
            - Work-related score: ${hint.workRelatedScore}
            - Importance candidate: ${hint.importanceHint ?: "unknown"}
            - Meeting score: ${hint.meetingScore}
            - Project score: ${hint.projectScore}
            - Classifier confidence: ${hint.confidence}
            - Classifier model: ${hint.modelName}
            """.trimIndent()
        } ?: "none"}

        User personalization rules:
        ${input.userPersonalizationRules}

        Judgement rules:
        - importance URGENT: due today or soon, immediate action, urgent request, professor/lead/senior/responsible person's request, last-minute meeting change.
        - importance HIGH: important project, meeting, assignment, schedule, email, or work collaboration.
        - importance NORMAL: ordinary work, school, or schedule information.
        - importance LOW: ads, coupons, small talk, simple app notices, low-value information.
        - isWorkRelated true when about meetings, projects, assignments, work, team work, schedules, mail, reports, presentations, code, tests, deadlines, or place changes.
        - meetingDetected true when about meetings, Zoom, presentations, attendance, place, or time changes.
        - projectDetected true when about projects, team work, development, document changes, code, reports, tests, or task ownership.
        - senderScope INDIVIDUAL for one-to-one conversations, GROUP for group rooms, UNKNOWN when uncertain.

        Return only JSON with this exact shape:
        {
          "oneLineSummary": "...",
          "importance": "NORMAL",
          "isWorkRelated": true,
          "senderScope": "GROUP",
          "noteTitle": "...",
          "updatedFinalSummary": "...",
          "retainedFacts": ["..."],
          "actionItems": ["..."],
          "deadlineText": null,
          "meetingDetected": false,
          "projectDetected": false,
          "shouldAskFeedback": false,
          "confidence": 0.8
        }

        updatedFinalSummary rules:
        - It is the final text saved into the note.
        - Combine the existing summary and the new notification into the latest state.
        - Do not paste raw text verbatim.
        - Keep it under 1200 Korean characters or equivalent.
        - Preserve recent, urgent, and work-related information first.
        - Compress elapsed or seemingly completed schedules.
        - Ads or low-value chatter should leave little or no trace.

        retainedFacts rules:
        - Keep only 3 to 10 durable facts.
        - Good facts include deadline, meeting time, project name, owner, place, and action item.
    """.trimIndent()

    fun buildLocalPrompt(input: NoteUpdateInput): String = """
        You summarize a mobile notification into the user's note.
        Output ONLY one JSON object. No markdown, no code fences, no comments,
        and no text before or after it. Start with { and end with }.

        Platform: ${input.platform}
        Scope: ${input.senderScope}
        Display name: ${input.senderDisplayName}
        Previous summary: ${input.previousFinalSummary.orEmpty().ifBlank { "none" }}
        New text: ${input.newNotificationText}
        Classifier hint: ${input.classificationHint ?: "none"}

        Return exactly this JSON shape, keeping the keys and filling the values:
        {
          "oneLineSummary": "한 줄 요약",
          "importance": "NORMAL",
          "isWorkRelated": false,
          "senderScope": "INDIVIDUAL",
          "noteTitle": "제목",
          "updatedFinalSummary": "기존 요약과 새 알림을 합친 최신 요약",
          "retainedFacts": [],
          "actionItems": [],
          "deadlineText": null,
          "meetingDetected": false,
          "projectDetected": false,
          "shouldAskFeedback": false,
          "confidence": 0.8
        }

        Constraints:
        - importance is one of LOW, NORMAL, HIGH, URGENT.
        - senderScope is one of INDIVIDUAL, GROUP, UNKNOWN.
        - isWorkRelated, meetingDetected, projectDetected, shouldAskFeedback are true or false.
        - confidence is a number between 0 and 1; deadlineText is a string or null.
        - retainedFacts and actionItems are arrays of short strings (may be empty).
        - Write summaries in Korean. Do not paste the raw notification text verbatim.
    """.trimIndent()
}
