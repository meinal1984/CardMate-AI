package com.example.sync

import android.util.Log
import com.example.data.model.BusinessCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Client for Google People API v1 (Google Contacts API)
 * Official endpoint: https://people.googleapis.com/v1/people:createContact
 * Scope: https://www.googleapis.com/auth/contacts
 */
object GoogleContactsApiClient {

    private const val TAG = "GoogleContactsApi"
    private const val BASE_URL = "https://people.googleapis.com/v1"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    data class GoogleContactResult(
        val isSuccess: Boolean,
        val resourceName: String? = null,
        val etag: String? = null,
        val webUrl: String? = null,
        val errorMessage: String? = null
    )

    data class GoogleContactPerson(
        val resourceName: String,
        val displayName: String,
        val email: String? = null,
        val phone: String? = null,
        val company: String? = null
    )

    data class GoogleBatchSyncReport(
        val totalCards: Int,
        val successfulCount: Int,
        val failedCount: Int,
        val syncedResourceNames: List<String> = emptyList()
    )

    /**
     * Builds the JSON body required by Google People API createContact endpoint.
     */
    fun buildPersonJson(card: BusinessCard): JSONObject {
        val person = JSONObject()

        // 1. Names
        if (card.fullName.isNotBlank()) {
            val namesArray = JSONArray()
            val nameObj = JSONObject()
            val parts = card.fullName.trim().split("\\s+".toRegex())
            if (parts.size > 1) {
                nameObj.put("givenName", parts.first())
                nameObj.put("familyName", parts.drop(1).joinToString(" "))
            } else {
                nameObj.put("givenName", card.fullName.trim())
            }
            nameObj.put("displayName", card.fullName.trim())
            namesArray.put(nameObj)
            person.put("names", namesArray)
        }

        // 2. Organizations
        if (card.company.isNotBlank() || card.jobTitle.isNotBlank()) {
            val orgsArray = JSONArray()
            val orgObj = JSONObject()
            if (card.company.isNotBlank()) orgObj.put("name", card.company.trim())
            if (card.jobTitle.isNotBlank()) orgObj.put("title", card.jobTitle.trim())
            orgObj.put("type", "work")
            orgsArray.put(orgObj)
            person.put("organizations", orgsArray)
        }

        // 3. Phone numbers
        val phonesArray = JSONArray()
        if (card.phone.isNotBlank()) {
            phonesArray.put(JSONObject().apply {
                put("value", card.phone.trim())
                put("type", "work")
            })
        }
        if (card.secondaryPhone.isNotBlank()) {
            phonesArray.put(JSONObject().apply {
                put("value", card.secondaryPhone.trim())
                put("type", "mobile")
            })
        }
        if (phonesArray.length() > 0) {
            person.put("phoneNumbers", phonesArray)
        }

        // 4. Email addresses
        if (card.email.isNotBlank()) {
            val emailsArray = JSONArray()
            emailsArray.put(JSONObject().apply {
                put("value", card.email.trim())
                put("type", "work")
            })
            person.put("emailAddresses", emailsArray)
        }

        // 5. Addresses
        if (card.address.isNotBlank()) {
            val addressesArray = JSONArray()
            addressesArray.put(JSONObject().apply {
                put("formattedValue", card.address.trim())
                put("type", "work")
            })
            person.put("addresses", addressesArray)
        }

        // 6. Websites / URLs
        if (card.website.isNotBlank()) {
            val urlsArray = JSONArray()
            urlsArray.put(JSONObject().apply {
                put("value", card.website.trim())
                put("type", "work")
            })
            person.put("urls", urlsArray)
        }

        // 7. Biographies / Notes
        val fullNote = buildString {
            if (card.notes.isNotBlank()) append(card.notes).append("\n\n")
            append("Category: ${card.category}\n")
            if (card.socialLinks.isNotBlank()) append("Links: ${card.socialLinks}\n")
            append("CardMate AI Digital Contact")
        }
        val bioArray = JSONArray()
        bioArray.put(JSONObject().apply {
            put("value", fullNote)
            put("contentType", "TEXT_PLAIN")
        })
        person.put("biographies", bioArray)

        // 8. User Defined attributes
        val userDefinedArray = JSONArray()
        userDefinedArray.put(JSONObject().apply {
            put("key", "CardMate Category")
            put("value", card.category)
        })
        userDefinedArray.put(JSONObject().apply {
            put("key", "CardMate Card ID")
            put("value", card.id.toString())
        })
        person.put("userDefined", userDefinedArray)

        return person
    }

