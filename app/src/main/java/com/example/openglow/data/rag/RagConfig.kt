package com.example.openglow.data.rag

object RagConfig {
    // Android emulator -> host PC localhost.
    const val EMULATOR_BASE_URL = "http://10.0.2.2:7860"

    // Real Android device -> PC on the same Wi-Fi. Replace this with the PC IPv4 address.
    const val DEVICE_BASE_URL = "http://172.16.6.101:7860"

    // Current target. Use EMULATOR_BASE_URL for emulator tests, DEVICE_BASE_URL for real devices.
    const val BASE_URL = DEVICE_BASE_URL

    // If Langflow API access shows an API key, paste it here for local testing.
    // Leave blank only when Langflow is started with auth disabled for development.
    const val API_KEY = "sk-dLUB2zQC93qkyqSqwXj5ywnmpUaoo-alSg5_INBYMMs"

    // Langflow's default Flow API is /api/v1/run/{FLOW_ID}. Replace these IDs with real Flow IDs.
    const val INGEST_FLOW_ID = "79928ccd-f021-4f2b-b563-bba5bf028801"
    const val QUERY_FLOW_ID = "a20bda83-9281-4afb-997b-e75c0e53919c"
    const val INGEST_PATH = "/api/v1/run/$INGEST_FLOW_ID"
    const val QUERY_PATH = "/api/v1/run/$QUERY_FLOW_ID"

    const val DEFAULT_USER_ID = "local_test_user"
    const val BATCH_SIZE = 50
    const val TOP_K = 5
}