    /**
     * Create contact directly on Google Contacts via Google People API v1.
     * POST https://people.googleapis.com/v1/people:createContact
     */
    suspend fun createContact(card: BusinessCard, accessToken: String): GoogleContactResult = withContext(Dispatchers.IO) {
        try {
            val jsonBody = buildPersonJson(card).toString()
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("$BASE_URL/people:createContact")
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val resourceName = jsonResponse.optString("resourceName")
                val etag = jsonResponse.optString("etag")
                val contactId = resourceName.substringAfter("people/", "")
                val webUrl = if (contactId.isNotBlank()) "https://contacts.google.com/person/$contactId" else "https://contacts.google.com"

                Log.i(TAG, "Successfully created Google Contact: $resourceName")
                GoogleContactResult(
                    isSuccess = true,
                    resourceName = resourceName,
                    etag = etag,
                    webUrl = webUrl
                )
            } else {
                val errorObj = try { JSONObject(responseBody).optJSONObject("error") } catch (e: Exception) { null }
                val errorMsg = errorObj?.optString("message") ?: "HTTP ${response.code}: ${response.message}"
                Log.w(TAG, "Google Contacts API error: $errorMsg (code ${response.code})")
                GoogleContactResult(
                    isSuccess = false,
                    errorMessage = errorMsg
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception creating contact via Google People API", e)
            GoogleContactResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Network connection failure"
            )
        }
    }

    /**
     * Fetch list of contacts from Google Contacts to verify sync
     * GET https://people.googleapis.com/v1/people/me/connections
     */
    suspend fun listContacts(accessToken: String, pageSize: Int = 50): Result<List<GoogleContactPerson>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/people/me/connections?personFields=names,emailAddresses,phoneNumbers,organizations&pageSize=$pageSize")
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseBody)
                val connectionsArray = jsonResponse.optJSONArray("connections") ?: JSONArray()
                val list = mutableListOf<GoogleContactPerson>()

                for (i in 0 until connectionsArray.length()) {
                    val personObj = connectionsArray.getJSONObject(i)
                    val resourceName = personObj.optString("resourceName")
                    
                    val namesArray = personObj.optJSONArray("names")
                    val displayName = namesArray?.optJSONObject(0)?.optString("displayName") ?: "Unknown"

                    val emailsArray = personObj.optJSONArray("emailAddresses")
                    val email = emailsArray?.optJSONObject(0)?.optString("value")

                    val phonesArray = personObj.optJSONArray("phoneNumbers")
                    val phone = phonesArray?.optJSONObject(0)?.optString("value")

                    val orgsArray = personObj.optJSONArray("organizations")
                    val company = orgsArray?.optJSONObject(0)?.optString("name")

                    list.add(GoogleContactPerson(resourceName, displayName, email, phone, company))
                }
                Result.success(list)
            } else {
                val errorObj = try { JSONObject(responseBody).optJSONObject("error") } catch (e: Exception) { null }
                val errorMsg = errorObj?.optString("message") ?: "HTTP ${response.code}"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Quickly verify if the Google OAuth token is valid.
     */
    suspend fun testToken(accessToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/people/me?personFields=names")
                .header("Authorization", "Bearer $accessToken")
                .header("Accept", "application/json")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
